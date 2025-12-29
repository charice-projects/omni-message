package com.omnimsg.domain.models.voice

import java.util.*

// 唤醒词检测结果
data class WakeWordDetection(
    val id: Long = 0,
    val detectionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val wakeWord: String = "熙熙",
    val detectedAt: Date = Date(),
    val confidence: Float,
    val audioSampleUri: String? = null,
    val audioFeatures: ByteArray? = null,
    val backgroundNoiseLevel: Float = 0f,
    val signalToNoiseRatio: Float = 0f,
    val deviceInfo: Map<String, String> = emptyMap(),
    val locationData: Map<String, String>? = null,
    val contextData: Map<String, String> = emptyMap(),
    val modelVersion: String = "1.0.0",
    val verificationMethod: String = "VOICE_PRINT",
    val verificationScore: Float? = null,
    val isVerified: Boolean = false,
    val triggeredAction: String? = null,
    val createdAt: Date = Date()
) {
    val isValid: Boolean
        get() = confidence >= 0.8f && (verificationScore ?: 1.0f) >= 0.7f
    
    val isHighConfidence: Boolean
        get() = confidence >= 0.9f
    
    val formattedTime: String
        get() {
            val now = Date()
            val diff = now.time - detectedAt.time
            
            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> {
                    val dateFormat = java.text.SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                    dateFormat.format(detectedAt)
                }
            }
        }
}

// 语音命令
data class VoiceCommand(
    val id: Long = 0,
    val commandId: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: VoiceCommandType = VoiceCommandType.CUSTOM,
    val triggerPhrase: String,
    val voiceText: String,
    val normalizedText: String,
    val intent: String,
    val entities: Map<String, String> = emptyMap(),
    val confidence: Float,
    val actionType: String,
    val actionData: Map<String, String> = emptyMap(),
    val executionResult: String? = null,
    val errorMessage: String? = null,
    val status: VoiceCommandStatus = VoiceCommandStatus.PENDING,
    val responseText: String? = null,
    val responseAudioUri: String? = null,
    val contextData: Map<String, String> = emptyMap(),
    val deviceInfo: Map<String, String> = emptyMap(),
    val audioDuration: Long = 0,
    val audioQuality: Float = 0f,
    val backgroundNoiseLevel: Float = 0f,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val privacyLevel: Int = 0,
    val modelVersion: String = "1.0.0",
    val executedAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val isSuccessful: Boolean
        get() = status == VoiceCommandStatus.COMPLETED
    
    val hasError: Boolean
        get() = status == VoiceCommandStatus.FAILED
    
    val canRetry: Boolean
        get() = status == VoiceCommandStatus.FAILED || status == VoiceCommandStatus.TIMEOUT
    
    val processingTime: Long
        get() {
            return if (executedAt != null && createdAt != null) {
                executedAt.time - createdAt.time
            } else {
                0L
            }
        }
    
    val actionSummary: String
        get() = when (actionType) {
            "SEND_MESSAGE" -> "发送消息"
            "CALL_CONTACT" -> "呼叫联系人"
            "SEARCH_CONTACT" -> "搜索联系人"
            "CREATE_REMINDER" -> "创建提醒"
            "SET_ALARM" -> "设置闹钟"
            "OPEN_APP" -> "打开应用"
            "PLAY_MUSIC" -> "播放音乐"
            "NAVIGATE" -> "导航"
            "EMERGENCY_ALERT" -> "紧急报警"
            else -> "未知操作"
        }
    
    fun markAsExecuted(result: String? = null): VoiceCommand {
        return copy(
            status = VoiceCommandStatus.COMPLETED,
            executionResult = result,
            executedAt = Date(),
            updatedAt = Date()
        )
    }
    
    fun markAsFailed(error: String): VoiceCommand {
        return copy(
            status = VoiceCommandStatus.FAILED,
            errorMessage = error,
            executedAt = Date(),
            updatedAt = Date()
        )
    }
    
    fun markAsFavorite(isFavorite: Boolean = true): VoiceCommand {
        return copy(
            isFavorite = isFavorite,
            updatedAt = Date()
        )
    }
}

