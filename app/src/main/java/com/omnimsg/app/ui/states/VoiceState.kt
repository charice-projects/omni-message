// 📁 app/src/main/java/com/omnimsg/app/ui/states/VoiceState.kt
package com.omnimsg.app.ui.states

import com.omnimsg.app.ui.viewmodels.VoiceCommand
import com.omnimsg.app.ui.viewmodels.VoiceLanguage

data class VoiceState(
    // 语音控制总开关
    val voiceControlEnabled: Boolean = true,
    
    // 唤醒词设置
    val wakeWordEnabled: Boolean = true,
    val wakeWord: String = "熙熙",
    val wakeWordTrained: Boolean = false,
    val wakeWordAccuracy: Float = 0.0f,
    val wakeWordLastTrained: Long? = null,
    val personalizedWakeWordEnabled: Boolean = false,
    
    // 语音反馈设置
    val voiceFeedbackEnabled: Boolean = true,
    val voiceFeedbackVolume: Float = 0.8f,
    val voiceFeedbackSpeed: Float = 1.0f,
    val voiceFeedbackPitch: Float = 1.0f,
    
    // 语音识别设置
    val voiceRecognitionLanguage: VoiceLanguage = VoiceLanguage.ZH_CN,
    val voiceRecognitionConfidence: Float = 0.7f,
    val lastRecognitionResult: String? = null,
    val recognitionConfidence: Float = 0.0f,
    
    // 声纹识别设置
    val voicePrintEnabled: Boolean = false,
    val voicePrintRegistered: Boolean = false,
    val autoVoicePrintUpdate: Boolean = false,
    val voicePrintConfidence: Float = 0.0f,
    
    // 语音命令管理
    val voiceCommands: List<VoiceCommand> = emptyList(),
    val filteredCommands: List<VoiceCommand> = emptyList(),
    val selectedCommand: VoiceCommand? = null,
    
    // 高级设置
    val voiceCommandTimeout: Int = 5000, // 5秒
    val backgroundListening: Boolean = false,
    val sensitivity: Float = 0.8f,
    val noiseSuppression: Boolean = true,
    val echoCancellation: Boolean = true,
    
    // 操作状态
    val isRecording: Boolean = false,
    val recordingProgress: Float = 0f,
    val isTestingRecognition: Boolean = false,
    val isTestingSynthesis: Boolean = false,
    val isTrainingWakeWord: Boolean = false,
    val isAddingCommand: Boolean = false,
    val isEditingCommand: Boolean = false,
    
    // UI状态
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val showWakeWordTrainer: Boolean = false,
    val showVoicePrintDialog: Boolean = false,
    val showLanguagePicker: Boolean = false,
    val showConfidenceDialog: Boolean = false,
    val showSensitivityDialog: Boolean = false
)