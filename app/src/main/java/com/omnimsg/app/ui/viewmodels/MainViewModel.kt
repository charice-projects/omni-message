// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/MainViewModel.kt
package com.omnimsg.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnimsg.app.ui.screens.UserInfo
import com.omnimsg.feature.voice.WakeWordDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val wakeWordDetector: WakeWordDetector,
    private val userRepository: UserRepository
) : ViewModel() {
    
    data class UiState(
        val isDarkMode: Boolean = false,
        val isLoggedIn: Boolean = true,
        val userInfo: UserInfo = UserInfo(
            id = "user_001",
            displayName = "未登录用户",
            email = "",
            isOnline = false
        ),
        val appVersion: String = "1.0.0",
        val isVoiceWakeWordActive: Boolean = false,
        val isEmergencyActive: Boolean = false,
        val unreadNotificationCount: Int = 0,
        val batteryLevel: Int = 85,
        val networkConnected: Boolean = true
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private var wakeWordListener: WakeWordDetector.WakeWordListener? = null
    
    init {
        loadUserInfo()
        setupWakeWordListener()
        startSystemMonitoring()
    }
    
    /**
     * 加载用户信息
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _uiState.update {
                    it.copy(
                        userInfo = UserInfo(
                            id = user.id,
                            displayName = user.displayName,
                            email = user.email,
                            avatarUrl = user.avatarUrl,
                            isOnline = true,
                            contactCount = user.contactCount,
                            unreadMessageCount = user.unreadMessageCount,
                            storageUsage = user.storageUsage
                        ),
                        isLoggedIn = true
                    )
                }
            } catch (e: Exception) {
                // 如果用户未登录，保持默认状态
            }
        }
    }
    
    /**
     * 设置唤醒词监听器
     */
    private fun setupWakeWordListener() {
        wakeWordListener = object : WakeWordDetector.WakeWordListener {
            override fun onWakeWordDetected(confidence: Float) {
                _uiState.update { it.copy(isVoiceWakeWordActive = true) }
                
                // 3秒后自动隐藏
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(isVoiceWakeWordActive = false) }
                }
            }
            
            override fun onError(error: String) {
                // 处理错误
            }
            
            override fun onStatusChanged(isListening: Boolean) {
                // 更新状态
            }
        }
        
        wakeWordDetector.addListener(wakeWordListener!!)
    }
    
    /**
     * 开始系统监控
     */
    private fun startSystemMonitoring() {
        viewModelScope.launch {
            // 模拟定期更新系统状态
            while (true) {
                kotlinx.coroutines.delay(10000) // 每10秒更新一次
                
                // 这里应该获取真实的系统状态
                // 简化实现：随机变化
                val currentState = _uiState.value
                _uiState.update {
                    it.copy(
                        batteryLevel = kotlin.random.Random.nextInt(20, 100),
                        networkConnected = kotlin.random.Random.nextBoolean(),
                        unreadNotificationCount = kotlin.random.Random.nextInt(0, 10)
                    )
                }
            }
        }
    }
    
    /**
     * 切换夜间模式
     */
    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
        
        // 保存到用户偏好设置
        viewModelScope.launch {
            // TODO: 保存到DataStore
        }
    }
    
    /**
     * 处理退出登录
     */
    fun logout() {
        viewModelScope.launch {
            // TODO: 执行登出逻辑
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    userInfo = UserInfo(
                        id = "",
                        displayName = "未登录用户",
                        email = "",
                        isOnline = false
                    )
                )
            }
        }
    }
    
    /**
     * 设置紧急状态
     */
    fun setEmergencyStatus(active: Boolean) {
        _uiState.update { it.copy(isEmergencyActive = active) }
    }
    
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        wakeWordListener?.let { wakeWordDetector.removeListener(it) }
    }
}