// 语音配置文件
data class VoiceProfile(
    val id: Long = 0,
    val profileId: String = UUID.randomUUID().toString(),
    val userId: String,
    val profileName: String,
    val voiceFeatures: ByteArray? = null,
    val voicePrintHash: String? = null,
    val voicePrintStatus: VoicePrintStatus = VoicePrintStatus.INCOMPLETE,
    val trainingSamples: Int = 0,
    val accuracyScore: Float = 0f,
    val verificationThreshold: Float = 0.85f,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val preferredLanguage: String = "zh-CN",
    val accentType: String = "standard",
    val speechRate: Float = 1.0f,
    val pitchLevel: Float = 0f,
    val volumeLevel: Float = 0f,
    val voiceCharacteristics: Map<String, Float> = emptyMap(),
    val privacySettings: Map<String, Boolean> = emptyMap(),
    val modelData: ByteArray? = null,
    val lastTrainedAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val isReady: Boolean
        get() = voicePrintStatus == VoicePrintStatus.READY
    
    val needsTraining: Boolean
        get() = voicePrintStatus == VoicePrintStatus.INCOMPLETE || trainingSamples < 10
    
    val trainingProgress: Float
        get() = trainingSamples.coerceAtMost(20).toFloat() / 20f
    
    fun activate(isActive: Boolean = true): VoiceProfile {
        return copy(
            isActive = isActive,
            updatedAt = Date()
        )
    }
    
    fun setAsDefault(isDefault: Boolean = true): VoiceProfile {
        return copy(
            isDefault = isDefault,
            updatedAt = Date()
        )
    }
    
    fun updateTraining(samples: Int, accuracy: Float): VoiceProfile {
        val newStatus = if (samples >= 10 && accuracy >= 0.8f) {
            VoicePrintStatus.READY
        } else if (samples >= 5) {
            VoicePrintStatus.TRAINING
        } else {
            VoicePrintStatus.INCOMPLETE
        }
        
        return copy(
            trainingSamples = samples,
            accuracyScore = accuracy,
            voicePrintStatus = newStatus,
            lastTrainedAt = Date(),
            updatedAt = Date()
        )
    }
}

// 语音识别结果
data class SpeechRecognitionResult(
    val text: String,
    val normalizedText: String,
    val confidence: Float,
    val isFinal: Boolean = true,
    val partialResults: List<PartialResult> = emptyList(),
    val language: String = "zh-CN",
    val duration: Long = 0,
    val audioQuality: Float = 0f,
    val backgroundNoiseLevel: Float = 0f,
    val timestamps: List<WordTimestamp> = emptyList(),
    val alternatives: List<Alternative> = emptyList()
) {
    data class PartialResult(
        val text: String,
        val confidence: Float,
        val timestamp: Long
    )
    
    data class WordTimestamp(
        val word: String,
        val startTime: Long,
        val endTime: Long,
        val confidence: Float
    )
    
    data class Alternative(
        val text: String,
        val confidence: Float
    )
    
    val hasHighConfidence: Boolean
        get() = confidence >= 0.8f
    
    val wordCount: Int
        get() = text.split("\\s+".toRegex()).size
}

// 语音命令意图
data class VoiceCommandIntent(
    val intent: String,
    val confidence: Float,
    val entities: Map<String, String> = emptyMap(),
    val slots: Map<String, Slot> = emptyMap()
) {
    data class Slot(
        val value: String,
        val confidence: Float,
        val rawValue: String? = null
    )
    
    val isConfident: Boolean
        get() = confidence >= 0.7f
    
    val primaryEntity: String?
        get() = entities.values.firstOrNull()
}

// 语音配置
data class VoiceConfig(
    val wakeWord: String = "熙熙",
    val wakeWordSensitivity: Float = 0.85f,
    val requireVoiceVerification: Boolean = true,
    val voiceVerificationThreshold: Float = 0.75f,
    val speechRecognitionLanguage: String = "zh-CN",
    val speechRecognitionModel: String = "default",
    val enableOfflineRecognition: Boolean = true,
    enableContinuousListening: Boolean = false,
    val continuousListeningTimeout: Long = 10000, // 10秒
    val maxBackgroundNoiseLevel: Float = 0.3f,
    val enableVoiceFeedback: Boolean = true,
    val voiceFeedbackLanguage: String = "zh-CN",
    val voiceFeedbackSpeed: Float = 1.0f,
    val voiceFeedbackVolume: Float = 1.0f,
    val enableVoicePrint: Boolean = true,
    val voicePrintSamplesRequired: Int = 10,
    val privacyLevel: VoicePrivacyLevel = VoicePrivacyLevel.MEDIUM,
    val saveAudioSamples: Boolean = false,
    val audioSampleRetentionDays: Int = 7,
    val enableCommandLearning: Boolean = true,
    val maxCommandHistory: Int = 100,
    val autoDeleteOldCommands: Boolean = true,
    val commandRetentionDays: Int = 30
)

// 枚举类型
enum class VoiceCommandType {
    WAKE_WORD, // 唤醒词
    SYSTEM_COMMAND, // 系统命令
    MESSAGE_COMMAND, // 消息命令
    CONTACT_COMMAND, // 联系人命令
    EMERGENCY_COMMAND, // 紧急命令
    CUSTOM // 自定义命令
}

enum class VoiceCommandStatus {
    PENDING, RECOGNIZED, PROCESSING, COMPLETED, FAILED, TIMEOUT, CANCELLED
}

enum class VoicePrintStatus {
    INCOMPLETE, TRAINING, READY, EXPIRED, LOCKED
}

enum class VoicePrivacyLevel {
    LOW, MEDIUM, HIGH, MAXIMUM
}

