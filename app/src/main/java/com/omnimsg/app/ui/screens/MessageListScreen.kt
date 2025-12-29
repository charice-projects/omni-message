// 📁 app/src/main/java/com/omnimsg/app/ui/screens/MessageListScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    viewModel: MessageViewModel = hiltViewModel(),
    onNavigate: (AppDestinations) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val events = viewModel.events.collectAsState(initial = null)
    val searchQuery by viewModel.searchQuery.collectAsState()
    val scaffoldState = rememberScaffoldState()
    
    // 处理事件
    LaunchedEffect(events.value) {
        events.value?.let { event ->
            when (event) {
                is MessageEvent.ShowMessage -> onShowSnackbar(event.message)
                is MessageEvent.ShowError -> onShowSnackbar("错误: ${event.error}")
                is MessageEvent.NavigateToConversation -> {
                    onNavigate(AppDestinations.ConversationDetail.createRoute(event.conversationId))
                }
                is MessageEvent.NavigateToNewMessage -> {
                    onNavigate(AppDestinations.NewMessage)
                }
                is MessageEvent.MessageSent -> onShowSnackbar(event.message)
                is MessageEvent.EncryptionStatusChanged -> {
                    // 更新加密状态
                }
            }
        }
    }
    
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            MessageListTopBar(
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedCount,
                currentTab = uiState.currentTab,
                unreadCount = uiState.unreadCount,
                connectionStatus = uiState.connectionStatus,
                onNavigationIconClick = { /* 打开抽屉 */ },
                onSearchIconClick = { /* 激活搜索 */ },
                onSelectionModeToggle = { /* 切换选择模式 */ },
                onSelectAll = { viewModel.toggleSelectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onDeleteSelected = { viewModel.deleteSelectedConversations() },
                onArchiveSelected = { /* 归档选中的对话 */ },
                onMarkAsReadSelected = { /* 标记已读 */ },
                onSyncClick = { viewModel.syncMessages() }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                MessageFloatingActions(
                    isConnected = uiState.isConnected,
                    onNewMessageClick = {
                        viewModel.events.trySend(MessageEvent.NavigateToNewMessage())
                    },
                    onScanQRClick = { /* 扫描二维码添加联系人 */ },
                    onVoiceMessageClick = { /* 语音消息 */ }
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            FullScreenLoading()
            return@Scaffold
        }
        
        MessageListContent(
            uiState = uiState,
            paddingValues = paddingValues,
            onConversationClick = { conversation ->
                if (uiState.isSelectionMode) {
                    viewModel.toggleConversationSelection(conversation.id)
                } else {
                    onNavigate(AppDestinations.ConversationDetail.createRoute(conversation.id))
                }
            },
            onConversationLongClick = { conversation ->
                viewModel.toggleConversationSelection(conversation.id)
            },
            onToggleStar = { conversationId ->
                viewModel.toggleStar(conversationId)
            },
            onTogglePin = { conversationId ->
                viewModel.togglePin(conversationId)
            },
            onToggleMute = { conversationId ->
                viewModel.toggleMute(conversationId)
            },
            onMarkAsRead = { conversationId ->
                viewModel.markAsRead(conversationId)
            },
            onMarkAsUnread = { conversationId ->
                viewModel.markAsUnread(conversationId)
            },
            onArchive = { conversationId ->
                viewModel.archiveConversation(conversationId)
            },
            onDelete = { conversationId ->
                showDeleteConfirmation(conversationId, viewModel)
            },
            onSearchQueryChanged = { query ->
                viewModel.setSearchQuery(query)
            },
            onTabSelected = { tab ->
                viewModel.setCurrentTab(tab)
            },
            onRefresh = { viewModel.refresh() },
            onFilterChanged = { options ->
                viewModel.applyFilter(options)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageListTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    currentTab: MessageTab,
    unreadCount: Int,
    connectionStatus: ConnectionStatus,
    onNavigationIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onSelectionModeToggle: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onArchiveSelected: () -> Unit,
    onMarkAsReadSelected: () -> Unit,
    onSyncClick: () -> Unit
) {
    if (isSelectionMode) {
        SelectionModeTopBar(
            selectedCount = selectedCount,
            onBackClick = onClearSelection,
            onSelectAll = onSelectAll,
            onDelete = onDeleteSelected,
            onArchive = onArchiveSelected,
            onMarkAsRead = onMarkAsReadSelected
        )
    } else {
        NormalModeTopBar(
            currentTab = currentTab,
            unreadCount = unreadCount,
            connectionStatus = connectionStatus,
            onNavigationIconClick = onNavigationIconClick,
            onSearchClick = onSearchIconClick,
            onSelectionModeToggle = onSelectionModeToggle,
            onSyncClick = onSyncClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalModeTopBar(
    currentTab: MessageTab,
    unreadCount: Int,
    connectionStatus: ConnectionStatus,
    onNavigationIconClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSelectionModeToggle: () -> Unit,
    onSyncClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentTab.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (unreadCount > 0 && currentTab == MessageTab.INBOX) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                // 连接状态指示器
                ConnectionStatusIndicator(
                    status = connectionStatus,
                    modifier = Modifier.size(8.dp)
                )
            }
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
            // 同步按钮
            IconButton(onClick = onSyncClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sync),
                    contentDescription = "同步"
                )
            }
            
            // 搜索按钮
            IconButton(onClick = onSearchClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "搜索"
                )
            }
            
            // 选择模式切换
            IconButton(onClick = onSelectionModeToggle) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_select_all),
                    contentDescription = "选择模式"
                )
            }
            
            // 更多选项
            MessageListMenu()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    )
}

