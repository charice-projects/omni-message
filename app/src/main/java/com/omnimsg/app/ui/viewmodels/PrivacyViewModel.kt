// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/PrivacyViewModel.kt
@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val privacyGuard: PrivacyGuard,
    private val encryptionManager: EncryptionManager,
    private val permissionManager: PermissionManager,
    private val dataAnonymizer: DataAnonymizer,
    private val securityAuditor: SecurityAuditor,
    private val threatDetector: ThreatDetector,
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    // 事件通道
    private val _events = Channel<PrivacyEvent>()
    val events = _events.receiveAsFlow()

    // 隐私审计日志
    private val _auditLogs = MutableStateFlow<List<PrivacyAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<PrivacyAuditLog>> = _auditLogs.asStateFlow()

    init {
        viewModelScope.launch {
            // 加载隐私数据
            loadPrivacyData()

            // 监听隐私设置变化
            observePrivacyChanges()

            // 监听权限变化
            observePermissionChanges()

            // 监听安全事件
            observeSecurityEvents()
        }
    }

    // 加载隐私数据
    fun loadPrivacyData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 并行加载所有隐私数据
                val privacyStatusDeferred = async { privacyGuard.getPrivacyStatus() }
                val encryptionStatusDeferred = async { encryptionManager.getEncryptionStatus() }
                val permissionStatusDeferred = async { permissionManager.getPermissionStatus() }
                val auditLogsDeferred = async { privacyGuard.getAuditLogs(100) }
                val securityScoreDeferred = async { securityAuditor.calculateSecurityScore() }
                val threatAssessmentDeferred = async { threatDetector.getThreatAssessment() }
                val dataFootprintDeferred = async { privacyGuard.calculateDataFootprint() }

                _uiState.update { state ->
                    state.copy(
                        privacyStatus = privacyStatusDeferred.await(),
                        encryptionStatus = encryptionStatusDeferred.await(),
                        permissionStatus = permissionStatusDeferred.await(),
                        securityScore = securityScoreDeferred.await(),
                        threatAssessment = threatAssessmentDeferred.await(),
                        dataFootprint = dataFootprintDeferred.await(),
                        isLoading = false,
                        error = null
                    )
                }

                _auditLogs.value = auditLogsDeferred.await()

                // 生成隐私报告
                generatePrivacyReport()

            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "加载隐私数据失败: ${e.message}"
                    )
                }

                _events.send(PrivacyEvent.ShowError("加载失败"))
            }
        }
    }

    // 监听隐私设置变化
    private fun observePrivacyChanges() {
        viewModelScope.launch {
            privacyGuard.observePrivacyStatus().collect { status ->
                _uiState.update { state ->
                    state.copy(privacyStatus = status)
                }
            }
        }
    }

    // 监听权限变化
    private fun observePermissionChanges() {
        viewModelScope.launch {
            permissionManager.observePermissions().collect { permissions ->
                _uiState.update { state ->
                    state.copy(permissionStatus = permissions)
                }
            }
        }
    }

    // 监听安全事件
    private fun observeSecurityEvents() {
        viewModelScope.launch {
            securityAuditor.observeSecurityEvents().collect { events ->
                _uiState.update { state ->
                    state.copy(recentSecurityEvents = events.take(10))
                }
            }
        }
    }

    // 生成隐私报告
    private suspend fun generatePrivacyReport() {
        val report = privacyGuard.generatePrivacyReport(
            startTime = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, // 最近30天
            endTime = System.currentTimeMillis()
        )

        _uiState.update { state ->
            state.copy(privacyReport = report)
        }
    }

    // 切换隐私设置
    fun togglePrivacySetting(setting: PrivacySetting, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val result = privacyGuard.updatePrivacySetting(setting, enabled)

                if (result.isSuccess) {
                    val message = if (enabled) "${setting.displayName}已启用" 
                                 else "${setting.displayName}已禁用"
                    _events.send(PrivacyEvent.ShowMessage(message))

                    // 记录审计日志
                    privacyGuard.logPrivacyEvent(
                        PrivacyEventType.PRIVACY_SETTING_CHANGED,
                        "settings",
                        "$message: $setting"
                    )

                    // 重新加载数据
                    loadPrivacyData()
                } else {
                    _events.send(PrivacyEvent.ShowError("更新失败: ${result.error}"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("更新异常: ${e.message}"))
            }
        }
    }

    // 启用所有隐私保护
    fun enableAllPrivacyProtections() {
        viewModelScope.launch {
            try {
                val result = privacyGuard.enableAllProtections()

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.ShowMessage("所有隐私保护已启用"))
                    loadPrivacyData()
                } else {
                    _events.send(PrivacyEvent.ShowError("启用失败"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("操作异常"))
            }
        }
    }

    // 禁用所有隐私保护
    fun disableAllPrivacyProtections() {
        viewModelScope.launch {
            try {
                val result = privacyGuard.disableAllProtections()

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.ShowMessage("所有隐私保护已禁用"))
                    loadPrivacyData()
                } else {
                    _events.send(PrivacyEvent.ShowError("禁用失败"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("操作异常"))
            }
        }
    }

    // 加密所有数据
    fun encryptAllData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isEncryptingData = true) }

                val result = encryptionManager.encryptAllData()

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.ShowMessage("所有数据已加密"))
                    
                    // 更新加密状态
                    _uiState.update { state ->
                        state.copy(
                            encryptionStatus = encryptionManager.getEncryptionStatus()
                        )
                    }
                } else {
                    _events.send(PrivacyEvent.ShowError("加密失败: ${result.error}"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("加密异常: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isEncryptingData = false) }
            }
        }
    }

    // 解密所有数据
    fun decryptAllData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDecryptingData = true) }

                val result = encryptionManager.decryptAllData()

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.ShowMessage("所有数据已解密"))
                    
                    _uiState.update { state ->
                        state.copy(
                            encryptionStatus = encryptionManager.getEncryptionStatus()
                        )
                    }
                } else {
                    _events.send(PrivacyEvent.ShowError("解密失败: ${result.error}"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("解密异常"))
            } finally {
                _uiState.update { it.copy(isDecryptingData = false) }
            }
        }
    }

    // 请求权限
    fun requestPermission(permission: PermissionType) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRequestingPermission = true) }

                val result = permissionManager.requestPermission(permission)

                when (result) {
                    is PermissionResult.Granted -> {
                        _events.send(PrivacyEvent.ShowMessage("权限已授予: ${permission.displayName}"))
                        
                        // 更新权限状态
                        _uiState.update { state ->
                            state.copy(
                                permissionStatus = permissionManager.getPermissionStatus()
                            )
                        }
                    }
                    is PermissionResult.Denied -> {
                        _events.send(PrivacyEvent.ShowError("权限被拒绝"))
                    }
                    is PermissionResult.PermanentlyDenied -> {
                        _events.send(PrivacyEvent.ShowError("权限被永久拒绝，请在设置中启用"))
                    }
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("请求权限异常"))
            } finally {
                _uiState.update { it.copy(isRequestingPermission = false) }
            }
        }
    }

    // 撤销权限
    fun revokePermission(permission: PermissionType) {
        viewModelScope.launch {
            try {
                val result = permissionManager.revokePermission(permission)

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.ShowMessage("权限已撤销: ${permission.displayName}"))
                    
                    _uiState.update { state ->
                        state.copy(
                            permissionStatus = permissionManager.getPermissionStatus()
                        )
                    }
                } else {
                    _events.send(PrivacyEvent.ShowError("撤销权限失败"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("操作异常"))
            }
        }
    }

    // 清理个人数据
    fun cleanPersonalData(dataType: DataType, scope: CleanupScope = CleanupScope.ALL) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCleaningData = true) }

                val result = privacyGuard.cleanPersonalData(dataType, scope)

                if (result.isSuccess) {
                    val message = when (scope) {
                        CleanupScope.ALL -> "所有${dataType.displayName}数据已清理"
                        CleanupScope.OLD -> "旧的${dataType.displayName}数据已清理"
                        CleanupScope.CACHE -> "${dataType.displayName}缓存已清理"
                    }
                    
                    _events.send(PrivacyEvent.ShowMessage(message))
                    
                    // 重新计算数据足迹
                    _uiState.update { state ->
                        state.copy(
                            dataFootprint = privacyGuard.calculateDataFootprint()
                        )
                    }

                    // 记录审计日志
                    privacyGuard.logPrivacyEvent(
                        PrivacyEventType.DATA_CLEANED,
                        dataType.name,
                        "$message ($scope)"
                    )

                } else {
                    _events.send(PrivacyEvent.ShowError("清理失败: ${result.error}"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("清理异常: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isCleaningData = false) }
            }
        }
    }

    // 匿名化所有数据
    fun anonymizeAllData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAnonymizingData = true) }

                val result = dataAnonymizer.anonymizeAllData()

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.ShowMessage("所有数据已匿名化"))
                    
                    // 重新计算数据足迹
                    _uiState.update { state ->
                        state.copy(
                            dataFootprint = privacyGuard.calculateDataFootprint()
                        )
                    }

                    // 记录审计日志
                    privacyGuard.logPrivacyEvent(
                        PrivacyEventType.DATA_ANONYMIZED,
                        "all",
                        "所有数据已匿名化"
                    )

                } else {
                    _events.send(PrivacyEvent.ShowError("匿名化失败: ${result.error}"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("匿名化异常"))
            } finally {
                _uiState.update { it.copy(isAnonymizingData = false) }
            }
        }
    }

    // 导出隐私数据
    fun exportPrivacyData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExportingData = true) }

                val result = privacyGuard.exportPrivacyData()

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.DataExported(result.fileUri, result.dataSize))
                } else {
                    _events.send(PrivacyEvent.ShowError("导出失败: ${result.error}"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("导出异常"))
            } finally {
                _uiState.update { it.copy(isExportingData = false) }
            }
        }
    }

    // 运行安全扫描
    fun runSecurityScan() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isScanning = true) }

                val result = securityAuditor.runSecurityScan()

                if (result.isSuccess) {
                    _events.send(PrivacyEvent.ShowMessage("安全扫描完成"))
                    
                    _uiState.update { state ->
                        state.copy(
                            securityScore = securityAuditor.calculateSecurityScore(),
                            threatAssessment = threatDetector.getThreatAssessment(),
                            securityIssues = result.issues
                        )
                    }

                } else {
                    _events.send(PrivacyEvent.ShowError("扫描失败: ${result.error}"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("扫描异常"))
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    // 查看审计日志
    fun viewAuditLogs(startTime: Long? = null, endTime: Long? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingAuditLogs = true) }

                val logs = privacyGuard.getAuditLogs(
                    limit = 1000,
                    startTime = startTime,
                    endTime = endTime
                )

                _auditLogs.value = logs
                _uiState.update { it.copy(isLoadingAuditLogs = false) }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("加载审计日志失败"))
                _uiState.update { it.copy(isLoadingAuditLogs = false) }
            }
        }
    }

    // 清除审计日志
    fun clearAuditLogs() {
        viewModelScope.launch {
            try {
                val result = privacyGuard.clearAuditLogs()

                if (result.isSuccess) {
                    _auditLogs.value = emptyList()
                    _events.send(PrivacyEvent.ShowMessage("审计日志已清除"))
                } else {
                    _events.send(PrivacyEvent.ShowError("清除失败"))
                }

            } catch (e: Exception) {
                _events.send(PrivacyEvent.ShowError("清除异常"))
            }
        }
    }

    // 刷新数据
    fun refresh() {
        loadPrivacyData()
    }
}

