// 📁 app/src/main/java/com/omnimsg/app/ui/screens/PrivacyCenterScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    viewModel: PrivacyViewModel = hiltViewModel(),
    onNavigate: (AppDestinations) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val events = viewModel.events.collectAsState(initial = null)
    val auditLogs by viewModel.auditLogs.collectAsState()
    
    // 处理事件
    LaunchedEffect(events.value) {
        events.value?.let { event ->
            when (event) {
                is PrivacyEvent.ShowMessage -> onShowSnackbar(event.message)
                is PrivacyEvent.ShowError -> onShowSnackbar("错误: ${event.error}")
                is PrivacyEvent.DataExported -> onShowSnackbar("隐私数据已导出")
                is PrivacyEvent.SecurityScanCompleted -> onShowSnackbar("安全扫描完成")
                is PrivacyEvent.ThreatDetected -> {
                    onShowSnackbar("检测到安全威胁: ${event.threat.type}")
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PrivacyTopBar(
                onNavigationIconClick = { /* 打开抽屉 */ },
                onHelpClick = { /* 打开帮助 */ },
                onRunScanClick = { viewModel.runSecurityScan() }
            )
        },
        floatingActionButton = {
            if (!uiState.isScanning) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.runSecurityScan() },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_security_scan),
                            contentDescription = "安全扫描"
                        )
                    },
                    text = { Text("安全扫描") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            FullScreenLoading()
            return@Scaffold
        }
        
        PrivacyContent(
            uiState = uiState,
            auditLogs = auditLogs,
            paddingValues = paddingValues,
            onTogglePrivacySetting = { setting, enabled ->
                viewModel.togglePrivacySetting(setting, enabled)
            },
            onEnableAllProtections = { viewModel.enableAllPrivacyProtections() },
            onDisableAllProtections = { viewModel.disableAllPrivacyProtections() },
            onEncryptAllData = { viewModel.encryptAllData() },
            onDecryptAllData = { viewModel.decryptAllData() },
            onRequestPermission = { permission ->
                viewModel.requestPermission(permission)
            },
            onRevokePermission = { permission ->
                viewModel.revokePermission(permission)
            },
            onCleanData = { dataType, scope ->
                viewModel.cleanPersonalData(dataType, scope)
            },
            onAnonymizeAllData = { viewModel.anonymizeAllData() },
            onExportData = { viewModel.exportPrivacyData() },
            onViewAuditLogs = { startTime, endTime ->
                viewModel.viewAuditLogs(startTime, endTime)
            },
            onClearAuditLogs = { viewModel.clearAuditLogs() },
            onRefresh = { viewModel.refresh() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyTopBar(
    onNavigationIconClick: () -> Unit,
    onHelpClick: () -> Unit,
    onRunScanClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "隐私中心",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "菜单"
                )
            }
        },
        actions = {
            IconButton(onClick = onRunScanClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_security_scan),
                    contentDescription = "安全扫描"
                )
            }
            
            IconButton(onClick = onHelpClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_help),
                    contentDescription = "帮助"
                )
            }
        }
    )
}

