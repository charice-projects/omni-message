package com.omnimsg.data.local.database.entities

import androidx.room.*
import com.omnimsg.domain.models.VoiceCommandStatus
import com.omnimsg.domain.models.VoiceCommandType
import java.util.*

@Entity(
    tableName = "voice_commands",
    indices = [
        Index(value = ["command_id"], unique = true),
        Index(value = ["user_id"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["is_favorite"])
    ]
)
data class VoiceCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "command_id")
    val commandId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "type")
    val type: VoiceCommandType = VoiceCommandType.CUSTOM,
    
    @ColumnInfo(name = "trigger_phrase")
    val triggerPhrase: String, // 触发短语
    
    @ColumnInfo(name = "voice_text")
    val voiceText: String, // 语音识别的原始文本
    
    @ColumnInfo(name = "normalized_text")
    val normalizedText: String, // 标准化后的文本
    
    @ColumnInfo(name = "intent")
    val intent: String, // 意图分类
    
    @ColumnInfo(name = "entities")
    val entities: Map<String, String>, // 提取的实体
    
    @ColumnInfo(name = "confidence")
    val confidence: Float, // 识别置信度
    
    @ColumnInfo(name = "action_type")
    val actionType: String, // 执行的动作类型
    
    @ColumnInfo(name = "action_data")
    val actionData: Map<String, String>, // 动作数据
    
    @ColumnInfo(name = "execution_result")
    val executionResult: String?, // 执行结果
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String?, // 错误信息
    
    @ColumnInfo(name = "status")
    val status: VoiceCommandStatus = VoiceCommandStatus.PENDING,
    
    @ColumnInfo(name = "response_text")
    val responseText: String?, // 语音响应文本
    
    @ColumnInfo(name = "response_audio_uri")
    val responseAudioUri: String?, // 语音响应音频URI
    
    @ColumnInfo(name = "context_data")
    val contextData: Map<String, String>, // 执行时的上下文数据
    
    @ColumnInfo(name = "device_info")
    val deviceInfo: Map<String, String>, // 设备信息
    
    @ColumnInfo(name = "audio_duration")
    val audioDuration: Long = 0, // 音频时长(ms)
    
    @ColumnInfo(name = "audio_quality")
    val audioQuality: Float = 0f, // 音频质量评分
    
    @ColumnInfo(name = "background_noise_level")
    val backgroundNoiseLevel: Float = 0f, // 背景噪音水平
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false, // 是否收藏
    
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(), // 标签
    
    @ColumnInfo(name = "privacy_level")
    val privacyLevel: Int = 0, // 隐私级别
    
    @ColumnInfo(name = "model_version")
    val modelVersion: String = "1.0.0", // 模型版本
    
    @ColumnInfo(name = "executed_at")
    val executedAt: Long?, // 执行时间
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isSuccessful: Boolean
        get() = status == VoiceCommandStatus.COMPLETED
    
    val hasError: Boolean
        get() = status == VoiceCommandStatus.FAILED
    
    val canRetry: Boolean
        get() = status == VoiceCommandStatus.FAILED || status == VoiceCommandStatus.TIMEOUT
    
    val processingTime: Long
        get() {
            return if (executedAt != null && createdAt != 0L) {
                executedAt - createdAt
            } else {
                0L
            }
        }
}

@Entity(
    tableName = "voice_profiles",
    indices = [
        Index(value = ["profile_id"], unique = true),
        Index(value = ["user_id"]),
        Index(value = ["is_active"]),
        Index(value = ["voice_print_status"])
    ]
)
data class VoiceProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "profile_id")
    val profileId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "profile_name")
    val profileName: String,
    
    @ColumnInfo(name = "voice_features")
    val voiceFeatures: ByteArray?, // 语音特征向量（加密存储）
    
    @ColumnInfo(name = "voice_print_hash")
    val voicePrintHash: String?, // 声纹哈希
    
    @ColumnInfo(name = "voice_print_status")
    val voicePrintStatus: VoicePrintStatus = VoicePrintStatus.INCOMPLETE,
    
    @ColumnInfo(name = "training_samples")
    val trainingSamples: Int = 0, // 训练样本数量
    
    @ColumnInfo(name = "accuracy_score")
    val accuracyScore: Float = 0f, // 识别准确率
    
    @ColumnInfo(name = "verification_threshold")
    val verificationThreshold: Float = 0.85f, // 验证阈值
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    
    @ColumnInfo(name = "preferred_language")
    val preferredLanguage: String = "zh-CN",
    
    @ColumnInfo(name = "accent_type")
    val accentType: String = "standard",
    
    @ColumnInfo(name = "speech_rate")
    val speechRate: Float = 1.0f, // 语速系数
    
    @ColumnInfo(name = "pitch_level")
    val pitchLevel: Float = 0f, // 音调水平
    
    @ColumnInfo(name = "volume_level")
    val volumeLevel: Float = 0f, // 音量水平
    
    @ColumnInfo(name = "voice_characteristics")
    val voiceCharacteristics: Map<String, Float>, // 声音特征
    
    @ColumnInfo(name = "privacy_settings")
    val privacySettings: Map<String, Boolean>, // 隐私设置
    
    @ColumnInfo(name = "model_data")
    val modelData: ByteArray?, // 个性化模型数据（加密）
    
    @ColumnInfo(name = "last_trained_at")
    val lastTrainedAt: Long?, // 最后训练时间
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    enum class VoicePrintStatus {
        INCOMPLETE, TRAINING, READY, EXPIRED, LOCKED
    }
}

@Entity(
    tableName = "wake_word_detections",
    indices = [
        Index(value = ["detection_id"], unique = true),
        Index(value = ["user_id"]),
        Index(value = ["wake_word"]),
        Index(value = ["detected_at"]),
        Index(value = ["confidence"])
    ]
)
data class WakeWordDetectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "detection_id")
    val detectionId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "wake_word")
    val wakeWord: String = "熙熙", // 唤醒词
    
    @ColumnInfo(name = "detected_at")
    val detectedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "confidence")
    val confidence: Float, // 检测置信度
    
    @ColumnInfo(name = "audio_sample_uri")
    val audioSampleUri: String?, // 音频样本URI
    
    @ColumnInfo(name = "audio_features")
    val audioFeatures: ByteArray?, // 音频特征
    
    @ColumnInfo(name = "background_noise_level")
    val backgroundNoiseLevel: Float = 0f,
    
    @ColumnInfo(name = "signal_to_noise_ratio")
    val signalToNoiseRatio: Float = 0f,
    
    @ColumnInfo(name = "device_info")
    val deviceInfo: Map<String, String>,
    
    @ColumnInfo(name = "location_data")
    val locationData: Map<String, String>?,
    
    @ColumnInfo(name = "context_data")
    val contextData: Map<String, String>,
    
    @ColumnInfo(name = "model_version")
    val modelVersion: String,
    
    @ColumnInfo(name = "verification_method")
    val verificationMethod: String = "VOICE_PRINT", // 验证方法
    
    @ColumnInfo(name = "verification_score")
    val verificationScore: Float?,
    
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,
    
    @ColumnInfo(name = "triggered_action")
    val triggeredAction: String?, // 触发的动作
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)