// 隐私UI状态
data class PrivacyUiState(
    val isLoading: Boolean = true,
    val isEncryptingData: Boolean = false,
    val isDecryptingData: Boolean = false,
    val isCleaningData: Boolean = false,
    val isAnonymizingData: Boolean = false,
    val isExportingData: Boolean = false,
    val isRequestingPermission: Boolean = false,
    val isScanning: Boolean = false,
    val isLoadingAuditLogs: Boolean = false,
    val error: String? = null,

    // 隐私状态
    val privacyStatus: PrivacyStatus = PrivacyStatus(),
    val encryptionStatus: EncryptionStatus = EncryptionStatus(),
    val permissionStatus: PermissionStatus = PermissionStatus(),

    // 安全评估
    val securityScore: SecurityScore = SecurityScore(),
    val threatAssessment: ThreatAssessment = ThreatAssessment(),
    val securityIssues: List<SecurityIssue> = emptyList(),

    // 数据足迹
    val dataFootprint: DataFootprint = DataFootprint(),

    // 隐私报告
    val privacyReport: PrivacyReport = PrivacyReport(),

    // 最近安全事件
    val recentSecurityEvents: List<SecurityEvent> = emptyList()
)

// 隐私事件
sealed class PrivacyEvent {
    data class ShowMessage(val message: String) : PrivacyEvent()
    data class ShowError(val error: String) : PrivacyEvent()
    data class DataExported(val fileUri: Uri, val dataSize: Long) : PrivacyEvent()
    data class SecurityScanCompleted(val issues: List<SecurityIssue>) : PrivacyEvent()
    data class ThreatDetected(val threat: SecurityThreat) : PrivacyEvent()
}

