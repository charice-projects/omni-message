// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/EmergencyViewModel.kt
package com.omnimsg.app.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnimsg.app.domain.usecases.emergency.*
import com.omnimsg.app.ui.events.UiEvent
import com.omnimsg.app.ui.states.EmergencyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val triggerEmergencyAlertUseCase: TriggerEmergencyAlertUseCase,
    private val cancelEmergencyAlertUseCase: CancelEmergencyAlertUseCase,
    private val updateEmergencyContactsUseCase: UpdateEmergencyContactsUseCase,
    private val testEmergencyAlertUseCase: TestEmergencyAlertUseCase
) : ViewModel() {

    // UI状态
    var state by mutableStateOf(EmergencyState())
        private set

    // 事件通道
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadEmergencySettings()
        loadEmergencyContacts()
        loadAlertHistory()
    }

    // 加载紧急设置
    private fun loadEmergencySettings() {
        viewModelScope.launch {
            // TODO: 从数据存储加载设置
            state = state.copy(
                isEmergencyEnabled = true,
                triggerMethods = setOf(
                    TriggerMethod.POWER_BUTTON_TRIPLE,
                    TriggerMethod.VOLUME_COMBO
                ),
                includeLocation = true,
                includeAudio = false,
                includePhotos = false,
                confirmationRequired = true,
                stealthMode = true,
                autoEscalate = true,
                escalationInterval = 300000L // 5分钟
            )
        }
    }

    // 加载紧急联系人
    private fun loadEmergencyContacts() {
        viewModelScope.launch {
            // TODO: 从数据库加载联系人
            state = state.copy(
                emergencyContacts = listOf(
                    EmergencyContact(
                        id = "1",
                        name = "张三",
                        phone = "+8613800138000",
                        relationship = "家人",
                        priority = 1,
                        canReceiveLocation = true,
                        canReceiveMedia = false
                    ),
                    EmergencyContact(
                        id = "2",
                        name = "李四",
                        phone = "+8613811381133",
                        relationship = "同事",
                        priority = 2,
                        canReceiveLocation = true,
                        canReceiveMedia = true
                    )
                )
            )
        }
    }

    // 加载警报历史
    private fun loadAlertHistory() {
        viewModelScope.launch {
            // TODO: 从数据库加载历史
            state = state.copy(
                alertHistory = listOf(
                    EmergencyAlert(
                        id = "alert_001",
                        timestamp = System.currentTimeMillis() - 86400000, // 昨天
                        triggerMethod = TriggerMethod.POWER_BUTTON_TRIPLE,
                        status = AlertStatus.RESPONDED,
                        recipientCount = 2,
                        respondedCount = 1,
                        locationIncluded = true,
                        mediaIncluded = false
                    )
                )
            )
        }
    }

    // 添加紧急联系人
    fun addEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                updateEmergencyContactsUseCase(
                    currentContacts = state.emergencyContacts,
                    newContact = contact,
                    operation = ContactOperation.ADD
                ).onSuccess { updatedContacts ->
                    state = state.copy(emergencyContacts = updatedContacts)
                    sendUiEvent(UiEvent.ShowSnackbar("已添加紧急联系人"))
                }.onFailure { error ->
                    sendUiEvent(UiEvent.ShowSnackbar("添加失败: ${error.message}"))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("添加联系人时出错"))
            }
        }
    }

    // 删除紧急联系人
    fun removeEmergencyContact(contactId: String) {
        viewModelScope.launch {
            val contactToRemove = state.emergencyContacts.find { it.id == contactId }
            if (contactToRemove != null) {
                try {
                    updateEmergencyContactsUseCase(
                        currentContacts = state.emergencyContacts,
                        contactToRemove = contactToRemove,
                        operation = ContactOperation.REMOVE
                    ).onSuccess { updatedContacts ->
                        state = state.copy(emergencyContacts = updatedContacts)
                        sendUiEvent(UiEvent.ShowSnackbar("已删除紧急联系人"))
                    }
                } catch (e: Exception) {
                    sendUiEvent(UiEvent.ShowSnackbar("删除失败: ${e.message}"))
                }
            }
        }
    }

    // 更新联系人优先级
    fun updateContactPriority(contactId: String, newPriority: Int) {
        viewModelScope.launch {
            state.emergencyContacts.find { it.id == contactId }?.let { contact ->
                val updatedContact = contact.copy(priority = newPriority)
                val updatedList = state.emergencyContacts.map {
                    if (it.id == contactId) updatedContact else it
                }
                state = state.copy(emergencyContacts = updatedList)
                
                // 持久化到数据库
                updateEmergencyContactsUseCase(
                    currentContacts = state.emergencyContacts,
                    updatedContact = updatedContact,
                    operation = ContactOperation.UPDATE
                )
            }
        }
    }

    // 更新触发方法设置
    fun updateTriggerMethods(methods: Set<TriggerMethod>) {
        state = state.copy(triggerMethods = methods)
        // TODO: 持久化到设置存储
    }

    // 更新位置共享设置
    fun updateLocationSharing(enabled: Boolean) {
        state = state.copy(includeLocation = enabled)
        // TODO: 持久化到设置存储
    }

    // 更新媒体共享设置
    fun updateMediaSharing(type: MediaType, enabled: Boolean) {
        when (type) {
            MediaType.AUDIO -> state = state.copy(includeAudio = enabled)
            MediaType.PHOTOS -> state = state.copy(includePhotos = enabled)
        }
        // TODO: 持久化到设置存储
    }

    // 更新隐身模式
    fun updateStealthMode(enabled: Boolean) {
        state = state.copy(stealthMode = enabled)
        // TODO: 持久化到设置存储
    }

    // 更新自动升级
    fun updateAutoEscalate(enabled: Boolean) {
        state = state.copy(autoEscalate = enabled)
        // TODO: 持久化到设置存储
    }

    // 测试紧急警报
    fun testEmergencyAlert() {
        viewModelScope.launch {
            try {
                testEmergencyAlertUseCase(
                    contacts = state.emergencyContacts,
                    includeLocation = state.includeLocation,
                    includeAudio = state.includeAudio,
                    includePhotos = state.includePhotos
                ).onSuccess { result ->
                    sendUiEvent(UiEvent.ShowSnackbar("测试警报已发送: ${result.successfulSends} 成功"))
                    
                    // 添加到历史记录
                    val newAlert = EmergencyAlert(
                        id = "test_${System.currentTimeMillis()}",
                        timestamp = System.currentTimeMillis(),
                        triggerMethod = TriggerMethod.TEST,
                        status = AlertStatus.SENT,
                        recipientCount = state.emergencyContacts.size,
                        respondedCount = 0,
                        locationIncluded = state.includeLocation,
                        mediaIncluded = state.includeAudio || state.includePhotos
                    )
                    state = state.copy(alertHistory = listOf(newAlert) + state.alertHistory)
                }.onFailure { error ->
                    sendUiEvent(UiEvent.ShowSnackbar("测试失败: ${error.message}"))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("测试过程中出错"))
            }
        }
    }

    // 触发真实紧急警报
    fun triggerEmergencyAlert() {
        viewModelScope.launch {
            try {
                triggerEmergencyAlertUseCase(
                    triggerMethod = TriggerMethod.MANUAL,
                    contacts = state.emergencyContacts,
                    includeLocation = state.includeLocation,
                    includeAudio = state.includeAudio,
                    includePhotos = state.includePhotos,
                    message = "紧急情况！我需要帮助！"
                ).onSuccess { alert ->
                    sendUiEvent(UiEvent.ShowSnackbar("紧急警报已发送"))
                    
                    // 添加到历史记录
                    state = state.copy(alertHistory = listOf(alert) + state.alertHistory)
                }.onFailure { error ->
                    sendUiEvent(UiEvent.ShowSnackbar("警报发送失败: ${error.message}"))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("触发警报时出错"))
            }
        }
    }

    // 取消紧急警报
    fun cancelEmergencyAlert(alertId: String) {
        viewModelScope.launch {
            try {
                cancelEmergencyAlertUseCase(alertId).onSuccess {
                    // 更新历史记录中的状态
                    val updatedHistory = state.alertHistory.map { alert ->
                        if (alert.id == alertId) alert.copy(status = AlertStatus.CANCELLED)
                        else alert
                    }
                    state = state.copy(alertHistory = updatedHistory)
                    sendUiEvent(UiEvent.ShowSnackbar("警报已取消"))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("取消失败: ${e.message}"))
            }
        }
    }

    // 发送UI事件
    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}