@Composable
private fun ConnectionStatusIndicator(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        ConnectionStatus.CONNECTED -> Color.Green
        ConnectionStatus.CONNECTING -> Color.Yellow
        ConnectionStatus.DISCONNECTED -> Color.Red
        ConnectionStatus.ERROR -> Color.Red
        ConnectionStatus.LIMITED -> Color.Orange
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun MessageListMenu() {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more_vert),
                contentDescription = "更多选项"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("过滤消息") },
                onClick = { /* 打开过滤对话框 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter),
                        contentDescription = null
                    )
                }
            )
            
            DropdownMenuItem(
                text = { Text("排序方式") },
                onClick = { /* 打开排序对话框 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sort),
                        contentDescription = null
                    )
                }
            )
            
            Divider()
            
            DropdownMenuItem(
                text = { Text("标记所有为已读") },
                onClick = { /* 标记所有 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mark_read),
                        contentDescription = null
                    )
                }
            )
            
            DropdownMenuItem(
                text = { Text("清除所有对话") },
                onClick = { /* 清除所有 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clear_all),
                        contentDescription = null
                    )
                }
            )
            
            Divider()
            
            DropdownMenuItem(
                text = { Text("消息设置") },
                onClick = { /* 导航到设置 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = null
                    )
                }
            )
            
            DropdownMenuItem(
                text = { Text("加密状态") },
                onClick = { /* 显示加密状态 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_encryption),
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionModeTopBar(
    selectedCount: Int,
    onBackClick: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "已选择 $selectedCount 项",
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "取消选择"
                )
            }
        },
        actions = {
            // 全选/取消全选
            IconButton(onClick = onSelectAll) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_select_all),
                    contentDescription = "全选"
                )
            }
            
            // 标记已读
            IconButton(onClick = onMarkAsRead) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mark_read),
                    contentDescription = "标记已读"
                )
            }
            
            // 归档
            IconButton(onClick = onArchive) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_archive),
                    contentDescription = "归档"
                )
            }
            
            // 删除
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun MessageFloatingActions(
    isConnected: Boolean,
    onNewMessageClick: () -> Unit,
    onScanQRClick: () -> Unit,
    onVoiceMessageClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        // 扩展的浮动按钮
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 72.dp)
            ) {
                // 语音消息按钮
                ExtendedFloatingActionButton(
                    onClick = {
                        onVoiceMessageClick()
                        expanded = false
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mic),
                            contentDescription = "语音消息"
                        )
                    },
                    text = { Text("语音消息") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
                
                // 扫描二维码按钮
                ExtendedFloatingActionButton(
                    onClick = {
                        onScanQRClick()
                        expanded = false
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_qr_code),
                            contentDescription = "扫描二维码"
                        )
                    },
                    text = { Text("扫描二维码") },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
        
        // 主浮动按钮
        FloatingActionButton(
            onClick = {
                if (expanded) {
                    onNewMessageClick()
                } else {
                    expanded = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            AnimatedContent(
                targetState = expanded,
                label = "fab-icon"
            ) { isExpanded ->
                if (isExpanded) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_message),
                        contentDescription = "新消息"
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "更多操作"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageListContent(
    uiState: MessageListUiState,
    paddingValues: PaddingValues,
    onConversationClick: (Conversation) -> Unit,
    onConversationLongClick: (Conversation) -> Unit,
    onToggleStar: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAsUnread: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTabSelected: (MessageTab) -> Unit,
    onRefresh: () -> Unit,
    onFilterChanged: (MessageFilterOptions) -> Unit
) {
    val isRefreshing by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(paddingValues)) {
        // 标签页
        MessageTabs(
            currentTab = uiState.currentTab,
            unreadCount = uiState.unreadCount,
            starredCount = uiState.starredCount,
            onTabSelected = onTabSelected,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 搜索栏（如果激活）
        if (uiState.isSearchActive) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onSearch = { },
                active = uiState.isSearchActive,
                onActiveChange = { /* 更新搜索状态 */ },
                placeholder = { Text("搜索消息") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // 统计卡片（收件箱页面）
        if (uiState.currentTab == MessageTab.INBOX && uiState.searchQuery.isEmpty()) {
            MessageStatisticsCard(
                statistics = uiState.statistics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // 对话列表
        PullRefresh(
            state = rememberPullRefreshState(isRefreshing, onRefresh),
            refreshing = isRefreshing,
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.filteredConversations.isEmpty()) {
                EmptyConversationState(
                    currentTab = uiState.currentTab,
                    onNewMessage = { onTabSelected(MessageTab.INBOX) }
                )
            } else {
                ConversationListView(
                    conversations = uiState.filteredConversations,
                    selectedIds = emptyList(), // 从ViewModel获取
                    onConversationClick = onConversationClick,
                    onConversationLongClick = onConversationLongClick,
                    onToggleStar = onToggleStar,
                    onTogglePin = onTogglePin,
                    onToggleMute = onToggleMute,
                    onMarkAsRead = onMarkAsRead,
                    onMarkAsUnread = onMarkAsUnread,
                    onArchive = onArchive,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun MessageTabs(
    currentTab: MessageTab,
    unreadCount: Int,
    starredCount: Int,
    onTabSelected: (MessageTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrollState by remember { mutableStateOf(0f) }
    
    val tabs = listOf(
        MessageTab.INBOX,
        MessageTab.UNREAD,
        MessageTab.STARRED,
        MessageTab.SENT,
        MessageTab.DRAFTS,
        MessageTab.ARCHIVED
    )
    
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(currentTab),
        modifier = modifier,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {
            TabRowDefaults.Divider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        },
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(currentTab)]),
                height = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        tabs.forEach { tab ->
            val badgeCount = when (tab) {
                MessageTab.INBOX -> unreadCount
                MessageTab.STARRED -> starredCount
                else -> 0
            }
            
            Tab(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge
                        )
                        
                        if (badgeCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = tab.icon),
                        contentDescription = tab.title,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun MessageStatisticsCard(
    statistics: MessageStatistics,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "消息统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${statistics.totalMessages} 条",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    value = statistics.todayMessages.toString(),
                    label = "今日消息",
                    icon = R.drawable.ic_today
                )
                
                StatisticItem(
                    value = statistics.unreadMessages.toString(),
                    label = "未读",
                    icon = R.drawable.ic_unread
                )
                
                StatisticItem(
                    value = statistics.encryptedMessages.toString(),
                    label = "加密",
                    icon = R.drawable.ic_encryption
                )
            }
            
            // 最活跃联系人
            if (statistics.topContacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "最活跃联系人",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                TopContactsList(
                    contacts = statistics.topContacts.take(3)
                )
            }
        }
    }
}

@Composable
private fun TopContactsList(
    contacts: List<MessageContact>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        contacts.forEach { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 联系人头像
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "${contact.messageCount} 条消息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = formatRelativeTime(contact.lastInteraction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun EmptyConversationState(
    currentTab: MessageTab,
    onNewMessage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = currentTab.icon),
            contentDescription = currentTab.title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when (currentTab) {
                MessageTab.INBOX -> "收件箱空空如也"
                MessageTab.UNREAD -> "没有未读消息"
                MessageTab.STARRED -> "暂无星标消息"
                MessageTab.SENT -> "没有已发送的消息"
                MessageTab.DRAFTS -> "暂无草稿"
                MessageTab.ARCHIVED -> "没有归档的消息"
                MessageTab.SPAM -> "没有垃圾消息"
                MessageTab.TRASH -> "回收站为空"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when (currentTab) {
                MessageTab.INBOX -> "发送第一条消息开始对话吧"
                MessageTab.UNREAD -> "所有消息都已阅读"
                MessageTab.STARRED -> "将重要消息标记为星标"
                MessageTab.SENT -> "发送消息后会显示在这里"
                MessageTab.DRAFTS -> "编写消息时自动保存为草稿"
                MessageTab.ARCHIVED -> "归档的对话会显示在这里"
                MessageTab.SPAM -> "垃圾消息会被自动过滤"
                MessageTab.TRASH -> "已删除的消息会保留30天"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (currentTab == MessageTab.INBOX) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onNewMessage,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_message),
                    contentDescription = "新消息"
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text("发送第一条消息")
            }
        }
    }
}