// 隐私状态
data class PrivacyStatus(
    val encryptionEnabled: Boolean = true,
    val locationPrivacy: Boolean = true,
    val contactPrivacy: Boolean = true,
    val messagePrivacy: Boolean = true,
    val mediaPrivacy: Boolean = true,
    val analyticsOptOut: Boolean = false,
    val personalizedAds: Boolean = false,
    val dataCollection: Boolean = false,
    val thirdPartySharing: Boolean = false,
    val privacyScore: Int = 85 // 0-100
)

// 加密状态
data class EncryptionStatus(
    val databaseEncrypted: Boolean = true,
    val filesEncrypted: Boolean = true,
    val messagesEncrypted: Boolean = true,
    val backupsEncrypted: Boolean = true,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.END_TO_END,
    val lastEncrypted: Long = System.currentTimeMillis(),
    val encryptionProgress: Int = 100 // 0-100
)

// 权限状态
data class PermissionStatus(
    val location: PermissionState = PermissionState.NOT_DETERMINED,
    val contacts: PermissionState = PermissionState.NOT_DETERMINED,
    val camera: PermissionState = PermissionState.NOT_DETERMINED,
    val microphone: PermissionState = PermissionState.NOT_DETERMINED,
    val storage: PermissionState = PermissionState.NOT_DETERMINED,
    val notifications: PermissionState = PermissionState.NOT_DETERMINED,
    val phone: PermissionState = PermissionState.NOT_DETERMINED,
    val sms: PermissionState = PermissionState.NOT_DETERMINED
)

