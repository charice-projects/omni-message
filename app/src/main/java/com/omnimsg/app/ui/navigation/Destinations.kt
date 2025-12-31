// 📁 app/src/main/java/com/omnimsg/app/ui/navigation/Destinations.kt
package com.omnimsg.app.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用导航目的地
 * 包含所有屏幕的路由定义
 */
sealed class AppDestinations(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val showInDrawer: Boolean = true,
    val showInBottomNav: Boolean = false,
    val requiresAuth: Boolean = true
) {
    // ==================== 认证相关 ====================
    object Splash : AppDestinations(
        route = "splash",
        title = "启动页",
        showInDrawer = false,
        showInBottomNav = false,
        requiresAuth = false
    )
    
    object Login : AppDestinations(
        route = "login",
        title = "登录",
        showInDrawer = false,
        showInBottomNav = false,
        requiresAuth = false
    )
    
    object Register : AppDestinations(
        route = "register",
        title = "注册",
        showInDrawer = false,
        showInBottomNav = false,
        requiresAuth = false
    )
    
    object ForgotPassword : AppDestinations(
        route = "forgot_password",
        title = "忘记密码",
        showInDrawer = false,
        showInBottomNav = false,
        requiresAuth = false
    )
    
    object Welcome : AppDestinations(
        route = "welcome",
        title = "欢迎",
        showInDrawer = false,
        showInBottomNav = false,
        requiresAuth = false
    )
    
    // ==================== 核心功能 ====================
    object Home : AppDestinations(
        route = "home",
        title = "首页",
        showInDrawer = true,
        showInBottomNav = true
    )
    
    object Messages : AppDestinations(
        route = "messages",
        title = "消息",
        showInDrawer = true,
        showInBottomNav = true
    )
    
    object MessageDetail : AppDestinations(
        route = "message/{messageId}",
        title = "消息详情",
        showInDrawer = false,
        showInBottomNav = false
    ) {
        fun createRoute(messageId: String) = "message/$messageId"
    }
    
    object NewMessage : AppDestinations(
        route = "new_message",
        title = "新建消息",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object MessageTemplate : AppDestinations(
        route = "message_template",
        title = "消息模板",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object MessageSchedule : AppDestinations(
        route = "message_schedule",
        title = "定时消息",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 联系人管理 ====================
    object Contacts : AppDestinations(
        route = "contacts",
        title = "联系人",
        showInDrawer = true,
        showInBottomNav = true
    )
    
    object ContactDetail : AppDestinations(
        route = "contact/{contactId}",
        title = "联系人详情",
        showInDrawer = false,
        showInBottomNav = false
    ) {
        fun createRoute(contactId: String) = "contact/$contactId"
    }
    
    object NewContact : AppDestinations(
        route = "new_contact",
        title = "新建联系人",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object ContactGroups : AppDestinations(
        route = "contact_groups",
        title = "联系人群组",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object ContactImport : AppDestinations(
        route = "contact_import",
        title = "导入联系人",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object ContactExport : AppDestinations(
        route = "contact_export",
        title = "导出联系人",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 语音控制 ====================
    object VoiceControl : AppDestinations(
        route = "voice_control",
        title = "语音控制",
        showInDrawer = true,
        showInBottomNav = true
    )
    
    object VoiceCommands : AppDestinations(
        route = "voice_commands",
        title = "语音命令",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object VoiceTraining : AppDestinations(
        route = "voice_training",
        title = "语音训练",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object VoiceSettings : AppDestinations(
        route = "voice_settings",
        title = "语音设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 紧急报警 ====================
    object Emergency : AppDestinations(
        route = "emergency",
        title = "紧急报警",
        showInDrawer = true,
        showInBottomNav = true
    )
    
    object EmergencySettings : AppDestinations(
        route = "emergency_settings",
        title = "紧急设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object EmergencyContacts : AppDestinations(
        route = "emergency_contacts",
        title = "紧急联系人",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object EmergencyHistory : AppDestinations(
        route = "emergency_history",
        title = "报警历史",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object EmergencyDrill : AppDestinations(
        route = "emergency_drill",
        title = "紧急演练",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== Excel导入 ====================
    object ExcelImport : AppDestinations(
        route = "excel_import",
        title = "Excel导入",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object ExcelTemplate : AppDestinations(
        route = "excel_template",
        title = "Excel模板",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object ImportHistory : AppDestinations(
        route = "import_history",
        title = "导入历史",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object ImportSettings : AppDestinations(
        route = "import_settings",
        title = "导入设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 文件管理 ====================
    object FileManager : AppDestinations(
        route = "file_manager",
        title = "文件管理",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object FileBrowser : AppDestinations(
        route = "file_browser",
        title = "文件浏览",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object FileUpload : AppDestinations(
        route = "file_upload",
        title = "文件上传",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object FileDownload : AppDestinations(
        route = "file_download",
        title = "文件下载",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object FileStorage : AppDestinations(
        route = "file_storage",
        title = "存储管理",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 团队协作 ====================
    object Team : AppDestinations(
        route = "team",
        title = "团队协作",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object TeamMembers : AppDestinations(
        route = "team_members",
        title = "团队成员",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object TeamProjects : AppDestinations(
        route = "team_projects",
        title = "团队项目",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object TeamChat : AppDestinations(
        route = "team_chat",
        title = "团队聊天",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object TeamSettings : AppDestinations(
        route = "team_settings",
        title = "团队设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 工作流 ====================
    object Workflow : AppDestinations(
        route = "workflow",
        title = "工作流",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object WorkflowDesigner : AppDestinations(
        route = "workflow_designer",
        title = "工作流设计",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object WorkflowTemplates : AppDestinations(
        route = "workflow_templates",
        title = "工作流模板",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object WorkflowHistory : AppDestinations(
        route = "workflow_history",
        title = "工作流历史",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object WorkflowSettings : AppDestinations(
        route = "workflow_settings",
        title = "工作流设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 数据分析 ====================
    object Analytics : AppDestinations(
        route = "analytics",
        title = "数据分析",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object Dashboard : AppDestinations(
        route = "dashboard",
        title = "仪表板",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object Reports : AppDestinations(
        route = "reports",
        title = "报告",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object Statistics : AppDestinations(
        route = "statistics",
        title = "统计",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object Insights : AppDestinations(
        route = "insights",
        title = "洞察",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 命令面板 ====================
    object Command : AppDestinations(
        route = "command",
        title = "命令面板",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object CommandHistory : AppDestinations(
        route = "command_history",
        title = "命令历史",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object CommandFavorites : AppDestinations(
        route = "command_favorites",
        title = "收藏命令",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object CommandSettings : AppDestinations(
        route = "command_settings",
        title = "命令设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 设置 ====================
    object Settings : AppDestinations(
        route = "settings",
        title = "设置",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object Profile : AppDestinations(
        route = "profile",
        title = "个人资料",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object AccountSettings : AppDestinations(
        route = "account_settings",
        title = "账户设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object AppearanceSettings : AppDestinations(
        route = "appearance_settings",
        title = "外观设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object NotificationSettings : AppDestinations(
        route = "notification_settings",
        title = "通知设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object PrivacySettings : AppDestinations(
        route = "privacy_settings",
        title = "隐私设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object SecuritySettings : AppDestinations(
        route = "security_settings",
        title = "安全设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object DataSettings : AppDestinations(
        route = "data_settings",
        title = "数据设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 隐私中心 ====================
    object PrivacyCenter : AppDestinations(
        route = "privacy_center",
        title = "隐私中心",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object PrivacyDashboard : AppDestinations(
        route = "privacy_dashboard",
        title = "隐私仪表板",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object DataPermissions : AppDestinations(
        route = "data_permissions",
        title = "数据权限",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object PrivacyAudit : AppDestinations(
        route = "privacy_audit",
        title = "隐私审计",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object DataExport : AppDestinations(
        route = "data_export",
        title = "数据导出",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 帮助与支持 ====================
    object Help : AppDestinations(
        route = "help",
        title = "帮助与反馈",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object HelpCenter : AppDestinations(
        route = "help_center",
        title = "帮助中心",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object FAQ : AppDestinations(
        route = "faq",
        title = "常见问题",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object Feedback : AppDestinations(
        route = "feedback",
        title = "意见反馈",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object ContactSupport : AppDestinations(
        route = "contact_support",
        title = "联系客服",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 关于 ====================
    object About : AppDestinations(
        route = "about",
        title = "关于我们",
        showInDrawer = true,
        showInBottomNav = false
    )
    
    object AboutApp : AppDestinations(
        route = "about_app",
        title = "关于应用",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object VersionInfo : AppDestinations(
        route = "version_info",
        title = "版本信息",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object Changelog : AppDestinations(
        route = "changelog",
        title = "更新日志",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object License : AppDestinations(
        route = "license",
        title = "许可证",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object OpenSource : AppDestinations(
        route = "open_source",
        title = "开源组件",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 其他功能 ====================
    object Search : AppDestinations(
        route = "search",
        title = "搜索",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object QuickActions : AppDestinations(
        route = "quick_actions",
        title = "快速操作",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object NotificationCenter : AppDestinations(
        route = "notification_center",
        title = "通知中心",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object BridgeSettings : AppDestinations(
        route = "bridge_settings",
        title = "桥接设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object PluginMarket : AppDestinations(
        route = "plugin_market",
        title = "插件市场",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object PluginManager : AppDestinations(
        route = "plugin_manager",
        title = "插件管理",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 渠道模块 ====================
    object ChannelSettings : AppDestinations(
        route = "channel_settings",
        title = "渠道设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object WeChatSettings : AppDestinations(
        route = "wechat_settings",
        title = "微信设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object WhatsAppSettings : AppDestinations(
        route = "whatsapp_settings",
        title = "WhatsApp设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object TelegramSettings : AppDestinations(
        route = "telegram_settings",
        title = "Telegram设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object EmailSettings : AppDestinations(
        route = "email_settings",
        title = "邮箱设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object SMSSettings : AppDestinations(
        route = "sms_settings",
        title = "短信设置",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    // ==================== 调试和开发 ====================
    object Debug : AppDestinations(
        route = "debug",
        title = "调试界面",
        showInDrawer = false,
        showInBottomNav = false,
        requiresAuth = false
    )
    
    object PerformanceMonitor : AppDestinations(
        route = "performance_monitor",
        title = "性能监控",
        showInDrawer = false,
        showInBottomNav = false
    )
    
    object LogViewer : AppDestinations(
        route = "log_viewer",
        title = "日志查看器",
        showInDrawer = false,
        showInBottomNav = false
    )
}

/**
 * 导航目的地组
 */
object DestinationGroups {
    // 主抽屉导航组
    val drawerDestinations = listOf(
        AppDestinations.Home,
        AppDestinations.Messages,
        AppDestinations.Contacts,
        AppDestinations.VoiceControl,
        AppDestinations.Emergency,
        AppDestinations.ExcelImport,
        AppDestinations.FileManager,
        AppDestinations.Team,
        AppDestinations.Workflow,
        AppDestinations.Analytics,
        AppDestinations.Settings,
        AppDestinations.PrivacyCenter,
        AppDestinations.Help,
        AppDestinations.About
    )
    
    // 底部导航目的地
    val bottomNavDestinations = listOf(
        AppDestinations.Home,
        AppDestinations.Messages,
        AppDestinations.Contacts,
        AppDestinations.VoiceControl,
        AppDestinations.Emergency
    )
    
    // 需要认证的目的地
    val authRequiredDestinations = AppDestinations::class.sealedSubclasses
        .filter { it.objectInstance?.requiresAuth == true }
        .mapNotNull { it.objectInstance }
    
    // 无需认证的目的地
    val noAuthRequiredDestinations = AppDestinations::class.sealedSubclasses
        .filter { it.objectInstance?.requiresAuth == false }
        .mapNotNull { it.objectInstance }
    
    // 功能模块组
    val contactModuleDestinations = listOf(
        AppDestinations.Contacts,
        AppDestinations.ContactDetail,
        AppDestinations.NewContact,
        AppDestinations.ContactGroups,
        AppDestinations.ContactImport,
        AppDestinations.ContactExport
    )
    
    val voiceModuleDestinations = listOf(
        AppDestinations.VoiceControl,
        AppDestinations.VoiceCommands,
        AppDestinations.VoiceTraining,
        AppDestinations.VoiceSettings
    )
    
    val emergencyModuleDestinations = listOf(
        AppDestinations.Emergency,
        AppDestinations.EmergencySettings,
        AppDestinations.EmergencyContacts,
        AppDestinations.EmergencyHistory,
        AppDestinations.EmergencyDrill
    )
    
    val excelImportModuleDestinations = listOf(
        AppDestinations.ExcelImport,
        AppDestinations.ExcelTemplate,
        AppDestinations.ImportHistory,
        AppDestinations.ImportSettings
    )
}

/**
 * 路由参数常量
 */
object RouteParams {
    const val MESSAGE_ID = "messageId"
    const val CONTACT_ID = "contactId"
    const val CONVERSATION_ID = "conversationId"
    const val GROUP_ID = "groupId"
    const val FILE_ID = "fileId"
    const val WORKFLOW_ID = "workflowId"
    const val IMPORT_BATCH_ID = "importBatchId"
    const val EMERGENCY_SESSION_ID = "emergencySessionId"
    
    // 参数模式
    const val MESSAGE_DETAIL_PATTERN = "message/{$MESSAGE_ID}"
    const val CONTACT_DETAIL_PATTERN = "contact/{$CONTACT_ID}"
    const val CONVERSATION_DETAIL_PATTERN = "conversation/{$CONVERSATION_ID}"
}

/**
 * 路由构建器
 */
object RouteBuilder {
    fun buildMessageDetailRoute(messageId: String): String {
        return "message/$messageId"
    }
    
    fun buildContactDetailRoute(contactId: String): String {
        return "contact/$contactId"
    }
    
    fun buildConversationDetailRoute(conversationId: String): String {
        return "conversation/$conversationId"
    }
    
    fun buildGroupDetailRoute(groupId: String): String {
        return "group/$groupId"
    }
    
    fun buildFileDetailRoute(fileId: String): String {
        return "file/$fileId"
    }
    
    fun buildWorkflowDetailRoute(workflowId: String): String {
        return "workflow/$workflowId"
    }
}