// 📁 feature/voice/VoiceFeedbackSystem.kt
package com.omnimsg.feature.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceFeedbackSystem @Inject constructor(
    private val context: Context
) {
    
    sealed class FeedbackType {
        object Success : FeedbackType()
        object Error : FeedbackType()
        object Warning : FeedbackType()
        object Information : FeedbackType()
        object Confirmation : FeedbackType()
        object Emergency : FeedbackType()
    }
    
    data class FeedbackRequest(
        val id: String = UUID.randomUUID().toString(),
        val message: String,
        val type: FeedbackType = FeedbackType.Information,
        val priority: Int = 5, // 1-10, 10最高
        val playSound: Boolean = true,
        val useTTS: Boolean = true,
        val requireAck: Boolean = false,
        val timeoutMs: Long = 10000
    )
    
    data class FeedbackResult(
        val requestId: String,
        val isDelivered: Boolean,
        val error: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    interface FeedbackListener {
        fun onFeedbackStarted(request: FeedbackRequest)
        fun onFeedbackCompleted(request: FeedbackRequest, result: FeedbackResult)
        fun onFeedbackError(request: FeedbackRequest, error: String)
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false
    private val feedbackQueue = mutableListOf<FeedbackRequest>()
    private var currentFeedback: FeedbackRequest? = null
    private var isSpeaking = false
    private val listeners = mutableListOf<FeedbackListener>()
    private var initializationJob: Job? = null
    
    /**
     * 初始化语音反馈系统
     */
    fun initialize() {
        if (isInitialized) return
        
        initializationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                textToSpeech = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        // 设置中文语言
                        val result = textToSpeech?.setLanguage(Locale.CHINA)
                        
                        if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            logger.e("VoiceFeedbackSystem", "中文TTS不支持")
                        } else {
                            isInitialized = true
                            logger.i("VoiceFeedbackSystem", "TTS初始化成功")
                            
                            // 开始处理队列中的反馈
                            processFeedbackQueue()
                        }
                    } else {
                        logger.e("VoiceFeedbackSystem", "TTS初始化失败")
                    }
                }
                
                // 设置TTS监听器
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        currentFeedback?.let { request ->
                            listeners.forEach { it.onFeedbackStarted(request) }
                        }
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        currentFeedback?.let { request ->
                            val result = FeedbackResult(
                                requestId = request.id,
                                isDelivered = true
                            )
                            listeners.forEach { it.onFeedbackCompleted(request, result) }
                        }
                        currentFeedback = null
                        
                        // 处理下一个反馈
                        processFeedbackQueue()
                    }
                    
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        currentFeedback?.let { request ->
                            listeners.forEach { 
                                it.onFeedbackError(request, "TTS播放错误") 
                            }
                        }
                        currentFeedback = null
                        
                        // 尝试处理下一个反馈
                        processFeedbackQueue()
                    }
                })
                
            } catch (e: Exception) {
                logger.e("VoiceFeedbackSystem", "初始化失败", e)
            }
        }
    }
    
    /**
     * 提供语音反馈
     */
    fun giveFeedback(
        message: String,
        context: VoiceFeedbackContext? = null,
        useTTS: Boolean = true,
        playSound: Boolean = true
    ): FeedbackResult {
        val feedbackType = context?.let {
            when {
                it.commandType is VoiceCommandCenter.CommandType.EmergencyAlert -> FeedbackType.Emergency
                !it.isSuccess -> FeedbackType.Error
                it.urgency == UrgencyLevel.HIGH -> FeedbackType.Warning
                else -> FeedbackType.Information
            }
        } ?: FeedbackType.Information
        
        val priority = context?.let {
            when (it.urgency) {
                UrgencyLevel.CRITICAL -> 10
                UrgencyLevel.HIGH -> 8
                UrgencyLevel.NORMAL -> 5
                UrgencyLevel.LOW -> 3
            }
        } ?: 5
        
        val request = FeedbackRequest(
            message = message,
            type = feedbackType,
            priority = priority,
            playSound = playSound,
            useTTS = useTTS
        )
        
        return queueFeedback(request)
    }
    
    /**
     * 队列反馈请求
     */
    fun queueFeedback(request: FeedbackRequest): FeedbackResult {
        // 插入队列，按优先级排序
        val insertIndex = feedbackQueue.indexOfFirst { it.priority < request.priority }
        if (insertIndex == -1) {
            feedbackQueue.add(request)
        } else {
            feedbackQueue.add(insertIndex, request)
        }
        
        logger.d("VoiceFeedbackSystem", "反馈已加入队列: ${request.message} (优先级: ${request.priority})")
        
        // 如果当前没有在播放反馈，立即开始处理
        if (!isSpeaking && currentFeedback == null) {
            processFeedbackQueue()
        }
        
        return FeedbackResult(
            requestId = request.id,
            isDelivered = false
        )
    }
    
    /**
     * 处理反馈队列
     */
    private fun processFeedbackQueue() {
        if (isSpeaking || feedbackQueue.isEmpty()) return
        
        if (!isInitialized) {
            // 如果TTS未初始化，先初始化
            initialize()
            return
        }
        
        val nextFeedback = feedbackQueue.removeFirst()
        currentFeedback = nextFeedback
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deliverFeedback(nextFeedback)
            } catch (e: Exception) {
                logger.e("VoiceFeedbackSystem", "处理反馈失败", e)
                currentFeedback = null
                processFeedbackQueue()
            }
        }
    }
    
    /**
     * 发送反馈
     */
    private suspend fun deliverFeedback(request: FeedbackRequest) {
        try {
            // 播放提示音（如果需要）
            if (request.playSound) {
                playFeedbackSound(request.type)
            }
            
            // 使用TTS朗读（如果需要）
            if (request.useTTS && isInitialized) {
                speakText(request.message, request.id)
            } else if (request.useTTS) {
                // TTS不可用，使用其他方式
                logger.w("VoiceFeedbackSystem", "TTS不可用，无法朗读: ${request.message}")
                currentFeedback = null
                processFeedbackQueue()
            } else {
                // 不需要TTS，直接完成
                currentFeedback = null
                processFeedbackQueue()
            }
            
        } catch (e: Exception) {
            logger.e("VoiceFeedbackSystem", "发送反馈失败", e)
            throw e
        }
    }
    
    /**
     * 播放反馈提示音
     */
    private fun playFeedbackSound(type: FeedbackType) {
        val soundResource = when (type) {
            FeedbackType.Success -> R.raw.success_sound
            FeedbackType.Error -> R.raw.error_sound
            FeedbackType.Warning -> R.raw.warning_sound
            FeedbackType.Confirmation -> R.raw.confirmation_sound
            FeedbackType.Emergency -> R.raw.emergency_sound
            else -> R.raw.notification_sound
        }
        
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                
                val afd = context.resources.openRawResourceFd(soundResource)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                
                setOnCompletionListener {
                    it.release()
                }
                
                setOnErrorListener { mp, what, extra ->
                    logger.e("VoiceFeedbackSystem", "播放提示音失败: what=$what, extra=$extra")
                    mp.release()
                    true
                }
                
                prepare()
                start()
            }
        } catch (e: Exception) {
            logger.e("VoiceFeedbackSystem", "播放提示音失败", e)
        }
    }
    
    /**
     * 朗读文本
     */
    private fun speakText(text: String, utteranceId: String) {
        try {
            textToSpeech?.let { tts ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                } else {
                    @Suppress("DEPRECATION")
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                }
            }
        } catch (e: Exception) {
            logger.e("VoiceFeedbackSystem", "TTS朗读失败", e)
            isSpeaking = false
            currentFeedback = null
            processFeedbackQueue()
        }
    }
    
    /**
     * 停止当前反馈
     */
    fun stopCurrentFeedback() {
        textToSpeech?.stop()
        mediaPlayer?.stop()
        isSpeaking = false
        currentFeedback = null
    }
    
    /**
     * 清除反馈队列
     */
    fun clearQueue() {
        feedbackQueue.clear()
        stopCurrentFeedback()
    }
    
    /**
     * 设置语音参数
     */
    fun setVoiceParameters(
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        volume: Float = 1.0f
    ) {
        textToSpeech?.let { tts ->
            tts.setSpeechRate(speechRate)
            tts.setPitch(pitch)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.setVolume(volume)
            }
        }
    }
    
    /**
     * 获取可用语音列表
     */
    fun getAvailableVoices(): List<VoiceInfo> {
        return textToSpeech?.let { tts ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.voices.map { voice ->
                    VoiceInfo(
                        name = voice.name,
                        locale = voice.locale,
                        quality = when {
                            voice.quality >= TextToSpeech.Engine.QUALITY_HIGH -> "高质量"
                            voice.quality >= TextToSpeech.Engine.QUALITY_NORMAL -> "正常"
                            else -> "低质量"
                        }
                    )
                }
            } else {
                emptyList()
            }
        } ?: emptyList()
    }
    
    /**
     * 选择语音
     */
    fun selectVoice(voiceName: String): Boolean {
        return textToSpeech?.let { tts ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val voice = tts.voices.find { it.name == voiceName }
                if (voice != null) {
                    tts.voice = voice
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } ?: false
    }
    
    /**
     * 添加监听器
     */
    fun addListener(listener: FeedbackListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除监听器
     */
    fun removeListener(listener: FeedbackListener) {
        listeners.remove(listener)
    }
    
    /**
     * 获取当前状态
     */
    fun getStatus(): VoiceFeedbackStatus {
        return VoiceFeedbackStatus(
            isInitialized = isInitialized,
            isSpeaking = isSpeaking,
            queueSize = feedbackQueue.size,
            currentFeedback = currentFeedback
        )
    }
    
    /**
     * 销毁资源
     */
    fun destroy() {
        initializationJob?.cancel()
        stopCurrentFeedback()
        textToSpeech?.shutdown()
        mediaPlayer?.release()
        feedbackQueue.clear()
        listeners.clear()
    }
}

// 数据类
data class VoiceInfo(
    val name: String,
    val locale: Locale,
    val quality: String
)

data class VoiceFeedbackStatus(
    val isInitialized: Boolean,
    val isSpeaking: Boolean,
    val queueSize: Int,
    val currentFeedback: FeedbackRequest?
)