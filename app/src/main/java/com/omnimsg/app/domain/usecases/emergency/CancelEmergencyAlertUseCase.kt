// 📁 app/src/main/java/com/omnimsg/app/domain/usecases/emergency/CancelEmergencyAlertUseCase.kt
package com.omnimsg.app.domain.usecases.emergency

import com.omnimsg.app.data.repository.EmergencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CancelEmergencyAlertUseCase @Inject constructor(
    private val emergencyRepository: EmergencyRepository
) {
    suspend operator fun invoke(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 检查警报是否存在且可取消
            val alert = emergencyRepository.getAlertById(alertId)
            if (alert == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("警报不存在")
                )
            }
            
            if (alert.initialStatus !in listOf("SENT", "DELIVERED")) {
                return@withContext Result.failure(
                    IllegalStateException("警报无法取消")
                )
            }
            
            // 通知所有联系人取消警报
            emergencyRepository.cancelAlert(alertId)
            
            // 更新数据库状态
            emergencyRepository.updateAlertStatus(alertId, "CANCELLED")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}