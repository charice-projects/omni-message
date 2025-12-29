// 📁 app/src/main/java/com/omnimsg/app/ui/screens/SettingsScreen.kt
package com.omnimsg.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omnimsg.app.R
import com.omnimsg.app.ui.components.Common.*
import com.omnimsg.app.ui.navigation.AppDestinations
import com.omnimsg.app.ui.viewmodels.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigate: (AppDestinations) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    // 收集UI状态
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // 收集UI事件
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(initialValue = null)
    
    // 处理UI事件
    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    onShowSnackbar(event.message)
                }
                is UiEvent.Navigate -> {
                    onNavigate(event.destination)
                }
                else -> {}
            }
        }
    }
    
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { /* 返回 */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 搜索设置 */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { /* 帮助 */ }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "帮助")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 用户资料卡片
            UserProfileCard(
                userName = state.userName,
                userEmail = state.userEmail,
                userAvatar = state.userAvatar,
                isLoggedIn = state.isLoggedIn,
                onLoginToggle = viewModel::toggleLogin
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 外观设置
            AppearanceSettingsSection(
                theme = state.theme,
                language = state.language,
                fontSize = state.fontSize,
                useSystemTheme = state.useSystemTheme,
                darkMode = state.darkMode,
                onThemeChange = viewModel::updateTheme,
                onLanguageChange = viewModel::updateLanguage,
                onFontSizeChange = viewModel::updateFontSize,
                onSystemThemeToggle = viewModel::toggleSystemTheme,
                onDarkModeChange = viewModel::toggleDarkMode
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 通知设置
            NotificationSettingsSection(
                notificationsEnabled = state.notificationsEnabled,
                soundEnabled = state.soundEnabled,
                vibrationEnabled = state.vibrationEnabled,
                quietHoursEnabled = state.quietHoursEnabled,
                quietStartTime = state.quietStartTime,
                quietEndTime = state.quietEndTime,
                onNotificationSettingsChange = viewModel::updateNotificationSettings
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 隐私设置
            PrivacySettingsSection(
                analyticsEnabled = state.analyticsEnabled,
                crashReportsEnabled = state.crashReportsEnabled,
                backupEnabled = state.backupEnabled,
                syncEnabled = state.syncEnabled,
                onPrivacySettingsChange = viewModel::updatePrivacySettings
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 消息设置
            MessageSettingsSection(
                syncInterval = state.messageSyncInterval,
                mediaQuality = state.mediaDownloadQuality,
                autoDelete = state.autoDeleteOldMessages,
                deleteDays = state.autoDeleteDays,
                onMessageSettingsChange = viewModel::updateMessageSettings
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 存储管理
            StorageManagementSection(
                totalStorage = state.totalStorage,
                usedStorage = state.usedStorage,
                messageStorage = state.messageStorage,
                mediaStorage = state.mediaStorage,
                cacheSize = state.cacheSize,
                onClearCache = viewModel::clearCache,
                onClearMedia = viewModel::clearMedia
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 备份与恢复
            BackupRestoreSection(
                backupEnabled = state.backupEnabled,
                lastBackupTime = state.lastBackupTime,
                isBackingUp = state.isBackingUp,
                isRestoring = state.isRestoring,
                onBackup = viewModel::performBackup,
                onRestore = viewModel::performRestore,
                onExport = viewModel::exportData
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 关于应用
            AboutAppSection(
                onRateApp = { /* TODO: 应用商店评分 */ },
                onShareApp = { /* TODO: 分享应用 */ },
                onPrivacyPolicy = { /* TODO: 隐私政策 */ },
                onTermsOfService = { /* TODO: 服务条款 */ },
                onVersionInfo = { /* TODO: 版本信息 */ },
                onHelpAndSupport = { /* TODO: 帮助与支持 */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 应用版本
            AppVersionCard()
        }
    }
}

@Composable
private fun UserProfileCard(
    userName: String,
    userEmail: String,
    userAvatar: String?,
    isLoggedIn: Boolean,
    onLoginToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 用户头像
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (userAvatar != null) Color.Transparent
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatar != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userAvatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = "用户头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            if (isLoggedIn) Icons.Default.Person else Icons.Default.PersonOutline,
                            contentDescription = "用户",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 用户信息
                Column {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isLoggedIn && userEmail.isNotEmpty()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Text(
                        text = if (isLoggedIn) "已登录" else "未登录",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLoggedIn) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // 登录/登出按钮
            Button(
                onClick = onLoginToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoggedIn) MaterialTheme.colorScheme.errorContainer
                                   else MaterialTheme.colorScheme.primary,
                    contentColor = if (isLoggedIn) MaterialTheme.colorScheme.onErrorContainer
                                  else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = if (isLoggedIn) "退出" else "登录",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    theme: AppTheme,
    language: AppLanguage,
    fontSize: FontSize,
    useSystemTheme: Boolean,
    darkMode: DarkMode,
    onThemeChange: (AppTheme) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onFontSizeChange: (FontSize) -> Unit,
    onSystemThemeToggle: () -> Unit,
    onDarkModeChange: (DarkMode) -> Unit
) {
    var expandedTheme by rememberSaveable { mutableStateOf(false) }
    var expandedLanguage by rememberSaveable { mutableStateOf(false) }
    var expandedFontSize by rememberSaveable { mutableStateOf(false) }
    var expandedDarkMode by rememberSaveable { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "外观设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 主题选择
            SettingsDropdownItem(
                title = "主题",
                value = getThemeName(theme),
                icon = Icons.Default.Palette,
                expanded = expandedTheme,
                onExpandedChange = { expandedTheme = it }
            ) {
                DropdownMenuItem(
                    text = { Text("默认主题") },
                    onClick = {
                        onThemeChange(AppTheme.DEFAULT)
                        expandedTheme = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("蓝色主题") },
                    onClick = {
                        onThemeChange(AppTheme.BLUE)
                        expandedTheme = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("绿色主题") },
                    onClick = {
                        onThemeChange(AppTheme.GREEN)
                        expandedTheme = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("紫色主题") },
                    onClick = {
                        onThemeChange(AppTheme.PURPLE)
                        expandedTheme = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("深蓝主题") },
                    onClick = {
                        onThemeChange(AppTheme.DARK_BLUE)
                        expandedTheme = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Material 主题") },
                    onClick = {
                        onThemeChange(AppTheme.MATERIAL)
                        expandedTheme = false
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 语言选择
            SettingsDropdownItem(
                title = "语言",
                value = getLanguageName(language),
                icon = Icons.Default.Language,
                expanded = expandedLanguage,
                onExpandedChange = { expandedLanguage = it }
            ) {
                DropdownMenuItem(
                    text = { Text("跟随系统") },
                    onClick = {
                        onLanguageChange(AppLanguage.SYSTEM)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("简体中文") },
                    onClick = {
                        onLanguageChange(AppLanguage.ZH_CN)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("English (US)") },
                    onClick = {
                        onLanguageChange(AppLanguage.EN_US)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("日本語") },
                    onClick = {
                        onLanguageChange(AppLanguage.JA)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("한국어") },
                    onClick = {
                        onLanguageChange(AppLanguage.KO)
                        expandedLanguage = false
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 字体大小
            SettingsDropdownItem(
                title = "字体大小",
                value = getFontSizeName(fontSize),
                icon = Icons.Default.FormatSize,
                expanded = expandedFontSize,
                onExpandedChange = { expandedFontSize = it }
            ) {
                DropdownMenuItem(
                    text = { Text("小") },
                    onClick = {
                        onFontSizeChange(FontSize.SMALL)
                        expandedFontSize = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("正常") },
                    onClick = {
                        onFontSizeChange(FontSize.NORMAL)
                        expandedFontSize = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("大") },
                    onClick = {
                        onFontSizeChange(FontSize.LARGE)
                        expandedFontSize = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("特大") },
                    onClick = {
                        onFontSizeChange(FontSize.XLARGE)
                        expandedFontSize = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("巨大") },
                    onClick = {
                        onFontSizeChange(FontSize.XXLARGE)
                        expandedFontSize = false
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 系统主题跟随
            SettingsSwitchItem(
                title = "跟随系统主题",
                description = "自动匹配系统深色/浅色模式",
                icon = Icons.Default.SystemUpdate,
                checked = useSystemTheme,
                onCheckedChange = { onSystemThemeToggle() }
            )
            
            if (!useSystemTheme) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // 黑暗模式选择
                SettingsDropdownItem(
                    title = "黑暗模式",
                    value = getDarkModeName(darkMode),
                    icon = Icons.Default.DarkMode,
                    expanded = expandedDarkMode,
                    onExpandedChange = { expandedDarkMode = it }
                ) {
                    DropdownMenuItem(
                        text = { Text("浅色模式") },
                        onClick = {
                            onDarkModeChange(DarkMode.LIGHT)
                            expandedDarkMode = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("深色模式") },
                        onClick = {
                            onDarkModeChange(DarkMode.DARK)
                            expandedDarkMode = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("自动") },
                        onClick = {
                            onDarkModeChange(DarkMode.AUTO)
                            expandedDarkMode = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDropdownItem(
    title: String,
    value: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = "展开",
            modifier = Modifier.size(24.dp)
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(200.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() }
        )
    }
}

@Composable
private fun NotificationSettingsSection(
    notificationsEnabled: Boolean,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    quietHoursEnabled: Boolean,
    quietStartTime: String,
    quietEndTime: String,
    onNotificationSettingsChange: (
        enabled: Boolean?,
        soundEnabled: Boolean?,
        vibrationEnabled: Boolean?,
        quietHoursEnabled: Boolean?,
        quietStartTime: String?,
        quietEndTime: String?
    ) -> Unit
) {
    var expandedQuietHours by rememberSaveable { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "通知设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 通知总开关
            SettingsSwitchItem(
                title = "启用通知",
                description = "接收应用通知",
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = {
                    onNotificationSettingsChange(
                        enabled = !notificationsEnabled,
                        soundEnabled = null,
                        vibrationEnabled = null,
                        quietHoursEnabled = null,
                        quietStartTime = null,
                        quietEndTime = null
                    )
                }
            )
            
            if (notificationsEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier.padding(start = 40.dp)
                ) {
                    // 声音开关
                    SettingsSwitchItem(
                        title = "提示音",
                        description = "通知时播放声音",
                        icon = Icons.Default.VolumeUp,
                        checked = soundEnabled,
                        onCheckedChange = {
                            onNotificationSettingsChange(
                                enabled = null,
                                soundEnabled = !soundEnabled,
                                vibrationEnabled = null,
                                quietHoursEnabled = null,
                                quietStartTime = null,
                                quietEndTime = null
                            )
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 振动开关
                    SettingsSwitchItem(
                        title = "振动",
                        description = "通知时振动",
                        icon = Icons.Default.Vibration,
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            onNotificationSettingsChange(
                                enabled = null,
                                soundEnabled = null,
                                vibrationEnabled = !vibrationEnabled,
                                quietHoursEnabled = null,
                                quietStartTime = null,
                                quietEndTime = null
                            )
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 静默时段
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedQuietHours = !expandedQuietHours },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = "静默时段",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "静默时段",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (quietHoursEnabled) "$quietStartTime - $quietEndTime" else "未启用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = {
                                onNotificationSettingsChange(
                                    enabled = null,
                                    soundEnabled = null,
                                    vibrationEnabled = null,
                                    quietHoursEnabled = !quietHoursEnabled,
                                    quietStartTime = null,
                                    quietEndTime = null
                                )
                            }
                        )
                    }
                    
                    // 静默时段编辑（展开时显示）
                    AnimatedVisibility(
                        visible = expandedQuietHours && quietHoursEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "设置静默时段",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 时间选择器（简化版）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TimeSelector(
                                    label = "开始时间",
                                    time = quietStartTime,
                                    onTimeChange = { newTime ->
                                        onNotificationSettingsChange(
                                            enabled = null,
                                            soundEnabled = null,
                                            vibrationEnabled = null,
                                            quietHoursEnabled = null,
                                            quietStartTime = newTime,
                                            quietEndTime = null
                                        )
                                    }
                                )
                                
                                TimeSelector(
                                    label = "结束时间",
                                    time = quietEndTime,
                                    onTimeChange = { newTime ->
                                        onNotificationSettingsChange(
                                            enabled = null,
                                            soundEnabled = null,
                                            vibrationEnabled = null,
                                            quietHoursEnabled = null,
                                            quietStartTime = null,
                                            quietEndTime = newTime
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSelector(
    label: String,
    time: String,
    onTimeChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        OutlinedTextField(
            value = time,
            onValueChange = onTimeChange,
            modifier = Modifier.width(100.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun PrivacySettingsSection(
    analyticsEnabled: Boolean,
    crashReportsEnabled: Boolean,
    backupEnabled: Boolean,
    syncEnabled: Boolean,
    onPrivacySettingsChange: (
        analyticsEnabled: Boolean?,
        crashReportsEnabled: Boolean?,
        backupEnabled: Boolean?,
        syncEnabled: Boolean?
    ) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "隐私与数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分析数据
            SettingsSwitchItem(
                title = "使用情况分析",
                description = "匿名收集应用使用数据",
                icon = Icons.Default.Analytics,
                checked = analyticsEnabled,
                onCheckedChange = {
                    onPrivacySettingsChange(
                        analyticsEnabled = !analyticsEnabled,
                        crashReportsEnabled = null,
                        backupEnabled = null,
                        syncEnabled = null
                    )
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 崩溃报告
            SettingsSwitchItem(
                title = "崩溃报告",
                description = "自动发送崩溃报告以帮助改进",
                icon = Icons.Default.BugReport,
                checked = crashReportsEnabled,
                onCheckedChange = {
                    onPrivacySettingsChange(
                        analyticsEnabled = null,
                        crashReportsEnabled = !crashReportsEnabled,
                        backupEnabled = null,
                        syncEnabled = null
                    )
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 自动备份
            SettingsSwitchItem(
                title = "自动备份",
                description = "定期备份消息和联系人",
                icon = Icons.Default.Backup,
                checked = backupEnabled,
                onCheckedChange = {
                    onPrivacySettingsChange(
                        analyticsEnabled = null,
                        crashReportsEnabled = null,
                        backupEnabled = !backupEnabled,
                        syncEnabled = null
                    )
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 云同步
            SettingsSwitchItem(
                title = "云同步",
                description = "跨设备同步消息和设置",
                icon = Icons.Default.CloudSync,
                checked = syncEnabled,
                onCheckedChange = {
                    onPrivacySettingsChange(
                        analyticsEnabled = null,
                        crashReportsEnabled = null,
                        backupEnabled = null,
                        syncEnabled = !syncEnabled
                    )
                }
            )
        }
    }
}

@Composable
private fun MessageSettingsSection(
    syncInterval: SyncInterval,
    mediaQuality: MediaQuality,
    autoDelete: Boolean,
    deleteDays: Int,
    onMessageSettingsChange: (
        syncInterval: SyncInterval?,
        mediaQuality: MediaQuality?,
        autoDelete: Boolean?,
        deleteDays: Int?
    ) -> Unit
) {
    var expandedSyncInterval by rememberSaveable { mutableStateOf(false) }
    var expandedMediaQuality by rememberSaveable { mutableStateOf(false) }
    var showDeleteDaysPicker by rememberSaveable { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "消息设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 同步间隔
            SettingsDropdownItem(
                title = "同步频率",
                value = getSyncIntervalName(syncInterval),
                icon = Icons.Default.Sync,
                expanded = expandedSyncInterval,
                onExpandedChange = { expandedSyncInterval = it }
            ) {
                DropdownMenuItem(
                    text = { Text("实时同步") },
                    onClick = {
                        onMessageSettingsChange(SyncInterval.REAL_TIME, null, null, null)
                        expandedSyncInterval = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("每5分钟") },
                    onClick = {
                        onMessageSettingsChange(SyncInterval.MINUTES_5, null, null, null)
                        expandedSyncInterval = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("每15分钟") },
                    onClick = {
                        onMessageSettingsChange(SyncInterval.MINUTES_15, null, null, null)
                        expandedSyncInterval = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("每30分钟") },
                    onClick = {
                        onMessageSettingsChange(SyncInterval.MINUTES_30, null, null, null)
                        expandedSyncInterval = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("每小时") },
                    onClick = {
                        onMessageSettingsChange(SyncInterval.HOUR_1, null, null, null)
                        expandedSyncInterval = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("手动同步") },
                    onClick = {
                        onMessageSettingsChange(SyncInterval.MANUAL, null, null, null)
                        expandedSyncInterval = false
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 媒体质量
            SettingsDropdownItem(
                title = "媒体下载质量",
                value = getMediaQualityName(mediaQuality),
                icon = Icons.Default.Hd,
                expanded = expandedMediaQuality,
                onExpandedChange = { expandedMediaQuality = it }
            ) {
                DropdownMenuItem(
                    text = { Text("原画质") },
                    onClick = {
                        onMessageSettingsChange(null, MediaQuality.ORIGINAL, null, null)
                        expandedMediaQuality = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("高画质") },
                    onClick = {
                        onMessageSettingsChange(null, MediaQuality.HIGH, null, null)
                        expandedMediaQuality = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("中等画质") },
                    onClick = {
                        onMessageSettingsChange(null, MediaQuality.MEDIUM, null, null)
                        expandedMediaQuality = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("低画质") },
                    onClick = {
                        onMessageSettingsChange(null, MediaQuality.LOW, null, null)
                        expandedMediaQuality = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("自动") },
                    onClick = {
                        onMessageSettingsChange(null, MediaQuality.AUTO, null, null)
                        expandedMediaQuality = false
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 自动删除
            SettingsSwitchItem(
                title = "自动删除旧消息",
                description = "自动清理30天前的消息",
                icon = Icons.Default.DeleteSweep,
                checked = autoDelete,
                onCheckedChange = {
                    onMessageSettingsChange(null, null, !autoDelete, null)
                }
            )
            
            if (autoDelete) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "删除时间：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 天数选择器
                    OutlinedTextField(
                        value = deleteDays.toString(),
                        onValueChange = { newValue ->
                            val days = newValue.toIntOrNull()
                            if (days != null && days in 1..365) {
                                onMessageSettingsChange(null, null, null, days)
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        suffix = { Text("天") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageManagementSection(
    totalStorage: Long,
    usedStorage: Long,
    messageStorage: Long,
    mediaStorage: Long,
    cacheSize: Long,
    onClearCache: () -> Unit,
    onClearMedia: () -> Unit
) {
    val formatter = remember { NumberFormat.getInstance(Locale.getDefault()) }
    val usedGB = remember(usedStorage) { usedStorage / 1024.0 / 1024.0 / 1024.0 }
    val totalGB = remember(totalStorage) { totalStorage / 1024.0 / 1024.0 / 1024.0 }
    val usedPercentage = remember(usedStorage, totalStorage) {
        if (totalStorage > 0) (usedStorage.toDouble() / totalStorage) * 100 else 0.0
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "存储管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${formatter.format(usedGB)} / ${formatter.format(totalGB)} GB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 存储使用进度条
            LinearProgressIndicator(
                progress = (usedPercentage / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 存储使用明细
            StorageUsageDetail(
                title = "消息存储",
                size = messageStorage,
                onClear = null // 消息存储不能直接清除
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            StorageUsageDetail(
                title = "媒体文件",
                size = mediaStorage,
                onClear = { onClearMedia() }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            StorageUsageDetail(
                title = "应用缓存",
                size = cacheSize,
                onClear = { onClearCache() }
            )
        }
    }
}

@Composable
private fun StorageUsageDetail(
    title: String,
    size: Long,
    onClear: (() -> Unit)?
) {
    val formatter = remember { NumberFormat.getInstance(Locale.getDefault()) }
    val sizeMB = remember(size) { size / 1024.0 / 1024.0 }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatter.format(sizeMB)} MB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (onClear != null && size > 0) {
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("清理")
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreSection(
    backupEnabled: Boolean,
    lastBackupTime: Long?,
    isBackingUp: Boolean,
    isRestoring: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val lastBackupStr = remember(lastBackupTime) {
        lastBackupTime?.let { dateFormat.format(Date(it)) } ?: "从未备份"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "备份与恢复",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 备份状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = "备份",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "自动备份",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (backupEnabled) "已启用" else "已禁用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = backupEnabled,
                    onCheckedChange = { /* 在隐私设置中控制 */ }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // 上次备份时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "历史",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "上次备份",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = lastBackupStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 立即备份按钮
                Button(
                    onClick = onBackup,
                    modifier = Modifier.weight(1f),
                    enabled = !isBackingUp && !isRestoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("备份中...")
                    } else {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "备份",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即备份")
                    }
                }
                
                // 恢复按钮
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    enabled = !isBackingUp && !isRestoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("恢复中...")
                    } else {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "恢复",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("恢复数据")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 导出按钮
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBackingUp && !isRestoring
            ) {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = "导出",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出数据")
            }
        }
    }
}

@Composable
private fun AboutAppSection(
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsOfService: () -> Unit,
    onVersionInfo: () -> Unit,
    onHelpAndSupport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "关于应用",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 功能列表
            AboutAppItem(
                title = "评分应用",
                icon = Icons.Default.Star,
                onClick = onRateApp
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            AboutAppItem(
                title = "分享应用",
                icon = Icons.Default.Share,
                onClick = onShareApp
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            AboutAppItem(
                title = "隐私政策",
                icon = Icons.Default.PrivacyTip,
                onClick = onPrivacyPolicy
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            AboutAppItem(
                title = "服务条款",
                icon = Icons.Default.Description,
                onClick = onTermsOfService
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            AboutAppItem(
                title = "版本信息",
                icon = Icons.Default.Info,
                onClick = onVersionInfo
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            AboutAppItem(
                title = "帮助与支持",
                icon = Icons.Default.Help,
                onClick = onHelpAndSupport
            )
        }
    }
}

@Composable
private fun AboutAppItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "跳转",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppVersionCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "OmniMessage Pro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "版本 1.0.0 (10000)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "© 2024 OmniMessage. 保留所有权利。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// 辅助函数：获取枚举值的显示名称
private fun getThemeName(theme: AppTheme): String = when (theme) {
    AppTheme.DEFAULT -> "默认主题"
    AppTheme.BLUE -> "蓝色主题"
    AppTheme.GREEN -> "绿色主题"
    AppTheme.PURPLE -> "紫色主题"
    AppTheme.DARK_BLUE -> "深蓝主题"
    AppTheme.MATERIAL -> "Material 主题"
    AppTheme.CUSTOM -> "自定义主题"
}

private fun getLanguageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> "跟随系统"
    AppLanguage.ZH_CN -> "简体中文"
    AppLanguage.ZH_TW -> "繁体中文"
    AppLanguage.EN_US -> "English (US)"
    AppLanguage.EN_UK -> "English (UK)"
    AppLanguage.JA -> "日本語"
    AppLanguage.KO -> "한국어"
    AppLanguage.FR -> "Français"
    AppLanguage.DE -> "Deutsch"
    AppLanguage.ES -> "Español"
    AppLanguage.RU -> "Русский"
    AppLanguage.AR -> "العربية"
}

private fun getFontSizeName(fontSize: FontSize): String = when (fontSize) {
    FontSize.SMALL -> "小"
    FontSize.NORMAL -> "正常"
    FontSize.LARGE -> "大"
    FontSize.XLARGE -> "特大"
    FontSize.XXLARGE -> "巨大"
}

private fun getDarkModeName(darkMode: DarkMode): String = when (darkMode) {
    DarkMode.LIGHT -> "浅色模式"
    DarkMode.DARK -> "深色模式"
    DarkMode.AUTO -> "自动"
}

private fun getSyncIntervalName(syncInterval: SyncInterval): String = when (syncInterval) {
    SyncInterval.REAL_TIME -> "实时同步"
    SyncInterval.MINUTES_5 -> "每5分钟"
    SyncInterval.MINUTES_15 -> "每15分钟"
    SyncInterval.MINUTES_30 -> "每30分钟"
    SyncInterval.HOUR_1 -> "每小时"
    SyncInterval.MANUAL -> "手动同步"
}

private fun getMediaQualityName(mediaQuality: MediaQuality): String = when (mediaQuality) {
    MediaQuality.ORIGINAL -> "原画质"
    MediaQuality.HIGH -> "高画质"
    MediaQuality.MEDIUM -> "中等画质"
    MediaQuality.LOW -> "低画质"
    MediaQuality.AUTO -> "自动"
}