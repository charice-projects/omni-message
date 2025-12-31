# OmniMessage Pro - 下一代全渠道智能消息平台

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0.0--alpha-blue?style=for-the-badge)

---

## 📖 文档导航

- [第一部分：新手入门指南](#第一部分新手入门指南) - 普通用户从这里开始
- [第二部分：技术开发指南](#第二部分技术开发指南) - 开发人员从这里开始

---

# 第一部分：新手入门指南

## 🎯 这是什么应用？

**OmniMessage Pro** 是一个智能消息管家，帮你统一管理所有消息渠道（微信、QQ、短信、邮件等），就像给你的手机装了一个聪明的私人秘书！

### 🤔 为什么需要这个应用？

| 日常烦恼 | OmniMessage 解决方案 |
|---------|---------------------|
| 消息太多看不过来？ | 所有消息一个收件箱查看 |
| 重要消息总被淹没？ | AI智能识别，优先显示重要消息 |
| 联系人太多难管理？ | 自动分组，智能搜索 |
| 紧急情况要求救？ | 一键隐蔽报警，自动发定位 |
| 开车时不能看手机？ | 语音控制，说"熙熙"唤醒 |

## 🚀 5分钟快速上手

### 第1步：下载安装
1. **方法一（推荐新手）**：从 [Releases](https://github.com/sx-charice/omni-message/releases) 下载安装包
2. **方法二（喜欢折腾）**：自己编译（看第二部分）

### 第2步：初次设置
1. 打开应用，允许必要权限
2. 设置你的基本信息
3. 选择喜欢的主题颜色

### 第3步：基础功能体验

#### 📱 统一消息收件箱
- **操作**：点击"消息"标签
- **效果**：看到所有平台的消息汇总
- **技巧**：长按消息可以分类标记

#### 👥 联系人管理
- **操作**：点击"联系人" → "导入"
- **效果**：从Excel导入联系人，自动整理
- **技巧**：Excel支持手机直接选择

#### 🎤 语音唤醒
- **操作**：大声说"熙熙，你好！"
- **效果**：听到回应后可以语音操作
- **技巧**：安静环境下效果更好

#### 🚨 紧急报警设置
- **操作**：设置 → 安全中心 → 紧急报警
- **效果**：设置触发方式和联系人
- **技巧**：先测试，再启用

## 📱 核心功能详解

### 1. 智能消息管理
- **统一查看**：微信、QQ、短信、邮件都在一个页面
- **智能分类**：自动分成工作、生活、紧急等类别
- **快捷回复**：预置常用回复，一键发送
- **定时发送**：设定时间自动发送消息

### 2. 智能联系人
- **Excel一键导入**：从Excel表格导入联系人
- **自动去重**：重复的联系人自动合并
- **智能分组**：按公司、行业、地区自动分组
- **生日提醒**：支持农历/阳历生日提醒

### 3. 语音控制中心
- **语音唤醒**：说"熙熙"激活语音助手
- **语音发消息**："给张三发微信说晚上开会"
- **语音查询**："李四上次什么时候联系的"
- **语音设置**："打开免打扰模式"

### 4. 安全防护功能
- **一键报警**：连按3次电源键求救
- **伪装界面**：紧急时界面变成计算器
- **位置共享**：临时分享实时位置
- **安全日志**：记录所有敏感操作

### 5. 智能场景模式
- **会议模式**：自动静音，只接重要电话
- **驾驶模式**：语音播报消息，语音回复
- **睡眠模式**：夜间免打扰
- **旅行模式**：自动记录行程

## 🔧 常见问题

### 安装问题
**Q：安装失败怎么办？**
A：检查手机存储空间，关闭杀毒软件，重新下载安装包。

**Q：需要哪些权限？**
A：需要通知权限（收消息）、通讯录权限（管理联系人）、麦克风权限（语音控制）。

### 使用问题
**Q：收不到消息通知？**
A：检查手机设置 → 应用 → OmniMessage → 允许通知。

**Q：语音唤醒不灵？**
A：在安静环境下重新训练唤醒词："设置 → 语音控制 → 训练唤醒词"。

**Q：Excel导入失败？**
A：确保Excel格式正确，不要有合并单元格，文件不要超过10MB。

### 安全疑问
**Q：我的数据安全吗？**
A：所有数据本地加密存储，不上传云端，除非你主动分享。

**Q：紧急报警会误触发吗？**
A：可以设置二次确认，避免误操作。

---

# 第二部分：技术开发指南

## 🏗️ 技术架构总览

### 架构设计理念
```
┌─────────────────────────────────────────────────────────┐
│                   表现层 (UI)                           │
│   Jetpack Compose + MVI + ViewModel                     │
├─────────────────────────────────────────────────────────┤
│                   领域层 (Domain)                       │
│   用例 + 业务规则 + 领域模型                            │
├─────────────────────────────────────────────────────────┤
│                   数据层 (Data)                         │
│   仓库 + 数据源 (本地/网络) + 数据映射                  │
└─────────────────────────────────────────────────────────┘
```

### 技术栈详情
- **开发语言**: Kotlin 1.9.0 + Java 17
- **UI框架**: Jetpack Compose + Material 3
- **架构模式**: Clean Architecture + MVI
- **异步处理**: Kotlin Coroutines + Flow
- **依赖注入**: Hilt
- **数据库**: Room + SQLCipher
- **网络**: Retrofit + OkHttp
- **AI引擎**: TensorFlow Lite + ONNX Runtime
- **安全**: Tink + Android KeyStore

## 📁 项目模块详解

### 核心模块结构
```
OmniMessage/
├── 📁 app/                          # 主应用模块 (Clean Architecture实现)
├── 📁 core/                         # 核心微内核
├── 📁 feature/                      # 功能模块
├── 📁 extension/                    # 扩展模块
├── 📁 shared/                       # 共享资源
└── 📁 test/                         # 测试模块
```

### 各模块功能详细说明

#### 🎯 **app/ 主应用模块**
- **功能**: 应用入口，协调各模块
- **核心文件**:
  - `MainActivity.kt`: 应用主入口
  - `di/`: 依赖注入配置
  - `ui/`: 主要界面组件
  - `domain/`: 领域逻辑
  - `data/`: 数据层实现
- **技术特点**: 完整Clean Architecture实现

#### 🔧 **core/ 核心微内核模块**
- **功能**: 系统核心，插件管理，服务注册
- **核心组件**:
  - `CoreKernel.kt`: 微内核主类
  - `ServiceRegistry.kt`: 服务注册表
  - `EventBus.kt`: 事件总线系统
  - `PluginManager.kt`: 插件管理器
  - `SecuritySandbox.kt`: 安全沙箱
- **技术特点**: 支持动态插件加载，服务发现

#### 🧠 **feature/ 功能模块集合**

**1. emotion/ 情感智能模块**
- **功能**: 消息情感分析，情绪识别
- **核心文件**:
  - `EmotionAnalyzer.kt`: 情感分析引擎
  - `SentimentDetector.kt`: 情感检测器
  - `MoodTracker.kt`: 情绪跟踪
- **AI模型**: sentiment_analyzer.tflite

**2. scene/ 场景感知模块**
- **功能**: 智能场景识别，环境感知
- **核心文件**:
  - `SmartSceneManager.kt`: 场景管理器
  - `ActivityRecognizer.kt`: 活动识别
  - `LocationIntelligence.kt`: 位置智能
  - `EnvironmentSensor.kt`: 环境传感器
- **使用场景**: 自动切换会议/驾驶/睡眠模式

**3. voice/ 语音交互模块**
- **功能**: 语音唤醒，语音识别，声纹验证
- **核心文件**:
  - `XiXiWakeWordDetector.kt`: 唤醒词检测器（"熙熙"）
  - `VoiceRecognition.kt`: 语音识别
  - `VoicePrintManager.kt`: 声纹管理
  - `PrivacyAwareVoiceProcessor.kt`: 隐私语音处理
- **AI模型**: wakeword_xixi.tflite

**4. quickactions/ 快速操作模块**
- **功能**: 紧急报警，快捷操作，手势识别
- **核心文件**:
  - `EmergencySystem.kt`: 紧急报警系统
  - `GestureRecognizer.kt`: 手势识别
  - `SmartSilentMode.kt`: 智能静音
  - `QuickActionManager.kt`: 快捷操作管理
- **触发方式**: 电源键三击，音量组合，特定手势

**5. excelimport/ Excel导入模块**
- **功能**: Excel智能解析，字段识别，数据清洗
- **核心文件**:
  - `ExcelIntelligentImporter.kt`: 智能导入器
  - `AIFieldRecognizer.kt`: AI字段识别
  - `ContactDuplicateResolver.kt`: 联系人去重
  - `ImportProcessor.kt`: 导入处理器
- **支持格式**: .xlsx, .xls, .csv

**6. command/ 命令模块**
- **功能**: AI命令识别，自然语言处理
- **核心文件**:
  - `AICommandPalette.kt`: AI命令面板
  - `NaturalLanguageParser.kt`: 自然语言解析
  - `CommandRegistry.kt`: 命令注册表
  - `IntentAnalyzer.kt`: 意图分析
- **AI模型**: intent_classifier.tflite

**7. messaging/ 消息核心模块**
- **功能**: 消息发送接收，协议转换，队列管理
- **核心文件**:
  - `MessageComposer.kt`: 消息编辑器
  - `ChannelManager.kt`: 通道管理
  - `BatchProcessor.kt`: 批处理
  - `TemplateEngine.kt`: 模板引擎
- **支持协议**: 微信，QQ，短信，邮件等

**8. notification/ 通知模块**
- **功能**: 智能通知管理，分组，优先级
- **核心文件**:
  - `IntelligentNotificationManager.kt`: 智能通知管理
  - `SmartNotificationGrouper.kt`: 通知分组器
  - `QuietHoursManager.kt`: 静默时段管理
  - `SmartReplyGenerator.kt`: 智能回复生成
- **特点**: 基于场景的通知优先级

**9. contact/ 联系人模块**
- **功能**: 联系人管理，分组，去重，分析
- **核心文件**:
  - `SmartContact.kt`: 智能联系人
  - `GroupManager.kt`: 群组管理
  - `DuplicateMerger.kt`: 重复合并
  - `ContactAnalytics.kt`: 联系人分析
- **数据源**: 本地通讯录，Excel导入，网络同步

**10. team/ 团队模块**
- **功能**: 团队协作，权限管理，任务分配
- **核心文件**:
  - `TeamManager.kt`: 团队管理
  - `CollaborationSpace.kt`: 协作空间
  - `PermissionSystem.kt`: 权限系统
  - `TaskManager.kt`: 任务管理
- **应用场景**: 企业团队内部通讯

**11. transfer/ 传输模块**
- **功能**: 文件传输，近距离分享，蓝牙传输
- **核心文件**:
  - `NearbyTransfer.kt`: 近距离传输
  - `WiFiDirectManager.kt`: WiFi直连管理
  - `BluetoothSharing.kt`: 蓝牙分享
  - `QRShareManager.kt`: 二维码分享
- **传输协议**: WiFi Direct, Bluetooth, NFC

**12. workflow/ 工作流模块**
- **功能**: 自动化工作流，规则引擎，触发器
- **核心文件**:
  - `WorkflowEngine.kt`: 工作流引擎
  - `RuleEngine.kt`: 规则引擎
  - `TriggerSystem.kt`: 触发器系统
  - `ConditionEvaluator.kt`: 条件评估器
- **应用场景**: 自动回复，定时任务，条件触发

**13. analytics/ 分析模块**
- **功能**: 数据分析，统计报表，预测模型
- **核心文件**:
  - `Dashboard.kt`: 数据仪表板
  - `PredictiveModel.kt`: 预测模型
  - `CostAnalyzer.kt`: 成本分析
  - `EffectivenessTracker.kt`: 效果跟踪
- **AI模型**: anomaly_detector.tflite

**14. settings/ 设置模块**
- **功能**: 应用设置，主题管理，备份恢复
- **核心文件**:
  - `PreferenceManager.kt`: 偏好设置管理
  - `ThemeManager.kt`: 主题管理
  - `BackupManager.kt`: 备份管理
  - `PrivacyCenter.kt`: 隐私中心
- **数据存储**: DataStore, SharedPreferences

#### 🔌 **extension/ 扩展模块**
**1. channels/ 通道扩展**
- **功能**: 各种消息通道实现
- **包含通道**:
  - 微信，QQ，钉钉，Telegram
  - 邮件，短信，电话
  - Matrix，Nostr（去中心化）
  - 自定义通道支持

**2. plugins/ 插件系统**
- **功能**: 动态插件加载，插件市场
- **核心文件**:
  - `PluginRuntime.kt`: 插件运行时
  - `PluginLoader.kt`: 插件加载器
  - `PluginMarket.kt`: 插件市场
  - `PluginSecurity.kt`: 插件安全

**3. integrations/ 集成扩展**
- **功能**: 第三方系统集成
- **包含集成**:
  - 日历同步
  - CRM系统集成
  - 云存储对接
  - 智能家居控制

#### 🤝 **shared/ 共享模块**
- **功能**: 公共组件，工具类，共享数据
- **包含内容**:
  - UI组件库
  - 工具函数
  - 数据实体
  - 依赖注入配置

## 🛠️ 开发环境搭建

### 系统要求
- **操作系统**: Windows 10+/macOS 11+/Linux Ubuntu 20.04+
- **内存**: 8GB RAM（推荐16GB）
- **存储**: 至少10GB可用空间

### 环境配置步骤

#### 1. 安装基础软件
```bash
# 安装JDK 17
# Windows: 从Oracle官网下载JDK 17安装包
# macOS: brew install openjdk@17
# Linux: sudo apt install openjdk-17-jdk

# 验证安装
java -version  # 应该显示17.x.x
```

#### 2. 安装Android Studio
1. 下载 [Android Studio Electric Eel](https://developer.android.com/studio) 或更高版本
2. 安装时选择"Standard"安装方式
3. 安装完成后，打开SDK Manager安装以下组件：
   - Android SDK 34
   - Android SDK Build-Tools 34
   - Android Emulator
   - Android SDK Platform-Tools

#### 3. 克隆项目
```bash
git clone https://github.com/sx-charice/omni-message.git
cd omni-message
```

#### 4. 配置项目
```bash
# 复制配置文件
cp environment_config.json.example environment_config.json

# 编辑配置文件（根据需求修改）
# 主要配置项：
# - API端点地址
# - 加密密钥（开发环境可以用默认值）
# - 功能开关
```

#### 5. 导入项目到Android Studio
1. 打开Android Studio
2. 选择"Open" → 选择omni-message文件夹
3. 等待Gradle同步完成（首次需要下载依赖）

#### 6. 构建项目
```bash
# 使用Gradle Wrapper构建
./gradlew build

# 运行测试
./gradlew test

# 构建调试版本
./gradlew assembleDebug
```

## 🧪 项目构建配置

### 构建变体说明
项目支持多种构建变体，便于不同环境使用：

| 变体 | 用途 | 特点 |
|------|------|------|
| `developmentDebug` | 开发调试 | 启用调试功能，包含分析工具 |
| `stagingDebug` | 测试环境 | 连接测试服务器，保留部分日志 |
| `productionRelease` | 正式发布 | 代码优化，移除调试信息 |

### 构建命令参考
```bash
# 清理构建
./gradlew clean

# 构建所有变体
./gradlew assemble

# 构建特定变体
./gradlew assembleDevelopmentDebug
./gradlew assembleProductionRelease

# 安装到连接的设备
./gradlew installDevelopmentDebug

# 运行测试
./gradlew testDevelopmentDebugUnitTest
```

### 代码质量检查
```bash
# 运行代码检查
./gradlew lint

# 运行Detekt静态分析
./gradlew detekt

# 运行KtLint格式化检查
./gradlew ktlintCheck

# 自动格式化代码
./gradlew ktlintFormat
```

## 🔍 代码结构解析

### Clean Architecture实现

#### 1. 数据层 (Data Layer)
```kotlin
// 数据源接口
interface ContactDataSource {
    suspend fun getContacts(): List<Contact>
    suspend fun saveContact(contact: Contact)
}

// 本地数据源实现
class LocalContactDataSource(
    private val contactDao: ContactDao
) : ContactDataSource {
    override suspend fun getContacts(): List<Contact> {
        return contactDao.getAll().map { it.toDomain() }
    }
}

// 远程数据源实现
class RemoteContactDataSource(
    private val api: ContactApi
) : ContactDataSource {
    override suspend fun getContacts(): List<Contact> {
        return api.getContacts().map { it.toDomain() }
    }
}
```

#### 2. 领域层 (Domain Layer)
```kotlin
// 领域模型
data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val tags: List<String>
)

// 用例
class AddContactUseCase(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(contact: Contact): Result<Contact> {
        return contactRepository.addContact(contact)
    }
}

// 仓库接口
interface ContactRepository {
    suspend fun getContacts(): List<Contact>
    suspend fun addContact(contact: Contact): Result<Contact>
    suspend fun updateContact(contact: Contact): Result<Contact>
}
```

#### 3. 表现层 (Presentation Layer)
```kotlin
// ViewModel
@HiltViewModel
class ContactViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactUseCase: AddContactUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(ContactState())
    val state: StateFlow<ContactState> = _state
    
    fun loadContacts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = getContactsUseCase()
            _state.update { 
                it.copy(
                    isLoading = false,
                    contacts = result.getOrElse { emptyList() },
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}

// UI状态
data class ContactState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## 🚀 开发工作流

### 1. 创建新功能模块
```bash
# 1. 创建模块目录
mkdir -p feature/yourfeature/src/main/java/com/omnimsg/feature/yourfeature

# 2. 复制build.gradle.kts模板
cp feature/template/build.gradle.kts feature/yourfeature/

# 3. 修改模板中的模块名
# 4. 在settings.gradle.kts中添加模块
include(":feature:yourfeature")
```

### 2. 实现模块代码结构
```
yourfeature/
├── src/main/java/com/omnimsg/feature/yourfeature/
│   ├── data/
│   │   ├── local/      # 本地数据源
│   │   ├── remote/     # 远程数据源
│   │   └── repository/ # 仓库实现
│   ├── domain/
│   │   ├── model/      # 领域模型
│   │   ├── repository/ # 仓库接口
│   │   └── usecase/    # 用例
│   ├── presentation/
│   │   ├── viewmodel/  # ViewModel
│   │   ├── state/      # UI状态
│   │   └── event/      # UI事件
│   └── ui/
│       ├── screen/     # 屏幕组件
│       ├── component/  # 可复用组件
│       └── theme/      # 主题配置
└── build.gradle.kts
```

### 3. 注册模块到主应用
```kotlin
// 在app模块的build.gradle.kts中添加依赖
dependencies {
    implementation(project(":feature:yourfeature"))
}

// 在DI模块中注册依赖
@Module
@InstallIn(SingletonComponent::class)
object YourFeatureModule {
    
    @Provides
    @Singleton
    fun provideYourFeatureRepository(): YourFeatureRepository {
        return YourFeatureRepositoryImpl()
    }
}
```

### 4. 添加导航路由
```kotlin
// 在导航图中添加路由
sealed class Screen(val route: String) {
    object YourFeatureScreen : Screen("yourfeature")
}

// 添加导航配置
NavHost(navController, startDestination = Screen.HomeScreen.route) {
    composable(Screen.YourFeatureScreen.route) {
        YourFeatureScreen()
    }
}
```

## 🧪 测试策略

### 单元测试
```kotlin
class AddContactUseCaseTest {
    
    @Test
    fun `添加联系人成功`() = runTest {
        // 准备
        val mockRepository = mockk<ContactRepository>()
        val useCase = AddContactUseCase(mockRepository)
        val contact = Contact("1", "张三", "13800138000")
        
        // 模拟
        every { mockRepository.addContact(contact) } returns Result.success(contact)
        
        // 执行
        val result = useCase(contact)
        
        // 验证
        assertTrue(result.isSuccess)
        assertEquals("张三", result.getOrNull()?.name)
    }
}
```

### UI测试
```kotlin
class ContactScreenTest {
    
    @Test
    fun 显示联系人列表() {
        // 启动Compose测试
        composeTestRule.setContent {
            ContactScreen(viewModel = fakeViewModel)
        }
        
        // 验证UI元素
        composeTestRule.onNodeWithText("联系人列表").assertIsDisplayed()
        composeTestRule.onNodeWithText("张三").assertIsDisplayed()
    }
}
```

### 集成测试
```kotlin
class ContactFeatureTest {
    
    @Test
    fun 完整联系人流程() = runTest {
        // 测试添加、编辑、删除整个流程
        val repository = ContactRepositoryImpl()
        val addUseCase = AddContactUseCase(repository)
        val getUseCase = GetContactsUseCase(repository)
        
        // 添加联系人
        val contact = Contact("1", "测试", "123456789")
        addUseCase(contact)
        
        // 验证添加
        val contacts = getUseCase()
        assertTrue(contacts.any { it.name == "测试" })
    }
}
```

## 🔗 外部依赖管理

### 主要依赖库
```kotlin
// 网络请求
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// 数据库
implementation("androidx.room:room-runtime:2.6.0")
ksp("androidx.room:room-compiler:2.6.0")

// 依赖注入
implementation("com.google.dagger:hilt-android:2.48")
ksp("com.google.dagger:hilt-compiler:2.48")

// AI/ML
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.15.0")

// 安全加密
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("com.google.crypto.tink:tink-android:1.10.0")
```

### 依赖更新命令
```bash
# 查看依赖更新
./gradlew dependencyUpdates

# 更新所有依赖
./gradlew useLatestVersions

# 检查依赖冲突
./gradlew app:dependencies
```

## 📊 性能优化指南

### 1. 启动优化
```kotlin
// 延迟初始化非关键组件
@HiltAndroidApp
class OmniMessageApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 关键组件立即初始化
        initCoreComponents()
        
        // 非关键组件延迟初始化
        AppStartupManager.getInstance()
            .addInitializer(PluginManagerInitializer::class.java)
            .addInitializer(AIModelInitializer::class.java)
            .start()
    }
}
```

### 2. 内存优化
```kotlin
// 使用弱引用缓存
class ImageCache {
    private val memoryCache = LruCache<String, SoftReference<Bitmap>>(maxSize)
    
    fun getBitmap(key: String): Bitmap? {
        return memoryCache.get(key)?.get()
    }
}

// 及时释放资源
@Composable
fun HeavyComponent() {
    val bitmap by rememberImagePainter(url).state
    DisposableEffect(Unit) {
        onDispose {
            bitmap?.recycle()
        }
    }
}
```

### 3. 网络优化
```kotlin
// 智能缓存策略
@Provides
@Singleton
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .cache(Cache(cacheDir, 10 * 1024 * 1024)) // 10MB缓存
        .addInterceptor(CacheInterceptor())
        .addNetworkInterceptor(OnlineCacheInterceptor())
        .build()
}

// 批量请求
class BatchRequestProcessor {
    suspend fun processBatch(requests: List<Request>): List<Response> {
        return withContext(Dispatchers.IO) {
            requests.chunked(10) // 每10个一批
                .flatMap { batch -> api.batchRequest(batch) }
        }
    }
}
```

## 🚨 故障排除

### 构建问题
| 问题 | 解决方案 |
|------|----------|
| Gradle同步失败 | 删除.gradle缓存，重新同步 |
| 依赖下载超时 | 使用国内镜像源，修改build.gradle |
| 编译内存不足 | 增加Gradle堆内存：`org.gradle.jvmargs=-Xmx4096m` |
| KSP处理失败 | 清理ksp缓存：`./gradlew cleanKspDebugKotlin` |

### 运行时问题
| 问题 | 解决方案 |
|------|----------|
| 数据库迁移失败 | 检查migration文件，使用Room的fallback策略 |
| 网络请求失败 | 检查SSL证书，添加网络权限 |
| 内存泄漏 | 使用LeakCanary检测，检查生命周期 |
| ANR问题 | 减少主线程工作，使用协程 |

### 测试问题
| 问题 | 解决方案 |
|------|----------|
| 测试运行失败 | 检查测试设备API版本 |
| Mock失败 | 确保正确配置Mockk或Mockito |
| UI测试不稳定 | 增加等待时间，使用idling resource |
| 覆盖率不准确 | 排除自动生成的代码 |

## 📈 监控与日志

### 日志配置
```kotlin
class AppLogger {
    companion object {
        private const val TAG = "OmniMessage"
        
        fun debug(message: String) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, message)
            }
        }
        
        fun error(throwable: Throwable, message: String = "") {
            Log.e(TAG, message, throwable)
            // 上报到监控平台
            Crashlytics.logException(throwable)
        }
    }
}
```

### 性能监控
```kotlin
// 使用Jetpack Macrobenchmark
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.omnimsg",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10
    ) {
        pressHome()
        startActivityAndWait()
    }
}
```

## 🤝 贡献指南

### 贡献流程
1. **Fork仓库**
2. **创建功能分支**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **提交更改**
   ```bash
   git commit -m "feat: 添加了XX功能"
   ```
4. **推送到分支**
   ```bash
   git push origin feature/amazing-feature
   ```
5. **创建Pull Request**

### 代码规范
- **Kotlin风格**: 遵循官方编码规范
- **命名约定**: 使用清晰的命名，避免缩写
- **注释要求**: 公开API必须添加KDoc注释
- **测试覆盖**: 新功能必须包含单元测试

### 提交信息格式
```
类型(范围): 描述

