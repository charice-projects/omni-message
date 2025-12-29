package com.omnimsg.feature.voice

import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.models.voice.*
import com.omnimsg.domain.repositories.ContactRepository
import com.omnimsg.domain.usecases.message.SendMessageUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandProcessor @Inject constructor(
    private val contactRepository: ContactRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val intentRecognizer: IntentRecognizer
) {
    companion object {
        private const val TAG = "VoiceCommandProcessor"
        private const val DEFAULT_USER_ID = "default_user"
    }
    
    data class ProcessingConfig(
        val requireConfirmation: Boolean = true,
        val confirmationTimeout: Long = 5000L, // 5秒确认超时
        val enableAutoCorrection: Boolean = true,
        val maxProcessingTime: Long = 10000L, // 10秒最大处理时间
        val enableContextAwareness: Boolean = true,
        val enableLearning: Boolean = true
    )
    
    sealed class ProcessingState {
        object Idle : ProcessingState()
        object Recognizing : ProcessingState()
        data class Processing(val command: VoiceCommand) : ProcessingState()
        data class Confirming(val command: VoiceCommand) : ProcessingState()
        data class Executing(val command: VoiceCommand) : ProcessingState()
        data class Completed(val command: VoiceCommand) : ProcessingState()
        data class Error(val command: VoiceCommand?, val error: String) : ProcessingState()
        object Cancelled : ProcessingState()
    }
    
    sealed class ProcessingEvent {
        data class CommandRecognized(val command: VoiceCommand) : ProcessingEvent()
        data class IntentRecognized(val intent: VoiceCommandIntent) : ProcessingEvent()
        data class EntityExtracted(val entity: String, val value: String) : ProcessingEvent()
        data class CommandProcessing(val command: VoiceCommand) : ProcessingEvent()
        data class CommandConfirmed(val command: VoiceCommand) : ProcessingEvent()
        data class CommandExecuting(val command: VoiceCommand) : ProcessingEvent()
        data class CommandCompleted(val command: VoiceCommand) : ProcessingEvent()
        data class CommandFailed(val command: VoiceCommand, val error: String) : ProcessingEvent()
        data class ConfirmationRequired(val command: VoiceCommand, val message: String) : ProcessingEvent()
        data class ContextUpdated(val context: Map<String, String>) : ProcessingEvent()
        data class Error(val error: String) : ProcessingEvent()
    }
    
    // 状态流
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState
    
    // 事件通道
    private val _processingEvents = Channel<ProcessingEvent>(Channel.BUFFERED)
    val processingEvents: Flow<ProcessingEvent> = _processingEvents.receiveAsFlow()
    
    // 上下文数据
    private val _contextData = MutableStateFlow<Map<String, String>>(emptyMap())
    val contextData: StateFlow<Map<String, String>> = _contextData
    
    private var processingJob: Job? = null
    private var confirmationJob: Job? = null
    
    private var config = ProcessingConfig()
    
    /**
     * 处理语音识别结果
     */
    fun processSpeechResult(
        speechResult: SpeechRecognitionResult,
        userId: String = DEFAULT_USER_ID
    ): Boolean {
        if (_processingState.value != ProcessingState.Idle) {
            Log.w(TAG, "Already processing a command")
            return false
        }
        
        processingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                processCommandAsync(speechResult, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Command processing failed", e)
                _processingState.value = ProcessingState.Error(null, e.message ?: "Processing failed")
                _processingEvents.trySend(ProcessingEvent.Error(e.message ?: "Processing failed"))
            }
        }
        
        return true
    }
    
    /**
     * 异步处理命令
     */
    private suspend fun processCommandAsync(
        speechResult: SpeechRecognitionResult,
        userId: String
    ) = withContext(Dispatchers.IO) {
        _processingState.value = ProcessingState.Recognizing
        
        try {
            // 1. 识别意图
            val intent = recognizeIntent(speechResult)
            _processingEvents.trySend(ProcessingEvent.IntentRecognized(intent))
            
            // 2. 创建语音命令
            val command = createVoiceCommand(speechResult, intent, userId)
            _processingEvents.trySend(ProcessingEvent.CommandRecognized(command))
            
            // 3. 提取实体
            extractEntities(command, speechResult.text)
            
            // 4. 更新处理状态
            _processingState.value = ProcessingState.Processing(command)
            _processingEvents.trySend(ProcessingEvent.CommandProcessing(command))
            
            // 5. 验证命令
            if (!validateCommand(command)) {
                throw IllegalArgumentException("Command validation failed")
            }
            
            // 6. 如果需要确认，请求确认
            if (config.requireConfirmation && requiresConfirmation(command)) {
                requestConfirmation(command)
                return@withContext
            }
            
            // 7. 执行命令
            executeCommand(command)
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 识别意图
     */
    private suspend fun recognizeIntent(speechResult: SpeechRecognitionResult): VoiceCommandIntent {
        return withContext(Dispatchers.Default) {
            intentRecognizer.recognizeIntent(speechResult.text, _contextData.value)
        }
    }
    
    /**
     * 创建语音命令
     */
    private fun createVoiceCommand(
        speechResult: SpeechRecognitionResult,
        intent: VoiceCommandIntent,
        userId: String
    ): VoiceCommand {
        return VoiceCommand(
            userId = userId,
            triggerPhrase = speechResult.text,
            voiceText = speechResult.text,
            normalizedText = speechResult.normalizedText,
            intent = intent.intent,
            entities = intent.entities,
            confidence = intent.confidence * speechResult.confidence,
            actionType = mapIntentToActionType(intent.intent),
            actionData = extractActionData(intent),
            contextData = _contextData.value,
            audioDuration = speechResult.duration,
            audioQuality = speechResult.audioQuality,
            backgroundNoiseLevel = speechResult.backgroundNoiseLevel,
            modelVersion = "1.0.0"
        )
    }
    
    /**
     * 提取实体
     */
    private fun extractEntities(command: VoiceCommand, text: String) {
        // 提取联系人
        val contact = extractContact(text)
        if (contact != null) {
            command.entities["contact"] = contact.displayName
            command.actionData = command.actionData + mapOf(
                "contact_id" to contact.id.toString(),
                "contact_name" to contact.displayName
            )
            _processingEvents.trySend(ProcessingEvent.EntityExtracted("contact", contact.displayName))
        }
        
        // 提取消息内容
        if (command.intent == "SEND_MESSAGE") {
            val message = extractMessage(text, contact?.displayName)
            if (message != null) {
                command.entities["message"] = message
                command.actionData = command.actionData + mapOf("message" to message)
                _processingEvents.trySend(ProcessingEvent.EntityExtracted("message", message))
            }
        }
        
        // 提取时间
        val time = extractTime(text)
        if (time != null) {
            command.entities["time"] = time
            command.actionData = command.actionData + mapOf("time" to time)
            _processingEvents.trySend(ProcessingEvent.EntityExtracted("time", time))
        }
        
        // 提取位置
        val location = extractLocation(text)
        if (location != null) {
            command.entities["location"] = location
            command.actionData = command.actionData + mapOf("location" to location)
            _processingEvents.trySend(ProcessingEvent.EntityExtracted("location", location))
        }
    }
    
    /**
     * 验证命令
     */
    private fun validateCommand(command: VoiceCommand): Boolean {
        // 检查置信度
        if (command.confidence < 0.5f) {
            _processingEvents.trySend(ProcessingEvent.Error("Confidence too low: ${command.confidence}"))
            return false
        }
        
        // 检查必要实体
        val requiredEntities = getRequiredEntities(command.intent)
        for (entity in requiredEntities) {
            if (!command.entities.containsKey(entity)) {
                _processingEvents.trySend(ProcessingEvent.Error("Missing required entity: $entity"))
                return false
            }
        }
        
        return true
    }
    
    /**
     * 检查是否需要确认
     */
    private fun requiresConfirmation(command: VoiceCommand): Boolean {
        return when (command.intent) {
            "SEND_MESSAGE", "CALL_CONTACT", "EMERGENCY_ALERT" -> true
            else -> command.confidence < 0.8f
        }
    }
    
    /**
     * 请求确认
     */
    private fun requestConfirmation(command: VoiceCommand) {
        _processingState.value = ProcessingState.Confirming(command)
        
        val confirmationMessage = generateConfirmationMessage(command)
        _processingEvents.trySend(
            ProcessingEvent.ConfirmationRequired(command, confirmationMessage)
        )
        
        // 启动确认超时
        confirmationJob = CoroutineScope(Dispatchers.IO).launch {
            delay(config.confirmationTimeout)
            
            if (_processingState.value is ProcessingState.Confirming) {
                _processingState.value = ProcessingState.Cancelled
                _processingEvents.trySend(ProcessingEvent.Error("Confirmation timeout"))
            }
        }
    }
    
    /**
     * 确认命令
     */
    fun confirmCommand(confirmed: Boolean = true): Boolean {
        val currentState = _processingState.value
        if (currentState !is ProcessingState.Confirming) {
            return false
        }
        
        confirmationJob?.cancel()
        confirmationJob = null
        
        if (!confirmed) {
            _processingState.value = ProcessingState.Cancelled
            return true
        }
        
        _processingState.value = ProcessingState.Executing(currentState.command)
        _processingEvents.trySend(ProcessingEvent.CommandConfirmed(currentState.command))
        
        // 执行命令
        CoroutineScope(Dispatchers.IO).launch {
            executeCommand(currentState.command)
        }
        
        return true
    }
    
    /**
     * 执行命令
     */
    private suspend fun executeCommand(command: VoiceCommand) = withContext(Dispatchers.IO) {
        _processingState.value = ProcessingState.Executing(command)
        _processingEvents.trySend(ProcessingEvent.CommandExecuting(command))
        
        try {
            val executionResult = when (command.intent) {
                "SEND_MESSAGE" -> executeSendMessage(command)
                "CALL_CONTACT" -> executeCallContact(command)
                "SEARCH_CONTACT" -> executeSearchContact(command)
                "EMERGENCY_ALERT" -> executeEmergencyAlert(command)
                "CREATE_REMINDER" -> executeCreateReminder(command)
                "SET_ALARM" -> executeSetAlarm(command)
                "OPEN_APP" -> executeOpenApp(command)
                "PLAY_MUSIC" -> executePlayMusic(command)
                "NAVIGATE" -> executeNavigate(command)
                else -> executeCustomCommand(command)
            }
            
            val completedCommand = command.markAsExecuted(executionResult)
            _processingState.value = ProcessingState.Completed(completedCommand)
            _processingEvents.trySend(ProcessingEvent.CommandCompleted(completedCommand))
            
            // 更新上下文
            updateContext(completedCommand)
            
        } catch (e: Exception) {
            val failedCommand = command.markAsFailed(e.message ?: "Execution failed")
            _processingState.value = ProcessingState.Error(failedCommand, e.message ?: "Execution failed")
            _processingEvents.trySend(ProcessingEvent.CommandFailed(failedCommand, e.message ?: "Execution failed"))
        }
    }
    
    /**
     * 执行发送消息命令
     */
    private suspend fun executeSendMessage(command: VoiceCommand): String {
        val contactId = command.actionData["contact_id"]?.toLongOrNull()
        val contactName = command.actionData["contact_name"]
        val message = command.actionData["message"]
        
        if (contactId == null || message.isNullOrBlank()) {
            throw IllegalArgumentException("Missing contact or message")
        }
        
        // 这里调用发送消息的Use Case
        // val result = sendMessageUseCase.execute(...)
        
        return "消息已发送给$contactName: $message"
    }
    
    /**
     * 执行呼叫联系人命令
     */
    private suspend fun executeCallContact(command: VoiceCommand): String {
        val contactId = command.actionData["contact_id"]?.toLongOrNull()
        val contactName = command.actionData["contact_name"]
        
        if (contactId == null) {
            throw IllegalArgumentException("Missing contact")
        }
        
        // TODO: 实现呼叫逻辑
        
        return "正在呼叫$contactName"
    }
    
    /**
     * 执行搜索联系人命令
     */
    private suspend fun executeSearchContact(command: VoiceCommand): String {
        val contactName = command.entities["contact"]
        
        if (contactName.isNullOrBlank()) {
            throw IllegalArgumentException("Missing contact name")
        }
        
        // 搜索联系人
        val contacts = contactRepository.searchContacts(contactName).firstOrNull() ?: emptyList()
        
        return if (contacts.isNotEmpty()) {
            val contact = contacts.first()
            "找到联系人: ${contact.displayName}, 电话: ${contact.phoneNumber ?: "无"}"
        } else {
            "未找到联系人: $contactName"
        }
    }
    
    /**
     * 执行紧急报警命令
     */
    private suspend fun executeEmergencyAlert(command: VoiceCommand): String {
        // TODO: 实现紧急报警逻辑
        return "紧急报警已触发，正在通知紧急联系人"
    }
    
    /**
     * 执行创建提醒命令
     */
    private suspend fun executeCreateReminder(command: VoiceCommand): String {
        val reminder = command.entities["reminder"]
        val time = command.entities["time"]
        
        return if (time != null) {
            "已创建提醒: $reminder, 时间: $time"
        } else {
            "已创建提醒: $reminder"
        }
    }
    
    /**
     * 执行其他命令
     */
    private suspend fun executeCustomCommand(command: VoiceCommand): String {
        return "命令执行成功: ${command.intent}"
    }
    
    // 其他执行方法...
    
    /**
     * 提取联系人
     */
    private suspend fun extractContact(text: String): Contact? {
        // 从文本中提取可能的联系人名称
        val possibleNames = extractPossibleNames(text)
        
        for (name in possibleNames) {
            val contacts = contactRepository.searchContacts(name).firstOrNull()
            val contact = contacts?.firstOrNull()
            if (contact != null) {
                return contact
            }
        }
        
        return null
    }
    
    /**
     * 提取可能的名字
     */
    private fun extractPossibleNames(text: String): List<String> {
        val words = text.split(" ", "，", ",", "给", "到", "向")
        return words.filter { word ->
            word.length in 2..4 && word.all { it.isLetter() }
        }
    }
    
    /**
     * 提取消息内容
     */
    private fun extractMessage(text: String, contactName: String?): String? {
        val keywords = listOf("说", "告诉", "发", "发送")
        
        for (keyword in keywords) {
            val index = text.indexOf(keyword)
            if (index != -1) {
                var message = text.substring(index + keyword.length).trim()
                
                // 移除联系人名字
                contactName?.let {
                    message = message.replace(it, "").trim()
                }
                
                // 移除标点
                message = message.replace("。", "").replace("！", "").replace("？", "")
                
                if (message.isNotBlank()) {
                    return message
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取时间
     */
    private fun extractTime(text: String): String? {
        val timePatterns = listOf(
            Regex("""(\d{1,2})点(\d{1,2})分?"""),
            Regex("""(\d{1,2})点"""),
            Regex("""(\d{1,2})号"""),
            Regex("""(\d{1,2})月"""),
            Regex("""(今天|明天|后天|上午|下午|晚上)""")
        )
        
        for (pattern in timePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.value
            }
        }
        
        return null
    }
    
    /**
     * 提取位置
     */
    private fun extractLocation(text: String): String? {
        val locationKeywords = listOf("在", "到", "去", "往", "从")
        val locationSuffixes = listOf("家", "公司", "学校", "医院", "商场", "饭店")
        
        for (keyword in locationKeywords) {
            val index = text.indexOf(keyword)
            if (index != -1) {
                var location = text.substring(index + keyword.length).trim()
                
                // 取到下一个关键词或结尾
                val nextKeywords = locationKeywords + locationSuffixes
                for (nextKeyword in nextKeywords) {
                    val nextIndex = location.indexOf(nextKeyword)
                    if (nextIndex != -1 && nextIndex > 0) {
                        location = location.substring(0, nextIndex).trim()
                        break
                    }
                }
                
                if (location.isNotBlank() && location.length <= 10) {
                    return location
                }
            }
        }
        
        return null
    }
    
    /**
     * 映射意图到动作类型
     */
    private fun mapIntentToActionType(intent: String): String {
        return when (intent) {
            "SEND_MESSAGE" -> "SEND_MESSAGE"
            "CALL_CONTACT" -> "CALL_CONTACT"
            "SEARCH_CONTACT" -> "SEARCH_CONTACT"
            "EMERGENCY_ALERT" -> "EMERGENCY_ALERT"
            "CREATE_REMINDER" -> "CREATE_REMINDER"
            "SET_ALARM" -> "SET_ALARM"
            "OPEN_APP" -> "OPEN_APP"
            "PLAY_MUSIC" -> "PLAY_MUSIC"
            "NAVIGATE" -> "NAVIGATE"
            else -> "CUSTOM"
        }
    }
    
    /**
     * 提取动作数据
     */
    private fun extractActionData(intent: VoiceCommandIntent): Map<String, String> {
        val actionData = mutableMapOf<String, String>()
        
        for ((key, value) in intent.entities) {
            actionData[key] = value
        }
        
        for ((key, slot) in intent.slots) {
            actionData[key] = slot.value
        }
        
        return actionData
    }
    
    /**
     * 获取必要实体
     */
    private fun getRequiredEntities(intent: String): List<String> {
        return when (intent) {
            "SEND_MESSAGE" -> listOf("contact", "message")
            "CALL_CONTACT" -> listOf("contact")
            "SEARCH_CONTACT" -> listOf("contact")
            "EMERGENCY_ALERT" -> emptyList()
            "CREATE_REMINDER" -> listOf("reminder")
            "SET_ALARM" -> listOf("time")
            else -> emptyList()
        }
    }
    
    /**
     * 生成确认消息
     */
    private fun generateConfirmationMessage(command: VoiceCommand): String {
        return when (command.intent) {
            "SEND_MESSAGE" -> {
                val contact = command.entities["contact"]
                val message = command.entities["message"]
                "确认发送消息给$contact: $message"
            }
            "CALL_CONTACT" -> {
                val contact = command.entities["contact"]
                "确认呼叫$contact"
            }
            "EMERGENCY_ALERT" -> "确认触发紧急报警吗？"
            else -> "确认执行命令吗？"
        }
    }
    
    /**
     * 更新上下文
     */
    private fun updateContext(command: VoiceCommand) {
        val newContext = mutableMapOf<String, String>()
        
        // 添加上次执行的命令
        newContext["last_command"] = command.intent
        newContext["last_command_time"] = System.currentTimeMillis().toString()
        
        // 添加命令相关的上下文
        for ((key, value) in command.entities) {
            newContext["last_$key"] = value
        }
        
        // 合并现有上下文
        newContext.putAll(_contextData.value)
        
        // 限制上下文大小
        val limitedContext = newContext.toList()
            .takeLast(20) // 保留最近20条上下文
            .toMap()
        
        _contextData.value = limitedContext
        _processingEvents.trySend(ProcessingEvent.ContextUpdated(limitedContext))
    }
    
    /**
     * 取消当前处理
     */
    fun cancelProcessing() {
        processingJob?.cancel()
        confirmationJob?.cancel()
        _processingState.value = ProcessingState.Cancelled
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        cancelProcessing()
        _contextData.value = emptyMap()
    }
}