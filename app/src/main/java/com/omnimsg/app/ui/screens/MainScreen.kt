// 📁 app/src/main/java/com/omnimsg/app/ui/screens/MainScreen.kt
package com.omnimsg.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omnimsg.app.ui.navigation.AppDestinations
import com.omnimsg.app.ui.navigation.DrawerNavigationItem
import com.omnimsg.app.ui.viewmodels.MainViewModel
import com.omnimsg.feature.contact.ui.ContactListScreen
import com.omnimsg.feature.excelimport.ui.ExcelImportScreen
import com.omnimsg.feature.messaging.ui.MessageListScreen
import com.omnimsg.feature.quickactions.ui.EmergencyScreen
import com.omnimsg.feature.settings.ui.SettingsScreen
import com.omnimsg.feature.voice.ui.VoiceControlScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 主屏幕 - 应用的主要导航框架
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = hiltViewModel(),
    onShowSnackbar: (String) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 获取当前目的地
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 从ViewModel获取状态
    val uiState by viewModel.uiState.collectAsState()
    val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()
    val isVoiceWakeWordActive by viewModel.isVoiceWakeWordActive.collectAsState()
    val unreadMessageCount by viewModel.unreadMessageCount.collectAsState()
    
    // 获取当前目的地的路由
    val currentRoute = currentDestination?.route
    
    // 打开抽屉
    val openDrawer = {
        scope.launch {
            drawerState.open()
        }
    }
    
    // 关闭抽屉
    val closeDrawer = {
        scope.launch {
            drawerState.close()
        }
    }
    
    // 导航到目的地
    val onDestinationSelected = { destination: AppDestinations ->
        scope.launch {
            drawerState.close()
            navController.navigate(destination.route) {
                // 导航选项
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    
    // 根据当前路由确定是否显示FAB
    val showFAB = when (currentRoute) {
        AppDestinations.Home.route -> true
        AppDestinations.Messages.route -> true
        AppDestinations.Contacts.route -> true
        else -> false
    }
    
    // 根据当前路由确定FAB的图标和操作
    val (fabIcon, fabAction) = when (currentRoute) {
        AppDestinations.Home.route -> Pair(Icons.Default.Add, { 
            // TODO: 主页添加操作
            onShowSnackbar("主页添加功能")
        })
        AppDestinations.Messages.route -> Pair(Icons.Default.Create, { 
            // TODO: 新建消息
            onShowSnackbar("新建消息")
        })
        AppDestinations.Contacts.route -> Pair(Icons.Default.PersonAdd, { 
            // TODO: 添加联系人
            onShowSnackbar("添加联系人")
        })
        else -> Pair(Icons.Default.Add, {})
    }
    
    // 处理紧急按钮点击
    val onEmergencyButtonClick = {
        viewModel.triggerEmergencyAlert()
        onShowSnackbar("紧急报警已触发")
    }
    
    // 处理语音按钮点击
    val onVoiceButtonClick = {
        viewModel.startVoiceInput()
        onShowSnackbar("语音输入已启动")
    }
    
    // 处理搜索按钮点击
    val onSearchClick = {
        // TODO: 搜索功能
        onShowSnackbar("搜索功能")
    }
    
    // 处理通知按钮点击
    val onNotificationClick = {
        // TODO: 通知中心
        onShowSnackbar("通知中心")
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            AppDrawer(
                currentDestination = currentDestination,
                onDestinationSelected = onDestinationSelected,
                onCloseDrawer = closeDrawer,
                viewModel = viewModel,
                modifier = Modifier.width(300.dp)
            )
        }
    ) {
        Scaffold(
            topBar = {
                MainAppBar(
                    title = when (currentRoute) {
                        AppDestinations.Home.route -> "首页"
                        AppDestinations.Messages.route -> "消息"
                        AppDestinations.Contacts.route -> "联系人"
                        AppDestinations.VoiceControl.route -> "语音控制"
                        AppDestinations.Emergency.route -> "紧急报警"
                        AppDestinations.ExcelImport.route -> "Excel导入"
                        AppDestinations.Settings.route -> "设置"
                        AppDestinations.PrivacyCenter.route -> "隐私中心"
                        else -> "OmniMessage Pro"
                    },
                    onMenuClick = openDrawer,
                    onSearchClick = onSearchClick,
                    onNotificationClick = onNotificationClick,
                    onVoiceIconClick = onVoiceButtonClick,
                    onEmergencyIconClick = onEmergencyButtonClick,
                    showEmergencyIcon = isEmergencyActive,
                    showVoiceIcon = isVoiceWakeWordActive
                )
            },
            bottomBar = {
                MainBottomNavigation(
                    currentDestination = currentDestination,
                    onDestinationSelected = onDestinationSelected,
                    showBottomNav = when (currentRoute) {
                        AppDestinations.Home.route -> true
                        AppDestinations.Messages.route -> true
                        AppDestinations.Contacts.route -> true
                        AppDestinations.VoiceControl.route -> true
                        AppDestinations.Emergency.route -> true
                        else -> false
                    },
                    unreadMessageCount = unreadMessageCount
                )
            },
            floatingActionButton = {
                if (showFAB) {
                    MainFloatingActionButton(
                        icon = fabIcon,
                        onClick = fabAction
                    )
                }
            },
            snackbarHost = { SnackbarHost(it) }
        ) { paddingValues ->
            // 紧急状态指示器
            AnimatedVisibility(
                visible = isEmergencyActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                ) {
                    EmergencyStatusIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                    )
                }
            }
            
            // 语音唤醒词指示器
            AnimatedVisibility(
                visible = isVoiceWakeWordActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                ) {
                    VoiceWakeWordIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                    )
                }
            }
            
            // 主内容区域 - 导航宿主
            NavHost(
                navController = navController,
                startDestination = AppDestinations.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(
                        top = if (isEmergencyActive || isVoiceWakeWordActive) 48.dp else 0.dp
                    )
            ) {
                // 主屏幕
                composable(AppDestinations.Home.route) {
                    HomeScreen(
                        viewModel = hiltViewModel(),
                        onNavigate = onDestinationSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                }
                
                // 消息列表
                composable(AppDestinations.Messages.route) {
                    MessageListScreen(
                        viewModel = hiltViewModel(),
                        onNavigate = onDestinationSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                }
                
                // 消息详情
                composable(
                    route = AppDestinations.MessageDetail.route,
                    arguments = listOf(
                        navArgument("messageId") {
                            type = androidx.navigation.NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
                    // TODO: 实现消息详情屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("消息详情: $messageId")
                    }
                }
                
                // 新建消息
                composable(AppDestinations.NewMessage.route) {
                    // TODO: 实现新建消息屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("新建消息")
                    }
                }
                
                // 联系人列表
                composable(AppDestinations.Contacts.route) {
                    ContactListScreen(
                        viewModel = hiltViewModel(),
                        onNavigate = onDestinationSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                }
                
                // 联系人详情
                composable(
                    route = AppDestinations.ContactDetail.route,
                    arguments = listOf(
                        navArgument("contactId") {
                            type = androidx.navigation.NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                    // TODO: 实现联系人详情屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("联系人详情: $contactId")
                    }
                }
                
                // 语音控制
                composable(AppDestinations.VoiceControl.route) {
                    VoiceControlScreen(
                        viewModel = hiltViewModel(),
                        onNavigate = onDestinationSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                }
                
                // 紧急报警
                composable(AppDestinations.Emergency.route) {
                    EmergencyScreen(
                        viewModel = hiltViewModel(),
                        onNavigate = onDestinationSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                }
                
                // Excel导入
                composable(AppDestinations.ExcelImport.route) {
                    ExcelImportScreen(
                        viewModel = hiltViewModel(),
                        onNavigate = onDestinationSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                }
                
                // 设置
                composable(AppDestinations.Settings.route) {
                    SettingsScreen(
                        viewModel = hiltViewModel(),
                        onNavigate = onDestinationSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                }
                
                // 隐私中心
                composable(AppDestinations.PrivacyCenter.route) {
                    // TODO: 实现隐私中心屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("隐私中心")
                    }
                }
                
                // 个人资料
                composable(AppDestinations.Profile.route) {
                    // TODO: 实现个人资料屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("个人资料")
                    }
                }
                
                // 文件管理
                composable(AppDestinations.FileManager.route) {
                    // TODO: 实现文件管理屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("文件管理")
                    }
                }
                
                // 团队协作
                composable(AppDestinations.Team.route) {
                    // TODO: 实现团队协作屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("团队协作")
                    }
                }
                
                // 工作流
                composable(AppDestinations.Workflow.route) {
                    // TODO: 实现工作流屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("工作流")
                    }
                }
                
                // 数据分析
                composable(AppDestinations.Analytics.route) {
                    // TODO: 实现数据分析屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("数据分析")
                    }
                }
                
                // 帮助与反馈
                composable(AppDestinations.Help.route) {
                    // TODO: 实现帮助与反馈屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("帮助与反馈")
                    }
                }
                
                // 关于我们
                composable(AppDestinations.About.route) {
                    // TODO: 实现关于我们屏幕
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("关于我们")
                    }
                }
            }
        }
    }
}