@Composable
private fun ConversationListView(
    conversations: List<Conversation>,
    selectedIds: List<String>,
    onConversationClick: (Conversation) -> Unit,
    onConversationLongClick: (Conversation) -> Unit,
    onToggleStar: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAsUnread: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 置顶的对话
        val pinnedConversations = conversations.filter { it.isPinned }
        if (pinnedConversations.isNotEmpty()) {
            item {
                ConversationListSectionHeader(
                    title = "置顶对话",
                    count = pinnedConversations.size
                )
            }
            
            items(pinnedConversations, key = { it.id }) { conversation ->
                ConversationListItem(
                    conversation = conversation,
                    isSelected = selectedIds.contains(conversation.id),
                    onClick = { onConversationClick(conversation) },
                    onLongClick = { onConversationLongClick(conversation) },
                    onToggleStar = { onToggleStar(conversation.id) },
                    onTogglePin = { onTogglePin(conversation.id) },
                    onToggleMute = { onToggleMute(conversation.id) },
                    onMarkAsRead = { onMarkAsRead(conversation.id) },
                    onMarkAsUnread = { onMarkAsUnread(conversation.id) },
                    onArchive = { onArchive(conversation.id) },
                    onDelete = { onDelete(conversation.id) }
                )
            }
            
            item {
                ConversationListSectionHeader(
                    title = "所有对话",
                    count = conversations.size - pinnedConversations.size
                )
            }
        }
        
        // 其他对话
        val otherConversations = conversations.filter { !it.isPinned }
        items(otherConversations, key = { it.id }) { conversation ->
            ConversationListItem(
                conversation = conversation,
                isSelected = selectedIds.contains(conversation.id),
                onClick = { onConversationClick(conversation) },
                onLongClick = { onConversationLongClick(conversation) },
                onToggleStar = { onToggleStar(conversation.id) },
                onTogglePin = { onTogglePin(conversation.id) },
                onToggleMute = { onToggleMute(conversation.id) },
                onMarkAsRead = { onMarkAsRead(conversation.id) },
                onMarkAsUnread = { onMarkAsUnread(conversation.id) },
                onArchive = { onArchive(conversation.id) },
                onDelete = { onDelete(conversation.id) }
            )
        }
    }
}