// 安全评分
data class SecurityScore(
    val overallScore: Int = 85, // 0-100
    val encryptionScore: Int = 90,
    val permissionScore: Int = 80,
    val privacyScore: Int = 85,
    val networkScore: Int = 75,
    val deviceScore: Int = 95,
    val lastUpdated: Long = System.currentTimeMillis()
)

// 威胁评估
data class ThreatAssessment(
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val threats: List<SecurityThreat> = emptyList(),
    val recommendations: List<SecurityRecommendation> = emptyList(),
    val lastAssessed: Long = System.currentTimeMillis()
)

// 数据足迹
data class DataFootprint(
    val totalSize: Long = 0,
    val encryptedSize: Long = 0,
    val anonymizedSize: Long = 0,
    val personalDataSize: Long = 0,
    val cachedDataSize: Long = 0,
    val temporaryDataSize: Long = 0,
    val backupDataSize: Long = 0,
    val dataByType: Map<DataType, Long> = emptyMap()
)

// 隐私报告
data class PrivacyReport(
    val generatedAt: Long = System.currentTimeMillis(),
    val period: String = "最近30天",
    val summary: String = "",
    val findings: List<PrivacyFinding> = emptyList(),
    val recommendations: List<PrivacyRecommendation> = emptyList(),
    val scoreHistory: List<ScoreHistory> = emptyList()
)

