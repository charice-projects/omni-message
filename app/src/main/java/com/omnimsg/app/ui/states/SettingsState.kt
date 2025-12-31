// 📁 app/src/main/java/com/omnimsg/app/ui/states/SettingsState.kt
package com.omnimsg.app.ui.states

import com.omnimsg.app.ui.viewmodels.*

data class SettingsState(
    // 用户信息
    val userName: String = "未登录",
    val userEmail: String = "",
    val userAvatar: String? = null,
    val isLoggedIn: Boolean = false,
    
    // 外观设置
    val theme: AppTheme = AppTheme.DEFAULT,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontSize: FontSize = FontSize.NORMAL,
    val useSystemTheme: Boolean = true,
    val darkMode: DarkMode = DarkMode.AUTO,
    
    // 通知设置
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartTime: String = "22:00",
    val quietEndTime: String = "08:00",
    
    // 隐私设置
    val analyticsEnabled: Boolean = false,
    val crashReportsEnabled: Boolean = false,
    val backupEnabled: Boolean = true,
    val syncEnabled: Boolean = false,
    
    // 消息设置
    val messageSyncInterval: SyncInterval = SyncInterval.MINUTES_15,
    val mediaDownloadQuality: MediaQuality = MediaQuality.AUTO,
    val autoDeleteOldMessages: Boolean = false,
    val autoDeleteDays: Int = 30,
    
    // 存储信息
    val totalStorage: Long = 0L,
    val usedStorage: Long = 0L,
    val messageStorage: Long = 0L,
    val mediaStorage: Long = 0L,
    val cacheSize: Long = 0L,
    val lastBackupTime: Long? = null,
    
    // 操作状态
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isExporting: Boolean = false,
    val isClearingCache: Boolean = false,
    
    // UI状态
    val selectedSection: SettingsSection = SettingsSection.GENERAL,
    val showThemePicker: Boolean = false,
    val showLanguagePicker: Boolean = false,
    val showFontSizePicker: Boolean = false,
    val showQuietHoursDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)

// 设置分类枚举
enum class SettingsSection {
    GENERAL,        // 通用设置
    APPEARANCE,     // 外观设置
    NOTIFICATIONS,  // 通知设置
    PRIVACY,        // 隐私设置
    MESSAGES,       // 消息设置
    STORAGE,        // 存储管理
    BACKUP,         // 备份恢复
    ABOUT,          // 关于应用
    ADVANCED        // 高级设置
}