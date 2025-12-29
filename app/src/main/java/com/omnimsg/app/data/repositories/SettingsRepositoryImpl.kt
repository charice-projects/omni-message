// 📁 app/src/main/java/com/omnimsg/app/data/repositories/SettingsRepositoryImpl.kt
package com.omnimsg.app.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.omnimsg.app.data.local.database.daos.SettingsDao
import com.omnimsg.app.domain.repositories.SettingsRepository
import com.omnimsg.app.ui.viewmodels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl @Inject constructor(
    private val context: Context,
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    override suspend fun getUserPreferences(): UserPreferences {
        return context.settingsDataStore.data.map { preferences ->
            UserPreferences(
                theme = AppTheme.valueOf(
                    preferences[PreferencesKeys.THEME] ?: AppTheme.DEFAULT.name
                ),
                language = AppLanguage.valueOf(
                    preferences[PreferencesKeys.LANGUAGE] ?: AppLanguage.SYSTEM.name
                ),
                fontSize = FontSize.valueOf(
                    preferences[PreferencesKeys.FONT_SIZE] ?: FontSize.NORMAL.name
                ),
                useSystemTheme = preferences[PreferencesKeys.USE_SYSTEM_THEME] ?: true,
                darkMode = DarkMode.valueOf(
                    preferences[PreferencesKeys.DARK_MODE] ?: DarkMode.AUTO.name
                ),
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true,
                quietHoursEnabled = preferences[PreferencesKeys.QUIET_HOURS_ENABLED] ?: false,
                quietStartTime = preferences[PreferencesKeys.QUIET_START_TIME] ?: "22:00",
                quietEndTime = preferences[PreferencesKeys.QUIET_END_TIME] ?: "08:00",
                analyticsEnabled = preferences[PreferencesKeys.ANALYTICS_ENABLED] ?: false,
                crashReportsEnabled = preferences[PreferencesKeys.CRASH_REPORTS_ENABLED] ?: false,
                backupEnabled = preferences[PreferencesKeys.BACKUP_ENABLED] ?: true,
                syncEnabled = preferences[PreferencesKeys.SYNC_ENABLED] ?: false,
                messageSyncInterval = SyncInterval.valueOf(
                    preferences[PreferencesKeys.MESSAGE_SYNC_INTERVAL] ?: SyncInterval.MINUTES_15.name
                ),
                mediaDownloadQuality = MediaQuality.valueOf(
                    preferences[PreferencesKeys.MEDIA_DOWNLOAD_QUALITY] ?: MediaQuality.AUTO.name
                ),
                autoDeleteOldMessages = preferences[PreferencesKeys.AUTO_DELETE_OLD_MESSAGES] ?: false,
                autoDeleteDays = preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 30
            )
        }.first()
    }
    
    override suspend fun updateTheme(theme: AppTheme) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }
    
    override suspend fun updateLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language.name
        }
    }
    
    override suspend fun updateFontSize(fontSize: FontSize) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = fontSize.name
        }
    }
    
    override suspend fun toggleSystemTheme(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_SYSTEM_THEME] = enabled
        }
    }
    
    override suspend fun updateDarkMode(mode: DarkMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = mode.name
        }
    }
    
    override suspend fun updateNotificationSettings(
        enabled: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        quietHoursEnabled: Boolean,
        quietStartTime: String,
        quietEndTime: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
            preferences[PreferencesKeys.SOUND_ENABLED] = soundEnabled
            preferences[PreferencesKeys.VIBRATION_ENABLED] = vibrationEnabled
            preferences[PreferencesKeys.QUIET_HOURS_ENABLED] = quietHoursEnabled
            preferences[PreferencesKeys.QUIET_START_TIME] = quietStartTime
            preferences[PreferencesKeys.QUIET_END_TIME] = quietEndTime
        }
    }
    
    override suspend fun updatePrivacySettings(
        analyticsEnabled: Boolean,
        crashReportsEnabled: Boolean,
        backupEnabled: Boolean,
        syncEnabled: Boolean
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ANALYTICS_ENABLED] = analyticsEnabled
            preferences[PreferencesKeys.CRASH_REPORTS_ENABLED] = crashReportsEnabled
            preferences[PreferencesKeys.BACKUP_ENABLED] = backupEnabled
            preferences[PreferencesKeys.SYNC_ENABLED] = syncEnabled
        }
    }
    
    override suspend fun updateMessageSettings(
        syncInterval: SyncInterval,
        mediaQuality: MediaQuality,
        autoDelete: Boolean,
        deleteDays: Int
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.MESSAGE_SYNC_INTERVAL] = syncInterval.name
            preferences[PreferencesKeys.MEDIA_DOWNLOAD_QUALITY] = mediaQuality.name
            preferences[PreferencesKeys.AUTO_DELETE_OLD_MESSAGES] = autoDelete
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = deleteDays
        }
    }
    
    override suspend fun getStorageInfo(): StorageInfo {
        return try {
            val cacheDir = context.cacheDir
            val filesDir = context.filesDir
            val externalCacheDir = context.externalCacheDir
            
            // 计算缓存大小
            val cacheSize = calculateDirectorySize(cacheDir) +
                          calculateDirectorySize(externalCacheDir)
            
            // 计算文件存储大小（简化版）
            val messageStorage = 0L // TODO: 从数据库计算消息存储
            val mediaStorage = 0L // TODO: 计算媒体文件存储
            
            val usedStorage = cacheSize + messageStorage + mediaStorage
            val totalStorage = 0L // TODO: 获取设备总存储
            
            StorageInfo(
                total = totalStorage,
                used = usedStorage,
                messageStorage = messageStorage,
                mediaStorage = mediaStorage,
                cacheSize = cacheSize,
                lastBackupTime = getLastBackupTime()
            )
        } catch (e: Exception) {
            StorageInfo()
        }
    }
    
    override suspend fun clearCache(): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun clearMedia(): Boolean {
        return try {
            // TODO: 清理媒体文件目录
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun performBackup(): BackupResult {
        return try {
            // TODO: 实现备份逻辑
            kotlinx.coroutines.delay(2000) // 模拟备份过程
            BackupResult(success = true)
        } catch (e: Exception) {
            BackupResult(success = false, errorMessage = e.message)
        }
    }
    
    override suspend fun performRestore(): BackupResult {
        return try {
            // TODO: 实现恢复逻辑
            kotlinx.coroutines.delay(2000) // 模拟恢复过程
            BackupResult(success = true)
        } catch (e: Exception) {
            BackupResult(success = false, errorMessage = e.message)
        }
    }
    
    override suspend fun exportData(): ExportResult {
        return try {
            // TODO: 实现数据导出逻辑
            ExportResult(success = true, filePath = null)
        } catch (e: Exception) {
            ExportResult(success = false, errorMessage = e.message)
        }
    }
    
    private fun calculateDirectorySize(directory: File?): Long {
        if (directory == null || !directory.exists()) return 0L
        
        var size = 0L
        directory.walk().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
    
    private suspend fun getLastBackupTime(): Long? {
        return context.settingsDataStore.data.map { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIME]
        }.first()
    }
}

// DataStore 键定义
private object PreferencesKeys {
    val THEME = stringPreferencesKey("theme")
    val LANGUAGE = stringPreferencesKey("language")
    val FONT_SIZE = stringPreferencesKey("font_size")
    val USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_START_TIME = stringPreferencesKey("quiet_start_time")
    val QUIET_END_TIME = stringPreferencesKey("quiet_end_time")
    
    val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
    val CRASH_REPORTS_ENABLED = booleanPreferencesKey("crash_reports_enabled")
    val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    
    val MESSAGE_SYNC_INTERVAL = stringPreferencesKey("message_sync_interval")
    val MEDIA_DOWNLOAD_QUALITY = stringPreferencesKey("media_download_quality")
    val AUTO_DELETE_OLD_MESSAGES = booleanPreferencesKey("auto_delete_old_messages")
    val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
    
    val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
}

// 数据模型
data class UserPreferences(
    val theme: AppTheme = AppTheme.DEFAULT,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontSize: FontSize = FontSize.NORMAL,
    val useSystemTheme: Boolean = true,
    val darkMode: DarkMode = DarkMode.AUTO,
    
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartTime: String = "22:00",
    val quietEndTime: String = "08:00",
    
    val analyticsEnabled: Boolean = false,
    val crashReportsEnabled: Boolean = false,
    val backupEnabled: Boolean = true,
    val syncEnabled: Boolean = false,
    
    val messageSyncInterval: SyncInterval = SyncInterval.MINUTES_15,
    val mediaDownloadQuality: MediaQuality = MediaQuality.AUTO,
    val autoDeleteOldMessages: Boolean = false,
    val autoDeleteDays: Int = 30
)

data class StorageInfo(
    val total: Long = 0L,
    val used: Long = 0L,
    val messageStorage: Long = 0L,
    val mediaStorage: Long = 0L,
    val cacheSize: Long = 0L,
    val lastBackupTime: Long? = null
)

data class BackupResult(
    val success: Boolean,
    val errorMessage: String? = null
)

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val errorMessage: String? = null
)