@Composable
private fun PrivacyContent(
    uiState: PrivacyUiState,
    auditLogs: List<PrivacyAuditLog>,
    paddingValues: PaddingValues,
    onTogglePrivacySetting: (PrivacySetting, Boolean) -> Unit,
    onEnableAllProtections: () -> Unit,
    onDisableAllProtections: () -> Unit,
    onEncryptAllData: () -> Unit,
    onDecryptAllData: () -> Unit,
    onRequestPermission: (PermissionType) -> Unit,
    onRevokePermission: (PermissionType) -> Unit,
    onCleanData: (DataType, CleanupScope) -> Unit,
    onAnonymizeAllData: () -> Unit,
    onExportData: () -> Unit,
    onViewAuditLogs: (Long?, Long?) -> Unit,
    onClearAuditLogs: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .verticalScroll(scrollState)
    ) {
        // 安全评分卡片
        SecurityScoreCard(
            securityScore = uiState.securityScore,
            threatAssessment = uiState.threatAssessment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // 隐私状态卡片
        PrivacyStatusCard(
            privacyStatus = uiState.privacyStatus,
            onToggleSetting = onTogglePrivacySetting,
            onEnableAll = onEnableAllProtections,
            onDisableAll = onDisableAllProtections,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 加密状态卡片
        EncryptionStatusCard(
            encryptionStatus = uiState.encryptionStatus,
            onEncryptAll = onEncryptAllData,
            onDecryptAll = onDecryptAllData,
            isEncrypting = uiState.isEncryptingData,
            isDecrypting = uiState.isDecryptingData,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 权限管理卡片
        PermissionManagementCard(
            permissionStatus = uiState.permissionStatus,
            onRequestPermission = onRequestPermission,
            onRevokePermission = onRevokePermission,
            isRequesting = uiState.isRequestingPermission,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 数据足迹卡片
        DataFootprintCard(
            dataFootprint = uiState.dataFootprint,
            onCleanData = onCleanData,
            onAnonymizeAll = onAnonymizeAllData,
            isCleaning = uiState.isCleaningData,
            isAnonymizing = uiState.isAnonymizingData,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 安全问题列表
        if (uiState.securityIssues.isNotEmpty()) {
            SecurityIssuesCard(
                issues = uiState.securityIssues,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // 审计日志卡片
        AuditLogsCard(
            auditLogs = auditLogs,
            isLoading = uiState.isLoadingAuditLogs,
            onViewLogs = onViewAuditLogs,
            onClearLogs = onClearAuditLogs,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 数据导出卡片
        DataExportCard(
            onExportData = onExportData,
            isExporting = uiState.isExportingData,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SecurityScoreCard(
    securityScore: SecurityScore,
    threatAssessment: ThreatAssessment,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "安全评分",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // 整体评分
                CircularScoreIndicator(
                    score = securityScore.overallScore,
                    size = 60.dp,
                    strokeWidth = 6.dp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 详细评分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreItem(
                    label = "加密",
                    score = securityScore.encryptionScore,
                    color = MaterialTheme.colorScheme.primary
                )
                
                ScoreItem(
                    label = "权限",
                    score = securityScore.permissionScore,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                ScoreItem(
                    label = "隐私",
                    score = securityScore.privacyScore,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                ScoreItem(
                    label = "网络",
                    score = securityScore.networkScore,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // 威胁评估
            if (threatAssessment.threats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                ThreatAssessmentSection(
                    threatAssessment = threatAssessment
                )
            }
        }
    }
}

@Composable
private fun CircularScoreIndicator(
    score: Int,
    size: Dp = 60.dp,
    strokeWidth: Dp = 6.dp
) {
    val color = when (score) {
        in 80..100 -> Color.Green
        in 60..79 -> Color.Yellow
        else -> Color.Red
    }
    
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = score / 100f,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = strokeWidth,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Text(
            text = "$score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScoreItem(
    label: String,
    score: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThreatAssessmentSection(
    threatAssessment: ThreatAssessment
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "威胁评估",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            RiskLevelBadge(riskLevel = threatAssessment.riskLevel)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 威胁列表
        threatAssessment.threats.take(3).forEach { threat ->
            ThreatItem(threat = threat)
        }
        
        // 建议
        if (threatAssessment.recommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "建议",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            threatAssessment.recommendations.take(2).forEach { recommendation ->
                RecommendationItem(recommendation = recommendation)
            }
        }
    }
}

@Composable
private fun RiskLevelBadge(riskLevel: RiskLevel) {
    val (text, color) = when (riskLevel) {
        RiskLevel.LOW -> Pair("低风险", Color.Green)
        RiskLevel.MEDIUM -> Pair("中风险", Color.Yellow)
        RiskLevel.HIGH -> Pair("高风险", Color.Orange)
        RiskLevel.CRITICAL -> Pair("严重", Color.Red)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThreatItem(threat: SecurityThreat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                id = when (threat.level) {
                    RiskLevel.CRITICAL -> R.drawable.ic_warning
                    RiskLevel.HIGH -> R.drawable.ic_error
                    else -> R.drawable.ic_info
                }
            ),
            contentDescription = "威胁",
            tint = when (threat.level) {
                RiskLevel.CRITICAL -> Color.Red
                RiskLevel.HIGH -> Color.Orange
                RiskLevel.MEDIUM -> Color.Yellow
                RiskLevel.LOW -> Color.Green
            },
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = threat.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (!threat.resolved) {
            Badge(
                containerColor = Color.Red,
                contentColor = Color.White,
                modifier = Modifier.size(8.dp)
            )
        }
    }
}

@Composable
private fun RecommendationItem(recommendation: SecurityRecommendation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_circle),
            contentDescription = "建议",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = recommendation.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PrivacyStatusCard(
    privacyStatus: PrivacyStatus,
    onToggleSetting: (PrivacySetting, Boolean) -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "隐私设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onEnableAll) {
                        Text("全部启用")
                    }
                    
                    TextButton(onClick = onDisableAll) {
                        Text("全部禁用")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 隐私设置列表
            PrivacySettingItem(
                setting = PrivacySetting.ENCRYPTION,
                enabled = privacyStatus.encryptionEnabled,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.ENCRYPTION, enabled) }
            )
            
            PrivacySettingItem(
                setting = PrivacySetting.LOCATION_PRIVACY,
                enabled = privacyStatus.locationPrivacy,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.LOCATION_PRIVACY, enabled) }
            )
            
            PrivacySettingItem(
                setting = PrivacySetting.CONTACT_PRIVACY,
                enabled = privacyStatus.contactPrivacy,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.CONTACT_PRIVACY, enabled) }
            )
            
            PrivacySettingItem(
                setting = PrivacySetting.MESSAGE_PRIVACY,
                enabled = privacyStatus.messagePrivacy,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.MESSAGE_PRIVACY, enabled) }
            )
            
            PrivacySettingItem(
                setting = PrivacySetting.MEDIA_PRIVACY,
                enabled = privacyStatus.mediaPrivacy,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.MEDIA_PRIVACY, enabled) }
            )
            
            PrivacySettingItem(
                setting = PrivacySetting.ANALYTICS_OPT_OUT,
                enabled = privacyStatus.analyticsOptOut,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.ANALYTICS_OPT_OUT, enabled) }
            )
            
            PrivacySettingItem(
                setting = PrivacySetting.DATA_COLLECTION,
                enabled = privacyStatus.dataCollection,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.DATA_COLLECTION, enabled) }
            )
            
            PrivacySettingItem(
                setting = PrivacySetting.THIRD_PARTY_SHARING,
                enabled = privacyStatus.thirdPartySharing,
                onToggle = { enabled -> onToggleSetting(PrivacySetting.THIRD_PARTY_SHARING, enabled) }
            )
        }
    }
}