// 紧急联系人数据类
data class EmergencyContact(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val priority: Int = 1,
    val canReceiveLocation: Boolean = true,
    val canReceiveMedia: Boolean = false,
    val lastNotified: Long? = null,
    val isActive: Boolean = true
)

// 紧急警报数据类
data class EmergencyAlert(
    val id: String,
    val timestamp: Long,
    val triggerMethod: TriggerMethod,
    val status: AlertStatus,
    val recipientCount: Int,
    val respondedCount: Int,
    val locationIncluded: Boolean,
    val mediaIncluded: Boolean
)

// 触发方法枚举
enum class TriggerMethod {
    POWER_BUTTON_TRIPLE,    // 电源键三击
    VOLUME_COMBO,           // 音量键组合
    GESTURE,                // 手势
    VOICE_COMMAND,          // 语音命令
    MANUAL,                 // 手动触发
    TEST                    // 测试
}

// 警报状态枚举
enum class AlertStatus {
    SENT,                   // 已发送
    DELIVERED,              // 已送达
    RESPONDED,              // 已响应
    CANCELLED               // 已取消
}

// 媒体类型枚举
enum class MediaType {
    AUDIO,                  // 音频
    PHOTOS                  // 照片
}

// 联系人操作枚举
enum class ContactOperation {
    ADD,                    // 添加
    REMOVE,                 // 删除
    UPDATE                  // 更新
}