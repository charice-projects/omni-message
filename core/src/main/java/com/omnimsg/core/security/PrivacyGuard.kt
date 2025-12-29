// 📁 core/security/PrivacyGuard.kt
package com.omnimsg.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyGuard @Inject constructor(
    private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    
    data class PrivacyAuditLog(
        val id: Long = 0,
        val timestamp: Long = System.currentTimeMillis(),
        val eventType: PrivacyEventType,
        val userId: String? = null,
        val resourceType: ResourceType,
        val resourceId: String? = null,
        val action: PrivacyAction,
        val result: PrivacyActionResult,
        val details: Map<String, String> = emptyMap(),
        val ipAddress: String? = null,
        val deviceId: String? = null
    )
    
    data class PrivacySettings(
        val dataRetentionDays: Int = 90,
        val autoDeleteOldData: Boolean = true,
        val allowAnalytics: Boolean = false,
        val allowCrashReports: Boolean = true,
        val allowPersonalizedAds: Boolean = false,
        val encryptAllData: Boolean = true,
        val useBiometricAuth: Boolean = true,
        val hideSensitiveContent: Boolean = true,
        val blurSensitiveImages: Boolean = true,
        val anonymizeLocation: Boolean = true,
        val locationPrecision: LocationPrecision = LocationPrecision.CITY,
        val deleteMetadata: Boolean = true,
        val thirdPartySharing: Boolean = false
    )
    
    enum class PrivacyEventType {
        DATA_ACCESS,      // 数据访问
        DATA_MODIFICATION, // 数据修改
        DATA_DELETION,    // 数据删除
        PERMISSION_GRANT, // 权限授予
        PERMISSION_DENY,  // 权限拒绝
        LOGIN,           // 登录
        LOGOUT,          // 登出
        ENCRYPTION,      // 加密操作
        DECRYPTION,      // 解密操作
        EXPORT,          // 数据导出
        IMPORT,          // 数据导入
        BACKUP,          // 备份
        RESTORE,         // 恢复
        SHARING,         // 数据共享
        ANONYMIZATION    // 匿名化
    }
    
    enum class ResourceType {
        CONTACT,          // 联系人
        MESSAGE,          // 消息
        CONVERSATION,     // 对话
        FILE,             // 文件
        LOCATION,         // 位置
        AUDIO,            // 音频
        IMAGE,            // 图片
        VIDEO,            // 视频
        NOTE,             // 笔记
        CALENDAR,         // 日历
        DEVICE_INFO,      // 设备信息
        NETWORK_INFO,     // 网络信息
        APP_USAGE,        // 应用使用情况
        SENSOR_DATA       // 传感器数据
    }
    
    enum class PrivacyAction {
        CREATE,          // 创建
        READ,            // 读取
        UPDATE,          // 更新
        DELETE,          // 删除
        EXPORT,          // 导出
        IMPORT,          // 导入
        SHARE,           // 分享
        BACKUP,          // 备份
        RESTORE,         // 恢复
        ENCRYPT,         // 加密
        DECRYPT,         // 解密
        ANONYMIZE,       // 匿名化
        MASK             // 掩码
    }
    
    enum class PrivacyActionResult {
        SUCCESS,         // 成功
        FAILED,          // 失败
        DENIED,          // 拒绝
        PARTIAL_SUCCESS, // 部分成功
        PENDING,         // 待处理
        CANCELLED        // 已取消
    }
    
    enum class LocationPrecision {
        EXACT,           // 精确位置
        STREET,          // 街道级别
        CITY,            // 城市级别
        REGION,          // 区域级别
        COUNTRY          // 国家级别
    }
    
    private val auditLogs = mutableListOf<PrivacyAuditLog>()
    private var privacySettings: PrivacySettings = PrivacySettings()
    
    /**
     * 初始化隐私保护器
     */
    suspend fun initialize(settings: PrivacySettings): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                privacySettings = settings
                
                // 加载现有的审计日志
                loadAuditLogs()
                
                // 清理过期的审计日志
                cleanupOldAuditLogs()
                
                logger.i("PrivacyGuard", "隐私保护器初始化成功")
                true
            } catch (e: Exception) {
                logger.e("PrivacyGuard", "隐私保护器初始化失败", e)
                false
            }
        }
    }
    
    /**
     * 检查权限
     */
    fun checkPermission(permission: String): Boolean {
        return try {
            ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            logger.e("PrivacyGuard", "检查权限失败: $permission", e)
            false
        }
    }
    
    /**
     * 检查敏感权限组
     */
    fun checkSensitivePermissions(): Map<String, Boolean> {
        val sensitivePermissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.READ_CALL_LOG
        )
        
        return sensitivePermissions.associateWith { checkPermission(it) }
    }
    
    /**
     * 记录隐私事件
     */
    fun logPrivacyEvent(
        eventType: PrivacyEventType,
        resourceType: ResourceType,
        action: PrivacyAction,
        result: PrivacyActionResult,
        userId: String? = null,
        resourceId: String? = null,
        details: Map<String, String> = emptyMap()
    ) {
        try {
            val log = PrivacyAuditLog(
                eventType = eventType,
                userId = userId,
                resourceType = resourceType,
                resourceId = resourceId,
                action = action,
                result = result,
                details = details,
                deviceId = getDeviceId()
            )
            
            auditLogs.add(log)
            
            // 保存到文件或数据库（在实际应用中）
            saveAuditLog(log)
            
            logger.d("PrivacyGuard", "记录隐私事件: $eventType - $action - $result")
            
        } catch (e: Exception) {
            logger.e("PrivacyGuard", "记录隐私事件失败", e)
        }
    }
    
    /**
     * 匿名化数据
     */
    suspend fun anonymizeData(data: String, dataType: ResourceType): String {
        return withContext(Dispatchers.IO) {
            try {
                when (dataType) {
                    ResourceType.CONTACT -> anonymizeContact(data)
                    ResourceType.LOCATION -> anonymizeLocation(data)
                    ResourceType.DEVICE_INFO -> anonymizeDeviceInfo(data)
                    ResourceType.NETWORK_INFO -> anonymizeNetworkInfo(data)
                    else -> data // 默认不处理
                }
            } catch (e: Exception) {
                logger.e("PrivacyGuard", "匿名化数据失败", e)
                data // 出错时返回原始数据
            }
        }
    }
    
    /**
     * 匿名化联系人信息
     */
    private fun anonymizeContact(contactInfo: String): String {
        // 简单的匿名化处理，实际应用中应该使用更复杂的算法
        return if (privacySettings.encryptAllData) {
            // 使用加密哈希代替真实数据
            encryptionManager.calculateHash(contactInfo.toByteArray(), "SHA-256")
        } else {
            // 简单的掩码处理
            contactInfo.replace(Regex("\\b\\d{11}\\b"), "***") // 手机号
                .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "***@***") // 邮箱
                .replace(Regex("\\b[A-Za-z]{2,}\\b")) { matchResult ->
                    val name = matchResult.value
                    if (name.length > 2) {
                        name.first() + "*".repeat(name.length - 2) + name.last()
                    } else {
                        "**"
                    }
                }
        }
    }
    
    /**
     * 匿名化位置信息
     */
    private fun anonymizeLocation(location: String): String {
        if (!privacySettings.anonymizeLocation) {
            return location
        }
        
        return try {
            when (privacySettings.locationPrecision) {
                LocationPrecision.EXACT -> location
                LocationPrecision.STREET -> anonymizeToStreetLevel(location)
                LocationPrecision.CITY -> anonymizeToCityLevel(location)
                LocationPrecision.REGION -> anonymizeToRegionLevel(location)
                LocationPrecision.COUNTRY -> anonymizeToCountryLevel(location)
            }
        } catch (e: Exception) {
            logger.e("PrivacyGuard", "匿名化位置失败", e)
            location
        }
    }
    
    /**
     * 匿名化到街道级别
     */
    private fun anonymizeToStreetLevel(location: String): String {
        // 这里应该实现地理编码和反向地理编码
        // 简化实现：保留前几位坐标
        return location.take(20) + "..."
    }
    
    /**
     * 匿名化到城市级别
     */
    private fun anonymizeToCityLevel(location: String): String {
        return location.take(10) + "..."
    }
    
    /**
     * 匿名化到区域级别
     */
    private fun anonymizeToRegionLevel(location: String): String {
        return location.take(5) + "..."
    }
    
    /**
     * 匿名化到国家级别
     */
    private fun anonymizeToCountryLevel(location: String): String {
        return "*****" // 完全匿名
    }
    
    /**
     * 匿名化设备信息
     */
    private fun anonymizeDeviceInfo(deviceInfo: String): String {
        return if (privacySettings.encryptAllData) {
            encryptionManager.calculateHash(deviceInfo.toByteArray(), "SHA-256")
        } else {
            // 移除或掩码敏感信息
            deviceInfo
                .replace(Regex("\\b[A-Z0-9]{14,17}\\b"), "***") // IMEI/序列号
                .replace(Regex("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"), "***") // UUID
                .replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"), "***") // IP地址
        }
    }
    
    /**
     * 匿名化网络信息
     */
    private fun anonymizeNetworkInfo(networkInfo: String): String {
        return networkInfo
            .replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"), "***") // IP地址
            .replace(Regex("\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b"), "***") // MAC地址
            .replace(Regex("\\b[\\w\\s]{3,20}\\b")) { matchResult ->
                val ssid = matchResult.value
                if (ssid.length > 4) {
                    ssid.take(2) + "*".repeat(ssid.length - 4) + ssid.takeLast(2)
                } else {
                    "***"
                }
            } // WiFi SSID
    }
    
    /**
     * 模糊敏感图片
     */
    suspend fun blurSensitiveImage(imagePath: String, outputPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (!privacySettings.blurSensitiveImages) {
                // 如果不启用模糊，直接复制文件
                return@withContext try {
                    File(imagePath).copyTo(File(outputPath), overwrite = true)
                    true
                } catch (e: Exception) {
                    logger.e("PrivacyGuard", "复制图片失败", e)
                    false
                }
            }
            
            try {
                // 这里应该实现图片模糊算法
                // 可以使用Android的RenderScript或第三方库
                // 简化实现：记录日志并复制文件
                logger.i("PrivacyGuard", "模糊处理图片: $imagePath -> $outputPath")
                File(imagePath).copyTo(File(outputPath), overwrite = true)
                
                // 记录隐私事件
                logPrivacyEvent(
                    eventType = PrivacyEventType.ANONYMIZATION,
                    resourceType = ResourceType.IMAGE,
                    action = PrivacyAction.MASK,
                    result = PrivacyActionResult.SUCCESS,
                    details = mapOf(
                        "original_path" to imagePath,
                        "blurred_path" to outputPath
                    )
                )
                
                true
            } catch (e: Exception) {
                logger.e("PrivacyGuard", "模糊图片失败", e)
                
                // 记录失败事件
                logPrivacyEvent(
                    eventType = PrivacyEventType.ANONYMIZATION,
                    resourceType = ResourceType.IMAGE,
                    action = PrivacyAction.MASK,
                    result = PrivacyActionResult.FAILED,
                    details = mapOf("error" to e.message ?: "未知错误")
                )
                
                false
            }
        }
    }
    
    /**
     * 清理过期数据
     */
    suspend fun cleanupExpiredData(): CleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!privacySettings.autoDeleteOldData) {
                    return@withContext CleanupResult(
                        success = true,
                        message = "自动清理已禁用",
                        deletedItems = 0
                    )
                }
                
                val retentionDays = privacySettings.dataRetentionDays
                val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
                
                var deletedCount = 0
                
                // 清理旧的审计日志
                val oldLogs = auditLogs.filter { it.timestamp < cutoffTime }
                auditLogs.removeAll(oldLogs)
                deletedCount += oldLogs.size
                
                // 清理旧的临时文件
                val tempDir = File(context.cacheDir, "temp")
                if (tempDir.exists()) {
                    tempDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < cutoffTime) {
                            if (file.delete()) {
                                deletedCount++
                            }
                        }
                    }
                }
                
                // 记录清理事件
                logPrivacyEvent(
                    eventType = PrivacyEventType.DATA_DELETION,
                    resourceType = ResourceType.DEVICE_INFO,
                    action = PrivacyAction.DELETE,
                    result = PrivacyActionResult.SUCCESS,
                    details = mapOf(
                        "deleted_items" to deletedCount.toString(),
                        "retention_days" to retentionDays.toString()
                    )
                )
                
                CleanupResult(
                    success = true,
                    message = "清理完成",
                    deletedItems = deletedCount
                )
            } catch (e: Exception) {
                logger.e("PrivacyGuard", "清理过期数据失败", e)
                
                CleanupResult(
                    success = false,
                    message = "清理失败: ${e.message}",
                    deletedItems = 0
                )
            }
        }
    }
    
    /**
     * 导出隐私数据
     */
    suspend fun exportPrivacyData(outputPath: String): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val exportData = mutableMapOf<String, Any>()
                
                // 收集隐私设置
                exportData["privacy_settings"] = mapOf(
                    "data_retention_days" to privacySettings.dataRetentionDays,
                    "auto_delete_old_data" to privacySettings.autoDeleteOldData,
                    "allow_analytics" to privacySettings.allowAnalytics,
                    "encrypt_all_data" to privacySettings.encryptAllData,
                    "use_biometric_auth" to privacySettings.useBiometricAuth
                )
                
                // 收集权限状态
                exportData["permissions"] = checkSensitivePermissions()
                
                // 收集审计日志统计
                exportData["audit_logs_summary"] = mapOf(
                    "total_logs" to auditLogs.size,
                    "last_30_days" to auditLogs.count { 
                        it.timestamp > System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) 
                    }
                )
                
                // 将数据写入文件
                val jsonString = convertToJson(exportData)
                File(outputPath).writeText(jsonString)
                
                // 记录导出事件
                logPrivacyEvent(
                    eventType = PrivacyEventType.EXPORT,
                    resourceType = ResourceType.DEVICE_INFO,
                    action = PrivacyAction.EXPORT,
                    result = PrivacyActionResult.SUCCESS,
                    details = mapOf("export_path" to outputPath)
                )
                
                ExportResult(
                    success = true,
                    filePath = outputPath,
                    dataSize = jsonString.length.toLong()
                )
            } catch (e: Exception) {
                logger.e("PrivacyGuard", "导出隐私数据失败", e)
                
                ExportResult(
                    success = false,
                    errorMessage = "导出失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 生成隐私报告
     */
    suspend fun generatePrivacyReport(): PrivacyReport {
        return withContext(Dispatchers.IO) {
            try {
                val reportId = "privacy_report_${System.currentTimeMillis()}"
                val generatedAt = System.currentTimeMillis()
                
                // 收集报告数据
                val permissions = checkSensitivePermissions()
                val grantedPermissions = permissions.count { it.value }
                val totalPermissions = permissions.size
                
                val recentEvents = auditLogs
                    .filter { it.timestamp > System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) }
                    .groupBy { it.eventType }
                    .mapValues { it.value.size }
                
                val dataAccessSummary = auditLogs
                    .filter { it.eventType == PrivacyEventType.DATA_ACCESS }
                    .groupBy { it.resourceType }
                    .mapValues { it.value.size }
                
                PrivacyReport(
                    id = reportId,
                    generatedAt = generatedAt,
                    privacySettings = privacySettings,
                    permissionsSummary = PermissionsSummary(
                        granted = grantedPermissions,
                        total = totalPermissions,
                        details = permissions
                    ),
                    recentActivity = recentEvents,
                    dataAccessSummary = dataAccessSummary,
                    recommendations = generateRecommendations()
                )
            } catch (e: Exception) {
                logger.e("PrivacyGuard", "生成隐私报告失败", e)
                throw e
            }
        }
    }
    
    /**
     * 生成改进建议
     */
    private fun generateRecommendations(): List<PrivacyRecommendation> {
        val recommendations = mutableListOf<PrivacyRecommendation>()
        
        // 检查权限
        val permissions = checkSensitivePermissions()
        permissions.forEach { (permission, granted) ->
            if (granted) {
                recommendations.add(
                    PrivacyRecommendation(
                        type = RecommendationType.PERMISSION_REVIEW,
                        priority = Priority.MEDIUM,
                        title = "权限使用情况",
                        description = "已授予权限: ${getPermissionDescription(permission)}",
                        action = "检查使用频率"
                    )
                )
            }
        }
        
        // 检查数据保留设置
        if (!privacySettings.autoDeleteOldData) {
            recommendations.add(
                PrivacyRecommendation(
                    type = RecommendationType.DATA_RETENTION,
                    priority = Priority.HIGH,
                    title = "启用自动数据清理",
                    description = "建议启用自动清理以保护隐私",
                    action = "前往设置开启"
                )
            )
        }
        
        // 检查加密设置
        if (!privacySettings.encryptAllData) {
            recommendations.add(
                PrivacyRecommendation(
                    type = RecommendationType.ENCRYPTION,
                    priority = Priority.HIGH,
                    title = "启用全数据加密",
                    description = "建议启用加密以增强数据安全",
                    action = "前往设置开启"
                )
            )
        }
        
        // 检查位置精度
        if (!privacySettings.anonymizeLocation) {
            recommendations.add(
                PrivacyRecommendation(
                    type = RecommendationType.LOCATION,
                    priority = RecommendationType.Priority.MEDIUM,
                    title = "启用位置匿名化",
                    description = "建议启用位置匿名化以保护位置隐私",
                    action = "前往设置开启"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * 获取权限描述
     */
    private fun getPermissionDescription(permission: String): String {
        return when (permission) {
            android.Manifest.permission.ACCESS_FINE_LOCATION -> "精确位置"
            android.Manifest.permission.READ_CONTACTS -> "读取联系人"
            android.Manifest.permission.READ_SMS -> "读取短信"
            android.Manifest.permission.RECORD_AUDIO -> "录音"
            android.Manifest.permission.CAMERA -> "相机"
            android.Manifest.permission.READ_CALENDAR -> "读取日历"
            android.Manifest.permission.READ_CALL_LOG -> "读取通话记录"
            else -> permission
        }
    }
    
    /**
     * 保存审计日志
     */
    private fun saveAuditLog(log: PrivacyAuditLog) {
        // 在实际应用中，这里应该保存到数据库或文件
        // 简化实现：只保存在内存中
    }
    
    /**
     * 加载审计日志
     */
    private fun loadAuditLogs() {
        // 在实际应用中，这里应该从数据库或文件加载
        // 简化实现：从内存中加载（实际为空）
    }
    
    /**
     * 清理旧的审计日志
     */
    private fun cleanupOldAuditLogs() {
        if (!privacySettings.autoDeleteOldData) return
        
        val retentionDays = privacySettings.dataRetentionDays
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        
        auditLogs.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * 获取设备ID（匿名化）
     */
    private fun getDeviceId(): String {
        return try {
            val deviceInfo = Build.MANUFACTURER + Build.MODEL + Build.SERIAL
            encryptionManager.calculateHash(deviceInfo.toByteArray(), "SHA-256")
        } catch (e: Exception) {
            "anonymous_device"
        }
    }
    
    /**
     * 转换为JSON（简化实现）
     */
    private fun convertToJson(data: Map<String, Any>): String {
        // 在实际应用中，应该使用JSON库
        return data.toString()
    }
    
    /**
     * 获取当前隐私设置
     */
    fun getPrivacySettings(): PrivacySettings {
        return privacySettings
    }
    
    /**
     * 更新隐私设置
     */
    fun updatePrivacySettings(settings: PrivacySettings) {
        privacySettings = settings
        
        // 记录设置更新事件
        logPrivacyEvent(
            eventType = PrivacyEventType.DATA_MODIFICATION,
            resourceType = ResourceType.DEVICE_INFO,
            action = PrivacyAction.UPDATE,
            result = PrivacyActionResult.SUCCESS,
            details = mapOf("settings_updated" to "true")
        )
    }
    
    /**
     * 获取审计日志
     */
    fun getAuditLogs(limit: Int = 100): List<PrivacyAuditLog> {
        return auditLogs.takeLast(limit).reversed()
    }
}

// 数据类
data class CleanupResult(
    val success: Boolean,
    val message: String,
    val deletedItems: Int
)

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val dataSize: Long = 0,
    val errorMessage: String? = null
)

data class PrivacyReport(
    val id: String,
    val generatedAt: Long,
    val privacySettings: PrivacySettings,
    val permissionsSummary: PermissionsSummary,
    val recentActivity: Map<PrivacyEventType, Int>,
    val dataAccessSummary: Map<ResourceType, Int>,
    val recommendations: List<PrivacyRecommendation>
)

data class PermissionsSummary(
    val granted: Int,
    val total: Int,
    val details: Map<String, Boolean>
)

data class PrivacyRecommendation(
    val type: RecommendationType,
    val priority: Priority,
    val title: String,
    val description: String,
    val action: String
)

enum class RecommendationType {
    PERMISSION_REVIEW,
    DATA_RETENTION,
    ENCRYPTION,
    LOCATION,
    BACKUP,
    SHARING
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}