enum class WakeWordVerificationMethod {
    NONE, VOICE_PRINT, PASSWORD, BIOMETRIC, MULTI_FACTOR
}

// 语音处理事件
sealed class VoiceProcessingEvent {
    data class WakeWordDetected(
        val wakeWord: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    data class SpeechStarted(val timestamp: Long = System.currentTimeMillis()) : VoiceProcessingEvent()
    
    data class SpeechPartialResult(
        val text: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    data class SpeechRecognized(
        val result: SpeechRecognitionResult,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    data class IntentRecognized(
        val intent: VoiceCommandIntent,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    data class CommandExecuted(
        val command: VoiceCommand,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    data class Error(
        val errorType: VoiceErrorType,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    data class VoicePrintUpdated(
        val profile: VoiceProfile,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    data class AudioLevel(
        val level: Float,
        val isSpeaking: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : VoiceProcessingEvent()
    
    object ListeningStarted : VoiceProcessingEvent()
    object ListeningStopped : VoiceProcessingEvent()
    object ProcessingStarted : VoiceProcessingEvent()
    object ProcessingCompleted : VoiceProcessingEvent()
}

enum class VoiceErrorType {
    WAKE_WORD_DETECTION_FAILED,
    SPEECH_RECOGNITION_FAILED,
    INTENT_RECOGNITION_FAILED,
    COMMAND_EXECUTION_FAILED,
    VOICE_VERIFICATION_FAILED,
    PERMISSION_DENIED,
    AUDIO_INPUT_ERROR,
    NETWORK_ERROR,
    TIMEOUT,
    UNKNOWN
}

// 语音命令模板
data class VoiceCommandTemplate(
    val id: String,
    val name: String,
    val description: String,
    val triggerPhrases: List<String>,
    val intent: String,
    val actionType: String,
    val actionTemplate: Map<String, String>,
    val requiredPermissions: List<String>,
    val requiredEntities: List<String>,
    val examples: List<String>,
    val category: String,
    val isEnabled: Boolean = true,
    val priority: Int = 0
) {
    companion object {
        val DEFAULT_TEMPLATES = listOf(
            VoiceCommandTemplate(
                id = "send_message",
                name = "发送消息",
                description = "发送消息给联系人",
                triggerPhrases = listOf(
                    "发消息给{contact}说{message}",
                    "给{contact}发消息说{message}",
                    "告诉{contact}{message}"
                ),
                intent = "SEND_MESSAGE",
                actionType = "SEND_MESSAGE",
                actionTemplate = mapOf(
                    "contact" to "{contact}",
                    "message" to "{message}"
                ),
                requiredPermissions = listOf("CONTACTS", "MESSAGES"),
                requiredEntities = listOf("contact", "message"),
                examples = listOf(
                    "发消息给张三说晚上一起吃饭",
                    "给李四发消息说我马上到"
                ),
                category = "MESSAGING",
                priority = 100
            ),
            VoiceCommandTemplate(
                id = "call_contact",
                name = "呼叫联系人",
                description = "呼叫联系人电话",
                triggerPhrases = listOf(
                    "打电话给{contact}",
                    "呼叫{contact}",
                    "给{contact}打电话"
                ),
                intent = "CALL_CONTACT",
                actionType = "CALL_CONTACT",
                actionTemplate = mapOf(
                    "contact" to "{contact}"
                ),
                requiredPermissions = listOf("CONTACTS", "PHONE"),
                requiredEntities = listOf("contact"),
                examples = listOf(
                    "打电话给张三",
                    "呼叫李四"
                ),
                category = "COMMUNICATION",
                priority = 90
            ),
            VoiceCommandTemplate(
                id = "search_contact",
                name = "搜索联系人",
                description = "搜索联系人信息",
                triggerPhrases = listOf(
                    "查找{contact}",
                    "搜索{contact}",
                    "找到{contact}的联系方式"
                ),
                intent = "SEARCH_CONTACT",
                actionType = "SEARCH_CONTACT",
                actionTemplate = mapOf(
                    "contact" to "{contact}"
                ),
                requiredPermissions = listOf("CONTACTS"),
                requiredEntities = listOf("contact"),
                examples = listOf(
                    "查找张三",
                    "搜索李四的联系方式"
                ),
                category = "CONTACTS",
                priority = 80
            ),
            VoiceCommandTemplate(
                id = "emergency_alert",
                name = "紧急报警",
                description = "触发紧急报警系统",
                triggerPhrases = listOf(
                    "紧急报警",
                    "救命",
                    "帮帮我",
                    "SOS"
                ),
                intent = "EMERGENCY_ALERT",
                actionType = "EMERGENCY_ALERT",
                actionTemplate = emptyMap(),
                requiredPermissions = listOf("LOCATION", "EMERGENCY"),
                requiredEntities = emptyList(),
                examples = listOf(
                    "紧急报警",
                    "救命"
                ),
                category = "EMERGENCY",
                priority = 1000
            )
        )
    }
}