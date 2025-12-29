// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/MessageViewModel.kt
@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val conversationRepository: ConversationRepository,
    private val privacyGuard: PrivacyGuard,
    private val analyticsRepository: AnalyticsRepository,
    private val encryptionManager: EncryptionManager,
    private val notificationService: NotificationService
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(MessageListUiState())
    val uiState: StateFlow<MessageListUiState> = _uiState.asStateFlow()
    
    // 事件通道
    private val _events = Channel<MessageEvent>()
    val events = _events.receiveAsFlow()
    
    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 选中的对话
    private val _selectedConversations = mutableStateListOf<String>()
    val selectedConversations: List<String> = _selectedConversations
    
    // 过滤选项
    private val _filterOptions = MutableStateFlow(MessageFilterOptions())
    
    // 当前标签（收件箱、未读、星标、归档等）
    private val _currentTab = MutableStateFlow(MessageTab.INBOX)
    
    init {
        viewModelScope.launch {
            // 初始加载
            loadConversations()
            
            // 监听搜索查询变化
            _searchQuery.debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    filterConversations(query)
                }
            
            // 监听消息变化
            observeMessageChanges()
            
            // 监听对话变化
            observeConversationChanges()
            
            // 监听连接状态
            observeConnectionStatus()
        }
    }
    
    // 加载对话列表
    fun loadConversations() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val conversations = conversationRepository.getConversations(
                    tab = _currentTab.value,
                    filter = _filterOptions.value
                )
                
                val unreadCount = conversationRepository.getUnreadCount()
                val starredCount = conversationRepository.getStarredCount()
                
                _uiState.update { state ->
                    state.copy(
                        conversations = conversations,
                        filteredConversations = conversations,
                        unreadCount = unreadCount,
                        starredCount = starredCount,
                        isLoading = false,
                        error = null
                    )
                }
                
                // 更新统计信息
                updateStatistics()
                
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "加载消息失败: ${e.message}"
                    )
                }
                
                _events.send(MessageEvent.ShowError("加载失败"))
            }
        }
    }
    
    // 监听消息变化
    private fun observeMessageChanges() {
        viewModelScope.launch {
            messageRepository.observeNewMessages().collect { newMessages ->
                // 更新未读计数
                val unreadCount = conversationRepository.getUnreadCount()
                _uiState.update { it.copy(unreadCount = unreadCount) }
                
                // 如果有新消息，刷新列表
                if (newMessages.isNotEmpty()) {
                    loadConversations()
                }
            }
        }
    }
    
    // 监听对话变化
    private fun observeConversationChanges() {
        viewModelScope.launch {
            conversationRepository.observeConversations().collect { conversations ->
                val filtered = if (_searchQuery.value.isNotEmpty()) {
                    filterConversationsList(conversations, _searchQuery.value)
                } else conversations
                
                _uiState.update { state ->
                    state.copy(
                        conversations = conversations,
                        filteredConversations = filtered
                    )
                }
            }
        }
    }
    
    // 监听连接状态
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            messageRepository.observeConnectionStatus().collect { status ->
                _uiState.update { state ->
                    state.copy(
                        connectionStatus = status,
                        isConnected = status == ConnectionStatus.CONNECTED
                    )
                }
            }
        }
    }
    
    // 搜索过滤
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    private fun filterConversations(query: String) {
        viewModelScope.launch {
            val filtered = filterConversationsList(_uiState.value.conversations, query)
            
            _uiState.update { state ->
                state.copy(
                    filteredConversations = filtered,
                    searchQuery = query
                )
            }
        }
    }
    
    private fun filterConversationsList(
        conversations: List<Conversation>,
        query: String
    ): List<Conversation> {
        if (query.isEmpty()) return conversations
        
        return conversations.filter { conversation ->
            conversation.title.contains(query, ignoreCase = true) ||
            conversation.lastMessageContent.contains(query, ignoreCase = true) ||
            conversation.participants.any { participant ->
                participant.name.contains(query, ignoreCase = true) ||
                participant.phone?.contains(query) ?: false ||
                participant.email?.contains(query, ignoreCase = true) ?: false
            }
        }
    }
    
    // 更新统计信息
    private fun updateStatistics() {
        viewModelScope.launch {
            val stats = conversationRepository.getStatistics()
            
            _uiState.update { state ->
                state.copy(statistics = stats)
            }
        }
    }
    
    // 切换标签页
    fun setCurrentTab(tab: MessageTab) {
        _currentTab.value = tab
        
        viewModelScope.launch {
            _uiState.update { it.copy(currentTab = tab) }
            loadConversations()
        }
    }
    
    // 应用过滤器
    fun applyFilter(options: MessageFilterOptions) {
        _filterOptions.value = options
        
        viewModelScope.launch {
            _uiState.update { it.copy(filterOptions = options) }
            loadConversations()
        }
    }
    
    // 标记对话为已读
    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.markAsRead(conversationId)
                
                // 清除通知
                notificationService.cancelConversationNotification(conversationId)
                
                // 更新本地状态
                updateConversationReadStatus(conversationId, true)
                
                _events.send(MessageEvent.ShowMessage("标记为已读"))
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("操作失败"))
            }
        }
    }
    
    // 标记对话为未读
    fun markAsUnread(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.markAsUnread(conversationId)
                updateConversationReadStatus(conversationId, false)
                
                _events.send(MessageEvent.ShowMessage("标记为未读"))
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("操作失败"))
            }
        }
    }
    
    // 切换星标状态
    fun toggleStar(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.toggleStar(conversationId)
                
                // 更新本地状态
                val updated = _uiState.value.conversations.map { conv ->
                    if (conv.id == conversationId) {
                        conv.copy(isStarred = !conv.isStarred)
                    } else conv
                }
                
                _uiState.update { state ->
                    state.copy(conversations = updated)
                }
                
                _events.send(MessageEvent.ShowMessage("星标状态已更新"))
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("操作失败"))
            }
        }
    }
    
    // 归档对话
    fun archiveConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.archive(conversationId)
                removeConversationFromList(conversationId)
                
                _events.send(MessageEvent.ShowMessage("对话已归档"))
                
                // 记录隐私审计
                privacyGuard.logPrivacyEvent(
                    PrivacyEventType.DATA_ARCHIVED,
                    "conversation",
                    conversationId
                )
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("归档失败"))
            }
        }
    }
    
    // 删除对话
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.delete(conversationId)
                removeConversationFromList(conversationId)
                
                _events.send(MessageEvent.ShowMessage("对话已删除"))
                
                // 记录隐私审计
                privacyGuard.logPrivacyEvent(
                    PrivacyEventType.DATA_DELETED,
                    "conversation",
                    conversationId
                )
                
                // 记录分析事件
                analyticsRepository.logConversationDeleted()
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("删除失败"))
            }
        }
    }
    
    // 批量删除对话
    fun deleteSelectedConversations() {
        viewModelScope.launch {
            try {
                val selected = _selectedConversations.toList()
                
                selected.forEach { conversationId ->
                    conversationRepository.delete(conversationId)
                }
                
                _selectedConversations.clear()
                loadConversations() // 重新加载列表
                
                _events.send(MessageEvent.ShowMessage("已删除 ${selected.size} 个对话"))
                
                // 记录隐私审计
                privacyGuard.logPrivacyEvent(
                    PrivacyEventType.BATCH_DATA_DELETED,
                    "conversation",
                    "批量删除 ${selected.size} 个对话"
                )
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("批量删除失败"))
            }
        }
    }
    
    // 清除所有对话
    fun clearAllConversations() {
        viewModelScope.launch {
            try {
                conversationRepository.clearAll()
                _uiState.update { state ->
                    state.copy(
                        conversations = emptyList(),
                        filteredConversations = emptyList()
                    )
                }
                
                _events.send(MessageEvent.ShowMessage("所有对话已清除"))
                
                // 记录隐私审计
                privacyGuard.logPrivacyEvent(
                    PrivacyEventType.DATA_CLEARED,
                    "conversation",
                    "清除所有对话"
                )
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("清除失败"))
            }
        }
    }
    
    // 选中/取消选中对话
    fun toggleConversationSelection(conversationId: String) {
        if (_selectedConversations.contains(conversationId)) {
            _selectedConversations.remove(conversationId)
        } else {
            _selectedConversations.add(conversationId)
        }
        
        _uiState.update { state ->
            state.copy(
                selectedCount = _selectedConversations.size,
                isSelectionMode = _selectedConversations.isNotEmpty()
            )
        }
    }
    
    // 全选/取消全选
    fun toggleSelectAll() {
        val allIds = _uiState.value.filteredConversations.map { it.id }
        
        if (_selectedConversations.size == allIds.size) {
            _selectedConversations.clear()
        } else {
            _selectedConversations.clear()
            _selectedConversations.addAll(allIds)
        }
        
        _uiState.update { state ->
            state.copy(
                selectedCount = _selectedConversations.size,
                isSelectionMode = _selectedConversations.isNotEmpty()
            )
        }
    }
    
    // 清除选择
    fun clearSelection() {
        _selectedConversations.clear()
        _uiState.update { state ->
            state.copy(
                selectedCount = 0,
                isSelectionMode = false
            )
        }
    }
    
    // 置顶/取消置顶对话
    fun togglePin(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.togglePin(conversationId)
                
                // 更新本地状态
                val updated = _uiState.value.conversations.map { conv ->
                    if (conv.id == conversationId) {
                        conv.copy(isPinned = !conv.isPinned)
                    } else conv
                }.sortedWith(compareByDescending<Conversation> { it.isPinned }
                    .thenByDescending { it.lastMessageTime })
                
                _uiState.update { state ->
                    state.copy(conversations = updated)
                }
                
                _events.send(MessageEvent.ShowMessage("置顶状态已更新"))
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("操作失败"))
            }
        }
    }
    
    // 静音/取消静音对话
    fun toggleMute(conversationId: String) {
        viewModelScope.launch {
            try {
                conversationRepository.toggleMute(conversationId)
                
                // 更新本地状态
                val updated = _uiState.value.conversations.map { conv ->
                    if (conv.id == conversationId) {
                        conv.copy(isMuted = !conv.isMuted)
                    } else conv
                }
                
                _uiState.update { state ->
                    state.copy(conversations = updated)
                }
                
                val message = if (_uiState.value.conversations
                    .firstOrNull { it.id == conversationId }?.isMuted == true) {
                    "对话已静音"
                } else "对话已取消静音"
                
                _events.send(MessageEvent.ShowMessage(message))
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("操作失败"))
            }
        }
    }
    
    // 检查加密状态
    fun checkEncryptionStatus(conversationId: String): EncryptionStatus {
        return encryptionManager.getConversationEncryptionStatus(conversationId)
    }
    
    // 发送消息
    fun sendMessage(conversationId: String, content: String, type: MessageType = MessageType.TEXT) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSendingMessage = true) }
                
                val result = messageRepository.sendMessage(
                    conversationId = conversationId,
                    content = content,
                    type = type
                )
                
                if (result.isSuccess) {
                    _events.send(MessageEvent.MessageSent("消息发送成功"))
                    
                    // 刷新对话列表
                    loadConversations()
                } else {
                    _events.send(MessageEvent.ShowError("消息发送失败: ${result.error}"))
                }
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("发送失败: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isSendingMessage = false) }
            }
        }
    }
    
    // 重新发送失败的消息
    fun retryFailedMessage(messageId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRetryingMessage = true) }
                
                val result = messageRepository.retryMessage(messageId)
                
                if (result.isSuccess) {
                    _events.send(MessageEvent.ShowMessage("重新发送成功"))
                } else {
                    _events.send(MessageEvent.ShowError("重新发送失败"))
                }
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("操作失败"))
            } finally {
                _uiState.update { it.copy(isRetryingMessage = false) }
            }
        }
    }
    
    // 同步消息
    fun syncMessages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSyncing = true) }
                
                val result = messageRepository.syncMessages()
                
                if (result.isSuccess) {
                    _events.send(MessageEvent.ShowMessage("同步完成"))
                    loadConversations() // 刷新列表
                } else {
                    _events.send(MessageEvent.ShowError("同步失败: ${result.error}"))
                }
                
            } catch (e: Exception) {
                _events.send(MessageEvent.ShowError("同步异常"))
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }
    
    // 刷新数据
    fun refresh() {
        loadConversations()
    }
    
    // 辅助方法
    private fun updateConversationReadStatus(conversationId: String, isRead: Boolean) {
        val updated = _uiState.value.conversations.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(unreadCount = if (isRead) 0 else conv.unreadCount + 1)
            } else conv
        }
        
        _uiState.update { state ->
            state.copy(conversations = updated)
        }
    }
    
    private fun removeConversationFromList(conversationId: String) {
        val updated = _uiState.value.conversations.filter { it.id != conversationId }
        _uiState.update { state ->
            state.copy(
                conversations = updated,
                filteredConversations = updated
            )
        }
    }
}