@Composable
private fun PrivacySettingItem(
    setting: PrivacySetting,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = getPrivacySettingDescription(setting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

private fun getPrivacySettingDescription(setting: PrivacySetting): String {
    return when (setting) {
        PrivacySetting.ENCRYPTION -> "端到端加密保护您的消息安全"
        PrivacySetting.LOCATION_PRIVACY -> "限制位置信息收集和共享"
        PrivacySetting.CONTACT_PRIVACY -> "保护联系人信息不被滥用"
        PrivacySetting.MESSAGE_PRIVACY -> "加密存储和传输消息内容"
        PrivacySetting.MEDIA_PRIVACY -> "保护媒体文件隐私"
        PrivacySetting.ANALYTICS_OPT_OUT -> "退出匿名数据分析"
        PrivacySetting.PERSONALIZED_ADS -> "个性化广告推荐"
        PrivacySetting.DATA_COLLECTION -> "允许收集使用数据改进产品"
        PrivacySetting.THIRD_PARTY_SHARING -> "与合作伙伴共享匿名数据"
    }
}

@Composable
private fun EncryptionStatusCard(
    encryptionStatus: EncryptionStatus,
    onEncryptAll: () -> Unit,
    onDecryptAll: () -> Unit,
    isEncrypting: Boolean,
    isDecrypting: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "加密状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${encryptionStatus.encryptionProgress}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 加密进度条
            LinearProgressIndicator(
                progress = encryptionStatus.encryptionProgress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 加密状态列表
            EncryptionStatusItem(
                label = "数据库加密",
                status = encryptionStatus.databaseEncrypted
            )
            
            EncryptionStatusItem(
                label = "文件加密",
                status = encryptionStatus.filesEncrypted
            )
            
            EncryptionStatusItem(
                label = "消息加密",
                status = encryptionStatus.messagesEncrypted
            )
            
            EncryptionStatusItem(
                label = "备份加密",
                status = encryptionStatus.backupsEncrypted
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 加密操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onEncryptAll,
                    enabled = !isEncrypting && !isDecrypting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isEncrypting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_encryption),
                            contentDescription = "加密"
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text("加密所有数据")
                }
                
                Button(
                    onClick = onDecryptAll,
                    enabled = !isEncrypting && !isDecrypting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isDecrypting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_decryption),
                            contentDescription = "解密"
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text("解密所有数据")
                }
            }
        }
    }
}

