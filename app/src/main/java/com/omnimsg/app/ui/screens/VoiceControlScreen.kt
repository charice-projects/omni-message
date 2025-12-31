// 📁 app/src/main/java/com/omnimsg/app/ui/screens/VoiceControlScreen.kt
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnimsg.app.R
import com.omnimsg.app.ui.components.Common.*
import com.omnimsg.app.ui.navigation.AppDestinations
import com.omnimsg.app.ui.viewmodels.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceControlScreen(
    viewModel: VoiceViewModel = hiltViewModel(),
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
    
    // 添加命令对话框状态
    var showAddCommandDialog by rememberSaveable { mutableStateOf(false) }
    var newCommandPhrase by rememberSaveable { mutableStateOf("") }
    var newCommandDescription by rememberSaveable { mutableStateOf("") }
    var newCommandAction by rememberSaveable { mutableStateOf("") }
    var newCommandCategory by rememberSaveable { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音控制") },
                navigationIcon = {
                    IconButton(onClick = { /* 返回 */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 帮助 */ }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "帮助")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCommandDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加语音命令")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 语音控制状态卡片
            VoiceControlStatusCard(
                enabled = state.voiceControlEnabled,
                wakeWord = state.wakeWord,
                wakeWordTrained = state.wakeWordTrained,
                onToggleVoiceControl = { viewModel.toggleVoiceControl(!state.voiceControlEnabled) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 唤醒词设置
            WakeWordSettingsSection(
                wakeWordEnabled = state.wakeWordEnabled,
                wakeWord = state.wakeWord,
                wakeWordTrained = state.wakeWordTrained,
                wakeWordAccuracy = state.wakeWordAccuracy,
                wakeWordLastTrained = state.wakeWordLastTrained,
                personalizedWakeWordEnabled = state.personalizedWakeWordEnabled,
                isTraining = state.isTrainingWakeWord,
                onToggleWakeWord = { viewModel.toggleWakeWord(!state.wakeWordEnabled) },
                onUpdateWakeWord = viewModel::updateWakeWord,
                onTrainWakeWord = viewModel::trainPersonalizedWakeWord
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 语音识别设置
            VoiceRecognitionSection(
                language = state.voiceRecognitionLanguage,
                confidence = state.voiceRecognitionConfidence,
                sensitivity = state.sensitivity,
                lastRecognitionResult = state.lastRecognitionResult,
                recognitionConfidence = state.recognitionConfidence,
                isTesting = state.isTestingRecognition,
                onUpdateLanguage = viewModel::updateLanguage,
                onUpdateConfidence = viewModel::updateConfidenceThreshold,
                onUpdateSensitivity = viewModel::updateSensitivity,
                onTestRecognition = viewModel::testVoiceRecognition
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 语音反馈设置
            VoiceFeedbackSection(
                voiceFeedbackEnabled = state.voiceFeedbackEnabled,
                volume = state.voiceFeedbackVolume,
                speed = state.voiceFeedbackSpeed,
                pitch = state.voiceFeedbackPitch,
                isTesting = state.isTestingSynthesis,
                onToggleFeedback = { viewModel.toggleVoiceFeedback(!state.voiceFeedbackEnabled) },
                onUpdateVolume = { /* TODO: 实现音量更新 */ },
                onUpdateSpeed = { /* TODO: 实现语速更新 */ },
                onUpdatePitch = { /* TODO: 实现音调更新 */ },
                onTestSynthesis = viewModel::testVoiceSynthesis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 声纹识别设置
            VoicePrintSection(
                voicePrintEnabled = state.voicePrintEnabled,
                voicePrintRegistered = state.voicePrintRegistered,
                autoVoicePrintUpdate = state.autoVoicePrintUpdate,
                voicePrintConfidence = state.voicePrintConfidence,
                onToggleVoicePrint = { viewModel.toggleVoicePrint(!state.voicePrintEnabled) },
                onToggleAutoUpdate = { viewModel.toggleAutoVoicePrintUpdate(!state.autoVoicePrintUpdate) },
                onRegisterVoicePrint = { /* TODO: 实现声纹注册 */ },
                onVerifyVoicePrint = { /* TODO: 实现声纹验证 */ }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 语音命令列表
            VoiceCommandsSection(
                commands = state.filteredCommands,
                searchQuery = state.searchQuery,
                onSearch = viewModel::searchVoiceCommands,
                onDeleteCommand = viewModel::deleteVoiceCommand,
                onToggleCommand = { command ->
                    val updated = command.copy(enabled = !command.enabled)
                    viewModel.updateVoiceCommand(updated)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 高级设置
            AdvancedVoiceSettingsSection(
                backgroundListening = state.backgroundListening,
                noiseSuppression = state.noiseSuppression,
                echoCancellation = state.echoCancellation,
                commandTimeout = state.voiceCommandTimeout,
                onToggleBackgroundListening = { /* TODO: 实现后台监听切换 */ },
                onToggleNoiseSuppression = { /* TODO: 实现噪声抑制切换 */ },
                onToggleEchoCancellation = { /* TODO: 实现回声消除切换 */ },
                onUpdateCommandTimeout = { /* TODO: 实现超时设置更新 */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 测试区域
            VoiceTestSection(
                isRecording = state.isRecording,
                recordingProgress = state.recordingProgress,
                onStartRecording = viewModel::startRecording,
                onStopRecording = viewModel::stopRecording
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // 添加语音命令对话框
        if (showAddCommandDialog) {
            AddVoiceCommandDialog(
                phrase = newCommandPhrase,
                description = newCommandDescription,
                action = newCommandAction,
                category = newCommandCategory,
                onPhraseChange = { newCommandPhrase = it },
                onDescriptionChange = { newCommandDescription = it },
                onActionChange = { newCommandAction = it },
                onCategoryChange = { newCommandCategory = it },
                onDismiss = {
                    showAddCommandDialog = false
                    newCommandPhrase = ""
                    newCommandDescription = ""
                    newCommandAction = ""
                    newCommandCategory = ""
                },
                onConfirm = {
                    if (newCommandPhrase.isNotBlank() && newCommandAction.isNotBlank()) {
                        viewModel.addVoiceCommand(
                            VoiceCommand(
                                id = "cmd_${System.currentTimeMillis()}",
                                phrase = newCommandPhrase,
                                description = newCommandDescription,
                                action = newCommandAction,
                                category = newCommandCategory.ifBlank { "CUSTOM" }
                            )
                        )
                        showAddCommandDialog = false
                        newCommandPhrase = ""
                        newCommandDescription = ""
                        newCommandAction = ""
                        newCommandCategory = ""
                    } else {
                        onShowSnackbar("请填写命令短语和动作")
                    }
                }
            )
        }
    }
}

@Composable
private fun VoiceControlStatusCard(
    enabled: Boolean,
    wakeWord: String,
    wakeWordTrained: Boolean,
    onToggleVoiceControl: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "语音控制系统",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = if (enabled) "运行中" else "已禁用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "唤醒词",
                        modifier = Modifier.size(16.dp),
                        tint = if (wakeWordTrained) MaterialTheme.colorScheme.primary 
                              else MaterialTheme.colorScheme.outline
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "唤醒词: $wakeWord",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (wakeWordTrained) {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
            
            // 语音控制开关
            Switch(
                checked = enabled,
                onCheckedChange = { onToggleVoiceControl() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun WakeWordSettingsSection(
    wakeWordEnabled: Boolean,
    wakeWord: String,
    wakeWordTrained: Boolean,
    wakeWordAccuracy: Float,
    wakeWordLastTrained: Long?,
    personalizedWakeWordEnabled: Boolean,
    isTraining: Boolean,
    onToggleWakeWord: () -> Unit,
    onUpdateWakeWord: (String) -> Unit,
    onTrainWakeWord: () -> Unit
) {
    var editingWakeWord by rememberSaveable { mutableStateOf(false) }
    var newWakeWord by rememberSaveable { mutableStateOf(wakeWord) }
    
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
                    text = "唤醒词设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = wakeWordEnabled,
                    onCheckedChange = { onToggleWakeWord() }
                )
            }
            
            if (wakeWordEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 唤醒词输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (editingWakeWord) newWakeWord else wakeWord,
                        onValueChange = { newWakeWord = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("唤醒词") },
                        enabled = editingWakeWord,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    if (editingWakeWord) {
                        Button(
                            onClick = {
                                onUpdateWakeWord(newWakeWord)
                                editingWakeWord = false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("保存")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                editingWakeWord = false
                                newWakeWord = wakeWord
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("取消")
                        }
                    } else {
                        IconButton(
                            onClick = { editingWakeWord = true }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑唤醒词")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 唤醒词训练状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "个性化训练",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (wakeWordTrained) {
                            Text(
                                text = "准确率: ${String.format("%.1f", wakeWordAccuracy * 100)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            wakeWordLastTrained?.let {
                                val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                                Text(
                                    text = "上次训练: ${dateFormat.format(Date(it))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "未训练个性化唤醒词",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    
                    Button(
                        onClick = onTrainWakeWord,
                        enabled = !isTraining,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wakeWordTrained) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.secondary,
                            contentColor = if (wakeWordTrained) MaterialTheme.colorScheme.onPrimary 
                                          else MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isTraining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("训练中...")
                        } else {
                            Text(if (wakeWordTrained) "重新训练" else "开始训练")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 训练说明
                Text(
                    text = "训练个性化唤醒词可提高识别准确率。请在不同环境、不同语气下重复说出唤醒词。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VoiceRecognitionSection(
    language: VoiceLanguage,
    confidence: Float,
    sensitivity: Float,
    lastRecognitionResult: String?,
    recognitionConfidence: Float,
    isTesting: Boolean,
    onUpdateLanguage: (VoiceLanguage) -> Unit,
    onUpdateConfidence: (Float) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    onTestRecognition: () -> Unit
) {
    var expandedLanguage by rememberSaveable { mutableStateOf(false) }
    var showConfidenceSlider by rememberSaveable { mutableStateOf(false) }
    var showSensitivitySlider by rememberSaveable { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "语音识别设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 语言选择
            SettingsDropdownItem(
                title = "识别语言",
                value = getVoiceLanguageName(language),
                icon = Icons.Default.Language,
                expanded = expandedLanguage,
                onExpandedChange = { expandedLanguage = it }
            ) {
                DropdownMenuItem(
                    text = { Text("跟随系统") },
                    onClick = {
                        onUpdateLanguage(VoiceLanguage.SYSTEM)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("简体中文") },
                    onClick = {
                        onUpdateLanguage(VoiceLanguage.ZH_CN)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("English (US)") },
                    onClick = {
                        onUpdateLanguage(VoiceLanguage.EN_US)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("日本語") },
                    onClick = {
                        onUpdateLanguage(VoiceLanguage.JA)
                        expandedLanguage = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("한국어") },
                    onClick = {
                        onUpdateLanguage(VoiceLanguage.KO)
                        expandedLanguage = false
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // 置信度阈值
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showConfidenceSlider = !showConfidenceSlider },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = "置信度",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "置信度阈值",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.0f", confidence * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    if (showConfidenceSlider) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showConfidenceSlider) "收起" else "展开"
                )
            }
            
            // 置信度滑块
            AnimatedVisibility(
                visible = showConfidenceSlider,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 40.dp)
                ) {
                    Slider(
                        value = confidence,
                        onValueChange = onUpdateConfidence,
                        valueRange = 0.1f..1.0f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "宽松 (10%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "严格 (100%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // 灵敏度设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSensitivitySlider = !showSensitivitySlider },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "灵敏度",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "识别灵敏度",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.0f", sensitivity * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    if (showSensitivitySlider) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showSensitivitySlider) "收起" else "展开"
                )
            }
            
            // 灵敏度滑块
            AnimatedVisibility(
                visible = showSensitivitySlider,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 40.dp)
                ) {
                    Slider(
                        value = sensitivity,
                        onValueChange = onUpdateSensitivity,
                        valueRange = 0.1f..1.0f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "低 (10%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "高 (100%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // 识别测试
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "识别测试",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Button(
                        onClick = onTestRecognition,
                        enabled = !isTesting,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("识别中...")
                        } else {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "测试",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始测试")
                        }
                    }
                }
                
                // 测试结果
                lastRecognitionResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "识别结果:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "置信度: ${String.format("%.1f", recognitionConfidence * 100)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceFeedbackSection(
    voiceFeedbackEnabled: Boolean,
    volume: Float,
    speed: Float,
    pitch: Float,
    isTesting: Boolean,
    onToggleFeedback: () -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdatePitch: (Float) -> Unit,
    onTestSynthesis: () -> Unit
) {
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
                    text = "语音反馈设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = voiceFeedbackEnabled,
                    onCheckedChange = { onToggleFeedback() }
                )
            }
            
            if (voiceFeedbackEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 音量设置
                VoiceFeedbackSlider(
                    title = "音量",
                    value = volume,
                    icon = Icons.Default.VolumeUp,
                    onValueChange = onUpdateVolume
                )
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // 语速设置
                VoiceFeedbackSlider(
                    title = "语速",
                    value = speed,
                    icon = Icons.Default.Speed,
                    onValueChange = onUpdateSpeed
                )
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // 音调设置
                VoiceFeedbackSlider(
                    title = "音调",
                    value = pitch,
                    icon = Icons.Default.MusicNote,
                    onValueChange = onUpdatePitch
                )
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // 语音合成测试
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "语音合成测试",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Button(
                        onClick = onTestSynthesis,
                        enabled = !isTesting,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("合成中...")
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "测试",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("播放测试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceFeedbackSlider(
    title: String,
    value: Float,
    icon: ImageVector,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
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
                
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = 0.1f..2.0f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "低",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", value)}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "高",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VoicePrintSection(
    voicePrintEnabled: Boolean,
    voicePrintRegistered: Boolean,
    autoVoicePrintUpdate: Boolean,
    voicePrintConfidence: Float,
    onToggleVoicePrint: () -> Unit,
    onToggleAutoUpdate: () -> Unit,
    onRegisterVoicePrint: () -> Unit,
    onVerifyVoicePrint: () -> Unit
) {
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
                    text = "声纹识别",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = voicePrintEnabled,
                    onCheckedChange = { onToggleVoicePrint() }
                )
            }
            
            if (voicePrintEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 声纹状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (voicePrintRegistered) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (voicePrintRegistered) Icons.Default.Fingerprint else Icons.Outlined.Fingerprint,
                            contentDescription = "声纹",
                            modifier = Modifier.size(24.dp),
                            tint = if (voicePrintRegistered) MaterialTheme.colorScheme.primary 
                                  else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (voicePrintRegistered) "声纹已注册" else "声纹未注册",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (voicePrintRegistered) {
                            Text(
                                text = "识别置信度: ${String.format("%.1f", voicePrintConfidence * 100)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "注册声纹以提高安全性",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Button(
                        onClick = if (voicePrintRegistered) onVerifyVoicePrint else onRegisterVoicePrint,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(if (voicePrintRegistered) "验证声纹" else "注册声纹")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 自动更新开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Update,
                        contentDescription = "自动更新",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "自动更新声纹",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "根据使用情况自动优化声纹模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = autoVoicePrintUpdate,
                        onCheckedChange = { onToggleAutoUpdate() }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 声纹说明
                Text(
                    text = "声纹识别用于验证语音命令的身份，提高安全性。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VoiceCommandsSection(
    commands: List<VoiceCommand>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onDeleteCommand: (String) -> Unit,
    onToggleCommand: (VoiceCommand) -> Unit
) {
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
                    text = "语音命令",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${commands.size} 个命令",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索语音命令...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (commands.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.MicOff,
                        contentDescription = "无命令",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "未找到相关命令" else "暂无语音命令",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角按钮添加命令",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commands) { command ->
                        VoiceCommandItem(
                            command = command,
                            onDelete = { onDeleteCommand(command.id) },
                            onToggle = { onToggleCommand(command) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCommandItem(
    command: VoiceCommand,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = command.phrase,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 状态指示器
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (command.enabled) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = command.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row {
                        // 类别标签
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = command.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 使用次数
                        if (command.usageCount > 0) {
                            Text(
                                text = "使用 ${command.usageCount} 次",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (command.requiresConfirmation) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = "需要确认",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 操作按钮
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Switch(
                        checked = command.enabled,
                        onCheckedChange = { onToggle() }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // 动作说明
            Text(
                text = "动作: ${command.action}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AdvancedVoiceSettingsSection(
    backgroundListening: Boolean,
    noiseSuppression: Boolean,
    echoCancellation: Boolean,
    commandTimeout: Int,
    onToggleBackgroundListening: () -> Unit,
    onToggleNoiseSuppression: () -> Unit,
    onToggleEchoCancellation: () -> Unit,
    onUpdateCommandTimeout: (Int) -> Unit
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
                text = "高级设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 后台监听
            SettingsSwitchItem(
                title = "后台监听",
                description = "允许在后台监听唤醒词",
                icon = Icons.Default.Background,
                checked = backgroundListening,
                onCheckedChange = { onToggleBackgroundListening() }
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // 噪声抑制
            SettingsSwitchItem(
                title = "噪声抑制",
                description = "降低环境噪声干扰",
                icon = Icons.Default.NoiseControlOff,
                checked = noiseSuppression,
                onCheckedChange = { onToggleNoiseSuppression() }
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // 回声消除
            SettingsSwitchItem(
                title = "回声消除",
                description = "消除设备自身声音回声",
                icon = Icons.Default.Echo,
                checked = echoCancellation,
                onCheckedChange = { onToggleEchoCancellation() }
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // 命令超时时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "超时时间",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "命令超时时间",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${commandTimeout / 1000} 秒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // TODO: 添加超时时间选择器
                Text(
                    text = "${commandTimeout / 1000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun VoiceTestSection(
    isRecording: Boolean,
    recordingProgress: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "实时测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 录音可视化
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isRecording) 3.dp else 1.dp,
                        color = if (isRecording) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 录音波纹效果
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(100.dp * recordingProgress)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        Color.Transparent
                                    ),
                                    center = Offset(0.5f, 0.5f),
                                    radius = 0.8f
                                )
                            )
                    )
                }
                
                Icon(
                    if (isRecording) Icons.Default.Mic else Icons.Outlined.Mic,
                    contentDescription = if (isRecording) "停止录音" else "开始录音",
                    modifier = Modifier.size(48.dp),
                    tint = if (isRecording) MaterialTheme.colorScheme.primary 
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 录音进度条
            if (isRecording) {
                LinearProgressIndicator(
                    progress = recordingProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "录音中... ${String.format("%.0f", recordingProgress * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 录音按钮
            Button(
                onClick = if (isRecording) onStopRecording else onStartRecording,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.primary,
                    contentColor = if (isRecording) MaterialTheme.colorScheme.onError 
                                  else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "停止" else "开始录音",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRecording) "停止录音" else "开始录音测试")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "点击按钮开始录音，测试语音识别效果",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVoiceCommandDialog(
    phrase: String,
    description: String,
    action: String,
    category: String,
    onPhraseChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onActionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("添加语音命令")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = onPhraseChange,
                    label = { Text("命令短语*") },
                    placeholder = { Text("例如：打电话给张三") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("描述") },
                    placeholder = { Text("命令的功能描述") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = action,
                    onValueChange = onActionChange,
                    label = { Text("执行动作*") },
                    placeholder = { Text("例如：打开联系人张三并拨打") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("分类") },
                    placeholder = { Text("例如：联系人、消息、设置等") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = phrase.isNotBlank() && action.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 辅助函数：获取语音语言的显示名称
private fun getVoiceLanguageName(language: VoiceLanguage): String = when (language) {
    VoiceLanguage.SYSTEM -> "跟随系统"
    VoiceLanguage.ZH_CN -> "简体中文"
    VoiceLanguage.ZH_TW -> "繁体中文"
    VoiceLanguage.EN_US -> "英语（美国）"
    VoiceLanguage.EN_UK -> "英语（英国）"
    VoiceLanguage.JA -> "日语"
    VoiceLanguage.KO -> "韩语"
    VoiceLanguage.FR -> "法语"
    VoiceLanguage.DE -> "德语"
    VoiceLanguage.ES -> "西班牙语"
}