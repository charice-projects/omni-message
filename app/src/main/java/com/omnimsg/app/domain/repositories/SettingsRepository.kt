// 📁 app/src/main/java/com/omnimsg/app/domain/repositories/SettingsRepository.kt
package com.omnimsg.app.domain.repositories

import com.omnimsg.app.ui.viewmodels.*

interface SettingsRepository {
    // 获取用户偏好设置
    suspend fun getUserPreferences(): UserPreferences
    
    // 外观设置
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateFontSize(fontSize: FontSize)
    suspend fun toggleSystemTheme(enabled: Boolean)
    suspend fun updateDarkMode(mode: DarkMode)
    
    // 通知设置
    suspend fun updateNotificationSettings(
        enabled: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        quietHoursEnabled: Boolean,
        quietStartTime: String,
        quietEndTime: String
    )
    
    // 隐私设置
    suspend fun updatePrivacySettings(
        analyticsEnabled: Boolean,
        crashReportsEnabled: Boolean,
        backupEnabled: Boolean,
        syncEnabled: Boolean
    )
    
    // 消息设置
    suspend fun updateMessageSettings(
        syncInterval: SyncInterval,
        mediaQuality: MediaQuality,
        autoDelete: Boolean,
        deleteDays: Int
    )
    
    // 存储管理
    suspend fun getStorageInfo(): StorageInfo
    suspend fun clearCache(): Boolean
    suspend fun clearMedia(): Boolean
    
    // 备份恢复
    suspend fun performBackup(): BackupResult
    suspend fun performRestore(): BackupResult
    suspend fun exportData(): ExportResult
}