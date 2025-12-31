// 📁 app/src/main/java/com/omnimsg/app/domain/repositories/VoiceRepository.kt
package com.omnimsg.app.domain.repositories

import com.omnimsg.app.domain.usecases.voice.RecognitionResult
import com.omnimsg.app.ui.viewmodels.*

interface VoiceRepository {
    // 语音设置管理
    suspend fun getVoiceSettings(): VoiceSettings
    suspend fun updateVoiceControlEnabled(enabled: Boolean)
    suspend fun updateWakeWordEnabled(enabled: Boolean)
    suspend fun updateWakeWord(wakeWord: String)
    suspend fun updateVoiceFeedbackEnabled(enabled: Boolean)
    suspend fun updateLanguage(language: VoiceLanguage)
    suspend fun updateConfidenceThreshold(threshold: Float)
    suspend fun updateVoicePrintEnabled(enabled: Boolean)
    suspend fun updateAutoVoicePrintUpdate(enabled: Boolean)
    suspend fun updateSensitivity(sensitivity: Float)
    
    // 唤醒词管理
    suspend fun getWakeWordInfo(): WakeWordInfo
    suspend fun trainWakeWord(audioData: ByteArray, wakeWord: String): Result<WakeWordTrainingResult>
    
    // 语音命令管理
    suspend fun getVoiceCommands(): List<VoiceCommand>
    suspend fun addVoiceCommand(command: VoiceCommand)
    suspend fun deleteVoiceCommand(commandId: String)
    suspend fun updateVoiceCommand(command: VoiceCommand)
    
    // 语音识别
    suspend fun recognizeSpeech(audioData: ByteArray): RecognitionResult
    
    // 语音合成
    suspend fun synthesizeSpeech(text: String): Result<ByteArray>
}

data class VoiceSettings(
    val enabled: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    val wakeWord: String = "熙熙",
    val voiceFeedbackEnabled: Boolean = true,
    val language: VoiceLanguage = VoiceLanguage.ZH_CN,
    val confidenceThreshold: Float = 0.7f,
    val voicePrintEnabled: Boolean = false,
    val autoVoicePrintUpdate: Boolean = false,
    val commandTimeout: Int = 5000,
    val backgroundListening: Boolean = false,
    val sensitivity: Float = 0.8f
)

data class WakeWordInfo(
    val isTrained: Boolean = false,
    val accuracy: Float = 0.0f,
    val lastTrainedTime: Long? = null,
    val personalizedEnabled: Boolean = false
)