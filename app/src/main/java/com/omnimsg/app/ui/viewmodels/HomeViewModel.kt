// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/HomeViewModel.kt
class HomeViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val notificationRepository: NotificationRepository,
    private val privacyGuard: PrivacyGuard,
    private val emergencySystem: EmergencySystem,
    private val voiceService: VoiceService,
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // 事件通道
    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()
    
    init {
        viewModelScope.launch {
            // 初始化数据加载
            loadHomeData()
            
            // 监听实时更新
            observeRealtimeUpdates()
            
            // 检查系统状态
            checkSystemStatus()
        }
    }
    
    // 加载首页数据
    private suspend fun loadHomeData() {
        try {
            // 并行加载所有数据
            val recentMessagesDeferred = async { messageRepository.getRecentMessages(20) }
            val favoriteContactsDeferred = async { contactRepository.getFavoriteContacts() }
            val unreadNotificationsDeferred = async { notificationRepository.getUnreadCount() }
            val privacyStatusDeferred = async { privacyGuard.getPrivacyStatus() }
            
            val recentMessages = recentMessagesDeferred.await()
            val favoriteContacts = favoriteContactsDeferred.await()
            val unreadNotifications = unreadNotificationsDeferred.await()
            val privacyStatus = privacyStatusDeferred.await()
            
            _uiState.update { state ->
                state.copy(
                    recentMessages = recentMessages,
                    favoriteContacts = favoriteContacts,
                    unreadNotifications = unreadNotifications,
                    privacyStatus = privacyStatus,
                    isLoading = false,
                    error = null
                )
            }
            
            // 记录分析事件
            analyticsRepository.logHomeView()
            
        } catch (e: Exception) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = e.message ?: "数据加载失败"
                )
            }
            
            _events.send(HomeEvent.ShowError("首页数据加载失败"))
        }
    }
    
    // 监听实时更新
    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            // 监听新消息
            messageRepository.observeNewMessages().collect { newMessages ->
                _uiState.update { state ->
                    state.copy(
                        recentMessages = newMessages.take(20),
                        newMessageCount = newMessages.size
                    )
                }
            }
        }
        
        viewModelScope.launch {
            // 监听通知
            notificationRepository.observeNotifications().collect { notifications ->
                _uiState.update { state ->
                    state.copy(
                        notifications = notifications.take(5),
                        unreadNotifications = notifications.count { !it.isRead }
                    )
                }
            }
        }
    }
    
    // 检查系统状态
    private fun checkSystemStatus() {
        viewModelScope.launch {
            // 检查紧急模式
            val emergencyActive = emergencySystem.isEmergencyModeActive()
            
            // 检查语音唤醒词状态
            val wakeWordEnabled = voiceService.isWakeWordEnabled()
            
            // 检查隐私保护状态
            val privacyProtectionActive = privacyGuard.isPrivacyProtectionActive()
            
            _uiState.update { state ->
                state.copy(
                    emergencyModeActive = emergencyActive,
                    voiceWakeWordEnabled = wakeWordEnabled,
                    privacyProtectionActive = privacyProtectionActive
                )
            }
        }
    }
    
    // 发送消息
    fun sendQuickMessage(recipientId: String, content: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSendingMessage = true) }
                
                val result = messageRepository.sendMessage(
                    recipientId = recipientId,
                    content = content,
                    type = MessageType.TEXT
                )
                
                if (result.isSuccess) {
                    _events.send(HomeEvent.MessageSent("消息发送成功"))
                    loadHomeData() // 刷新数据
                } else {
                    _events.send(HomeEvent.ShowError("消息发送失败: ${result.error}"))
                }
                
            } catch (e: Exception) {
                _events.send(HomeEvent.ShowError("消息发送异常: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isSendingMessage = false) }
            }
        }
    }
    
    // 开始语音输入
    fun startVoiceInput() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isVoiceInputActive = true) }
                
                voiceService.startVoiceRecognition { result ->
                    when (result) {
                        is VoiceRecognitionResult.Success -> {
                            handleVoiceCommand(result.text)
                        }
                        is VoiceRecognitionResult.Error -> {
                            _events.send(HomeEvent.ShowError("语音识别失败: ${result.error}"))
                        }
                        VoiceRecognitionResult.Cancelled -> {
                            // 用户取消
                        }
                    }
                    
                    _uiState.update { it.copy(isVoiceInputActive = false) }
                }
                
            } catch (e: Exception) {
                _events.send(HomeEvent.ShowError("语音服务启动失败"))
                _uiState.update { it.copy(isVoiceInputActive = false) }
            }
        }
    }
    
    // 处理语音命令
    private fun handleVoiceCommand(text: String) {
        viewModelScope.launch {
            val command = voiceService.processVoiceCommand(text)
            
            when (command) {
                is VoiceCommand.SendMessage -> {
                    sendQuickMessage(command.recipient, command.content)
                }
                is VoiceCommand.OpenScreen -> {
                    _events.send(HomeEvent.NavigateTo(command.screen))
                }
                is VoiceCommand.EmergencyAlert -> {
                    triggerEmergencyAlert()
                }
                is VoiceCommand.Search -> {
                    _events.send(HomeEvent.SearchRequested(command.query))
                }
                else -> {
                    _events.send(HomeEvent.ShowError("未识别的语音命令"))
                }
            }
        }
    }
    
    // 触发紧急警报
    private fun triggerEmergencyAlert() {
        viewModelScope.launch {
            val result = emergencySystem.triggerEmergencyAlert()
            
            when (result) {
                is EmergencyResult.Success -> {
                    _events.send(HomeEvent.EmergencyAlertTriggered("紧急警报已发送"))
                    
                    // 更新状态
                    _uiState.update { it.copy(emergencyModeActive = true) }
                }
                is EmergencyResult.Error -> {
                    _events.send(HomeEvent.ShowError("紧急警报失败: ${result.error}"))
                }
                EmergencyResult.Cancelled -> {
                    // 用户取消
                }
            }
        }
    }
    
    // 刷新数据
    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadHomeData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    // 切换隐私保护
    fun togglePrivacyProtection() {
        viewModelScope.launch {
            val newState = !_uiState.value.privacyProtectionActive
            
            privacyGuard.setPrivacyProtectionEnabled(newState)
            
            _uiState.update { it.copy(privacyProtectionActive = newState) }
            
            val message = if (newState) "隐私保护已开启" else "隐私保护已关闭"
            _events.send(HomeEvent.ShowMessage(message))
        }
    }
    
    // 快速操作
    fun performQuickAction(action: QuickAction) {
        viewModelScope.launch {
            when (action) {
                QuickAction.NEW_MESSAGE -> {
                    _events.send(HomeEvent.NavigateTo(AppDestinations.MessageList))
                }
                QuickAction.NEW_CONTACT -> {
                    _events.send(HomeEvent.NavigateTo(AppDestinations.ContactList))
                }
                QuickAction.EXCEL_IMPORT -> {
                    _events.send(HomeEvent.NavigateTo(AppDestinations.ExcelImport))
                }
                QuickAction.VOICE_COMMAND -> {
                    startVoiceInput()
                }
                QuickAction.EMERGENCY -> {
                    triggerEmergencyAlert()
                }
                QuickAction.SEARCH -> {
                    _events.send(HomeEvent.NavigateTo(AppDestinations.Search))
                }
                QuickAction.WORKFLOW -> {
                    _events.send(HomeEvent.NavigateTo(AppDestinations.Workflow))
                }
                QuickAction.PRIVACY_CHECK -> {
                    _events.send(HomeEvent.NavigateTo(AppDestinations.PrivacyCenter))
                }
            }
            
            // 记录分析事件
            analyticsRepository.logQuickAction(action)
        }
    }
}

