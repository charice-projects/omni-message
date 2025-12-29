// 📁 app/src/main/java/com/omnimsg/app/ui/screens/HomeScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (AppDestinations) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val events = viewModel.events.collectAsState(initial = null)
    val scrollState = rememberScrollState()
    
    // 处理事件
    LaunchedEffect(events.value) {
        events.value?.let { event ->
            when (event) {
                is HomeEvent.ShowMessage -> onShowSnackbar(event.message)
                is HomeEvent.ShowError -> onShowSnackbar("错误: ${event.error}")
                is HomeEvent.NavigateTo -> onNavigate(event.destination)
                is HomeEvent.MessageSent -> onShowSnackbar(event.message)
                is HomeEvent.EmergencyAlertTriggered -> onShowSnackbar(event.message)
                is HomeEvent.SearchRequested -> {
                    onNavigate(AppDestinations.Search)
                    // 这里可以传递搜索查询参数
                }
            }
        }
    }
    
    // 加载状态
    if (uiState.isLoading) {
        FullScreenLoading()
        return
    }
    
    Scaffold(
        topBar = {
            HomeAppBar(
                unreadNotifications = uiState.unreadNotifications,
                emergencyModeActive = uiState.emergencyModeActive,
                onNotificationClick = { onNavigate(AppDestinations.NotificationCenter) },
                onEmergencyClick = { onNavigate(AppDestinations.EmergencySettings) },
                onSearchClick = { viewModel.performQuickAction(QuickAction.SEARCH) }
            )
        },
        floatingActionButton = {
            HomeFloatingActions(
                isVoiceInputActive = uiState.isVoiceInputActive,
                onVoiceClick = { viewModel.startVoiceInput() },
                onMessageClick = { viewModel.performQuickAction(QuickAction.NEW_MESSAGE) }
            )
        },
        content = { paddingValues ->
            HomeContent(
                uiState = uiState,
                scrollState = scrollState,
                paddingValues = paddingValues,
                onRefresh = { viewModel.refreshData() },
                onQuickAction = { viewModel.performQuickAction(it) },
                onContactClick = { contact ->
                    onNavigate(AppDestinations.ContactDetail.createRoute(contact.id))
                },
                onMessageClick = { message ->
                    // 导航到对话详情
                    onNavigate(AppDestinations.ConversationDetail.createRoute(message.conversationId))
                },
                onNotificationClick = { notification ->
                    // 处理通知点击
                    onNavigate(AppDestinations.NotificationCenter)
                },
                onTogglePrivacy = { viewModel.togglePrivacyProtection() }
            )
        }
    )
    
    // 语音输入指示器
    if (uiState.isVoiceInputActive) {
        VoiceInputOverlay(
            onCancel = { /* 取消逻辑由ViewModel处理 */ }
        )
    }
}

// 首页应用栏
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAppBar(
    unreadNotifications: Int,
    emergencyModeActive: Boolean,
    onNotificationClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "OmniMessage",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "OmniMessage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (emergencyModeActive) {
                    Badge(
                        containerColor = Color.Red.copy(alpha = 0.9f),
                        contentColor = Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        actions = {
            // 搜索按钮
            IconButton(onClick = onSearchClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "搜索"
                )
            }
            
            // 紧急按钮
            IconButton(onClick = onEmergencyClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_emergency),
                    contentDescription = "紧急设置",
                    tint = if (emergencyModeActive) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 通知按钮
            Box(modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = "通知"
                    )
                }
                
                if (unreadNotifications > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Text(
                            text = if (unreadNotifications > 99) "99+" else unreadNotifications.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    )
}

// 首页内容
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    scrollState: ScrollState,
    paddingValues: PaddingValues,
    onRefresh: () -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    onContactClick: (Contact) -> Unit,
    onMessageClick: (Message) -> Unit,
    onNotificationClick: (Notification) -> Unit,
    onTogglePrivacy: () -> Unit
) {
    val isRefreshing by remember { mutableStateOf(false) }
    
    PullRefresh(
        state = rememberPullRefreshState(isRefreshing, onRefresh),
        refreshing = isRefreshing,
        modifier = Modifier.padding(paddingValues)
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 系统状态卡片
            item {
                SystemStatusSection(
                    emergencyModeActive = uiState.emergencyModeActive,
                    privacyProtectionActive = uiState.privacyProtectionActive,
                    voiceWakeWordEnabled = uiState.voiceWakeWordEnabled,
                    onTogglePrivacy = onTogglePrivacy
                )
            }
            
            // 快速操作
            item {
                QuickActionsSection(
                    quickActions = uiState.quickActions,
                    onQuickAction = onQuickAction
                )
            }
            
            // 最近消息
            item {
                RecentMessagesSection(
                    messages = uiState.recentMessages,
                    onMessageClick = onMessageClick
                )
            }
            
            // 常用联系人
            item {
                FavoriteContactsSection(
                    contacts = uiState.favoriteContacts,
                    onContactClick = onContactClick
                )
            }
            
            // 通知
            item {
                NotificationsSection(
                    notifications = uiState.notifications,
                    onNotificationClick = onNotificationClick
                )
            }
            
            // 统计信息
            item {
                StatisticsSection(
                    todayMessages = uiState.todayMessages,
                    todayCalls = uiState.todayCalls,
                    storageUsage = uiState.storageUsage
                )
            }
            
            // 隐私状态
            item {
                PrivacyStatusSection(
                    privacyStatus = uiState.privacyStatus
                )
            }
        }
    }
}

