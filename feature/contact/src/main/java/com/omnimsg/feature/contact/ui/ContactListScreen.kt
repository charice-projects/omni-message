// 📁 feature/contact/ui/ContactListScreen.kt
package com.omnimsg.feature.contact.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.omnimsg.app.ui.navigation.AppDestinations
import com.omnimsg.feature.contact.data.Contact
import com.omnimsg.feature.contact.data.ContactGroup
import com.omnimsg.feature.contact.data.RelationshipType
import com.omnimsg.feature.contact.viewmodels.ContactListViewModel
import com.omnimsg.shared.ui.components.LoadingIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactListScreen(
    navController: NavController,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    // 分组状态
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    
    // 选择状态
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedContacts by remember { mutableStateOf(setOf<String>()) }
    
    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 悬浮操作栏可见性
    val showQuickActions by remember { derivedStateOf {
        !isSearchActive && uiState.contacts.isNotEmpty() && !isSelectionMode
    } }
    
    // 处理下拉刷新
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.loadContacts()
            isRefreshing = false
        }
    )
    
    // 处理联系人点击
    fun onContactClick(contactId: String) {
        if (isSelectionMode) {
            selectedContacts = if (selectedContacts.contains(contactId)) {
                selectedContacts - contactId
            } else {
                selectedContacts + contactId
            }
        } else {
            navController.navigate("${AppDestinations.ContactDetail.route}/$contactId")
        }
    }
    
    // 处理联系人长按
    fun onContactLongClick(contactId: String) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedContacts = setOf(contactId)
        }
    }
    
    // 处理创建联系人
    fun onCreateContact() {
        navController.navigate(AppDestinations.ContactCreate.route)
    }
    
    // 处理群组管理
    fun onGroupManagement() {
        navController.navigate(AppDestinations.GroupManagement.route)
    }
    
    // 处理全选/取消全选
    fun onSelectAll() {
        selectedContacts = if (selectedContacts.size == uiState.contacts.size) {
            emptySet()
        } else {
            uiState.contacts.map { it.id }.toSet()
        }
    }
    
    // 处理取消选择模式
    fun onCancelSelection() {
        isSelectionMode = false
        selectedContacts = emptySet()
    }
    
    // 处理删除选中的联系人
    fun onDeleteSelected() {
        viewModel.deleteContacts(selectedContacts.toList())
        isSelectionMode = false
        selectedContacts = emptySet()
    }
    
    // 分组联系人（按首字母）
    val groupedContacts = remember(uiState.contacts, searchQuery) {
        if (searchQuery.isNotEmpty()) {
            uiState.contacts.groupBy { 
                it.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            }.toSortedMap()
        } else {
            uiState.contacts.groupBy { 
                it.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            }.toSortedMap()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 搜索栏
            ContactSearchBar(
                query = searchQuery,
                onQueryChange = { 
                    searchQuery = it
                    viewModel.searchContacts(it)
                },
                onSearchActiveChange = { isSearchActive = it },
                isSearchActive = isSearchActive,
                onClearClick = { 
                    searchQuery = ""
                    viewModel.clearSearch()
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 选择模式的应用栏
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                SelectionAppBar(
                    selectedCount = selectedContacts.size,
                    totalCount = uiState.contacts.size,
                    onSelectAll = ::onSelectAll,
                    onCancel = ::onCancelSelection,
                    onDelete = ::onDeleteSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 联系人列表内容
            PullRefresh(
                state = pullRefreshState,
                refreshing = isRefreshing,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingIndicator(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    }
                    
                    uiState.contacts.isEmpty() -> {
                        EmptyContactsPlaceholder(
                            onImportClick = { navController.navigate(AppDestinations.ExcelImport.route) },
                            onCreateClick = ::onCreateContact,
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    }
                    
                    else -> {
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // 按字母分组显示
                            groupedContacts.forEach { (initial, contacts) ->
                                // 字母索引标题
                                stickyHeader {
                                    LetterIndexHeader(
                                        letter = initial.toString(),
                                        contactCount = contacts.size,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                
                                // 联系人项
                                items(contacts, key = { it.id }) { contact ->
                                    ContactListItem(
                                        contact = contact,
                                        isSelected = selectedContacts.contains(contact.id),
                                        isSelectionMode = isSelectionMode,
                                        onClick = { onContactClick(contact.id) },
                                        onLongClick = { onContactLongClick(contact.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItemPlacement()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 快速操作按钮（仅在非搜索、非选择模式下显示）
        AnimatedVisibility(
            visible = showQuickActions,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            ContactQuickActions(
                onAddClick = ::onCreateContact,
                onImportClick = { navController.navigate(AppDestinations.ExcelImport.route) },
                onGroupClick = ::onGroupManagement,
                onSearchClick = { isSearchActive = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
            )
        }
        
        // 字母索引侧边栏（仅在非搜索、非选择模式下显示）
        if (!isSearchActive && !isSelectionMode && uiState.contacts.isNotEmpty() && groupedContacts.size > 5) {
            ContactIndexSidebar(
                letters = groupedContacts.keys.map { it.toString() },
                currentLetter = getCurrentVisibleLetter(scrollState, groupedContacts),
                onLetterClick = { letter ->
                    scope.launch {
                        val index = groupedContacts.entries
                            .indexOfFirst { it.key.toString() == letter }
                        if (index != -1) {
                            // 计算要滚动到的位置（考虑sticky header）
                            val itemIndex = groupedContacts.entries
                                .take(index)
                                .sumOf { it.value.size }
                            scrollState.animateScrollToItem(itemIndex)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )
        }
        
        // 错误提示
        uiState.error?.let { error ->
            ErrorSnackbar(
                message = error,
                onRetry = { viewModel.loadContacts() },
                onDismiss = { viewModel.clearError() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
        
        // 空搜索结果提示
        if (searchQuery.isNotEmpty() && uiState.contacts.isEmpty() && !uiState.isLoading) {
            EmptySearchResult(
                query = searchQuery,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    isSearchActive: Boolean,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSearchActive) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { onSearchActiveChange(false) },
            active = isSearchActive,
            onActiveChange = onSearchActiveChange,
            placeholder = { Text("搜索联系人") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearClick) {
                        Icon(Icons.Default.Close, contentDescription = "清除")
                    }
                }
            },
            modifier = modifier,
            shape = RoundedCornerShape(0.dp)
        ) {
            // 搜索建议 - 显示最近搜索或联系人建议
            if (query.length >= 2) {
                SearchSuggestions(
                    query = query,
                    onSuggestionClick = { suggestion ->
                        onQueryChange(suggestion)
                        onSearchActiveChange(false)
                    }
                )
            }
        }
    } else {
        Surface(
            onClick = { onSearchActiveChange(true) },
            modifier = modifier,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "搜索联系人",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestions(
    query: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 这里应该从ViewModel获取搜索建议
    val suggestions = remember(query) {
        // 模拟搜索建议
        listOf(
            "$query (姓名)",
            "$query (电话)",
            "$query (公司)"
        )
    }
    
    Column(modifier = modifier) {
        suggestions.forEach { suggestion ->
            Surface(
                onClick = { onSuggestionClick(suggestion) },
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionAppBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "已选择 $selectedCount/${totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = if (selectedCount == totalCount) 
                            Icons.Default.Deselect 
                        else 
                            Icons.Default.SelectAll,
                        contentDescription = if (selectedCount == totalCount) "取消全选" else "全选"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    enabled = selectedCount > 0
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
private fun LetterIndexHeader(
    letter: String,
    contactCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$contactCount 个联系人",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择复选框
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null, // 由父组件处理
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            
            // 联系人头像
            ContactAvatar(
                contact = contact,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 联系人信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 最后联系时间
                    contact.lastContacted?.let { timestamp ->
                        Text(
                            text = formatLastContactTime(timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 联系信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    contact.phoneNumber?.let { phone ->
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } ?: contact.email?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } ?: run {
                        Text(
                            text = "暂无联系方式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // 关系标签
                    if (contact.relationship != RelationshipType.OTHER) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RelationshipTag(
                            relationship = contact.relationship,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    // 收藏状态
                    if (contact.isFavorite) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "收藏",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFC107)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactAvatar(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = getAvatarColor(contact.displayName),
        contentColor = Color.White,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 这里应该加载头像图片
            // 暂时显示首字母
            val initials = contact.displayName.take(2).uppercase()
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 如果是收藏的联系人，在头像上添加一个装饰
            if (contact.isFavorite) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(2.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC107)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "收藏",
                            modifier = Modifier.size(8.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipTag(
    relationship: RelationshipType,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (relationship) {
        RelationshipType.FAMILY -> "家人" to Color(0xFFE91E63)
        RelationshipType.FRIEND -> "朋友" to Color(0xFF2196F3)
        RelationshipType.COLLEAGUE -> "同事" to Color(0xFF4CAF50)
        RelationshipType.CLASSMATE -> "同学" to Color(0xFFFF9800)
        RelationshipType.BUSINESS -> "商务" to Color(0xFF9C27B0)
        else -> "其他" to Color(0xFF795548)
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyContactsPlaceholder(
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Contacts,
            contentDescription = "空联系人",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "暂无联系人",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "添加您的第一个联系人",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            OutlinedButton(
                onClick = onCreateClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建联系人")
            }
            
            Button(
                onClick = onImportClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("批量导入")
            }
        }
    }
}

@Composable
private fun ContactQuickActions(
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    onGroupClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Box(modifier = modifier) {
        // 扩展的按钮
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 72.dp),
                horizontalAlignment = Alignment.End
            ) {
                QuickActionButton(
                    icon = Icons.Default.Search,
                    label = "搜索",
                    onClick = {
                        onSearchClick()
                        expanded = false
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.Group,
                    label = "群组",
                    onClick = {
                        onGroupClick()
                        expanded = false
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.TableChart,
                    label = "导入",
                    onClick = {
                        onImportClick()
                        expanded = false
                    }
                )
            }
        }
        
        // 主浮动按钮
        ExtendedFloatingActionButton(
            onClick = {
                if (expanded) {
                    onAddClick()
                    expanded = false
                } else {
                    expanded = true
                }
            },
            icon = {
                Icon(
                    if (expanded) Icons.Default.PersonAdd else Icons.Default.MoreVert,
                    contentDescription = if (expanded) "添加" else "更多"
                )
            },
            text = { Text(if (expanded) "新建联系人" else "更多操作") },
            modifier = Modifier
                .shadow(8.dp, CircleShape, clip = false)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = modifier.size(56.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                contentDescription = label, 
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ContactIndexSidebar(
    letters: List<String>,
    currentLetter: String?,
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        letters.forEach { letter ->
            val isCurrent = letter == currentLetter
            Surface(
                shape = CircleShape,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onLetterClick(letter) },
                tonalElevation = if (isCurrent) 4.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorSnackbar(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Row {
                TextButton(onClick = onRetry) {
                    Text("重试")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = "无搜索结果",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未找到相关联系人",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "搜索词：\"$query\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* 建议创建新联系人 */ }) {
            Text("创建新联系人")
        }
    }
}

// 辅助函数
private fun getAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF2196F3), // 蓝色
        Color(0xFF4CAF50), // 绿色
        Color(0xFF9C27B0), // 紫色
        Color(0xFFFF9800), // 橙色
        Color(0xFFF44336), // 红色
        Color(0xFF00BCD4), // 青色
        Color(0xFF3F51B5), // 靛蓝
        Color(0xFFFF5722), // 深橙
    )
    val index = name.hashCode().absoluteValue % colors.size
    return colors[index]
}

private fun formatLastContactTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> {
            val formatter = SimpleDateFormat("MM/dd", Locale.CHINA)
            formatter.format(Date(timestamp))
        }
    }
}

// 获取当前可见的字母
private fun getCurrentVisibleLetter(
    scrollState: LazyListState,
    groupedContacts: SortedMap<Char, List<Contact>>
): String? {
    val layoutInfo = scrollState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    
    if (visibleItems.isEmpty()) return null
    
    // 查找当前可见的第一个字母标题
    var currentIndex = 0
    for ((letter, contacts) in groupedContacts) {
        if (currentIndex + contacts.size > visibleItems.first().index) {
            return letter.toString()
        }
        currentIndex += contacts.size
    }
    
    return null
}