// Home UI状态
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSendingMessage: Boolean = false,
    val isVoiceInputActive: Boolean = false,
    val error: String? = null,
    
    // 数据
    val recentMessages: List<Message> = emptyList(),
    val favoriteContacts: List<Contact> = emptyList(),
    val notifications: List<Notification> = emptyList(),
    val quickActions: List<QuickAction> = QuickAction.defaultActions(),
    
    // 系统状态
    val emergencyModeActive: Boolean = false,
    val voiceWakeWordEnabled: Boolean = false,
    val privacyProtectionActive: Boolean = true,
    val newMessageCount: Int = 0,
    val unreadNotifications: Int = 0,
    
    // 隐私状态
    val privacyStatus: PrivacyStatus = PrivacyStatus(),
    
    // 统计信息
    val todayMessages: Int = 0,
    val todayCalls: Int = 0,
    val storageUsage: StorageUsage = StorageUsage()
)

// Home事件
sealed class HomeEvent {
    data class ShowMessage(val message: String) : HomeEvent()
    data class ShowError(val error: String) : HomeEvent()
    data class NavigateTo(val destination: AppDestinations) : HomeEvent()
    data class SearchRequested(val query: String) : HomeEvent()
    data class MessageSent(val message: String) : HomeEvent()
    data class EmergencyAlertTriggered(val message: String) : HomeEvent()
}

// 快速操作枚举
enum class QuickAction(
    val title: String,
    val icon: Int,
    val description: String,
    val color: Color
) {
    NEW_MESSAGE("新消息", R.drawable.ic_new_message, "发送新消息", Color(0xFF4CAF50)),
    NEW_CONTACT("新联系人", R.drawable.ic_new_contact, "添加联系人", Color(0xFF2196F3)),
    EXCEL_IMPORT("Excel导入", R.drawable.ic_excel_import, "导入联系人", Color(0xFF673AB7)),
    VOICE_COMMAND("语音命令", R.drawable.ic_voice_command, "语音控制", Color(0xFFFF9800)),
    EMERGENCY("紧急警报", R.drawable.ic_emergency_alert, "紧急求助", Color(0xFFF44336)),
    SEARCH("智能搜索", R.drawable.ic_smart_search, "搜索一切", Color(0xFF009688)),
    WORKFLOW("工作流", R.drawable.ic_workflow_run, "自动化任务", Color(0xFF795548)),
    PRIVACY_CHECK("隐私检查", R.drawable.ic_privacy_check, "安全检查", Color(0xFF607D8B));
    
    companion object {
        fun defaultActions(): List<QuickAction> = listOf(
            NEW_MESSAGE, NEW_CONTACT, EXCEL_IMPORT, VOICE_COMMAND,
            EMERGENCY, SEARCH, WORKFLOW, PRIVACY_CHECK
        )
    }
}

// 存储使用情况
data class StorageUsage(
    val used: Long = 0,
    val total: Long = 100 * 1024 * 1024, // 100MB
    val encrypted: Long = 0
)

// 隐私状态
data class PrivacyStatus(
    val encryptionEnabled: Boolean = true,
    val dataAnonymization: Boolean = true,
    val locationPrivacy: Boolean = true,
    val microphonePrivacy: Boolean = true,
    val cameraPrivacy: Boolean = true,
    val contactsPrivacy: Boolean = true,
    val messagesEncrypted: Boolean = true
)