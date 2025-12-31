// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/VoiceViewModel.kt
package com.omnimsg.app.ui.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnimsg.app.data.repository.VoiceRepository
import com.omnimsg.app.domain.usecases.voice.*
import com.omnimsg.app.ui.events.UiEvent
import com.omnimsg.app.ui.states.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val recognizeSpeechUseCase: RecognizeSpeechUseCase,
    private val processVoiceCommandUseCase: ProcessVoiceCommandUseCase,
    private val generateVoiceFeedbackUseCase: GenerateVoiceFeedbackUseCase,
    private val trainWakeWordUseCase: TrainWakeWordUseCase,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    // UI状态
    var state by mutableStateOf(VoiceState())
        private set

    // 事件通道
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // 音频录制状态
    private var isRecording by mutableStateOf(false)
    private var recordedAudioData: ByteArray? = null

    init {
        loadVoiceSettings()
        loadVoiceCommands()
        loadWakeWordSettings()
    }

    // 加载语音设置
    private fun loadVoiceSettings() {
        viewModelScope.launch {
            try {
                val settings = voiceRepository.getVoiceSettings()
                state = state.copy(
                    voiceControlEnabled = settings.enabled,
                    wakeWordEnabled = settings.wakeWordEnabled,
                    wakeWord = settings.wakeWord,
                    voiceFeedbackEnabled = settings.voiceFeedbackEnabled,
                    voiceRecognitionLanguage = settings.language,
                    voiceRecognitionConfidence = settings.confidenceThreshold,
                    voicePrintEnabled = settings.voicePrintEnabled,
                    autoVoicePrintUpdate = settings.autoVoicePrintUpdate,
                    voiceCommandTimeout = settings.commandTimeout,
                    backgroundListening = settings.backgroundListening,
                    sensitivity = settings.sensitivity
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("加载语音设置失败: ${e.message}"))
            }
        }
    }

    // 加载语音命令
    private fun loadVoiceCommands() {
        viewModelScope.launch {
            try {
                val commands = voiceRepository.getVoiceCommands()
                state = state.copy(
                    voiceCommands = commands,
                    filteredCommands = commands // 初始显示所有命令
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("加载语音命令失败"))
            }
        }
    }

    // 加载唤醒词设置
    private fun loadWakeWordSettings() {
        viewModelScope.launch {
            try {
                val wakeWordInfo = voiceRepository.getWakeWordInfo()
                state = state.copy(
                    wakeWordTrained = wakeWordInfo.isTrained,
                    wakeWordAccuracy = wakeWordInfo.accuracy,
                    wakeWordLastTrained = wakeWordInfo.lastTrainedTime,
                    personalizedWakeWordEnabled = wakeWordInfo.personalizedEnabled
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("加载唤醒词信息失败"))
            }
        }
    }

    // 切换语音控制总开关
    fun toggleVoiceControl(enabled: Boolean) {
        viewModelScope.launch {
            try {
                voiceRepository.updateVoiceControlEnabled(enabled)
                state = state.copy(voiceControlEnabled = enabled)
                sendUiEvent(
                    UiEvent.ShowSnackbar(
                        if (enabled) "语音控制已启用" else "语音控制已禁用"
                    )
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新语音控制设置失败"))
            }
        }
    }

    // 切换唤醒词功能
    fun toggleWakeWord(enabled: Boolean) {
        viewModelScope.launch {
            try {
                voiceRepository.updateWakeWordEnabled(enabled)
                state = state.copy(wakeWordEnabled = enabled)
                sendUiEvent(
                    UiEvent.ShowSnackbar(
                        if (enabled) "唤醒词功能已启用" else "唤醒词功能已禁用"
                    )
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新唤醒词设置失败"))
            }
        }
    }

    // 更新唤醒词
    fun updateWakeWord(newWakeWord: String) {
        if (newWakeWord.length < 2 || newWakeWord.length > 10) {
            sendUiEvent(UiEvent.ShowSnackbar("唤醒词长度应在2-10个字符之间"))
            return
        }

        viewModelScope.launch {
            try {
                voiceRepository.updateWakeWord(newWakeWord)
                state = state.copy(wakeWord = newWakeWord)
                sendUiEvent(UiEvent.ShowSnackbar("唤醒词已更新为: $newWakeWord"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新唤醒词失败"))
            }
        }
    }

    // 切换语音反馈
    fun toggleVoiceFeedback(enabled: Boolean) {
        viewModelScope.launch {
            try {
                voiceRepository.updateVoiceFeedbackEnabled(enabled)
                state = state.copy(voiceFeedbackEnabled = enabled)
                sendUiEvent(
                    UiEvent.ShowSnackbar(
                        if (enabled) "语音反馈已启用" else "语音反馈已禁用"
                    )
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新语音反馈设置失败"))
            }
        }
    }

    // 更新语言设置
    fun updateLanguage(language: VoiceLanguage) {
        viewModelScope.launch {
            try {
                voiceRepository.updateLanguage(language)
                state = state.copy(voiceRecognitionLanguage = language)
                sendUiEvent(UiEvent.ShowSnackbar("语音识别语言已更新"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新语言设置失败"))
            }
        }
    }

    // 更新识别置信度阈值
    fun updateConfidenceThreshold(threshold: Float) {
        if (threshold < 0.1f || threshold > 1.0f) {
            sendUiEvent(UiEvent.ShowSnackbar("置信度阈值应在0.1到1.0之间"))
            return
        }

        viewModelScope.launch {
            try {
                voiceRepository.updateConfidenceThreshold(threshold)
                state = state.copy(voiceRecognitionConfidence = threshold)
                sendUiEvent(UiEvent.ShowSnackbar("置信度阈值已更新: ${String.format("%.1f", threshold)}"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新置信度阈值失败"))
            }
        }
    }

    // 切换声纹识别
    fun toggleVoicePrint(enabled: Boolean) {
        viewModelScope.launch {
            try {
                voiceRepository.updateVoicePrintEnabled(enabled)
                state = state.copy(voicePrintEnabled = enabled)
                sendUiEvent(
                    UiEvent.ShowSnackbar(
                        if (enabled) "声纹识别已启用" else "声纹识别已禁用"
                    )
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新声纹识别设置失败"))
            }
        }
    }

    // 切换自动声纹更新
    fun toggleAutoVoicePrintUpdate(enabled: Boolean) {
        viewModelScope.launch {
            try {
                voiceRepository.updateAutoVoicePrintUpdate(enabled)
                state = state.copy(autoVoicePrintUpdate = enabled)
                sendUiEvent(
                    UiEvent.ShowSnackbar(
                        if (enabled) "自动声纹更新已启用" else "自动声纹更新已禁用"
                    )
                )
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新自动声纹更新设置失败"))
            }
        }
    }

    // 更新灵敏度
    fun updateSensitivity(sensitivity: Float) {
        if (sensitivity < 0.1f || sensitivity > 1.0f) {
            sendUiEvent(UiEvent.ShowSnackbar("灵敏度应在0.1到1.0之间"))
            return
        }

        viewModelScope.launch {
            try {
                voiceRepository.updateSensitivity(sensitivity)
                state = state.copy(sensitivity = sensitivity)
                sendUiEvent(UiEvent.ShowSnackbar("灵敏度已更新: ${String.format("%.1f", sensitivity)}"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新灵敏度失败"))
            }
        }
    }

    // 开始录制语音命令
    fun startRecording() {
        viewModelScope.launch {
            try {
                isRecording = true
                state = state.copy(isRecording = true, recordingProgress = 0f)
                sendUiEvent(UiEvent.ShowSnackbar("开始录制语音..."))
                
                // 模拟录音过程
                for (i in 1..100) {
                    kotlinx.coroutines.delay(100)
                    state = state.copy(recordingProgress = i / 100f)
                }
                
                // 结束录音
                stopRecording()
            } catch (e: Exception) {
                isRecording = false
                state = state.copy(isRecording = false)
                sendUiEvent(UiEvent.ShowSnackbar("录音失败: ${e.message}"))
            }
        }
    }

    // 停止录制语音命令
    fun stopRecording() {
        isRecording = false
        state = state.copy(isRecording = false)
        
        // 模拟录制了一些音频数据
        recordedAudioData = ByteArray(1024) { (Math.random() * 256).toByte() }
        
        sendUiEvent(UiEvent.ShowSnackbar("语音录制完成"))
    }

    // 测试语音识别
    fun testVoiceRecognition() {
        viewModelScope.launch {
            try {
                state = state.copy(isTestingRecognition = true)
                
                // 模拟语音识别测试
                kotlinx.coroutines.delay(2000)
                
                val testText = "这是语音识别测试结果"
                state = state.copy(
                    lastRecognitionResult = testText,
                    recognitionConfidence = 0.85f,
                    isTestingRecognition = false
                )
                
                sendUiEvent(UiEvent.ShowSnackbar("语音识别测试完成: $testText"))
            } catch (e: Exception) {
                state = state.copy(isTestingRecognition = false)
                sendUiEvent(UiEvent.ShowSnackbar("语音识别测试失败"))
            }
        }
    }

    // 测试语音合成
    fun testVoiceSynthesis() {
        viewModelScope.launch {
            try {
                state = state.copy(isTestingSynthesis = true)
                
                // 模拟语音合成测试
                kotlinx.coroutines.delay(1500)
                
                state = state.copy(isTestingSynthesis = false)
                sendUiEvent(UiEvent.ShowSnackbar("语音合成测试完成"))
            } catch (e: Exception) {
                state = state.copy(isTestingSynthesis = false)
                sendUiEvent(UiEvent.ShowSnackbar("语音合成测试失败"))
            }
        }
    }

    // 训练个性化唤醒词
    fun trainPersonalizedWakeWord() {
        viewModelScope.launch {
            try {
                if (recordedAudioData == null) {
                    sendUiEvent(UiEvent.ShowSnackbar("请先录制语音样本"))
                    return@launch
                }

                state = state.copy(isTrainingWakeWord = true)
                
                trainWakeWordUseCase(
                    audioData = recordedAudioData!!,
                    wakeWord = state.wakeWord
                ).onSuccess { result ->
                    state = state.copy(
                        isTrainingWakeWord = false,
                        wakeWordTrained = true,
                        wakeWordAccuracy = result.accuracy,
                        wakeWordLastTrained = System.currentTimeMillis()
                    )
                    sendUiEvent(UiEvent.ShowSnackbar("唤醒词训练成功！准确率: ${String.format("%.1f", result.accuracy * 100)}%"))
                }.onFailure { error ->
                    state = state.copy(isTrainingWakeWord = false)
                    sendUiEvent(UiEvent.ShowSnackbar("唤醒词训练失败: ${error.message}"))
                }
            } catch (e: Exception) {
                state = state.copy(isTrainingWakeWord = false)
                sendUiEvent(UiEvent.ShowSnackbar("训练过程中出错"))
            }
        }
    }

    // 添加语音命令
    fun addVoiceCommand(command: VoiceCommand) {
        viewModelScope.launch {
            try {
                voiceRepository.addVoiceCommand(command)
                loadVoiceCommands() // 重新加载命令列表
                sendUiEvent(UiEvent.ShowSnackbar("语音命令已添加: ${command.phrase}"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("添加语音命令失败"))
            }
        }
    }

    // 删除语音命令
    fun deleteVoiceCommand(commandId: String) {
        viewModelScope.launch {
            try {
                voiceRepository.deleteVoiceCommand(commandId)
                loadVoiceCommands() // 重新加载命令列表
                sendUiEvent(UiEvent.ShowSnackbar("语音命令已删除"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("删除语音命令失败"))
            }
        }
    }

    // 更新语音命令
    fun updateVoiceCommand(command: VoiceCommand) {
        viewModelScope.launch {
            try {
                voiceRepository.updateVoiceCommand(command)
                loadVoiceCommands() // 重新加载命令列表
                sendUiEvent(UiEvent.ShowSnackbar("语音命令已更新"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("更新语音命令失败"))
            }
        }
    }

    // 搜索语音命令
    fun searchVoiceCommands(query: String) {
        val filtered = if (query.isBlank()) {
            state.voiceCommands
        } else {
            state.voiceCommands.filter { command ->
                command.phrase.contains(query, ignoreCase = true) ||
                command.description.contains(query, ignoreCase = true) ||
                command.category.contains(query, ignoreCase = true)
            }
        }
        
        state = state.copy(
            searchQuery = query,
            filteredCommands = filtered
        )
    }

    // 发送UI事件
    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}

// 语音命令数据类
data class VoiceCommand(
    val id: String,
    val phrase: String,
    val description: String,
    val action: String,
    val category: String,
    val enabled: Boolean = true,
    val requiresConfirmation: Boolean = false,
    val confidenceThreshold: Float = 0.7f,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val usageCount: Int = 0
)

// 语音语言枚举
enum class VoiceLanguage {
    SYSTEM,     // 跟随系统
    ZH_CN,      // 简体中文
    ZH_TW,      // 繁体中文
    EN_US,      // 英语（美国）
    EN_UK,      // 英语（英国）
    JA,         // 日语
    KO,         // 韩语
    FR,         // 法语
    DE,         // 德语
    ES,         // 西班牙语
}

// 语音命令类别枚举
enum class VoiceCommandCategory {
    MESSAGING,      // 消息相关
    CONTACTS,       // 联系人相关
    EMERGENCY,      // 紧急功能
    SETTINGS,       // 设置相关
    NAVIGATION,     // 导航相关
    MEDIA,          // 媒体控制
    APP_CONTROL,    // 应用控制
    SYSTEM,         // 系统功能
    CUSTOM          // 自定义命令
}

// 唤醒词训练结果
data class WakeWordTrainingResult(
    val success: Boolean,
    val accuracy: Float,
    val modelPath: String? = null,
    val trainingTime: Long
)