@Composable
private fun ConversationListSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ConversationListItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleStar: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择指示器
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_circle),
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // 对话头像
            ConversationAvatar(
                conversation = conversation,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 对话信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 时间
                        Text(
                            text = formatMessageTime(conversation.lastMessageTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 加密指示器
                        if (conversation.isEncrypted) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_encryption),
                                contentDescription = "已加密",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        // 静音指示器
                        if (conversation.isMuted) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mute),
                                contentDescription = "已静音",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 最后消息预览
                    Text(
                        text = conversation.lastMessageContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.unreadCount > 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 未读计数
                    if (conversation.unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" 
                                else conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                // 草稿提示
                conversation.draftMessage?.let { draft ->
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_draft),
                            contentDescription = "草稿",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Text(
                            text = "草稿: $draft",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // 星标按钮
            IconButton(
                onClick = onToggleStar,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (conversation.isStarred) R.drawable.ic_star_filled
                        else R.drawable.ic_star_border
                    ),
                    contentDescription = if (conversation.isStarred) "取消星标" else "标记星标",
                    tint = if (conversation.isStarred) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ConversationAvatar(
    conversation: Conversation,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (conversation.isGroup && conversation.groupIcon != null) {
            // 群组图标
            AsyncImage(
                model = conversation.groupIcon,
                contentDescription = conversation.title,
                modifier = Modifier.fillMaxSize()
            )
        } else if (conversation.participants.size == 1) {
            // 单人对话
            val participant = conversation.participants.firstOrNull()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (participant?.avatarUrl != null) {
                    AsyncImage(
                        model = participant.avatarUrl,
                        contentDescription = participant.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = participant?.name?.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // 在线状态
                if (participant?.isOnline == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                    )
                }
            }
        } else {
            // 多人对话（显示多个头像）
            val participants = conversation.participants.take(4)
            
            when (participants.size) {
                1 -> SingleAvatar(participant = participants[0])
                2 -> DoubleAvatar(participants = participants)
                3 -> TripleAvatar(participants = participants)
                4 -> QuadAvatar(participants = participants)
                else -> GroupAvatar(count = conversation.participants.size)
            }
        }
        
        // 置顶指示器
        if (conversation.isPinned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun SingleAvatar(participant: ConversationParticipant) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (participant.avatarUrl != null) {
            AsyncImage(
                model = participant.avatarUrl,
                contentDescription = participant.name,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = participant.name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DoubleAvatar(participants: List<ConversationParticipant>) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 第一个头像（左上）
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[0].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // 第二个头像（右下）
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[1].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TripleAvatar(participants: List<ConversationParticipant>) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 第一个头像（上中）
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[0].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // 第二个头像（左下）
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .align(Alignment.BottomStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[1].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        // 第三个头像（右下）
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[2].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun QuadAvatar(participants: List<ConversationParticipant>) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 左上
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[0].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // 右上
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[1].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        // 左下
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .align(Alignment.BottomStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[2].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        
        // 右下
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
                .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participants[3].name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun GroupAvatar(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 删除确认对话框
@Composable
private fun showDeleteConfirmation(
    conversationId: String,
    viewModel: MessageViewModel
) {
    var showDialog by remember { mutableStateOf(true) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("删除对话") },
            text = { Text("确定要删除这个对话吗？所有消息将被永久删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(conversationId)
                        showDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
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
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(timestamp)
    }
}