// 权限类型
enum class PermissionType(val displayName: String) {
    LOCATION("位置"),
    CONTACTS("联系人"),
    CAMERA("相机"),
    MICROPHONE("麦克风"),
    STORAGE("存储"),
    NOTIFICATIONS("通知"),
    PHONE("电话"),
    SMS("短信")
}

// 权限状态
enum class PermissionState {
    NOT_DETERMINED, GRANTED, DENIED, PERMANENTLY_DENIED
}

// 隐私设置
enum class PrivacySetting(val displayName: String) {
    ENCRYPTION("端到端加密"),
    LOCATION_PRIVACY("位置隐私"),
    CONTACT_PRIVACY("联系人隐私"),
    MESSAGE_PRIVACY("消息隐私"),
    MEDIA_PRIVACY("媒体隐私"),
    ANALYTICS_OPT_OUT("退出分析"),
    PERSONALIZED_ADS("个性化广告"),
    DATA_COLLECTION("数据收集"),
    THIRD_PARTY_SHARING("第三方分享")
}

// 数据类型
enum class DataType(val displayName: String) {
    CONTACTS("联系人"),
    MESSAGES("消息"),
    MEDIA("媒体"),
    LOCATION("位置"),
    CALL_LOGS("通话记录"),
    APP_USAGE("应用使用情况"),
    CACHE("缓存"),
    TEMPORARY("临时文件")
}

// 清理范围
enum class CleanupScope {
    ALL, OLD, CACHE
}

// 风险等级
enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

// 安全威胁
data class SecurityThreat(
    val type: ThreatType,
    val level: RiskLevel,
    val description: String,
    val detectedAt: Long,
    val resolved: Boolean = false
)

enum class ThreatType {
    PERMISSION_ABUSE, DATA_LEAK, WEAK_ENCRYPTION, NETWORK_THREAT, MALWARE, PHISHING
}

// 安全问题
data class SecurityIssue(
    val id: String,
    val type: IssueType,
    val severity: SeverityLevel,
    val description: String,
    val recommendation: String,
    val affectedItems: List<String> = emptyList(),
    val detectedAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false
)

enum class IssueType {
    PERMISSION, ENCRYPTION, NETWORK, STORAGE, APP_SECURITY, DATA_PROTECTION
}

enum class SeverityLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

// 安全事件
data class SecurityEvent(
    val id: String,
    val type: SecurityEventType,
    val timestamp: Long,
    val description: String,
    val source: String,
    val severity: EventSeverity = EventSeverity.INFO
)

enum class SecurityEventType {
    PERMISSION_CHANGED, ENCRYPTION_CHANGED, DATA_ACCESSED, DATA_MODIFIED,
    DATA_DELETED, LOGIN_ATTEMPT, NETWORK_CHANGE, APP_INSTALLED, APP_UPDATED
}

enum class EventSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

// 隐私发现
data class PrivacyFinding(
    val type: FindingType,
    val severity: SeverityLevel,
    val description: String,
    val impact: String,
    val evidence: List<String> = emptyList()
)

enum class FindingType {
    DATA_COLLECTION, THIRD_PARTY_SHARING, PERMISSION_OVERUSE,
    WEAK_PRIVACY_SETTINGS, DATA_RETENTION, LOCATION_TRACKING
}

// 隐私建议
data class PrivacyRecommendation(
    val priority: RecommendationPriority,
    val title: String,
    val description: String,
    val action: String,
    val estimatedTime: Int // 分钟
)

enum class RecommendationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

// 分数历史
data class ScoreHistory(
    val date: Long,
    val overallScore: Int,
    val encryptionScore: Int,
    val permissionScore: Int,
    val privacyScore: Int
)