/**
 * 主应用栏 - 增强版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppBar(
    title: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onVoiceIconClick: () -> Unit,
    onEmergencyIconClick: () -> Unit,
    showEmergencyIcon: Boolean = false,
    showVoiceIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "菜单")
            }
        },
        actions = {
            // 紧急按钮（条件显示）
            if (showEmergencyIcon) {
                IconButton(onClick = onEmergencyIconClick) {
                    Icon(Icons.Default.Warning, contentDescription = "紧急", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            // 语音按钮（条件显示）
            if (showVoiceIcon) {
                IconButton(onClick = onVoiceIconClick) {
                    Icon(Icons.Default.Mic, contentDescription = "语音", tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
            
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Default.Notifications, contentDescription = "通知")
            }
        },
        modifier = modifier
    )
}

/**
 * 底部导航栏 - 增强版
 */
@Composable
private fun MainBottomNavigation(
    currentDestination: NavDestination?,
    onDestinationSelected: (AppDestinations) -> Unit,
    showBottomNav: Boolean = true,
    unreadMessageCount: Int = 0,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showBottomNav,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        NavigationBar(
            modifier = modifier
        ) {
            // 首页
            NavigationBarItem(
                icon = { 
                    Icon(Icons.Default.Home, contentDescription = "首页") 
                },
                label = { Text("首页") },
                selected = currentDestination?.route == AppDestinations.Home.route,
                onClick = { onDestinationSelected(AppDestinations.Home) }
            )
            
            // 消息（带徽章）
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (unreadMessageCount > 0) {
                                Badge {
                                    Text(
                                        text = if (unreadMessageCount > 99) "99+" else unreadMessageCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "消息")
                    }
                },
                label = { Text("消息") },
                selected = currentDestination?.route == AppDestinations.Messages.route,
                onClick = { onDestinationSelected(AppDestinations.Messages) }
            )
            
            // 联系人
            NavigationBarItem(
                icon = { Icon(Icons.Default.Contacts, contentDescription = "联系人") },
                label = { Text("联系人") },
                selected = currentDestination?.route == AppDestinations.Contacts.route,
                onClick = { onDestinationSelected(AppDestinations.Contacts) }
            )
            
            // 语音控制
            NavigationBarItem(
                icon = { Icon(Icons.Default.Mic, contentDescription = "语音控制") },
                label = { Text("语音") },
                selected = currentDestination?.route == AppDestinations.VoiceControl.route,
                onClick = { onDestinationSelected(AppDestinations.VoiceControl) }
            )
            
            // 紧急报警
            NavigationBarItem(
                icon = { Icon(Icons.Default.Warning, contentDescription = "紧急报警") },
                label = { Text("紧急") },
                selected = currentDestination?.route == AppDestinations.Emergency.route,
                onClick = { onDestinationSelected(AppDestinations.Emergency) }
            )
        }
    }
}