@Composable
private fun EncryptionStatusItem(
    label: String,
    status: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                id = if (status) R.drawable.ic_check_circle else R.drawable.ic_error
            ),
            contentDescription = label,
            tint = if (status) Color.Green else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = if (status) "已加密" else "未加密",
            style = MaterialTheme.typography.bodySmall,
            color = if (status) Color.Green else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun PermissionManagementCard(
    permissionStatus: PermissionStatus,
    onRequestPermission: (PermissionType) -> Unit,
    onRevokePermission: (PermissionType) -> Unit,
    isRequesting: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "权限管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 权限列表
            PermissionItem(
                permission = PermissionType.LOCATION,
                state = permissionStatus.location,
                onRequest = { onRequestPermission(PermissionType.LOCATION) },
                onRevoke = { onRevokePermission(PermissionType.LOCATION) },
                isRequesting = isRequesting
            )
            
            PermissionItem(
                permission = PermissionType.CONTACTS,
                state = permissionStatus.contacts,
                onRequest = { onRequestPermission(PermissionType.CONTACTS) },
                onRevoke = { onRevokePermission(PermissionType.CONTACTS) },
                isRequesting = isRequesting
            )
            
            PermissionItem(
                permission = PermissionType.CAMERA,
                state = permissionStatus.camera,
                onRequest = { onRequestPermission(PermissionType.CAMERA) },
                onRevoke = { onRevokePermission(PermissionType.CAMERA) },
                isRequesting = isRequesting
            )
            
            PermissionItem(
                permission = PermissionType.MICROPHONE,
                state = permissionStatus.microphone,
                onRequest = { onRequestPermission(PermissionType.MICROPHONE) },
                onRevoke = { onRevokePermission(PermissionType.MICROPHONE) },
                isRequesting = isRequesting
            )
            
            PermissionItem(
                permission = PermissionType.STORAGE,
                state = permissionStatus.storage,
                onRequest = { onRequestPermission(PermissionType.STORAGE) },
                onRevoke = { onRevokePermission(PermissionType.STORAGE) },
                isRequesting = isRequesting
            )
            
            PermissionItem(
                permission = PermissionType.NOTIFICATIONS,
                state = permissionStatus.notifications,
                onRequest = { onRequestPermission(PermissionType.NOTIFICATIONS) },
                onRevoke = { onRevokePermission(PermissionType.NOTIFICATIONS) },
                isRequesting = isRequesting
            )
        }
    }
}

@Composable
private fun PermissionItem(
    permission: PermissionType,
    state: PermissionState,
    onRequest: () -> Unit,
    onRevoke: () -> Unit,
    isRequesting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = getPermissionStateText(state),
                style = MaterialTheme.typography.bodySmall,
                color = getPermissionStateColor(state)
            )
        }
        
        when (state) {
            PermissionState.GRANTED -> {
                Button(
                    onClick = onRevoke,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("撤销")
                }
            }
            
            PermissionState.DENIED, PermissionState.PERMANENTLY_DENIED -> {
                Button(
                    onClick = onRequest,
                    enabled = !isRequesting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("请求")
                    }
                }
            }
            
            PermissionState.NOT_DETERMINED -> {
                Button(
                    onClick = onRequest,
                    enabled = !isRequesting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("询问")
                    }
                }
            }
        }
    }
}

