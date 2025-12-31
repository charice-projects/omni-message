package com.omnimsg.feature.voice

import android.content.Context
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import com.omnimsg.domain.models.voice.SpeechRecognitionResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognizer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SpeechRecognizer"
        private const val DEFAULT_LANGUAGE = "zh-CN"
        private const val TIMEOUT_MILLIS = 10000L // 10秒超时
    }
    
    data class Config(
        val language: String = DEFAULT_LANGUAGE,
        val enableOffline: Boolean = true,
        val maxResults: Int = 5,
        val partialResults: Boolean = true,
        val silenceTimeout: Long = 3000L, // 3秒静音超时
        val endpointerMode: EndpointerMode = EndpointerMode.AUTO
    )
    
    enum class EndpointerMode {
        AUTO, MANUAL, CONTINUOUS
    }
    
    sealed class RecognitionState {
        object Idle : RecognitionState()
        object Starting : RecognitionState()
        object Listening : RecognitionState()
        object Processing : RecognitionState()
        data class Recognizing(val partialText: String) : RecognitionState()
        data class Completed(val result: SpeechRecognitionResult) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
        object Timeout : RecognitionState()
        object Cancelled : RecognitionState()
    }
    
    sealed class RecognitionEvent {
        data class PartialResult(
            val text: String,
            val confidence: Float,
            val timestamp: Long = System.currentTimeMillis()
        ) : RecognitionEvent()
        
        data class Result(
            val recognitionResult: SpeechRecognitionResult,
            val timestamp: Long = System.currentTimeMillis()
        ) : RecognitionEvent()
        
        data class Error(
            val errorCode: Int,
            val message: String,
            val timestamp: Long = System.currentTimeMillis()
        ) : RecognitionEvent()
        
        object Started : RecognitionEvent()
        object Ended : RecognitionEvent()
        object Timeout : RecognitionEvent()
        
        data class AudioLevel(
            val level: Float,
            val isSpeaking: Boolean,
            val timestamp: Long = System.currentTimeMillis()
        ) : RecognitionEvent()
    }
    
    // 状态流
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState
    
    // 事件通道
    private val _recognitionEvents = Channel<RecognitionEvent>(Channel.BUFFERED)
    val recognitionEvents: Flow<RecognitionEvent> = _recognitionEvents.receiveAsFlow()
    
    private var androidSpeechRecognizer: AndroidSpeechRecognizer? = null
    private var recognitionJob: Job? = null
    private var timeoutJob: Job? = null
    
    private var config = Config()
    private var startTime = 0L
    
    /**
     * 初始化语音识别器
     */
    fun initialize(config: Config = Config()): Boolean {
        this.config = config
        
        return try {
            androidSpeechRecognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)
            // TODO: 设置监听器
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize speech recognizer", e)
            false
        }
    }
    
    /**
     * 开始语音识别
     */
    fun startRecognition(): Boolean {
        if (_recognitionState.value != RecognitionState.Idle) {
            Log.w(TAG, "Recognition already in progress")
            return false
        }
        
        return try {
            _recognitionState.value = RecognitionState.Starting
            
            // 启动超时监控
            startTimeoutMonitor()
            
            // 开始Android语音识别
            androidSpeechRecognizer?.startListening(createRecognitionIntent())
            
            _recognitionState.value = RecognitionState.Listening
            _recognitionEvents.trySend(RecognitionEvent.Started)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition", e)
            _recognitionState.value = RecognitionState.Error(e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopRecognition(): Boolean {
        return try {
            androidSpeechRecognizer?.stopListening()
            
            timeoutJob?.cancel()
            timeoutJob = null
            
            _recognitionState.value = RecognitionState.Processing
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recognition", e)
            false
        }
    }
    
    /**
     * 取消语音识别
     */
    fun cancelRecognition(): Boolean {
        return try {
            androidSpeechRecognizer?.cancel()
            
            timeoutJob?.cancel()
            timeoutJob = null
            
            _recognitionState.value = RecognitionState.Cancelled
            _recognitionEvents.trySend(RecognitionEvent.Ended)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recognition", e)
            false
        }
    }
    
    /**
     * 创建识别Intent
     */
    private fun createRecognitionIntent(): android.content.Intent {
        return android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, config.partialResults)
            putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, config.maxResults)
            
            if (config.enableOffline) {
                putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }
    
    /**
     * 启动超时监控
     */
    private fun startTimeoutMonitor() {
        timeoutJob?.cancel()
        
        timeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(TIMEOUT_MILLIS)
            
            if (_recognitionState.value == RecognitionState.Listening ||
                _recognitionState.value == RecognitionState.Starting) {
                _recognitionState.value = RecognitionState.Timeout
                _recognitionEvents.trySend(RecognitionEvent.Timeout)
                cancelRecognition()
            }
        }
    }
    
    /**
     * 处理识别结果
     */
    private fun handleRecognitionResult(results: List<String>, confidences: FloatArray?) {
        val mainText = results.firstOrNull() ?: ""
        val confidence = confidences?.firstOrNull() ?: 0f
        
        val recognitionResult = SpeechRecognitionResult(
            text = mainText,
            normalizedText = normalizeText(mainText),
            confidence = confidence,
            isFinal = true,
            alternatives = results.drop(1).mapIndexed { index, text ->
                SpeechRecognitionResult.Alternative(
                    text = text,
                    confidence = confidences?.getOrNull(index + 1) ?: 0f
                )
            }
        )
        
        _recognitionState.value = RecognitionState.Completed(recognitionResult)
        _recognitionEvents.trySend(RecognitionEvent.Result(recognitionResult))
        _recognitionEvents.trySend(RecognitionEvent.Ended)
    }
    
    /**
     * 处理部分结果
     */
    private fun handlePartialResult(text: String) {
        _recognitionState.value = RecognitionState.Recognizing(text)
        _recognitionEvents.trySend(
            RecognitionEvent.PartialResult(
                text = text,
                confidence = calculateConfidence(text)
            )
        )
    }
    
    /**
     * 处理错误
     */
    private fun handleError(errorCode: Int) {
        val errorMessage = when (errorCode) {
            AndroidSpeechRecognizer.ERROR_AUDIO -> "音频错误"
            AndroidSpeechRecognizer.ERROR_CLIENT -> "客户端错误"
            AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
            AndroidSpeechRecognizer.ERROR_NETWORK -> "网络错误"
            AndroidSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
            AndroidSpeechRecognizer.ERROR_NO_MATCH -> "无匹配结果"
            AndroidSpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
            AndroidSpeechRecognizer.ERROR_SERVER -> "服务器错误"
            AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
            else -> "未知错误"
        }
        
        _recognitionState.value = RecognitionState.Error(errorMessage)
        _recognitionEvents.trySend(
            RecognitionEvent.Error(errorCode, errorMessage)
        )
    }
    
    /**
     * 规范化文本
     */
    private fun normalizeText(text: String): String {
        var normalized = text.trim()
        
        // 移除多余空格
        normalized = normalized.replace("\\s+".toRegex(), " ")
        
        // 标准化标点符号
        normalized = normalized.replace("，", ",")
            .replace("。", ".")
            .replace("！", "!")
            .replace("？", "?")
            .replace("；", ";")
            .replace("：", ":")
            .replace("（", "(")
            .replace("）", ")")
            .replace("【", "[")
            .replace("】", "]")
            .replace("「", "\"")
            .replace("」", "\"")
            .replace("『", "'")
            .replace("』", "'")
        
        // 转换为小写（对于英文部分）
        normalized = normalized.lowercase(Locale.getDefault())
        
        return normalized
    }
    
    /**
     * 计算置信度
     */
    private fun calculateConfidence(text: String): Float {
        // 基于文本长度、清晰度等因素计算置信度
        var confidence = 0.5f
        
        // 长度因素
        val lengthFactor = text.length.coerceIn(0, 50).toFloat() / 50.0f
        confidence += lengthFactor * 0.2f
        
        // 词汇清晰度（简单实现）
        val wordCount = text.split("\\s+".toRegex()).size
        val clarityFactor = wordCount.coerceIn(0, 10).toFloat() / 10.0f
        confidence += clarityFactor * 0.3f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * 离线识别
     */
    suspend fun recognizeOffline(audioData: ByteArray): RecognitionResult = withContext(Dispatchers.Default) {
        return@withContext try {
            // TODO: 实现离线语音识别
            // 这里简化实现，实际应该使用本地模型
            
            val text = "离线识别结果"
            val confidence = 0.8f
            
            RecognitionResult.Success(
                SpeechRecognitionResult(
                    text = text,
                    normalizedText = normalizeText(text),
                    confidence = confidence
                )
            )
        } catch (e: Exception) {
            RecognitionResult.Error(e.message ?: "离线识别失败")
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        cancelRecognition()
        androidSpeechRecognizer?.destroy()
        androidSpeechRecognizer = null
    }
    
    sealed class RecognitionResult {
        data class Success(val result: SpeechRecognitionResult) : RecognitionResult()
        data class Error(val message: String) : RecognitionResult()
    }
    
    // Android SpeechRecognizer监听器
    private inner class RecognitionListener : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            Log.d(TAG, "Ready for speech")
        }
        
        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
            startTime = System.currentTimeMillis()
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            val level = (rmsdB + 10) / 20 // 转换为0-1范围
            _recognitionEvents.trySend(
                RecognitionEvent.AudioLevel(level, level > 0.1f)
            )
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            Log.d(TAG, "Buffer received")
        }
        
        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
            _recognitionState.value = RecognitionState.Processing
        }
        
        override fun onError(error: Int) {
            Log.e(TAG, "Recognition error: $error")
            handleError(error)
        }
        
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(AndroidSpeechRecognizer.CONFIDENCE_SCORES)
            
            if (matches.isNullOrEmpty()) {
                handleError(AndroidSpeechRecognizer.ERROR_NO_MATCH)
                return
            }
            
            handleRecognitionResult(matches, confidences)
        }
        
        override fun onPartialResults(partialResults: android.os.Bundle?) {
            if (!config.partialResults) return
            
            val matches = partialResults?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()
            
            if (!partialText.isNullOrBlank()) {
                handlePartialResult(partialText)
            }
        }
        
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {
            Log.d(TAG, "Recognition event: $eventType")
        }
    }
    
    init {
        initialize()
    }
}