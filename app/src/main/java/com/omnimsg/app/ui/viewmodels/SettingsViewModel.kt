// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/SettingsViewModel.kt
package com.omnimsg.app.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnimsg.app.data.repository.SettingsRepository
import com.omnimsg.app.data.repository.UserRepository
import com.omnimsg.app.ui.events.UiEvent
import com.omnimsg.app.ui.states.SettingsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI状态
    var state by mutableStateOf(SettingsState())
        private set

    // 事件通道
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // 用户配置文件
    private var userProfile by mutableStateOf<UserProfile?>(null)

    init {
        loadAllSettings()
    }

    // 加载所有设置
    private fun loadAllSettings() {
        viewModelScope.launch {
            // 并行加载所有设置
            val profileDeferred = launch { loadUserProfile() }
            val preferencesDeferred = launch { loadPreferences() }
            val storageDeferred = launch { loadStorageInfo() }
            
            profileDeferred.join()
            preferencesDeferred.join()
            storageDeferred.join()
        }
    }

    // 加载用户资料
    private suspend fun loadUserProfile() {
        try {
            userProfile = userRepository.getUserProfile()
            state = state.copy(
                userName = userProfile?.name ?: "未登录",
                userEmail = userProfile?.email ?: "",
                userAvatar = userProfile?.avatarUrl,
                isLoggedIn = userProfile != null
            )
        } catch (e: Exception) {
            sendUiEvent(UiEvent.ShowSnackbar("加载用户资料失败"))
        }
    }

    // 加载偏好设置
    private suspend fun loadPreferences() {
        try {
            val preferences = settingsRepository.getUserPreferences()
            
            state = state.copy(
                theme = preferences.theme,
                language = preferences.language,
                fontSize = preferences.fontSize,
                useSystemTheme = preferences.useSystemTheme,
                darkMode = preferences.darkMode,
                
                // 通知设置
                notificationsEnabled = preferences.notificationsEnabled,
                soundEnabled = preferences.soundEnabled,
                vibrationEnabled = preferences.vibrationEnabled,
                quietHoursEnabled = preferences.quietHoursEnabled,
                quietStartTime = preferences.quietStartTime,
                quietEndTime = preferences.quietEndTime,
                
                // 隐私设置
                analyticsEnabled = preferences.analyticsEnabled,
                crashReportsEnabled = preferences.crashReportsEnabled,
                backupEnabled = preferences.backupEnabled,
                syncEnabled = preferences.syncEnabled,
                
                // 消息设置
                messageSyncInterval = preferences.messageSyncInterval,
                mediaDownloadQuality = preferences.mediaDownloadQuality,
                autoDeleteOldMessages = preferences.autoDeleteOldMessages,
                autoDeleteDays = preferences.autoDeleteDays
            )
        } catch (e: Exception) {
            sendUiEvent(UiEvent.ShowSnackbar("加载设置失败"))
        }
    }

    // 加载存储信息
    private suspend fun loadStorageInfo() {
        try {
            val storageInfo = settingsRepository.getStorageInfo()
            
            state = state.copy(
                totalStorage = storageInfo.total,
                usedStorage = storageInfo.used,
                messageStorage = storageInfo.messageStorage,
                mediaStorage = storageInfo.mediaStorage,
                cacheSize = storageInfo.cacheSize,
                lastBackupTime = storageInfo.lastBackupTime
            )
        } catch (e: Exception) {
            sendUiEvent(UiEvent.ShowSnackbar("加载存储信息失败"))
        }
    }

    // 更新主题设置
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                settingsRepository.updateTheme(theme)
                state = state.copy(theme = theme)
                sendUiEvent(UiEvent.ShowSnackbar("主题已更新"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("主题更新失败"))
            }
        }
    }

    // 更新语言设置
    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            try {
                settingsRepository.updateLanguage(language)
                state = state.copy(language = language)
                sendUiEvent(UiEvent.ShowSnackbar("语言设置已更新"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("语言更新失败"))
            }
        }
    }

    // 更新字体大小
    fun updateFontSize(fontSize: FontSize) {
        viewModelScope.launch {
            try {
                settingsRepository.updateFontSize(fontSize)
                state = state.copy(fontSize = fontSize)
                sendUiEvent(UiEvent.ShowSnackbar("字体大小已更新"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("字体大小更新失败"))
            }
        }
    }

    // 切换系统主题跟随
    fun toggleSystemTheme() {
        val newValue = !state.useSystemTheme
        viewModelScope.launch {
            try {
                settingsRepository.toggleSystemTheme(newValue)
                state = state.copy(useSystemTheme = newValue)
                val message = if (newValue) "已启用系统主题跟随" else "已禁用系统主题跟随"
                sendUiEvent(UiEvent.ShowSnackbar(message))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("系统主题设置更新失败"))
            }
        }
    }

    // 切换黑暗模式
    fun toggleDarkMode(mode: DarkMode) {
        viewModelScope.launch {
            try {
                settingsRepository.updateDarkMode(mode)
                state = state.copy(darkMode = mode)
                sendUiEvent(UiEvent.ShowSnackbar("黑暗模式已更新"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("黑暗模式更新失败"))
            }
        }
    }

    // 更新通知设置
    fun updateNotificationSettings(
        enabled: Boolean? = null,
        soundEnabled: Boolean? = null,
        vibrationEnabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietStartTime: String? = null,
        quietEndTime: String? = null
    ) {
        viewModelScope.launch {
            try {
                val newState = state.copy(
                    notificationsEnabled = enabled ?: state.notificationsEnabled,
                    soundEnabled = soundEnabled ?: state.soundEnabled,
                    vibrationEnabled = vibrationEnabled ?: state.vibrationEnabled,
                    quietHoursEnabled = quietHoursEnabled ?: state.quietHoursEnabled,
                    quietStartTime = quietStartTime ?: state.quietStartTime,
                    quietEndTime = quietEndTime ?: state.quietEndTime
                )
                
                settingsRepository.updateNotificationSettings(
                    enabled = newState.notificationsEnabled,
                    soundEnabled = newState.soundEnabled,
                    vibrationEnabled = newState.vibrationEnabled,
                    quietHoursEnabled = newState.quietHoursEnabled,
                    quietStartTime = newState.quietStartTime,
                    quietEndTime = newState.quietEndTime
                )
                
                state = newState
                sendUiEvent(UiEvent.ShowSnackbar("通知设置已保存"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("通知设置保存失败"))
            }
        }
    }

    // 更新隐私设置
    fun updatePrivacySettings(
        analyticsEnabled: Boolean? = null,
        crashReportsEnabled: Boolean? = null,
        backupEnabled: Boolean? = null,
        syncEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                val newState = state.copy(
                    analyticsEnabled = analyticsEnabled ?: state.analyticsEnabled,
                    crashReportsEnabled = crashReportsEnabled ?: state.crashReportsEnabled,
                    backupEnabled = backupEnabled ?: state.backupEnabled,
                    syncEnabled = syncEnabled ?: state.syncEnabled
                )
                
                settingsRepository.updatePrivacySettings(
                    analyticsEnabled = newState.analyticsEnabled,
                    crashReportsEnabled = newState.crashReportsEnabled,
                    backupEnabled = newState.backupEnabled,
                    syncEnabled = newState.syncEnabled
                )
                
                state = newState
                sendUiEvent(UiEvent.ShowSnackbar("隐私设置已保存"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("隐私设置保存失败"))
            }
        }
    }

    // 更新消息设置
    fun updateMessageSettings(
        syncInterval: SyncInterval? = null,
        mediaQuality: MediaQuality? = null,
        autoDelete: Boolean? = null,
        deleteDays: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val newState = state.copy(
                    messageSyncInterval = syncInterval ?: state.messageSyncInterval,
                    mediaDownloadQuality = mediaQuality ?: state.mediaDownloadQuality,
                    autoDeleteOldMessages = autoDelete ?: state.autoDeleteOldMessages,
                    autoDeleteDays = deleteDays ?: state.autoDeleteDays
                )
                
                settingsRepository.updateMessageSettings(
                    syncInterval = newState.messageSyncInterval,
                    mediaQuality = newState.mediaDownloadQuality,
                    autoDelete = newState.autoDeleteOldMessages,
                    deleteDays = newState.autoDeleteDays
                )
                
                state = newState
                sendUiEvent(UiEvent.ShowSnackbar("消息设置已保存"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("消息设置保存失败"))
            }
        }
    }

    // 清理缓存
    fun clearCache() {
        viewModelScope.launch {
            try {
                settingsRepository.clearCache()
                loadStorageInfo() // 重新加载存储信息
                sendUiEvent(UiEvent.ShowSnackbar("缓存已清理"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("缓存清理失败"))
            }
        }
    }

    // 清理媒体文件
    fun clearMedia() {
        viewModelScope.launch {
            try {
                settingsRepository.clearMedia()
                loadStorageInfo() // 重新加载存储信息
                sendUiEvent(UiEvent.ShowSnackbar("媒体文件已清理"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("媒体文件清理失败"))
            }
        }
    }

    // 执行备份
    fun performBackup() {
        viewModelScope.launch {
            try {
                state = state.copy(isBackingUp = true)
                val result = settingsRepository.performBackup()
                
                if (result.success) {
                    state = state.copy(lastBackupTime = System.currentTimeMillis())
                    sendUiEvent(UiEvent.ShowSnackbar("备份成功"))
                } else {
                    sendUiEvent(UiEvent.ShowSnackbar("备份失败: ${result.errorMessage}"))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("备份过程中出错"))
            } finally {
                state = state.copy(isBackingUp = false)
            }
        }
    }

    // 执行恢复
    fun performRestore() {
        viewModelScope.launch {
            try {
                state = state.copy(isRestoring = true)
                val result = settingsRepository.performRestore()
                
                if (result.success) {
                    // 重新加载所有设置
                    loadAllSettings()
                    sendUiEvent(UiEvent.ShowSnackbar("恢复成功"))
                } else {
                    sendUiEvent(UiEvent.ShowSnackbar("恢复失败: ${result.errorMessage}"))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("恢复过程中出错"))
            } finally {
                state = state.copy(isRestoring = false)
            }
        }
    }

    // 导出数据
    fun exportData() {
        viewModelScope.launch {
            try {
                state = state.copy(isExporting = true)
                val result = settingsRepository.exportData()
                
                if (result.success) {
                    sendUiEvent(UiEvent.ShowSnackbar("数据导出成功"))
                } else {
                    sendUiEvent(UiEvent.ShowSnackbar("数据导出失败"))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("数据导出过程中出错"))
            } finally {
                state = state.copy(isExporting = false)
            }
        }
    }

    // 登录/登出
    fun toggleLogin() {
        viewModelScope.launch {
            try {
                if (state.isLoggedIn) {
                    userRepository.logout()
                    state = state.copy(
                        isLoggedIn = false,
                        userName = "未登录",
                        userEmail = "",
                        userAvatar = null
                    )
                    sendUiEvent(UiEvent.ShowSnackbar("已退出登录"))
                } else {
                    // TODO: 实现登录逻辑
                    sendUiEvent(UiEvent.Navigate(AppDestinations.Login))
                }
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("登录状态切换失败"))
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

// 用户资料数据类
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?,
    val createdAt: Long,
    val lastLogin: Long,
    val isVerified: Boolean
)

// 应用主题枚举
enum class AppTheme {
    DEFAULT,    // 默认主题
    BLUE,       // 蓝色主题
    GREEN,      // 绿色主题
    PURPLE,     // 紫色主题
    DARK_BLUE,  // 深蓝主题
    MATERIAL,   // Material 主题
    CUSTOM      // 自定义主题
}

// 语言枚举
enum class AppLanguage {
    SYSTEM,     // 跟随系统
    ZH_CN,      // 简体中文
    ZH_TW,      // 繁体中文
    EN_US,      // 英语（美国）
    EN_UK,      // 英语（英国）
    JA,         // 日语
    KO,         // 韩语
    FR,         // 法语
    DE,         // 德语
    ES,         // 西班牙语
    RU,         // 俄语
    AR          // 阿拉伯语
}

// 字体大小枚举
enum class FontSize {
    SMALL,      // 小
    NORMAL,     // 正常
    LARGE,      // 大
    XLARGE,     // 特大
    XXLARGE     // 巨大（无障碍）
}

// 黑暗模式枚举
enum class DarkMode {
    LIGHT,      // 浅色模式
    DARK,       // 深色模式
    AUTO        // 自动（根据时间）
}

// 同步间隔枚举
enum class SyncInterval {
    REAL_TIME,  // 实时
    MINUTES_5,  // 每5分钟
    MINUTES_15, // 每15分钟
    MINUTES_30, // 每30分钟
    HOUR_1,     // 每小时
    MANUAL      // 手动
}

// 媒体质量枚举
enum class MediaQuality {
    ORIGINAL,   // 原画质
    HIGH,       // 高画质
    MEDIUM,     // 中等画质
    LOW,        // 低画质
    AUTO        // 自动（根据网络）
}