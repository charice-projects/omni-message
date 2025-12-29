package com.omnimsg.feature.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.omnimsg.domain.models.voice.WakeWordDetection
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeWordDetector @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        private const val WAKE_WORD = "熙熙"
        private const val WAKE_WORD_ENGLISH = "xixi"
        
        // 音频配置
        private const val SAMPLE_RATE = 16000 // 16kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 1024
        
        // 模型配置
        private const val MODEL_INPUT_SIZE = 16000 // 1秒音频
        private const val MODEL_OUTPUT_SIZE = 2 // 唤醒词概率和非唤醒词概率
        private const val DETECTION_THRESHOLD = 0.85f
        private const val COOLDOWN_PERIOD = 2000L // 2秒冷却期
        
        // 特征提取配置
        private const val MFCC_FEATURE_SIZE = 40
        private const val FFT_SIZE = 512
        private const val MEL_BANDS = 40
    }
    
    data class Config(
        val sensitivity: Float = DETECTION_THRESHOLD,
        val requireVoiceVerification: Boolean = true,
        val maxBackgroundNoiseLevel: Float = 0.3f,
        val enableContinuousListening: Boolean = false,
        val timeoutSeconds: Int = 10,
        val wakeWord: String = WAKE_WORD,
        val modelPath: String = "models/wakeword_xixi.tflite"
    )
    
    sealed class DetectionResult {
        data class WakeWordDetected(
            val wakeWord: String,
            val confidence: Float,
            val timestamp: Long,
            val audioFeatures: ByteArray? = null
        ) : DetectionResult()
        
        data class Error(val error: String) : DetectionResult()
        object ListeningStarted : DetectionResult()
        object ListeningStopped : DetectionResult()
        data class AudioLevel(val level: Float, val isSpeaking: Boolean) : DetectionResult()
    }
    
    sealed class ListeningState {
        object Stopped : ListeningState()
        object Starting : ListeningState()
        object Listening : ListeningState()
        object Processing : ListeningState()
        object Stopping : ListeningState()
        data class Error(val message: String) : ListeningState()
    }
    
    // 状态流
    private val _listeningState = MutableStateFlow<ListeningState>(ListeningState.Stopped)
    val listeningState: StateFlow<ListeningState> = _listeningState
    
    // 事件通道
    private val _detectionEvents = Channel<DetectionResult>(Channel.BUFFERED)
    val detectionEvents: Flow<DetectionResult> = _detectionEvents.receiveAsFlow()
    
    private var audioRecord: AudioRecord? = null
    private var interpreter: Interpreter? = null
    private var detectionJob: Job? = null
    private var lastDetectionTime = 0L
    
    private var config = Config()
    
    /**
     * 初始化唤醒词检测器
     */
    suspend fun initialize(config: Config = Config()): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            this@WakeWordDetector.config = config
            
            // 加载TensorFlow Lite模型
            loadModel(config.modelPath)
            
            // 检查音频权限和设备
            if (!checkAudioPermissions()) {
                throw IllegalStateException("Audio permissions not granted")
            }
            
            if (!checkAudioDevice()) {
                throw IllegalStateException("Audio device not available")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize wake word detector", e)
            false
        }
    }
    
    /**
     * 开始监听唤醒词
     */
    fun startListening(): Boolean {
        if (_listeningState.value != ListeningState.Stopped) {
            Log.w(TAG, "Already listening or in process")
            return false
        }
        
        return try {
            _listeningState.value = ListeningState.Starting
            
            // 初始化音频录制
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
            )
            
            audioRecord?.startRecording()
            
            // 开始检测协程
            detectionJob = CoroutineScope(Dispatchers.IO).launch {
                performDetection()
            }
            
            _listeningState.value = ListeningState.Listening
            _detectionEvents.trySend(DetectionResult.ListeningStarted)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _listeningState.value = ListeningState.Error(e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 停止监听唤醒词
     */
    suspend fun stopListening(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            _listeningState.value = ListeningState.Stopping
            
            // 停止检测协程
            detectionJob?.cancel()
            detectionJob = null
            
            // 停止音频录制
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            _listeningState.value = ListeningState.Stopped
            _detectionEvents.trySend(DetectionResult.ListeningStopped)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
            _listeningState.value = ListeningState.Error(e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 执行唤醒词检测
     */
    private suspend fun performDetection() {
        val audioBuffer = ShortArray(BUFFER_SIZE)
        val featureBuffer = mutableListOf<Short>()
        
        while (isActive && _listeningState.value == ListeningState.Listening) {
            try {
                val bytesRead = audioRecord?.read(audioBuffer, 0, BUFFER_SIZE) ?: 0
                
                if (bytesRead > 0) {
                    // 计算音频电平
                    val audioLevel = calculateAudioLevel(audioBuffer, bytesRead)
                    val isSpeaking = audioLevel > config.maxBackgroundNoiseLevel
                    
                    // 发送音频电平事件
                    _detectionEvents.trySend(
                        DetectionResult.AudioLevel(audioLevel, isSpeaking)
                    )
                    
                    // 收集音频特征
                    featureBuffer.addAll(audioBuffer.take(bytesRead))
                    
                    // 如果收集到足够的数据，进行检测
                    if (featureBuffer.size >= MODEL_INPUT_SIZE) {
                        processAudioBuffer(featureBuffer.take(MODEL_INPUT_SIZE).toShortArray())
                        featureBuffer.clear()
                    }
                    
                    // 限制缓冲区大小
                    if (featureBuffer.size > MODEL_INPUT_SIZE * 2) {
                        featureBuffer.removeFirst(featureBuffer.size - MODEL_INPUT_SIZE)
                    }
                }
                
                // 短暂延迟以避免CPU过载
                delay(10)
            } catch (e: Exception) {
                Log.e(TAG, "Error in detection loop", e)
                if (e is CancellationException) {
                    break
                }
            }
        }
    }
    
    /**
     * 处理音频缓冲区
     */
    private suspend fun processAudioBuffer(audioBuffer: ShortArray) {
        if (!isActive) return
        
        // 检查冷却期
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDetectionTime < COOLDOWN_PERIOD) {
            return
        }
        
        try {
            // 提取音频特征
            val features = extractFeatures(audioBuffer)
            
            // 运行模型推理
            val confidence = runModelInference(features)
            
            // 检查是否检测到唤醒词
            if (confidence >= config.sensitivity) {
                lastDetectionTime = currentTime
                
                // 发送检测事件
                _detectionEvents.trySend(
                    DetectionResult.WakeWordDetected(
                        wakeWord = config.wakeWord,
                        confidence = confidence,
                        timestamp = currentTime,
                        audioFeatures = features
                    )
                )
                
                // 如果需要验证，进行声纹验证
                if (config.requireVoiceVerification) {
                    // TODO: 实现声纹验证
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio buffer", e)
        }
    }
    
    /**
     * 提取音频特征
     */
    private fun extractFeatures(audioBuffer: ShortArray): ByteArray {
        // 将PCM数据转换为浮点数
        val floatBuffer = FloatArray(audioBuffer.size)
        for (i in audioBuffer.indices) {
            floatBuffer[i] = audioBuffer[i] / 32768.0f
        }
        
        // TODO: 实现MFCC特征提取
        // 这里简化实现，实际应该计算MFCC特征
        return floatBuffer.map { it.toByte() }.toByteArray()
    }
    
    /**
     * 运行模型推理
     */
    private fun runModelInference(features: ByteArray): Float {
        val interpreter = interpreter ?: return 0f
        
        return try {
            // 准备输入缓冲区
            val inputBuffer = ByteBuffer.allocateDirect(features.size)
                .order(ByteOrder.nativeOrder())
            inputBuffer.put(features)
            
            // 准备输出缓冲区
            val outputBuffer = Array(1) { FloatArray(MODEL_OUTPUT_SIZE) }
            
            // 运行推理
            interpreter.run(inputBuffer, outputBuffer)
            
            // 获取唤醒词概率
            outputBuffer[0][0]
        } catch (e: Exception) {
            Log.e(TAG, "Model inference failed", e)
            0f
        }
    }
    
    /**
     * 加载TensorFlow Lite模型
     */
    private fun loadModel(modelPath: String) {
        return try {
            // 从assets加载模型
            val modelFile = File(context.filesDir, modelPath)
            if (!modelFile.exists()) {
                // TODO: 从assets复制模型文件
                Log.w(TAG, "Model file not found: $modelPath")
                return
            }
            
            val options = Interpreter.Options()
            options.setNumThreads(4)
            
            interpreter = Interpreter(modelFile, options)
            Log.d(TAG, "Model loaded successfully: $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            interpreter = null
        }
    }
    
    /**
     * 训练个性化唤醒词
     */
    suspend fun trainPersonalizedWakeWord(
        audioSamples: List<ByteArray>,
        wakeWord: String = WAKE_WORD
    ): TrainingResult = withContext(Dispatchers.Default) {
        return@withContext try {
            if (audioSamples.size < 3) {
                return@withContext TrainingResult.Error("至少需要3个音频样本进行训练")
            }
            
            // 提取特征
            val features = audioSamples.map { extractFeatures(it.toShortArray()) }
            
            // TODO: 实现个性化模型训练
            // 这里简化实现，实际应该使用迁移学习或重新训练
            
            TrainingResult.Success(0.85f)
        } catch (e: Exception) {
            TrainingResult.Error(e.message ?: "训练失败")
        }
    }
    
    /**
     * 验证唤醒词
     */
    fun validateWakeWord(
        audioBuffer: ShortArray,
        expectedWord: String = WAKE_WORD
    ): ValidationResult {
        return try {
            // 提取特征
            val features = extractFeatures(audioBuffer)
            
            // 运行模型推理
            val confidence = runModelInference(features)
            
            // 计算匹配分数
            val matchScore = if (confidence >= config.sensitivity) {
                confidence
            } else {
                0f
            }
            
            ValidationResult(
                matchScore = matchScore,
                isValid = matchScore >= config.sensitivity
            )
        } catch (e: Exception) {
            ValidationResult(
                matchScore = 0f,
                isValid = false,
                error = e.message
            )
        }
    }
    
    /**
     * 计算音频电平
     */
    private fun calculateAudioLevel(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / length)
        return (rms / 32768.0).toFloat()
    }
    
    /**
     * 检查音频权限
     */
    private fun checkAudioPermissions(): Boolean {
        // TODO: 检查录音权限
        return true
    }
    
    /**
     * 检查音频设备
     */
    private fun checkAudioDevice(): Boolean {
        return try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            minBufferSize > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        interpreter?.close()
        interpreter = null
        
        audioRecord?.release()
        audioRecord = null
        
        detectionJob?.cancel()
        detectionJob = null
    }
    
    data class ValidationResult(
        val matchScore: Float,
        val isValid: Boolean,
        val error: String? = null
    )
    
    sealed class TrainingResult {
        data class Success(val accuracy: Float) : TrainingResult()
        data class Error(val message: String) : TrainingResult()
    }
    
    private fun sqrt(value: Double): Double {
        return kotlin.math.sqrt(value)
    }
    
    private fun ByteArray.toShortArray(): ShortArray {
        val shortArray = ShortArray(this.size / 2)
        ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
        return shortArray
    }
}