// 系统状态区域
@Composable
private fun SystemStatusSection(
    emergencyModeActive: Boolean,
    privacyProtectionActive: Boolean,
    voiceWakeWordEnabled: Boolean,
    onTogglePrivacy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "系统状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    painter = painterResource(id = R.drawable.ic_security),
                    contentDescription = "安全状态",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 状态项
            SystemStatusItem(
                icon = R.drawable.ic_emergency,
                title = "紧急模式",
                status = if (emergencyModeActive) "已激活" else "未激活",
                isActive = emergencyModeActive,
                activeColor = Color.Red
            )
            
            SystemStatusItem(
                icon = R.drawable.ic_privacy,
                title = "隐私保护",
                status = if (privacyProtectionActive) "已开启" else "已关闭",
                isActive = privacyProtectionActive,
                onToggle = onTogglePrivacy,
                activeColor = MaterialTheme.colorScheme.primary
            )
            
            SystemStatusItem(
                icon = R.drawable.ic_voice_wake,
                title = "语音唤醒",
                status = if (voiceWakeWordEnabled) "已启用" else "已禁用",
                isActive = voiceWakeWordEnabled,
                activeColor = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// 系统状态项
@Composable
private fun SystemStatusItem(
    icon: Int,
    title: String,
    status: String,
    isActive: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onToggle: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = title,
            tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (onToggle != null) {
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = activeColor,
                    checkedTrackColor = activeColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// 快速操作区域
@Composable
private fun QuickActionsSection(
    quickActions: List<QuickAction>,
    onQuickAction: (QuickAction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "快速操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 快速操作网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(quickActions) { action ->
                    QuickActionItem(
                        action = action,
                        onClick = { onQuickAction(action) }
                    )
                }
            }
        }
    }
}

// 快速操作项
@Composable
private fun QuickActionItem(
    action: QuickAction,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(action.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = action.icon),
                contentDescription = action.title,
                tint = action.color,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = action.title,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = action.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 最近消息区域
@Composable
private fun RecentMessagesSection(
    messages: List<Message>,
    onMessageClick: (Message) -> Unit
) {
    if (messages.isEmpty()) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近消息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (messages.size > 3) {
                    TextButton(onClick = { /* 查看更多 */ }) {
                        Text(text = "查看更多")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            messages.take(3).forEach { message ->
                RecentMessageItem(
                    message = message,
                    onClick = { onMessageClick(message) }
                )
            }
        }
    }
}

// 最近消息项
@Composable
private fun RecentMessageItem(
    message: Message,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 联系人头像
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.senderName.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 消息内容
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = message.previewContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 未读指示器
        if (!message.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(8.dp)
            )
        }
    }
}

// 常用联系人区域
@Composable
private fun FavoriteContactsSection(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit
) {
    if (contacts.isEmpty()) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "常用联系人",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(contacts) { contact ->
                    FavoriteContactItem(
                        contact = contact,
                        onClick = { onContactClick(contact) }
                    )
                }
            }
        }
    }
}

// 常用联系人项
@Composable
private fun FavoriteContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (contact.avatarUrl != null) {
                // 显示联系人头像
                AsyncImage(
                    model = contact.avatarUrl,
                    contentDescription = contact.displayName,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = contact.displayName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = contact.displayName,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 通知区域
@Composable
private fun NotificationsSection(
    notifications: List<Notification>,
    onNotificationClick: (Notification) -> Unit
) {
    if (notifications.isEmpty()) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近通知",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(text = notifications.size.toString())
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            notifications.forEach { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { onNotificationClick(notification) }
                )
            }
        }
    }
}

