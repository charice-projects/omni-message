// 📁 app/src/main/java/com/omnimsg/app/domain/usecases/emergency/TriggerEmergencyAlertUseCase.kt
package com.omnimsg.app.domain.usecases.emergency

import com.omnimsg.app.data.repository.EmergencyRepository
import com.omnimsg.app.domain.models.AlertResult
import com.omnimsg.app.ui.viewmodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TriggerEmergencyAlertUseCase @Inject constructor(
    private val emergencyRepository: EmergencyRepository
) {
    suspend operator fun invoke(
        triggerMethod: TriggerMethod,
        contacts: List<EmergencyContact>,
        includeLocation: Boolean,
        includeAudio: Boolean,
        includePhotos: Boolean,
        message: String? = null
    ): Result<AlertResult> = withContext(Dispatchers.IO) {
        try {
            // 验证紧急联系人列表
            if (contacts.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("未设置紧急联系人")
                )
            }
            
            // 生成警报ID
            val alertId = "alert_${System.currentTimeMillis()}"
            
            // 收集警报信息
            val alertInfo = emergencyRepository.collectEmergencyInfo(
                includeLocation = includeLocation,
                includeAudio = includeAudio,
                includePhotos = includePhotos
            )
            
            // 生成警报消息
            val alertMessage = message ?: generateEmergencyMessage(
                triggerMethod = triggerMethod,
                info = alertInfo
            )
            
            // 发送警报给所有联系人
            val results = contacts.map { contact ->
                emergencyRepository.sendEmergencyAlert(
                    alertId = alertId,
                    contact = contact,
                    message = alertMessage,
                    info = alertInfo,
                    isTest = false
                )
            }
            
            // 计算发送结果
            val successfulSends = results.count { it.isSuccess }
            val failedSends = results.count { it.isFailure }
            
            // 创建警报记录
            val alertResult = AlertResult(
                id = alertId,
                timestamp = System.currentTimeMillis(),
                triggerMethod = triggerMethod,
                totalContacts = contacts.size,
                successfulSends = successfulSends,
                failedSends = failedSends,
                includeLocation = includeLocation,
                includeAudio = includeAudio,
                includePhotos = includePhotos,
                initialStatus = if (successfulSends > 0) "SENT" else "FAILED"
            )
            
            // 保存到数据库
            emergencyRepository.saveAlertRecord(alertResult)
            
            Result.success(alertResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateEmergencyMessage(
        triggerMethod: TriggerMethod,
        info: EmergencyInfo
    ): String {
        val triggerDescription = when (triggerMethod) {
            TriggerMethod.POWER_BUTTON_TRIPLE -> "电源键紧急触发"
            TriggerMethod.VOLUME_COMBO -> "音量键紧急触发"
            TriggerMethod.GESTURE -> "手势紧急触发"
            TriggerMethod.VOICE_COMMAND -> "语音紧急触发"
            TriggerMethod.MANUAL -> "手动紧急触发"
            else -> "紧急触发"
        }
        
        return buildString {
            append("【紧急警报】")
            append("\n触发方式：$triggerDescription")
            append("\n时间：${info.timestamp}")
            
            info.location?.let { location ->
                append("\n位置：${location.coordinates.latitude}, ${location.coordinates.longitude}")
                location.address?.let { address ->
                    append("\n地址：${address.firstLine}")
                }
            }
            
            append("\n设备电量：${info.deviceInfo.batteryLevel}%")
            append("\n网络状态：${info.deviceInfo.networkStatus}")
            append("\n紧急联系人请尽快回应！")
        }
    }
}