// 消息列表UI状态
data class MessageListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSendingMessage: Boolean = false,
    val isRetryingMessage: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    
    // 数据
    val conversations: List<Conversation> = emptyList(),
    val filteredConversations: List<Conversation> = emptyList(),
    
    // 搜索
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    
    // 选择模式
    val isSelectionMode: Boolean = false,
    val selectedCount: Int = 0,
    
    // 标签页
    val currentTab: MessageTab = MessageTab.INBOX,
    val unreadCount: Int = 0,
    val starredCount: Int = 0,
    
    // 过滤选项
    val filterOptions: MessageFilterOptions = MessageFilterOptions(),
    
    // 连接状态
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val isConnected: Boolean = false,
    
    // 统计信息
    val statistics: MessageStatistics = MessageStatistics()
)

// 消息事件
sealed class MessageEvent {
    data class ShowMessage(val message: String) : MessageEvent()
    data class ShowError(val error: String) : MessageEvent()
    data class NavigateToConversation(val conversationId: String) : MessageEvent()
    data class NavigateToNewMessage(val recipientId: String? = null) : MessageEvent()
    data class MessageSent(val message: String) : MessageEvent()
    data class EncryptionStatusChanged(val conversationId: String, val status: EncryptionStatus) : MessageEvent()
}

