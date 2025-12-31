// 📁 app/src/main/java/com/omnimsg/app/ui/screens/ContactListScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactViewModel = hiltViewModel(),
    onNavigate: (AppDestinations) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val events = viewModel.events.collectAsState(initial = null)
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // 处理事件
    LaunchedEffect(events.value) {
        events.value?.let { event ->
            when (event) {
                is ContactEvent.ShowMessage -> onShowSnackbar(event.message)
                is ContactEvent.ShowError -> onShowSnackbar("错误: ${event.error}")
                is ContactEvent.NavigateToContactDetail -> {
                    onNavigate(AppDestinations.ContactDetail.createRoute(event.contactId))
                }
                is ContactEvent.NavigateToNewContact -> {
                    onNavigate(AppDestinations.NewContact)
                }
                is ContactEvent.ExportCompleted -> {
                    onShowSnackbar("已导出 ${event.contactCount} 个联系人")
                }
                is ContactEvent.ImportCompleted -> {
                    val message = buildString {
                        append("导入完成: ${event.importedCount} 成功")
                        if (event.skippedCount > 0) append(", ${event.skippedCount} 跳过")
                        if (event.errors.isNotEmpty()) append(", ${event.errors.size} 错误")
                    }
                    onShowSnackbar(message)
                }
                is ContactEvent.GroupCreated -> {
                    onNavigate(AppDestinations.GroupDetail.createRoute(event.groupId))
                }
            }
        }
    }
    
    val scaffoldState = rememberScaffoldState()
    
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ContactListTopBar(
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedCount,
                onNavigationIconClick = { /* 打开抽屉 */ },
                onSearchIconClick = { /* 激活搜索 */ },
                onSelectionModeToggle = { /* 切换选择模式 */ },
                onSelectAll = { viewModel.toggleSelectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onDeleteSelected = { viewModel.deleteSelectedContacts() },
                onExportSelected = { /* 导出选中的联系人 */ },
                onShareSelected = { /* 分享选中的联系人 */ }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.events.trySend(ContactEvent.NavigateToNewContact()) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add_person),
                            contentDescription = "添加联系人"
                        )
                    },
                    text = { Text("添加联系人") },
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
        
        ContactListContent(
            uiState = uiState,
            paddingValues = paddingValues,
            onContactClick = { contact ->
                if (uiState.isSelectionMode) {
                    viewModel.toggleContactSelection(contact.id)
                } else {
                    onNavigate(AppDestinations.ContactDetail.createRoute(contact.id))
                }
            },
            onContactLongClick = { contact ->
                viewModel.toggleContactSelection(contact.id)
            },
            onToggleFavorite = { contactId ->
                viewModel.toggleFavorite(contactId)
            },
            onDeleteContact = { contactId ->
                // 显示确认对话框
                showDeleteConfirmation(contactId, viewModel)
            },
            onSearchQueryChanged = { query ->
                viewModel.setSearchQuery(query)
            },
            onRefresh = { viewModel.refresh() },
            onGroupSelected = { groupId ->
                viewModel.filterByGroup(groupId)
            },
            onDisplayModeChanged = { /* 更新显示模式 */ },
            onSortOrderChanged = { /* 更新排序方式 */ }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactListTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onNavigationIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onSelectionModeToggle: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onShareSelected: () -> Unit
) {
    if (isSelectionMode) {
        SelectionModeTopBar(
            selectedCount = selectedCount,
            onBackClick = onClearSelection,
            onSelectAll = onSelectAll,
            onDelete = onDeleteSelected,
            onExport = onExportSelected,
            onShare = onShareSelected
        )
    } else {
        NormalModeTopBar(
            onNavigationIconClick = onNavigationIconClick,
            onSearchClick = onSearchIconClick,
            onSelectionModeToggle = onSelectionModeToggle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalModeTopBar(
    onNavigationIconClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSelectionModeToggle: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "联系人",
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
            ContactListMenu()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionModeTopBar(
    selectedCount: Int,
    onBackClick: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit
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
            
            // 分享
            IconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = "分享"
                )
            }
            
            // 导出
            IconButton(onClick = onExport) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_export),
                    contentDescription = "导出"
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
private fun ContactListMenu() {
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
                text = { Text("导入联系人") },
                onClick = { /* 打开导入对话框 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_import),
                        contentDescription = null
                    )
                }
            )
            
            DropdownMenuItem(
                text = { Text("导出所有联系人") },
                onClick = { /* 打开导出对话框 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_export),
                        contentDescription = null
                    )
                }
            )
            
            Divider()
            
            DropdownMenuItem(
                text = { Text("显示模式") },
                onClick = { /* 显示模式选择 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_view),
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
            
            DropdownMenuItem(
                text = { Text("过滤选项") },
                onClick = { /* 打开过滤对话框 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter),
                        contentDescription = null
                    )
                }
            )
            
            Divider()
            
            DropdownMenuItem(
                text = { Text("分组管理") },
                onClick = { /* 导航到分组管理 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_group),
                        contentDescription = null
                    )
                }
            )
            
            DropdownMenuItem(
                text = { Text("合并重复联系人") },
                onClick = { /* 合并重复项 */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_merge),
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactListContent(
    uiState: ContactListUiState,
    paddingValues: PaddingValues,
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onGroupSelected: (String?) -> Unit,
    onDisplayModeChanged: (DisplayMode) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit
) {
    val isRefreshing by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(paddingValues)) {
        // 搜索栏
        if (uiState.isSearchActive) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onSearch = { },
                active = uiState.isSearchActive,
                onActiveChange = { /* 更新搜索状态 */ },
                placeholder = { Text("搜索联系人") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // 统计卡片
        if (!uiState.isSearchActive && uiState.searchQuery.isEmpty()) {
            ContactStatisticsCard(
                statistics = uiState.statistics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // 分组筛选
        ContactGroupFilter(
            groups = uiState.groups,
            currentGroup = null,
            onGroupSelected = onGroupSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 联系人列表
        PullRefresh(
            state = rememberPullRefreshState(isRefreshing, onRefresh),
            refreshing = isRefreshing,
            modifier = Modifier.fillMaxSize()
        ) {
            when (uiState.displayMode) {
                DisplayMode.LIST -> ContactListView(
                    contacts = uiState.filteredContacts,
                    selectedIds = emptyList(), // 从ViewModel获取
                    onContactClick = onContactClick,
                    onContactLongClick = onContactLongClick,
                    onToggleFavorite = onToggleFavorite,
                    onDeleteContact = onDeleteContact
                )
                
                DisplayMode.GRID -> ContactGridView(
                    contacts = uiState.filteredContacts,
                    selectedIds = emptyList(),
                    onContactClick = onContactClick,
                    onContactLongClick = onContactLongClick,
                    onToggleFavorite = onToggleFavorite
                )
                
                DisplayMode.COMPACT -> ContactCompactView(
                    contacts = uiState.filteredContacts,
                    selectedIds = emptyList(),
                    onContactClick = onContactClick,
                    onContactLongClick = onContactLongClick,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun ContactStatisticsCard(
    statistics: ContactStatistics,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "联系人统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${statistics.totalContacts} 人",
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
                    value = statistics.favoriteContacts.toString(),
                    label = "收藏",
                    icon = R.drawable.ic_favorite
                )
                
                StatisticItem(
                    value = statistics.recentContacts.toString(),
                    label = "最近联系",
                    icon = R.drawable.ic_recent
                )
                
                StatisticItem(
                    value = "${statistics.interactionStats.todayMessages}",
                    label = "今日消息",
                    icon = R.drawable.ic_message
                )
            }
            
            // 公司分布（如果有数据）
            if (statistics.companyDistribution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "公司分布",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                CompanyDistributionChart(
                    distribution = statistics.companyDistribution
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    value: String,
    label: String,
    icon: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompanyDistributionChart(
    distribution: Map<String, Int>,
    maxItems: Int = 5
) {
    val sortedCompanies = distribution.entries
        .sortedByDescending { it.value }
        .take(maxItems)
    
    val total = distribution.values.sum()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        sortedCompanies.forEach { (company, count) ->
            val percentage = if (total > 0) (count.toFloat() / total * 100) else 0f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = company,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "$count (${"%.1f".format(percentage)}%)",
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
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ContactGroupFilter(
    groups: List<ContactGroup>,
    currentGroup: String?,
    onGroupSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 所有联系人按钮
        FilterChip(
            selected = currentGroup == null,
            onClick = { onGroupSelected(null) },
            label = { Text("所有联系人") },
            leadingIcon = if (currentGroup == null) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 分组筛选
        groups.take(3).forEach { group ->
            FilterChip(
                selected = currentGroup == group.id,
                onClick = { onGroupSelected(group.id) },
                label = { Text("${group.name} (${group.memberCount})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = group.color,
                    selectedLabelColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // 更多分组
        if (groups.size > 3) {
            Box {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text("更多") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "更多分组"
                        )
                    }
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    groups.drop(3).forEach { group ->
                        DropdownMenuItem(
                            text = { Text("${group.name} (${group.memberCount})") },
                            onClick = {
                                onGroupSelected(group.id)
                                expanded = false
                            }
                        )
                    }
                    
                    Divider()
                    
                    DropdownMenuItem(
                        text = { Text("管理分组") },
                        onClick = {
                            // 导航到分组管理
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactListView(
    contacts: List<Contact>,
    selectedIds: List<String>,
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteContact: (String) -> Unit
) {
    if (contacts.isEmpty()) {
        EmptyState(
            title = "暂无联系人",
            description = "点击右下角按钮添加第一个联系人",
            icon = R.drawable.ic_contact
        )
        return
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 按首字母分组
        val groupedContacts = contacts.groupBy { 
            it.displayName.firstOrNull()?.uppercaseChar() ?: '#'
        }.toSortedMap()
        
        groupedContacts.forEach { (initial, groupContacts) ->
            item {
                ContactListSectionHeader(
                    initial = initial.toString(),
                    count = groupContacts.size
                )
            }
            
            items(groupContacts, key = { it.id }) { contact ->
                ContactListItem(
                    contact = contact,
                    isSelected = selectedIds.contains(contact.id),
                    onClick = { onContactClick(contact) },
                    onLongClick = { onContactLongClick(contact) },
                    onToggleFavorite = { onToggleFavorite(contact.id) },
                    onDelete = { onDeleteContact(contact.id) }
                )
            }
        }
    }
}

@Composable
private fun ContactListSectionHeader(
    initial: String,
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
            text = initial,
            style = MaterialTheme.typography.titleMedium,
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
private fun ContactListItem(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
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
            
            // 联系人头像
            ContactAvatar(
                contact = contact,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 联系人信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (contact.isFavorite) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_favorite_filled),
                            contentDescription = "已收藏",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 主要电话号码
                contact.phoneNumbers.firstOrNull { it.isPrimary }?.let { phone ->
                    Text(
                        text = phone.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 公司信息
                contact.company?.let { company ->
                    Text(
                        text = company,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 标签（如果有）
                if (contact.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        contact.tags.take(3).forEach { tag ->
                            SuggestionChip(
                                onClick = { /* 按标签筛选 */ },
                                label = { Text(tag) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }
            
            // 操作菜单
            ContactItemMenu(
                contact = contact,
                onToggleFavorite = onToggleFavorite,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun ContactAvatar(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (contact.avatarUrl != null) {
            // 显示头像图片
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
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // 在线状态指示器
        if (contact.lastContacted != null && 
            System.currentTimeMillis() - contact.lastContacted!! < 5 * 60 * 1000) {
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
}

@Composable
private fun ContactItemMenu(
    contact: Contact,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more_vert),
                contentDescription = "更多操作"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 收藏/取消收藏
            DropdownMenuItem(
                text = { 
                    Text(if (contact.isFavorite) "取消收藏" else "添加到收藏") 
                },
                onClick = {
                    onToggleFavorite()
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            id = if (contact.isFavorite) R.drawable.ic_favorite_border
                            else R.drawable.ic_favorite
                        ),
                        contentDescription = null
                    )
                }
            )
            
            // 发送消息
            DropdownMenuItem(
                text = { Text("发送消息") },
                onClick = { 
                    // 导航到消息发送界面
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_message),
                        contentDescription = null
                    )
                }
            )
            
            // 拨打电话
            if (contact.phoneNumbers.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("拨打电话") },
                    onClick = {
                        // 拨打电话
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_call),
                            contentDescription = null
                        )
                    }
                )
            }
            
            // 编辑
            DropdownMenuItem(
                text = { Text("编辑") },
                onClick = {
                    // 导航到编辑界面
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = null
                    )
                }
            )
            
            Divider()
            
            // 添加到分组
            DropdownMenuItem(
                text = { Text("添加到分组") },
                onClick = { 
                    // 显示分组选择对话框
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_group_add),
                        contentDescription = null
                    )
                }
            )
            
            // 分享联系人
            DropdownMenuItem(
                text = { Text("分享") },
                onClick = {
                    // 分享联系人信息
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = null
                    )
                }
            )
            
            Divider()
            
            // 删除
            DropdownMenuItem(
                text = { 
                    Text(
                        "删除",
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                onClick = {
                    onDelete()
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
private fun ContactGridView(
    contacts: List<Contact>,
    selectedIds: List<String>,
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (contacts.isEmpty()) {
        EmptyState(
            title = "暂无联系人",
            description = "点击右下角按钮添加第一个联系人",
            icon = R.drawable.ic_contact
        )
        return
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(contacts, key = { it.id }) { contact ->
            ContactGridItem(
                contact = contact,
                isSelected = selectedIds.contains(contact.id),
                onClick = { onContactClick(contact) },
                onLongClick = { onContactLongClick(contact) },
                onToggleFavorite = { onToggleFavorite(contact.id) }
            )
        }
    }
}

@Composable
private fun ContactGridItem(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.8f)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 选择指示器
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // 头像
            ContactAvatar(
                contact = contact,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 姓名
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 公司
            contact.company?.let { company ->
                Text(
                    text = company,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 收藏按钮
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (contact.isFavorite) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_border
                    ),
                    contentDescription = if (contact.isFavorite) "取消收藏" else "收藏",
                    tint = if (contact.isFavorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactCompactView(
    contacts: List<Contact>,
    selectedIds: List<String>,
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (contacts.isEmpty()) {
        EmptyState(
            title = "暂无联系人",
            description = "点击右下角按钮添加第一个联系人",
            icon = R.drawable.ic_contact
        )
        return
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(contacts, key = { it.id }) { contact ->
            ContactCompactItem(
                contact = contact,
                isSelected = selectedIds.contains(contact.id),
                onClick = { onContactClick(contact) },
                onLongClick = { onContactLongClick(contact) }
            )
        }
    }
}

@Composable
private fun ContactCompactItem(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选择指示器
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_circle),
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        // 小型头像
        ContactAvatar(
            contact = contact,
            modifier = Modifier.size(36.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 姓名
        Text(
            text = contact.displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // 收藏状态
        if (contact.isFavorite) {
            Icon(
                painter = painterResource(id = R.drawable.ic_favorite_filled),
                contentDescription = "已收藏",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    description: String,
    icon: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// 删除确认对话框
@Composable
private fun showDeleteConfirmation(
    contactId: String,
    viewModel: ContactViewModel
) {
    var showDialog by remember { mutableStateOf(true) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("删除联系人") },
            text = { Text("确定要删除这个联系人吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContact(contactId)
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