[可选正文]

[可选脚注]
```

**类型**:
- `feat`: 新功能
- `fix`: bug修复
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建或工具更新

## 🔮 未来发展规划

### 近期目标 (1-3个月)
- [ ] 完善核心消息功能
- [ ] 优化语音识别准确率
- [ ] 增加更多消息平台支持
- [ ] 提升应用性能

### 中期目标 (3-12个月)
- [ ] 插件市场上线
- [ ] 跨平台版本开发
- [ ] 企业级功能增强
- [ ] AI能力升级

### 长期愿景 (1-3年)
- [ ] 成为消息管理标准
- [ ] 构建完整生态系统
- [ ] 国际化多语言支持
- [ ] 创新技术集成

## 📞 技术支持

### 获取帮助
- **GitHub Issues**: 报告bug和问题
- **Discussions**: 技术讨论和想法交流
- **邮件支持**: omni-message-support@example.com

### 社区资源
- [项目Wiki](https://github.com/sx-charice/omni-message/wiki)
- [API文档](docs/api/)
- [开发指南](docs/development/)
- [常见问题](docs/faq/)

### 联系维护者
- **项目负责人**: sx-charice
- **主要维护**: sx-charice
- **技术支持**: 通过GitHub Issues优先

---

<div align="center">

## 🚀 开始你的OmniMessage之旅

**无论你是普通用户还是开发者，现在就开始吧！**

[📱 下载应用](https://github.com/sx-charice/omni-message/releases) ·
[💻 查看代码](https://github.com/sx-charice/omni-message) ·
[📖 阅读文档](https://github.com/sx-charice/omni-message/wiki)

**用技术改变沟通方式，让消息管理更智能**

</div>

---

**最后更新**: 2025年12月31日  
**项目状态**: 🟢 积极开发中  
**兼容性**: Android 7.0+ (API 24+)  
**开发工具**: Android Studio + JDK 17  
**许可证**: MIT License  

> 💡 **给开发者的建议**: 建议从`feature/excelimport`或`feature/voice`模块开始阅读代码，这些模块功能相对独立，易于理解。