// 通知项
@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            }
        ),
        border = if (!notification.isRead) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = notification.icon),
                contentDescription = notification.title,
                tint = notification.type.color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatNotificationTime(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            if (!notification.isRead) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}

// 统计信息区域
@Composable
private fun StatisticsSection(
    todayMessages: Int,
    todayCalls: Int,
    storageUsage: StorageUsage
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "今日统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    icon = R.drawable.ic_message,
                    value = todayMessages.toString(),
                    label = "消息"
                )
                
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                StatisticItem(
                    icon = R.drawable.ic_call,
                    value = todayCalls.toString(),
                    label = "通话"
                )
                
                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                StatisticItem(
                    icon = R.drawable.ic_storage,
                    value = "${(storageUsage.used / (1024 * 1024))}MB",
                    label = "已使用"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 存储使用进度条
            StorageUsageProgress(
                storageUsage = storageUsage
            )
        }
    }
}

// 统计项
@Composable
private fun StatisticItem(
    icon: Int,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 存储使用进度条
@Composable
private fun StorageUsageProgress(
    storageUsage: StorageUsage
) {
    val usedPercentage = if (storageUsage.total > 0) {
        (storageUsage.used.toFloat() / storageUsage.total) * 100
    } else 0f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "存储空间",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "${String.format("%.1f", usedPercentage)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    usedPercentage > 90 -> Color.Red
                    usedPercentage > 70 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = usedPercentage / 100,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = when {
                usedPercentage > 90 -> Color.Red
                usedPercentage > 70 -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${formatFileSize(storageUsage.used)} / ${formatFileSize(storageUsage.total)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 隐私状态区域
@Composable
private fun PrivacyStatusSection(
    privacyStatus: PrivacyStatus
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "隐私保护状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    painter = painterResource(id = R.drawable.ic_privacy_shield),
                    contentDescription = "隐私保护",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 隐私保护项
            PrivacyStatusItem(
                title = "端到端加密",
                enabled = privacyStatus.encryptionEnabled
            )
            
            PrivacyStatusItem(
                title = "数据匿名化",
                enabled = privacyStatus.dataAnonymization
            )
            
            PrivacyStatusItem(
                title = "位置隐私",
                enabled = privacyStatus.locationPrivacy
            )
            
            PrivacyStatusItem(
                title = "麦克风隐私",
                enabled = privacyStatus.microphonePrivacy
            )
            
            PrivacyStatusItem(
                title = "相机隐私",
                enabled = privacyStatus.cameraPrivacy
            )
            
            PrivacyStatusItem(
                title = "联系人隐私",
                enabled = privacyStatus.contactsPrivacy
            )
            
            PrivacyStatusItem(
                title = "消息加密",
                enabled = privacyStatus.messagesEncrypted
            )
        }
    }
}

// 隐私状态项
@Composable
private fun PrivacyStatusItem(
    title: String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = if (enabled) R.drawable.ic_check_circle else R.drawable.ic_error),
            contentDescription = title,
            tint = if (enabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = if (enabled) "已保护" else "未保护",
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    }
}

// 首页浮动按钮
@Composable
private fun HomeFloatingActions(
    isVoiceInputActive: Boolean,
    onVoiceClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    // 主浮动按钮
    FloatingActionButton(
        onClick = onMessageClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_add_message),
            contentDescription = "新消息"
        )
    }
    
    // 语音输入浮动按钮
    if (!isVoiceInputActive) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            FloatingActionButton(
                onClick = onVoiceClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.padding(end = 72.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mic),
                    contentDescription = "语音输入"
                )
            }
        }
    }
}

// 语音输入覆盖层
@Composable
private fun VoiceInputOverlay(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 录音动画
            LottieAnimation(
                composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.voice_recording)),
                modifier = Modifier.size(200.dp),
                iterations = LottieConstants.IterateForever
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "正在聆听...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "请说出您的命令",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f),
                    contentColor = Color.White
                ),
                border = BorderStroke(2.dp, Color.White)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "停止",
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(text = "取消录音")
            }
        }
    }
}

// 全屏加载
@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "正在准备您的安全通信环境",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// 工具函数
@Composable
private fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(timestamp)
    }
}

@Composable
private fun formatNotificationTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)
}

@Composable
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}