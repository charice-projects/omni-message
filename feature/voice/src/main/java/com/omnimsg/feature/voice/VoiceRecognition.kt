// 📁 feature/voice/VoiceRecognition.kt
package com.omnimsg.feature.voice

import android.content.Context
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class VoiceRecognition @Inject constructor(
    private val context: Context
) {
    
    sealed class RecognitionResult {
        data class Success(
            val text: String,
            val confidence: Float,
            val alternatives: List<String> = emptyList()
        ) : RecognitionResult()
        
        data class Partial(
            val text: String
        ) : RecognitionResult()
        
        object NoMatch : RecognitionResult()
        data class Error(
            val errorCode: Int,
            val message: String
        ) : RecognitionResult()
    }
    
    interface VoiceRecognitionListener {
        fun onResult(result: RecognitionResult)
        fun onError(error: String)
        fun onStatusChanged(isListening: Boolean)
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val listeners = mutableListOf<VoiceRecognitionListener>()
    
    /**
     * 初始化语音识别
     */
    fun initialize(): Boolean {
        return try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            true
        } catch (e: Exception) {
            logger.e("VoiceRecognition", "初始化语音识别失败", e)
            false
        }
    }
    
    /**
     * 开始语音识别
     */
    suspend fun startRecognition(
        language: String = "zh-CN",
        partialResults: Boolean = true
    ): Flow<RecognitionResult> = callbackFlow {
        if (!initialize()) {
            trySend(RecognitionResult.Error(-1, "语音识别初始化失败"))
            close()
            return@callbackFlow
        }
        
        val recognizer = speechRecognizer ?: return@callbackFlow
        
        // 设置识别监听器
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle) {
                logger.d("VoiceRecognition", "准备就绪，可以开始说话")
                isListening = true
                notifyStatusChanged(true)
            }
            
            override fun onBeginningOfSpeech() {
                logger.d("VoiceRecognition", "检测到语音开始")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // 音量变化，可用于可视化
            }
            
            override fun onBufferReceived(buffer: ByteArray) {
                // 音频缓冲区接收
            }
            
            override fun onEndOfSpeech() {
                logger.d("VoiceRecognition", "语音结束")
                isListening = false
                notifyStatusChanged(false)
            }
            
            override fun onError(error: Int) {
                isListening = false
                notifyStatusChanged(false)
                
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "没有匹配的结果"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                    else -> "未知错误: $error"
                }
                
                logger.e("VoiceRecognition", "语音识别错误: $errorMessage")
                notifyError(errorMessage)
                
                trySend(RecognitionResult.Error(error, errorMessage))
            }
            
            override fun onResults(results: android.os.Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    val confidence = confidences?.getOrNull(0) ?: 0.5f
                    val alternatives = matches.drop(1)
                    
                    logger.d("VoiceRecognition", "识别结果: $bestMatch (置信度: $confidence)")
                    
                    val recognitionResult = RecognitionResult.Success(
                        text = bestMatch,
                        confidence = confidence,
                        alternatives = alternatives
                    )
                    
                    trySend(recognitionResult)
                    notifyResult(recognitionResult)
                } else {
                    trySend(RecognitionResult.NoMatch)
                }
            }
            
            override fun onPartialResults(partialResults: android.os.Bundle) {
                if (partialResults) {
                    val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { partialText ->
                        logger.d("VoiceRecognition", "部分结果: $partialText")
                        trySend(RecognitionResult.Partial(partialText))
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: android.os.Bundle) {
                // 事件回调
            }
        }
        
        recognizer.setRecognitionListener(recognitionListener)
        
        // 设置识别意图
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResults)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        }
        
        // 开始识别
        recognizer.startListening(intent)
        
        awaitClose {
            stopRecognition()
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopRecognition() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        isListening = false
        notifyStatusChanged(false)
    }
    
    /**
     * 单次语音识别
     */
    suspend fun recognizeOneShot(
        language: String = "zh-CN",
        timeoutMillis: Long = 10000
    ): RecognitionResult {
        return suspendCancellableCoroutine { continuation ->
            if (!initialize()) {
                continuation.resume(RecognitionResult.Error(-1, "语音识别初始化失败"))
                return@suspendCancellableCoroutine
            }
            
            val recognizer = speechRecognizer!!
            var timeoutJob: Job? = null
            
            val recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle) {
                    logger.d("VoiceRecognition", "准备就绪，可以开始说话")
                    isListening = true
                    
                    // 设置超时
                    timeoutJob = CoroutineScope(Dispatchers.IO).launch {
                        delay(timeoutMillis)
                        if (isListening) {
                            onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        }
                    }
                }
                
                override fun onBeginningOfSpeech() {
                    logger.d("VoiceRecognition", "检测到语音开始")
                }
                
                override fun onEndOfSpeech() {
                    logger.d("VoiceRecognition", "语音结束")
                    isListening = false
                    timeoutJob?.cancel()
                }
                
                override fun onError(error: Int) {
                    isListening = false
                    timeoutJob?.cancel()
                    
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "没有匹配的结果"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "说话超时"
                        else -> "未知错误: $error"
                    }
                    
                    logger.e("VoiceRecognition", "单次识别错误: $errorMessage")
                    
                    if (!continuation.isCancelled) {
                        continuation.resume(RecognitionResult.Error(error, errorMessage))
                    }
                    
                    cleanup()
                }
                
                override fun onResults(results: android.os.Bundle) {
                    isListening = false
                    timeoutJob?.cancel()
                    
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidences = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    
                    if (!matches.isNullOrEmpty()) {
                        val bestMatch = matches[0]
                        val confidence = confidences?.getOrNull(0) ?: 0.5f
                        
                        logger.d("VoiceRecognition", "单次识别结果: $bestMatch (置信度: $confidence)")
                        
                        if (!continuation.isCancelled) {
                            continuation.resume(
                                RecognitionResult.Success(
                                    text = bestMatch,
                                    confidence = confidence,
                                    alternatives = matches.drop(1)
                                )
                            )
                        }
                    } else {
                        if (!continuation.isCancelled) {
                            continuation.resume(RecognitionResult.NoMatch)
                        }
                    }
                    
                    cleanup()
                }
                
                override fun onPartialResults(partialResults: android.os.Bundle) {
                    // 单次识别不使用部分结果
                }
                
                override fun onEvent(eventType: Int, params: android.os.Bundle) {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray) {}
            }
            
            recognizer.setRecognitionListener(recognitionListener)
            
            // 设置识别意图
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            }
            
            // 开始识别
            recognizer.startListening(intent)
            
            // 设置取消回调
            continuation.invokeOnCancellation {
                logger.d("VoiceRecognition", "单次识别被取消")
                recognizer.cancel()
                timeoutJob?.cancel()
                cleanup()
            }
        }
    }
    
    /**
     * 识别音频文件
     */
    suspend fun recognizeAudioFile(
        audioFilePath: String,
        language: String = "zh-CN"
    ): RecognitionResult {
        return suspendCancellableCoroutine { continuation ->
            if (!initialize()) {
                continuation.resume(RecognitionResult.Error(-1, "语音识别初始化失败"))
                return@suspendCancellableCoroutine
            }
            
            val recognizer = speechRecognizer!!
            
            val recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle) {
                    logger.d("VoiceRecognition", "准备识别音频文件")
                }
                
                override fun onBeginningOfSpeech() {
                    logger.d("VoiceRecognition", "开始识别音频文件")
                }
                
                override fun onEndOfSpeech() {
                    logger.d("VoiceRecognition", "音频文件识别结束")
                }
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音频文件错误"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "没有匹配的结果"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        else -> "未知错误: $error"
                    }
                    
                    logger.e("VoiceRecognition", "音频文件识别错误: $errorMessage")
                    
                    if (!continuation.isCancelled) {
                        continuation.resume(RecognitionResult.Error(error, errorMessage))
                    }
                    
                    cleanup()
                }
                
                override fun onResults(results: android.os.Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidences = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    
                    if (!matches.isNullOrEmpty()) {
                        val bestMatch = matches[0]
                        val confidence = confidences?.getOrNull(0) ?: 0.5f
                        
                        logger.d("VoiceRecognition", "音频文件识别结果: $bestMatch (置信度: $confidence)")
                        
                        if (!continuation.isCancelled) {
                            continuation.resume(
                                RecognitionResult.Success(
                                    text = bestMatch,
                                    confidence = confidence,
                                    alternatives = matches.drop(1)
                                )
                            )
                        }
                    } else {
                        if (!continuation.isCancelled) {
                            continuation.resume(RecognitionResult.NoMatch)
                        }
                    }
                    
                    cleanup()
                }
                
                override fun onPartialResults(partialResults: android.os.Bundle) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle) {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray) {}
            }
            
            recognizer.setRecognitionListener(recognitionListener)
            
            // 设置识别意图（使用音频文件）
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                
                // 设置音频文件路径
                putExtra("android.speech.extra.AUDIO_FILE_PATH", audioFilePath)
            }
            
            // 开始识别
            recognizer.startListening(intent)
            
            // 设置取消回调
            continuation.invokeOnCancellation {
                logger.d("VoiceRecognition", "音频文件识别被取消")
                recognizer.cancel()
                cleanup()
            }
        }
    }
    
    /**
     * 获取支持的语言
     */
    fun getSupportedLanguages(): List<Locale> {
        return try {
            val supported = SpeechRecognizer.getOnDeviceSpeechRecognizer(context)
                ?.getSupportedLanguages()
                ?: emptySet()
            
            supported.map { Locale.forLanguageTag(it) }
                .sortedBy { it.displayName }
        } catch (e: Exception) {
            logger.e("VoiceRecognition", "获取支持语言失败", e)
            listOf(Locale.CHINESE, Locale.ENGLISH, Locale("zh", "CN"))
        }
    }
    
    /**
     * 检查设备是否支持语音识别
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * 获取当前状态
     */
    fun getStatus(): VoiceRecognitionStatus {
        return VoiceRecognitionStatus(
            isInitialized = speechRecognizer != null,
            isListening = isListening,
            supportedLanguages = getSupportedLanguages()
        )
    }
    
    /**
     * 添加监听器
     */
    fun addListener(listener: VoiceRecognitionListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除监听器
     */
    fun removeListener(listener: VoiceRecognitionListener) {
        listeners.remove(listener)
    }
    
    /**
     * 通知结果
     */
    private fun notifyResult(result: RecognitionResult) {
        listeners.forEach { it.onResult(result) }
    }
    
    /**
     * 通知错误
     */
    private fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }
    
    /**
     * 通知状态变化
     */
    private fun notifyStatusChanged(isListening: Boolean) {
        listeners.forEach { it.onStatusChanged(isListening) }
    }
    
    /**
     * 清理资源
     */
    private fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
    
    /**
     * 销毁资源
     */
    fun destroy() {
        stopRecognition()
        cleanup()
    }
}

// 数据类
data class VoiceRecognitionStatus(
    val isInitialized: Boolean,
    val isListening: Boolean,
    val supportedLanguages: List<Locale>
)