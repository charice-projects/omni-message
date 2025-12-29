// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/ContactViewModel.kt
@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val privacyGuard: PrivacyGuard,
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()
    
    // 事件通道
    private val _events = Channel<ContactEvent>()
    val events = _events.receiveAsFlow()
    
    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 选中的联系人
    private val _selectedContacts = mutableStateListOf<String>()
    val selectedContacts: List<String> = _selectedContacts
    
    // 当前分组
    private val _currentGroup = MutableStateFlow<String?>(null)
    
    init {
        viewModelScope.launch {
            // 初始加载
            loadContacts()
            
            // 监听搜索查询变化
            _searchQuery.debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    filterContacts(query)
                }
            
            // 监听联系人变化
            observeContactChanges()
        }
    }
    
    // 加载联系人
    fun loadContacts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val contacts = contactRepository.getContacts()
                val groups = contactRepository.getContactGroups()
                
                _uiState.update { state ->
                    state.copy(
                        contacts = contacts,
                        filteredContacts = contacts,
                        groups = groups,
                        isLoading = false,
                        error = null
                    )
                }
                
                // 计算统计信息
                calculateStatistics(contacts)
                
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "加载联系人失败: ${e.message}"
                    )
                }
                
                _events.send(ContactEvent.ShowError("加载失败"))
            }
        }
    }
    
    // 监听联系人变化
    private fun observeContactChanges() {
        viewModelScope.launch {
            contactRepository.observeContacts().collect { contacts ->
                _uiState.update { state ->
                    val filtered = if (_searchQuery.value.isNotEmpty()) {
                        filterContactsList(contacts, _searchQuery.value)
                    } else contacts
                    
                    state.copy(
                        contacts = contacts,
                        filteredContacts = filtered
                    )
                }
                
                calculateStatistics(contacts)
            }
        }
    }
    
    // 搜索过滤
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    private fun filterContacts(query: String) {
        viewModelScope.launch {
            val filtered = filterContactsList(_uiState.value.contacts, query)
            
            _uiState.update { state ->
                state.copy(
                    filteredContacts = filtered,
                    searchQuery = query
                )
            }
        }
    }
    
    private fun filterContactsList(contacts: List<Contact>, query: String): List<Contact> {
        if (query.isEmpty()) return contacts
        
        return contacts.filter { contact ->
            contact.displayName.contains(query, ignoreCase = true) ||
            contact.firstName.contains(query, ignoreCase = true) ||
            contact.lastName.contains(query, ignoreCase = true) ||
            contact.phoneNumbers.any { it.number.contains(query) } ||
            contact.emails.any { it.address.contains(query, ignoreCase = true) } ||
            contact.company?.contains(query, ignoreCase = true) ?: false ||
            contact.tags.any { it.contains(query, ignoreCase = true) }
        }
    }
    
    // 计算统计信息
    private fun calculateStatistics(contacts: List<Contact>) {
        val total = contacts.size
        val favorite = contacts.count { it.isFavorite }
        val recent = contacts.count { it.lastContacted != null && 
            System.currentTimeMillis() - it.lastContacted!! < 7 * 24 * 60 * 60 * 1000 }
        val company = contacts.groupBy { it.company ?: "未分类" }
            .mapValues { it.value.size }
        
        _uiState.update { state ->
            state.copy(
                statistics = ContactStatistics(
                    totalContacts = total,
                    favoriteContacts = favorite,
                    recentContacts = recent,
                    companyDistribution = company
                )
            )
        }
    }
    
    // 切换收藏状态
    fun toggleFavorite(contactId: String) {
        viewModelScope.launch {
            try {
                contactRepository.toggleFavorite(contactId)
                _events.send(ContactEvent.ShowMessage("收藏状态已更新"))
                
                // 记录分析事件
                analyticsRepository.logContactInteraction(contactId, "toggle_favorite")
                
            } catch (e: Exception) {
                _events.send(ContactEvent.ShowError("操作失败"))
            }
        }
    }
    
    // 删除联系人
    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                contactRepository.deleteContact(contactId)
                _events.send(ContactEvent.ShowMessage("联系人已删除"))
                
                // 记录隐私审计
                privacyGuard.logPrivacyEvent(
                    PrivacyEventType.DATA_DELETED,
                    "contact",
                    contactId
                )
                
                // 记录分析事件
                analyticsRepository.logContactInteraction(contactId, "delete")
                
            } catch (e: Exception) {
                _events.send(ContactEvent.ShowError("删除失败"))
            }
        }
    }
    
    // 批量删除联系人
    fun deleteSelectedContacts() {
        viewModelScope.launch {
            try {
                val selected = _selectedContacts.toList()
                selected.forEach { contactId ->
                    contactRepository.deleteContact(contactId)
                }
                
                _selectedContacts.clear()
                
                _events.send(ContactEvent.ShowMessage("已删除 ${selected.size} 个联系人"))
                
                // 记录隐私审计
                privacyGuard.logPrivacyEvent(
                    PrivacyEventType.BATCH_DATA_DELETED,
                    "contact",
                    "批量删除 ${selected.size} 个联系人"
                )
                
            } catch (e: Exception) {
                _events.send(ContactEvent.ShowError("批量删除失败"))
            }
        }
    }
    
    // 选中/取消选中联系人
    fun toggleContactSelection(contactId: String) {
        if (_selectedContacts.contains(contactId)) {
            _selectedContacts.remove(contactId)
        } else {
            _selectedContacts.add(contactId)
        }
        
        _uiState.update { state ->
            state.copy(
                selectedCount = _selectedContacts.size,
                isSelectionMode = _selectedContacts.isNotEmpty()
            )
        }
    }
    
    // 全选/取消全选
    fun toggleSelectAll() {
        val allIds = _uiState.value.filteredContacts.map { it.id }
        
        if (_selectedContacts.size == allIds.size) {
            _selectedContacts.clear()
        } else {
            _selectedContacts.clear()
            _selectedContacts.addAll(allIds)
        }
        
        _uiState.update { state ->
            state.copy(
                selectedCount = _selectedContacts.size,
                isSelectionMode = _selectedContacts.isNotEmpty()
            )
        }
    }
    
    // 清除选择
    fun clearSelection() {
        _selectedContacts.clear()
        _uiState.update { state ->
            state.copy(
                selectedCount = 0,
                isSelectionMode = false
            )
        }
    }
    
    // 导出联系人
    fun exportContacts(format: ExportFormat = ExportFormat.VCF) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true) }
                
                val result = contactRepository.exportContacts(format)
                
                _events.send(
                    ContactEvent.ExportCompleted(
                        fileUri = result.fileUri,
                        contactCount = result.contactCount
                    )
                )
                
                // 记录分析事件
                analyticsRepository.logExportEvent(format.name, result.contactCount)
                
            } catch (e: Exception) {
                _events.send(ContactEvent.ShowError("导出失败: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }
    
    // 导入联系人
    fun importContacts(fileUri: Uri, format: ImportFormat = ImportFormat.VCF) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isImporting = true) }
                
                val result = contactRepository.importContacts(fileUri, format)
                
                _events.send(
                    ContactEvent.ImportCompleted(
                        importedCount = result.importedCount,
                        skippedCount = result.skippedCount,
                        errors = result.errors
                    )
                )
                
                // 刷新联系人列表
                loadContacts()
                
                // 记录分析事件
                analyticsRepository.logImportEvent(format.name, result.importedCount)
                
            } catch (e: Exception) {
                _events.send(ContactEvent.ShowError("导入失败: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }
    
    // 创建新分组
    fun createGroup(name: String, description: String? = null) {
        viewModelScope.launch {
            try {
                val group = contactRepository.createGroup(name, description)
                
                _events.send(ContactEvent.GroupCreated(group.id))
                _events.send(ContactEvent.ShowMessage("分组创建成功"))
                
                // 重新加载分组
                val groups = contactRepository.getContactGroups()
                _uiState.update { it.copy(groups = groups) }
                
            } catch (e: Exception) {
                _events.send(ContactEvent.ShowError("创建分组失败"))
            }
        }
    }
    
    // 过滤分组
    fun filterByGroup(groupId: String?) {
        _currentGroup.value = groupId
        
        viewModelScope.launch {
            val contacts = if (groupId != null) {
                contactRepository.getContactsByGroup(groupId)
            } else {
                contactRepository.getContacts()
            }
            
            _uiState.update { state ->
                state.copy(
                    contacts = contacts,
                    filteredContacts = if (_searchQuery.value.isNotEmpty()) {
                        filterContactsList(contacts, _searchQuery.value)
                    } else contacts
                )
            }
        }
    }
    
    // 刷新数据
    fun refresh() {
        loadContacts()
    }
}

// 联系人列表UI状态
data class ContactListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    
    // 数据
    val contacts: List<Contact> = emptyList(),
    val filteredContacts: List<Contact> = emptyList(),
    val groups: List<ContactGroup> = emptyList(),
    
    // 搜索
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    
    // 选择模式
    val isSelectionMode: Boolean = false,
    val selectedCount: Int = 0,
    
    // 统计
    val statistics: ContactStatistics = ContactStatistics(),
    
    // 显示选项
    val displayMode: DisplayMode = DisplayMode.LIST,
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val filterOptions: FilterOptions = FilterOptions()
)

// 联系人事件
sealed class ContactEvent {
    data class ShowMessage(val message: String) : ContactEvent()
    data class ShowError(val error: String) : ContactEvent()
    data class NavigateToContactDetail(val contactId: String) : ContactEvent()
    data class NavigateToNewContact(val prefilledData: ContactData? = null) : ContactEvent()
    data class ExportCompleted(val fileUri: Uri, val contactCount: Int) : ContactEvent()
    data class ImportCompleted(
        val importedCount: Int,
        val skippedCount: Int,
        val errors: List<String>
    ) : ContactEvent()
    data class GroupCreated(val groupId: String) : ContactEvent()
}

// 联系人统计
data class ContactStatistics(
    val totalContacts: Int = 0,
    val favoriteContacts: Int = 0,
    val recentContacts: Int = 0,
    val companyDistribution: Map<String, Int> = emptyMap(),
    val tagDistribution: Map<String, Int> = emptyMap(),
    val interactionStats: InteractionStats = InteractionStats()
)

data class InteractionStats(
    val todayMessages: Int = 0,
    val todayCalls: Int = 0,
    val weeklyAverage: Float = 0f,
    val mostActiveHour: Int = 12
)

// 显示模式
enum class DisplayMode {
    LIST, GRID, COMPACT
}

// 排序方式
enum class SortOrder {
    NAME_ASC, NAME_DESC, RECENT_ASC, RECENT_DESC, CREATED_ASC, CREATED_DESC
}

// 过滤选项
data class FilterOptions(
    val showFavoritesOnly: Boolean = false,
    val showRecentOnly: Boolean = false,
    val showWithPhoneOnly: Boolean = false,
    val showWithEmailOnly: Boolean = false,
    val tags: List<String> = emptyList(),
    val company: String? = null,
    val relationship: RelationshipType? = null
)

// 导出格式
enum class ExportFormat {
    VCF, CSV, EXCEL, JSON
}

// 导入格式
enum class ImportFormat {
    VCF, CSV, EXCEL, GOOGLE_CONTACTS, SYSTEM_CONTACTS
}

// 联系人分组
data class ContactGroup(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val memberCount: Int = 0,
    val color: Color = Color.Unspecified,
    val icon: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)