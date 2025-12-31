// 📁 app/src/main/java/com/omnimsg/app/domain/usecases/voice/RecognizeSpeechUseCase.kt
package com.omnimsg.app.domain.usecases.voice

import com.omnimsg.app.data.repository.VoiceRepository
import javax.inject.Inject

class RecognizeSpeechUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository
) {
    suspend operator fun invoke(audioData: ByteArray): Result<RecognitionResult> {
        return try {
            val result = voiceRepository.recognizeSpeech(audioData)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val language: String,
    val duration: Long
)