// 📁 app/src/main/java/com/omnimsg/app/domain/repositories/EmergencyRepository.kt
package com.omnimsg.app.domain.repositories

import com.omnimsg.app.domain.models.AlertResult
import com.omnimsg.app.ui.viewmodels.EmergencyContact
import com.omnimsg.app.ui.viewmodels.EmergencyInfo
import kotlinx.coroutines.flow.Flow

interface EmergencyRepository {
    // 联系人管理
    suspend fun getEmergencyContacts(): List<EmergencyContact>
    suspend fun saveEmergencyContacts(contacts: List<EmergencyContact>)
    suspend fun addEmergencyContact(contact: EmergencyContact)
    suspend fun removeEmergencyContact(contactId: String)
    suspend fun updateEmergencyContact(contact: EmergencyContact)
    
    // 警报发送
    suspend fun sendEmergencyAlert(
        alertId: String,
        contact: EmergencyContact,
        message: String,
        info: EmergencyInfo,
        isTest: Boolean
    ): Result<Unit>
    
    suspend fun cancelAlert(alertId: String): Result<Unit>
    
    // 信息收集
    suspend fun collectEmergencyInfo(
        includeLocation: Boolean,
        includeAudio: Boolean,
        includePhotos: Boolean
    ): EmergencyInfo
    
    // 警报记录
    suspend fun getAlertHistory(): Flow<List<AlertResult>>
    suspend fun getAlertById(alertId: String): AlertResult?
    suspend fun saveAlertRecord(alert: AlertResult)
    suspend fun updateAlertStatus(alertId: String, status: String)
    
    // 设置管理
    suspend fun getEmergencySettings(): Map<String, Any>
    suspend fun saveEmergencySettings(settings: Map<String, Any>)
}