/**
 * 浮动操作按钮
 */
@Composable
private fun MainFloatingActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = "操作")
    }
}

/**
 * 用户头像组件
 */
@Composable
fun UserAvatar(
    userInfo: UserInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (userInfo.avatarUrl != null) {
                // 这里应该使用图片加载库加载头像
                // 简化实现：显示首字母
                Text(
                    text = userInfo.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = userInfo.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 抽屉导航组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDrawer(
    currentDestination: NavDestination?,
    onDestinationSelected: (AppDestinations) -> Unit,
    onCloseDrawer: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerItems = rememberDrawerItems()
    val scope = rememberCoroutineScope()
    
    ModalDrawerSheet(
        modifier = modifier,
        drawerShape = RoundedCornerShape(
            topEnd = 16.dp,
            bottomEnd = 16.dp
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 抽屉头部
            item {
                DrawerHeader(
                    userInfo = uiState.userInfo,
                    onProfileClick = {
                        onDestinationSelected(AppDestinations.Profile)
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
            
            // 分隔线
            item {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
            
            // 主要导航项
            items(drawerItems.filter { it.section == DrawerSection.MAIN }) { item ->
                DrawerNavigationItem(
                    item = item,
                    selected = currentDestination?.route == item.destination.route,
                    onClick = {
                        onDestinationSelected(item.destination)
                        onCloseDrawer()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 工具项分隔标题
            item {
                Text(
                    text = "工具",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    )
                )
            }
            
            // 工具项
            items(drawerItems.filter { it.section == DrawerSection.TOOLS }) { item ->
                DrawerNavigationItem(
                    item = item,
                    selected = currentDestination?.route == item.destination.route,
                    onClick = {
                        onDestinationSelected(item.destination)
                        onCloseDrawer()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 设置项分隔标题
            item {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    )
                )
            }
            
            // 设置项
            items(drawerItems.filter { it.section == DrawerSection.SETTINGS }) { item ->
                DrawerNavigationItem(
                    item = item,
                    selected = currentDestination?.route == item.destination.route,
                    onClick = {
                        onDestinationSelected(item.destination)
                        onCloseDrawer()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 底部空间和版本信息
            item {
                Spacer(modifier = Modifier.weight(1f))
                
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                // 夜间模式切换
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (uiState.isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = "主题",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (uiState.isDarkMode) "夜间模式" else "日间模式",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = uiState.isDarkMode,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                viewModel.toggleDarkMode(isChecked)
                            }
                        },
                        thumbContent = {
                            Icon(
                                if (uiState.isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                }
                
                // 版本信息
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OmniMessage Pro",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "版本 ${uiState.appVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "© 2024 OmniMessage Team",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                // 退出登录按钮
                if (uiState.isLoggedIn) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.logout()
                                onCloseDrawer()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "退出登录",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("退出登录")
                    }
                }
            }
        }
    }
}

/**
 * 抽屉头部组件
 */
@Composable
private fun DrawerHeader(
    userInfo: UserInfo,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        Surface(
            onClick = onProfileClick,
            shape = CircleShape,
            modifier = Modifier.size(80.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (userInfo.avatarUrl != null) {
                    // 这里应该使用图片加载库加载头像
                    // 简化实现：显示首字母
                    Text(
                        text = userInfo.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = userInfo.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 用户信息
        Text(
            text = userInfo.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (userInfo.email.isNotBlank()) {
            Text(
                text = userInfo.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 用户状态
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (userInfo.isOnline) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (userInfo.isOnline) Color(0xFF4CAF50) 
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (userInfo.isOnline) "在线" else "离线",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (userInfo.isOnline) Color(0xFF4CAF50) 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 快速统计
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = userInfo.contactCount.toString(),
                label = "联系人",
                icon = Icons.Default.Contacts,
                onClick = onProfileClick
            )
            StatItem(
                value = userInfo.unreadMessageCount.toString(),
                label = "未读",
                icon = Icons.Default.Mail,
                onClick = onProfileClick
            )
            StatItem(
                value = userInfo.storageUsage,
                label = "存储",
                icon = Icons.Default.Storage,
                onClick = onProfileClick
            )
        }
    }
}

/**
 * 统计项组件
 */
@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 抽屉导航项组件
 */
@Composable
private fun DrawerNavigationItem(
    item: DrawerNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationDrawerItem(
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 显示徽章（如果有）
                item.badgeCount?.let { count ->
                    if (count > 0) {
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (count > 99) "99+" else count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = modifier.padding(horizontal = 12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

/**
 * 记住抽屉导航项
 */
@Composable
private fun rememberDrawerItems(): List<DrawerNavigationItem> {
    return remember {
        listOf(
            // 主要导航
            DrawerNavigationItem(
                title = "首页",
                icon = Icons.Default.Home,
                destination = AppDestinations.Home,
                section = DrawerSection.MAIN
            ),
            DrawerNavigationItem(
                title = "消息",
                icon = Icons.Default.Chat,
                destination = AppDestinations.Messages,
                section = DrawerSection.MAIN,
                badgeCount = 3 // 示例：3条未读消息
            ),
            DrawerNavigationItem(
                title = "联系人",
                icon = Icons.Default.Contacts,
                destination = AppDestinations.Contacts,
                section = DrawerSection.MAIN,
                badgeCount = 5 // 示例：5个新联系人请求
            ),
            DrawerNavigationItem(
                title = "语音控制",
                icon = Icons.Default.Mic,
                destination = AppDestinations.VoiceControl,
                section = DrawerSection.MAIN
            ),
            DrawerNavigationItem(
                title = "紧急报警",
                icon = Icons.Default.Warning,
                destination = AppDestinations.Emergency,
                section = DrawerSection.MAIN
            ),
            
            // 工具
            DrawerNavigationItem(
                title = "Excel导入",
                icon = Icons.Default.TableChart,
                destination = AppDestinations.ExcelImport,
                section = DrawerSection.TOOLS
            ),
            DrawerNavigationItem(
                title = "文件管理",
                icon = Icons.Default.Folder,
                destination = AppDestinations.FileManager,
                section = DrawerSection.TOOLS
            ),
            DrawerNavigationItem(
                title = "团队协作",
                icon = Icons.Default.Group,
                destination = AppDestinations.Team,
                section = DrawerSection.TOOLS
            ),
            DrawerNavigationItem(
                title = "工作流",
                icon = Icons.Default.Work,
                destination = AppDestinations.Workflow,
                section = DrawerSection.TOOLS
            ),
            DrawerNavigationItem(
                title = "数据分析",
                icon = Icons.Default.Analytics,
                destination = AppDestinations.Analytics,
                section = DrawerSection.TOOLS
            ),
            
            // 设置
            DrawerNavigationItem(
                title = "设置",
                icon = Icons.Default.Settings,
                destination = AppDestinations.Settings,
                section = DrawerSection.SETTINGS
            ),
            DrawerNavigationItem(
                title = "隐私中心",
                icon = Icons.Default.PrivacyTip,
                destination = AppDestinations.PrivacyCenter,
                section = DrawerSection.SETTINGS
            ),
            DrawerNavigationItem(
                title = "通知管理",
                icon = Icons.Default.Notifications,
                destination = AppDestinations.NotificationSettings,
                section = DrawerSection.SETTINGS
            ),
            DrawerNavigationItem(
                title = "帮助与反馈",
                icon = Icons.Default.Help,
                destination = AppDestinations.Help,
                section = DrawerSection.SETTINGS
            ),
            DrawerNavigationItem(
                title = "关于我们",
                icon = Icons.Default.Info,
                destination = AppDestinations.About,
                section = DrawerSection.SETTINGS
            )
        )
    }
}

/**
 * 语音唤醒词指示器
 */
@Composable
private fun VoiceWakeWordIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "语音唤醒",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "语音唤醒已激活",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            // 脉冲动画
            var pulse by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    pulse = !pulse
                    delay(500)
                }
            }
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (pulse) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

/**
 * 紧急状态指示器
 */
@Composable
private fun EmergencyStatusIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "紧急状态",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "紧急",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== 枚举和数据结构 ====================

/**
 * 抽屉分区枚举
 */
enum class DrawerSection {
    MAIN, TOOLS, SETTINGS
}

/**
 * 抽屉导航项数据类
 */
data class DrawerNavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val destination: AppDestinations,
    val section: DrawerSection,
    val badgeCount: Int? = null
)

/**
 * 用户信息数据类
 */
data class UserInfo(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = true,
    val contactCount: Int = 0,
    val unreadMessageCount: Int = 0,
    val storageUsage: String = "0.0 GB",
    val lastActive: Long = System.currentTimeMillis()
)