// 对话数据类
data class Conversation(
    val id: String = "",
    val title: String = "",
    val participants: List<ConversationParticipant> = emptyList(),
    val lastMessageContent: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastMessageSender: String = "",
    val lastMessageType: MessageType = MessageType.TEXT,
    val unreadCount: Int = 0,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isGroup: Boolean = false,
    val groupIcon: String? = null,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.END_TO_END,
    val isEncrypted: Boolean = true,
    val messageStatus: MessageStatus = MessageStatus.SENT,
    val draftMessage: String? = null,
    val customColor: Color? = null,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

data class ConversationParticipant(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val role: ParticipantRole = ParticipantRole.MEMBER
)

enum class ParticipantRole {
    OWNER, ADMIN, MEMBER, GUEST
}

// 消息标签页
enum class MessageTab(val title: String, val icon: Int) {
    INBOX("收件箱", R.drawable.ic_inbox),
    UNREAD("未读", R.drawable.ic_unread),
    STARRED("星标", R.drawable.ic_starred),
    SENT("已发送", R.drawable.ic_sent),
    DRAFTS("草稿", R.drawable.ic_draft),
    ARCHIVED("归档", R.drawable.ic_archive),
    SPAM("垃圾", R.drawable.ic_spam),
    TRASH("回收站", R.drawable.ic_trash)
}

// 消息过滤选项
data class MessageFilterOptions(
    val showUnreadOnly: Boolean = false,
    val showStarredOnly: Boolean = false,
    val showEncryptedOnly: Boolean = false,
    val showWithAttachmentsOnly: Boolean = false,
    val dateRange: DateRange? = null,
    val participants: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val minMessageCount: Int = 0
)

data class DateRange(
    val start: Long,
    val end: Long
)

// 连接状态
enum class ConnectionStatus {
    CONNECTED, CONNECTING, DISCONNECTED, ERROR, LIMITED
}

// 消息统计
data class MessageStatistics(
    val totalMessages: Int = 0,
    val todayMessages: Int = 0,
    val unreadMessages: Int = 0,
    val encryptedMessages: Int = 0,
    val failedMessages: Int = 0,
    val topContacts: List<MessageContact> = emptyList(),
    val busiestHour: Int = 12,
    val averageResponseTime: Long = 0
)

data class MessageContact(
    val contactId: String,
    val name: String,
    val messageCount: Int,
    val lastInteraction: Long
)