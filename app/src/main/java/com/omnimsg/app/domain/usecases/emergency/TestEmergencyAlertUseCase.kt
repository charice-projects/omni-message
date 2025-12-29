// 📁 app/src/main/java/com/omnimsg/app/domain/usecases/emergency/TestEmergencyAlertUseCase.kt
package com.omnimsg.app.domain.usecases.emergency

import com.omnimsg.app.data.repository.EmergencyRepository
import com.omnimsg.app.domain.models.AlertResult
import com.omnimsg.app.ui.viewmodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TestEmergencyAlertUseCase @Inject constructor(
    private val emergencyRepository: EmergencyRepository
) {
    suspend operator fun invoke(
        contacts: List<EmergencyContact>,
        includeLocation: Boolean,
        includeAudio: Boolean,
        includePhotos: Boolean
    ): Result<AlertResult> = withContext(Dispatchers.IO) {
        try {
            if (contacts.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("未设置紧急联系人")
                )
            }
            
            val alertId = "test_${System.currentTimeMillis()}"
            
            val alertInfo = emergencyRepository.collectEmergencyInfo(
                includeLocation = includeLocation,
                includeAudio = includeAudio,
                includePhotos = includePhotos
            )
            
            val testMessage = buildString {
                append("【测试警报】")
                append("\n这是紧急系统的测试警报，并非真实紧急情况")
                append("\n时间：${alertInfo.timestamp}")
                
                if (includeLocation) {
                    alertInfo.location?.let { location ->
                        append("\n测试位置：${location.coordinates.latitude}, ${location.coordinates.longitude}")
                    }
                }
                
                append("\n请忽略此消息，或回复确认收到")
            }
            
            val results = contacts.map { contact ->
                emergencyRepository.sendEmergencyAlert(
                    alertId = alertId,
                    contact = contact,
                    message = testMessage,
                    info = alertInfo,
                    isTest = true
                )
            }
            
            val successfulSends = results.count { it.isSuccess }
            val failedSends = results.count { it.isFailure }
            
            val alertResult = AlertResult(
                id = alertId,
                timestamp = System.currentTimeMillis(),
                triggerMethod = TriggerMethod.TEST,
                totalContacts = contacts.size,
                successfulSends = successfulSends,
                failedSends = failedSends,
                includeLocation = includeLocation,
                includeAudio = includeAudio,
                includePhotos = includePhotos,
                initialStatus = if (successfulSends > 0) "SENT" else "FAILED"
            )
            
            emergencyRepository.saveAlertRecord(alertResult)
            
            Result.success(alertResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