private fun getPermissionStateText(state: PermissionState): String {
    return when (state) {
        PermissionState.GRANTED -> "已授予"
        PermissionState.DENIED -> "已拒绝"
        PermissionState.PERMANENTLY_DENIED -> "永久拒绝"
        PermissionState.NOT_DETERMINED -> "未决定"
    }
}

private fun getPermissionStateColor(state: PermissionState): Color {
    return when (state) {
        PermissionState.GRANTED -> Color.Green
        PermissionState.DENIED -> Color.Orange
        PermissionState.PERMANENTLY_DENIED -> Color.Red
        PermissionState.NOT_DETERMINED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun DataFootprintCard(
    dataFootprint: DataFootprint,
    onCleanData: (DataType, CleanupScope) -> Unit,
    onAnonymizeAll: () -> Unit,
    isCleaning: Boolean,
    isAnonymizing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "数据足迹",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formatFileSize(dataFootprint.totalSize),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 数据分布
            DataDistributionChart(
                dataByType = dataFootprint.dataByType,
                totalSize = dataFootprint.totalSize
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 数据统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataStatisticItem(
                    label = "已加密",
                    value = formatFileSize(dataFootprint.encryptedSize),
                    color = MaterialTheme.colorScheme.primary
                )
                
                DataStatisticItem(
                    label = "已匿名",
                    value = formatFileSize(dataFootprint.anonymizedSize),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                DataStatisticItem(
                    label = "个人数据",
                    value = formatFileSize(dataFootprint.personalDataSize),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 数据清理按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 清理缓存
                Button(
                    onClick = { onCleanData(DataType.CACHE, CleanupScope.CACHE) },
                    enabled = !isCleaning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    if (isCleaning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("清理缓存")
                    }
                }
                
                // 匿名化所有数据
                Button(
                    onClick = onAnonymizeAll,
                    enabled = !isAnonymizing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isAnonymizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Text("匿名化")
                    }
                }
            }
        }
    }
}

@Composable
private fun DataDistributionChart(
    dataByType: Map<DataType, Long>,
    totalSize: Long
) {
    val sortedData = dataByType.entries
        .sortedByDescending { it.value }
        .take(5)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        sortedData.forEach { (type, size) ->
            val percentage = if (totalSize > 0) (size.toFloat() / totalSize * 100) else 0f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${"%.1f".format(percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = formatFileSize(size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            LinearProgressIndicator(
                progress = percentage / 100,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = getDataTypeColor(type)
            )
        }
    }
}

private fun getDataTypeColor(type: DataType): Color {
    return when (type) {
        DataType.CONTACTS -> Color(0xFF2196F3)
        DataType.MESSAGES -> Color(0xFF4CAF50)
        DataType.MEDIA -> Color(0xFFFF9800)
        DataType.LOCATION -> Color(0xFF9C27B0)
        DataType.CALL_LOGS -> Color(0xFFF44336)
        DataType.APP_USAGE -> Color(0xFF607D8B)
        DataType.CACHE -> Color(0xFF795548)
        DataType.TEMPORARY -> Color(0xFF009688)
    }
}

@Composable
private fun DataStatisticItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecurityIssuesCard(
    issues: List<SecurityIssue>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "安全问题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(text = issues.size.toString())
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 安全问题列表
            issues.take(3).forEach { issue ->
                SecurityIssueItem(issue = issue)
            }
            
            if (issues.size > 3) {
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { /* 查看所有问题 */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看所有 ${issues.size} 个问题")
                }
            }
        }
    }
}

@Composable
private fun SecurityIssueItem(issue: SecurityIssue) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (issue.severity) {
                SeverityLevel.CRITICAL -> Color.Red.copy(alpha = 0.1f)
                SeverityLevel.HIGH -> Color.Orange.copy(alpha = 0.1f)
                SeverityLevel.MEDIUM -> Color.Yellow.copy(alpha = 0.1f)
                SeverityLevel.LOW -> Color.Green.copy(alpha = 0.1f)
            }
        ),
        border = BorderStroke(
            1.dp,
            when (issue.severity) {
                SeverityLevel.CRITICAL -> Color.Red
                SeverityLevel.HIGH -> Color.Orange
                SeverityLevel.MEDIUM -> Color.Yellow
                SeverityLevel.LOW -> Color.Green
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = issue.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                SeverityBadge(severity = issue.severity)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = issue.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SeverityBadge(severity: SeverityLevel) {
    val (text, color) = when (severity) {
        SeverityLevel.CRITICAL -> Pair("严重", Color.Red)
        SeverityLevel.HIGH -> Pair("高危", Color.Orange)
        SeverityLevel.MEDIUM -> Pair("中危", Color.Yellow)
        SeverityLevel.LOW -> Pair("低危", Color.Green)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AuditLogsCard(
    auditLogs: List<PrivacyAuditLog>,
    isLoading: Boolean,
    onViewLogs: (Long?, Long?) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "隐私审计日志",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = onClearLogs,
                    enabled = auditLogs.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "清除日志",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (auditLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无审计日志",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 审计日志列表
                auditLogs.take(5).forEach { log ->
                    AuditLogItem(log = log)
                }
                
                if (auditLogs.size > 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { onViewLogs(null, null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("查看所有 ${auditLogs.size} 条日志")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogItem(log: PrivacyAuditLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = getAuditLogIcon(log.eventType)),
            contentDescription = log.eventType.name,
            tint = getAuditLogColor(log.eventType),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.eventType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Text(
            text = formatRelativeTime(log.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun getAuditLogIcon(eventType: PrivacyEventType): Int {
    return when (eventType) {
        PrivacyEventType.DATA_ACCESSED -> R.drawable.ic_data_access
        PrivacyEventType.DATA_MODIFIED -> R.drawable.ic_data_modified
        PrivacyEventType.DATA_DELETED -> R.drawable.ic_data_deleted
        PrivacyEventType.PERMISSION_CHANGED -> R.drawable.ic_permission
        PrivacyEventType.ENCRYPTION_CHANGED -> R.drawable.ic_encryption
        PrivacyEventType.LOGIN_ATTEMPT -> R.drawable.ic_login
        PrivacyEventType.NETWORK_CHANGE -> R.drawable.ic_network
        PrivacyEventType.APP_INSTALLED -> R.drawable.ic_app_install
        PrivacyEventType.APP_UPDATED -> R.drawable.ic_app_update
        PrivacyEventType.DATA_ANONYMIZED -> R.drawable.ic_anonymize
        PrivacyEventType.DATA_CLEANED -> R.drawable.ic_clean
        PrivacyEventType.PRIVACY_SETTING_CHANGED -> R.drawable.ic_settings
        PrivacyEventType.BATCH_DATA_DELETED -> R.drawable.ic_batch_delete
        PrivacyEventType.DATA_CLEARED -> R.drawable.ic_clear_all
    }
}

private fun getAuditLogColor(eventType: PrivacyEventType): Color {
    return when (eventType) {
        PrivacyEventType.DATA_DELETED,
        PrivacyEventType.BATCH_DATA_DELETED,
        PrivacyEventType.DATA_CLEARED -> MaterialTheme.colorScheme.error
        
        PrivacyEventType.LOGIN_ATTEMPT,
        PrivacyEventType.NETWORK_CHANGE -> MaterialTheme.colorScheme.primary
        
        PrivacyEventType.PERMISSION_CHANGED,
        PrivacyEventType.ENCRYPTION_CHANGED,
        PrivacyEventType.PRIVACY_SETTING_CHANGED -> MaterialTheme.colorScheme.secondary
        
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun DataExportCard(
    onExportData: () -> Unit,
    isExporting: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_export),
                contentDescription = "导出数据",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "导出隐私数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "导出您的隐私设置、审计日志和安全报告",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onExportData,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = "导出"
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text("导出隐私数据")
            }
        }
    }
}