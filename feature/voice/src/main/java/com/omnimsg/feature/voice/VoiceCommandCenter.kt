// 📁 feature/voice/VoiceCommandCenter.kt
package com.omnimsg.feature.voice

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandCenter @Inject constructor(
    private val context: Context,
    private val voiceRecognition: VoiceRecognition,
    private val voiceDialerPro: VoiceDialerPro,
    private val voiceFeedbackSystem: VoiceFeedbackSystem,
    private val privacyAwareVoiceProcessor: PrivacyAwareVoiceProcessor
) {
    
    sealed class CommandType {
        object SendMessage : CommandType()
        object MakeCall : CommandType()
        object SearchContact : CommandType()
        object EmergencyAlert : CommandType()
        object OpenApp : CommandType()
        object ControlSettings : CommandType()
        object CustomAction : CommandType()
    }
    
    data class VoiceCommand(
        val id: String,
        val type: CommandType,
        val triggerPhrases: List<String>,
        val action: suspend (VoiceContext) -> CommandResult,
        val description: String,
        val requiresConfirmation: Boolean = false,
        val priority: Int = 5 // 1-10, 10最高
    )
    
    data class VoiceContext(
        val userId: String,
        val timestamp: Long,
        val location: String? = null,
        val deviceState: DeviceState,
        val rawText: String,
        val confidence: Float
    )
    
    data class CommandResult(
        val isSuccess: Boolean,
        val message: String,
        val data: Any? = null,
        val shouldGiveFeedback: Boolean = true
    )
    
    data class DeviceState(
        val batteryLevel: Int,
        val networkConnected: Boolean,
        val screenOn: Boolean,
        val inCall: Boolean,
        val drivingMode: Boolean = false
    )
    
    private val commands = mutableMapOf<String, VoiceCommand>()
    private val commandHistory = mutableListOf<CommandExecution>()
    
    /**
     * 初始化语音命令中心
     */
    suspend fun initialize(): Boolean {
        return try {
            // 注册内置命令
            registerBuiltInCommands()
            
            // 加载用户自定义命令
            loadUserCommands()
            
            true
        } catch (e: Exception) {
            logger.e("VoiceCommandCenter", "初始化失败", e)
            false
        }
    }
    
    /**
     * 注册内置命令
     */
    private fun registerBuiltInCommands() {
        // 发送消息命令
        registerCommand(
            VoiceCommand(
                id = "send_message",
                type = CommandType.SendMessage,
                triggerPhrases = listOf(
                    "发消息给",
                    "发送消息",
                    "给{contact}发消息",
                    "告诉{contact}",
                    "消息{contact}"
                ),
                action = { context ->
                    executeSendMessage(context)
                },
                description = "发送消息给指定联系人",
                requiresConfirmation = true
            )
        )
        
        // 打电话命令
        registerCommand(
            VoiceCommand(
                id = "make_call",
                type = CommandType.MakeCall,
                triggerPhrases = listOf(
                    "打电话给",
                    "呼叫",
                    "拨打",
                    "给{contact}打电话"
                ),
                action = { context ->
                    executeMakeCall(context)
                },
                description = "打电话给指定联系人",
                requiresConfirmation = true,
                priority = 8
            )
        )
        
        // 紧急报警命令
        registerCommand(
            VoiceCommand(
                id = "emergency_alert",
                type = CommandType.EmergencyAlert,
                triggerPhrases = listOf(
                    "紧急求助",
                    "救命",
                    "SOS",
                    "紧急报警",
                    "帮我报警"
                ),
                action = { context ->
                    executeEmergencyAlert(context)
                },
                description = "触发紧急报警",
                requiresConfirmation = false,
                priority = 10
            )
        )
        
        // 搜索联系人命令
        registerCommand(
            VoiceCommand(
                id = "search_contact",
                type = CommandType.SearchContact,
                triggerPhrases = listOf(
                    "查找联系人",
                    "搜索",
                    "找一下{contact}",
                    "谁的电话是"
                ),
                action = { context ->
                    executeSearchContact(context)
                },
                description = "搜索联系人信息",
                requiresConfirmation = false
            )
        )
        
        // 打开应用命令
        registerCommand(
            VoiceCommand(
                id = "open_app",
                type = CommandType.OpenApp,
                triggerPhrases = listOf(
                    "打开应用",
                    "启动",
                    "进入",
                    "打开设置",
                    "打开联系人"
                ),
                action = { context ->
                    executeOpenApp(context)
                },
                description = "打开指定应用或功能",
                requiresConfirmation = false
            )
        )
    }
    
    /**
     * 注册命令
     */
    fun registerCommand(command: VoiceCommand) {
        commands[command.id] = command
        logger.i("VoiceCommandCenter", "注册命令: ${command.id} - ${command.description}")
    }
    
    /**
     * 取消注册命令
     */
    fun unregisterCommand(commandId: String) {
        commands.remove(commandId)
        logger.i("VoiceCommandCenter", "取消注册命令: $commandId")
    }
    
    /**
     * 处理语音输入
     */
    suspend fun processVoiceInput(
        text: String,
        confidence: Float = 0.8f,
        context: VoiceContext
    ): ProcessResult {
        return withContext(Dispatchers.Default) {
            try {
                // 1. 预处理文本
                val processedText = preprocessText(text)
                
                // 2. 隐私处理
                val privacyResult = privacyAwareVoiceProcessor.process(processedText)
                if (!privacyResult.isAllowed) {
                    return@withContext ProcessResult.Blocked(
                        reason = privacyResult.rejectionReason
                    )
                }
                
                // 3. 意图识别
                val intent = recognizeIntent(processedText, confidence)
                
                // 4. 实体提取
                val entities = extractEntities(processedText, intent)
                
                // 5. 命令匹配
                val matchedCommand = matchCommand(processedText, intent, entities)
                
                if (matchedCommand != null) {
                    // 6. 执行命令
                    val executionContext = context.copy(
                        rawText = processedText
                    )
                    
                    val result = executeCommand(matchedCommand, executionContext, entities)
                    
                    // 7. 记录执行历史
                    recordExecution(
                        CommandExecution(
                            commandId = matchedCommand.id,
                            inputText = text,
                            processedText = processedText,
                            intent = intent,
                            entities = entities,
                            result = result,
                            timestamp = System.currentTimeMillis(),
                            context = context
                        )
                    )
                    
                    // 8. 学习优化
                    learnFromExecution(matchedCommand.id, result, processedText)
                    
                    return@withContext ProcessResult.Executed(
                        command = matchedCommand,
                        result = result,
                        entities = entities
                    )
                } else {
                    // 未匹配到命令
                    return@withContext ProcessResult.NoMatch(
                        suggestedCommands = suggestCommands(processedText)
                    )
                }
                
            } catch (e: Exception) {
                logger.e("VoiceCommandCenter", "处理语音输入失败", e)
                return@withContext ProcessResult.Error(
                    error = "处理失败: ${e.message}",
                    shouldRetry = true
                )
            }
        }
    }
    
    /**
     * 预处理文本
     */
    private fun preprocessText(text: String): String {
        var processed = text.trim()
        
        // 移除多余空格
        processed = processed.replace(Regex("\\s+"), " ")
        
        // 转换为小写（中文不区分大小写，但保留英文大小写可能对某些场景有用）
        // processed = processed.lowercase()
        
        // 移除标点符号（保留中文标点）
        processed = processed.replace(Regex("[,.!?;:]"), "")
        
        // 标准化称呼
        processed = processed.replace("老爸", "父亲")
            .replace("老妈", "母亲")
            .replace("老婆", "妻子")
            .replace("老公", "丈夫")
            .replace("媳妇", "妻子")
            .replace("老头", "父亲")
            .replace("老妈子", "母亲")
        
        return processed
    }
    
    /**
     * 识别意图
     */
    private fun recognizeIntent(text: String, confidence: Float): Intent {
        // 这里应该使用AI模型进行意图识别
        // 简化实现：基于关键词匹配
        
        val intentKeywords = mapOf(
            "发消息" to "send_message",
            "发送" to "send_message",
            "告诉" to "send_message",
            "打电话" to "make_call",
            "呼叫" to "make_call",
            "拨打" to "make_call",
            "救命" to "emergency",
            "紧急" to "emergency",
            "SOS" to "emergency",
            "查找" to "search",
            "搜索" to "search",
            "找一下" to "search",
            "打开" to "open",
            "启动" to "open",
            "进入" to "open",
            "设置" to "settings",
            "音量" to "settings",
            "亮度" to "settings"
        )
        
        intentKeywords.forEach { (keyword, intentType) ->
            if (text.contains(keyword)) {
                return Intent(intentType, confidence)
            }
        }
        
        return Intent("unknown", confidence)
    }
    
    /**
     * 提取实体
     */
    private fun extractEntities(text: String, intent: Intent): Map<String, String> {
        val entities = mutableMapOf<String, String>()
        
        // 提取联系人
        val contactMatch = extractContact(text)
        contactMatch?.let { entities["contact"] = it }
        
        // 提取电话号码
        val phoneMatch = extractPhoneNumber(text)
        phoneMatch?.let { entities["phone"] = it }
        
        // 提取消息内容
        if (intent.type == "send_message") {
            val messageMatch = extractMessageContent(text)
            messageMatch?.let { entities["message"] = it }
        }
        
        // 提取应用名称
        if (intent.type == "open") {
            val appMatch = extractAppName(text)
            appMatch?.let { entities["app"] = it }
        }
        
        return entities
    }
    
    /**
     * 提取联系人
     */
    private fun extractContact(text: String): String? {
        // 简单的联系人提取逻辑
        // 实际应该从联系人数据库匹配
        
        val patterns = listOf(
            Regex("给(.+?)发消息"),
            Regex("告诉(.+?)"),
            Regex("打电话给(.+?)"),
            Regex("呼叫(.+?)"),
            Regex("拨打(.+?)"),
            Regex("查找(.+?)"),
            Regex("搜索(.+?)")
        )
        
        patterns.forEach { pattern ->
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val contact = match.groupValues[1].trim()
                if (contact.isNotBlank() && contact.length in 2..10) {
                    return contact
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取电话号码
     */
    private fun extractPhoneNumber(text: String): String? {
        val phonePattern = Regex("""(\d{3}[-\.\s]??\d{4}[-\.\s]??\d{4}|\(\d{3}\)\s*\d{3}[-\.\s]??\d{4}|\d{3}[-\.\s]??\d{4})""")
        return phonePattern.find(text)?.value?.replace(Regex("[\\s\\-\\.\\(\\)]"), "")
    }
    
    /**
     * 提取消息内容
     */
    private fun extractMessageContent(text: String): String? {
        val messagePatterns = listOf(
            Regex("""说(.+)$"""),
            Regex("""告诉.+?([，,].+)$"""),
            Regex("""内容[是:]?([^。，,.!?]+)""")
        )
        
        messagePatterns.forEach { pattern ->
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val content = match.groupValues[1].trim()
                if (content.isNotBlank()) {
                    return content
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取应用名称
     */
    private fun extractAppName(text: String): String? {
        val appKeywords = mapOf(
            "设置" to "settings",
            "联系人" to "contacts",
            "消息" to "messages",
            "电话" to "dialer",
            "相机" to "camera",
            "相册" to "gallery",
            "浏览器" to "browser",
            "地图" to "maps"
        )
        
        appKeywords.forEach { (chinese, english) ->
            if (text.contains(chinese)) {
                return english
            }
        }
        
        return null
    }
    
    /**
     * 匹配命令
     */
    private fun matchCommand(
        text: String,
        intent: Intent,
        entities: Map<String, String>
    ): VoiceCommand? {
        // 1. 根据意图类型过滤
        val filteredByIntent = commands.values.filter { command ->
            when (command.type) {
                is CommandType.SendMessage -> intent.type == "send_message"
                is CommandType.MakeCall -> intent.type == "make_call"
                is CommandType.EmergencyAlert -> intent.type == "emergency"
                is CommandType.SearchContact -> intent.type == "search"
                is CommandType.OpenApp -> intent.type == "open"
                is CommandType.ControlSettings -> intent.type == "settings"
                else -> true
            }
        }
        
        if (filteredByIntent.isEmpty()) return null
        
        // 2. 根据触发短语匹配
        val matchedByPhrase = filteredByIntent.filter { command ->
            command.triggerPhrases.any { phrase ->
                matchesPhrase(text, phrase, entities)
            }
        }
        
        if (matchedByPhrase.isNotEmpty()) {
            // 选择优先级最高的命令
            return matchedByPhrase.maxByOrNull { it.priority }
        }
        
        // 3. 根据实体匹配
        val matchedByEntity = filteredByIntent.filter { command ->
            matchesByEntities(command, entities)
        }
        
        return matchedByEntity.maxByOrNull { it.priority }
    }
    
    /**
     * 检查短语匹配
     */
    private fun matchesPhrase(
        text: String,
        phrase: String,
        entities: Map<String, String>
    ): Boolean {
        var processedPhrase = phrase
        
        // 替换实体占位符
        entities.forEach { (key, value) ->
            processedPhrase = processedPhrase.replace("{$key}", value)
        }
        
        // 检查是否包含短语
        return text.contains(processedPhrase)
    }
    
    /**
     * 根据实体匹配
     */
    private fun matchesByEntities(
        command: VoiceCommand,
        entities: Map<String, String>
    ): Boolean {
        return when (command.type) {
            is CommandType.SendMessage -> entities.containsKey("contact")
            is CommandType.MakeCall -> entities.containsKey("contact") || entities.containsKey("phone")
            is CommandType.EmergencyAlert -> true // 紧急命令总是匹配
            is CommandType.SearchContact -> entities.containsKey("contact")
            is CommandType.OpenApp -> entities.containsKey("app")
            else -> false
        }
    }
    
    /**
     * 执行命令
     */
    private suspend fun executeCommand(
        command: VoiceCommand,
        context: VoiceContext,
        entities: Map<String, String>
    ): CommandResult {
        return try {
            // 如果需要确认且不是紧急命令
            if (command.requiresConfirmation && command.type !is CommandType.EmergencyAlert) {
                // 这里应该显示确认对话框
                // 简化实现：直接执行
            }
            
            val result = command.action(context)
            
            // 如果需要反馈
            if (result.shouldGiveFeedback) {
                voiceFeedbackSystem.giveFeedback(
                    message = result.message,
                    context = VoiceFeedbackContext(
                        commandType = command.type,
                        isSuccess = result.isSuccess,
                        urgency = if (command.type is CommandType.EmergencyAlert) 
                            UrgencyLevel.HIGH else UrgencyLevel.NORMAL
                    )
                )
            }
            
            result
            
        } catch (e: Exception) {
            logger.e("VoiceCommandCenter", "执行命令失败: ${command.id}", e)
            
            // 给出错误反馈
            voiceFeedbackSystem.giveFeedback(
                message = "执行命令失败: ${e.message}",
                context = VoiceFeedbackContext(
                    commandType = command.type,
                    isSuccess = false,
                    urgency = UrgencyLevel.NORMAL
                )
            )
            
            CommandResult(
                isSuccess = false,
                message = "执行失败: ${e.message}"
            )
        }
    }
    
    /**
     * 执行发送消息命令
     */
    private suspend fun executeSendMessage(context: VoiceContext): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                // 提取联系人
                val contactMatch = extractContact(context.rawText)
                if (contactMatch == null) {
                    return@withContext CommandResult(
                        isSuccess = false,
                        message = "请指定要发送给谁"
                    )
                }
                
                // 提取消息内容
                val messageContent = extractMessageContent(context.rawText)
                if (messageContent.isNullOrBlank()) {
                    return@withContext CommandResult(
                        isSuccess = false,
                        message = "请告诉我要发送什么内容"
                    )
                }
                
                // 这里应该实际发送消息
                // 简化实现：记录日志
                logger.i("VoiceCommandCenter", "发送消息给 $contactMatch: $messageContent")
                
                CommandResult(
                    isSuccess = true,
                    message = "消息已发送给 $contactMatch",
                    data = mapOf(
                        "contact" to contactMatch,
                        "message" to messageContent,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                
            } catch (e: Exception) {
                logger.e("VoiceCommandCenter", "执行发送消息失败", e)
                CommandResult(
                    isSuccess = false,
                    message = "发送消息失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 执行打电话命令
     */
    private suspend fun executeMakeCall(context: VoiceContext): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                // 提取联系人
                val contactMatch = extractContact(context.rawText)
                val phoneMatch = extractPhoneNumber(context.rawText)
                
        if (contactMatch == null && phoneMatch == null) {
            return@withContext CommandResult(
                isSuccess = false,
                message = "请指定要打给谁"
            )
        }
                
                // 使用语音拨号器拨号
                val dialResult = if (phoneMatch != null) {
                    voiceDialerPro.dialNumber(phoneMatch, context)
                } else {
                    voiceDialerPro.dialContact(contactMatch!!, context)
                }
                
                CommandResult(
                    isSuccess = dialResult.isSuccess,
                    message = dialResult.message,
                    data = dialResult.data
                )
                
            } catch (e: Exception) {
                logger.e("VoiceCommandCenter", "执行打电话失败", e)
                CommandResult(
                    isSuccess = false,
                    message = "打电话失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 执行紧急报警命令
     */
    private suspend fun executeEmergencyAlert(context: VoiceContext): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                // 触发紧急报警
                // 这里应该调用紧急报警系统
                
                // 简化实现：启动紧急服务
                val intent = Intent(context, EmergencyService::class.java).apply {
                    action = "EMERGENCY_ALERT"
                    putExtra("trigger", "voice_command")
                    putExtra("timestamp", System.currentTimeMillis())
                    putExtra("context", context.toString())
                }
                
                ContextCompat.startForegroundService(context, intent)
                
                CommandResult(
                    isSuccess = true,
                    message = "紧急报警已触发",
                    data = mapOf(
                        "trigger" to "voice_command",
                        "timestamp" to System.currentTimeMillis()
                    ),
                    shouldGiveFeedback = true
                )
                
            } catch (e: Exception) {
                logger.e("VoiceCommandCenter", "执行紧急报警失败", e)
                CommandResult(
                    isSuccess = false,
                    message = "紧急报警失败: ${e.message}",
                    shouldGiveFeedback = true
                )
            }
        }
    }
    
    /**
     * 执行搜索联系人命令
     */
    private suspend fun executeSearchContact(context: VoiceContext): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                val contactMatch = extractContact(context.rawText)
                
                if (contactMatch == null) {
                    return@withContext CommandResult(
                        isSuccess = false,
                        message = "请指定要搜索的联系人"
                    )
                }
                
                // 这里应该从数据库搜索联系人
                // 简化实现：返回模拟数据
                val mockContact = mapOf(
                    "name" to contactMatch,
                    "phone" to "138****1234",
                    "lastContact" to "2024-01-15"
                )
                
                CommandResult(
                    isSuccess = true,
                    message = "找到联系人: $contactMatch",
                    data = mockContact
                )
                
            } catch (e: Exception) {
                logger.e("VoiceCommandCenter", "执行搜索联系人失败", e)
                CommandResult(
                    isSuccess = false,
                    message = "搜索失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 执行打开应用命令
     */
    private suspend fun executeOpenApp(context: VoiceContext): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                val appMatch = extractAppName(context.rawText)
                
                if (appMatch == null) {
                    return@withContext CommandResult(
                        isSuccess = false,
                        message = "请指定要打开的应用"
                    )
                }
                
                // 这里应该启动对应应用
                // 简化实现：记录日志
                logger.i("VoiceCommandCenter", "打开应用: $appMatch")
                
                CommandResult(
                    isSuccess = true,
                    message = "正在打开$appMatch",
                    data = mapOf("app" to appMatch)
                )
                
            } catch (e: Exception) {
                logger.e("VoiceCommandCenter", "执行打开应用失败", e)
                CommandResult(
                    isSuccess = false,
                    message = "打开应用失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 记录命令执行历史
     */
    private fun recordExecution(execution: CommandExecution) {
        commandHistory.add(execution)
        
        // 保持历史记录大小
        if (commandHistory.size > 100) {
            commandHistory.removeFirst()
        }
        
        logger.d("VoiceCommandCenter", "记录命令执行: ${execution.commandId}")
    }
    
    /**
     * 从执行中学习
     */
    private fun learnFromExecution(
        commandId: String,
        result: CommandResult,
        inputText: String
    ) {
        // 这里应该实现学习逻辑，优化命令匹配
        // 可以记录成功/失败的模式，调整匹配权重
        
        if (result.isSuccess) {
            logger.d("VoiceCommandCenter", "学习成功模式: $commandId - $inputText")
        } else {
            logger.d("VoiceCommandCenter", "学习失败模式: $commandId - $inputText")
        }
    }
    
    /**
     * 建议命令
     */
    private fun suggestCommands(text: String): List<VoiceCommand> {
        // 基于文本相似度推荐命令
        val suggestions = mutableListOf<VoiceCommand>()
        
        commands.values.forEach { command ->
            val similarity = calculateTextSimilarity(text, command.triggerPhrases.joinToString(" "))
            if (similarity > 0.3) {
                suggestions.add(command)
            }
        }
        
        return suggestions.sortedByDescending { it.priority }
    }
    
    /**
     * 计算文本相似度
     */
    private fun calculateTextSimilarity(text1: String, text2: String): Float {
        // 简单的相似度计算
        if (text1 == text2) return 1.0f
        
        val words1 = text1.split(" ").toSet()
        val words2 = text2.split(" ").toSet()
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) intersection.toFloat() / union else 0f
    }
    
    /**
     * 获取命令列表
     */
    fun getCommands(): List<VoiceCommand> {
        return commands.values.sortedByDescending { it.priority }
    }
    
    /**
     * 获取执行历史
     */
    fun getExecutionHistory(limit: Int = 20): List<CommandExecution> {
        return commandHistory.takeLast(limit).reversed()
    }
    
    /**
     * 清除历史记录
     */
    fun clearHistory() {
        commandHistory.clear()
    }
    
    /**
     * 加载用户自定义命令
     */
    private suspend fun loadUserCommands() {
        // 从数据库或文件加载用户自定义命令
        // 这里应该实现实际的加载逻辑
    }
    
    /**
     * 保存用户自定义命令
     */
    suspend fun saveUserCommand(command: VoiceCommand): Boolean {
        return try {
            // 保存到数据库或文件
            registerCommand(command)
            true
        } catch (e: Exception) {
            logger.e("VoiceCommandCenter", "保存用户命令失败", e)
            false
        }
    }
}

// 数据类
data class Intent(
    val type: String,
    val confidence: Float
)

data class CommandExecution(
    val commandId: String,
    val inputText: String,
    val processedText: String,
    val intent: Intent,
    val entities: Map<String, String>,
    val result: CommandResult,
    val timestamp: Long,
    val context: VoiceCommandCenter.VoiceContext
)

sealed class ProcessResult {
    data class Executed(
        val command: VoiceCommand,
        val result: CommandResult,
        val entities: Map<String, String>
    ) : ProcessResult()
    
    data class NoMatch(
        val suggestedCommands: List<VoiceCommand>
    ) : ProcessResult()
    
    data class Blocked(
        val reason: String
    ) : ProcessResult()
    
    data class Error(
        val error: String,
        val shouldRetry: Boolean
    ) : ProcessResult()
}

enum class UrgencyLevel {
    LOW, NORMAL, HIGH, CRITICAL
}

data class VoiceFeedbackContext(
    val commandType: VoiceCommandCenter.CommandType,
    val isSuccess: Boolean,
    val urgency: UrgencyLevel
)