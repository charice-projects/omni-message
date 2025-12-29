// 📁 feature/voice/ui/VoiceControlScreen.kt
package com.omnimsg.feature.voice.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.omnimsg.feature.voice.VoiceControlViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceControlScreen(
    navController: NavController,
    viewModel: VoiceControlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTraining by remember { mutableStateOf(false) }
    
    // 语音输入音量动画
    val animatedVolume by animateFloatAsState(
        targetValue = uiState.voiceInputLevel,
        animationSpec = tween(durationMillis = 100),
        label = "voiceVolume"
    )
    
    LaunchedEffect(uiState.recognitionResult) {
        uiState.recognitionResult?.let { result ->
            val message = when (result) {
                is com.omnimsg.feature.voice.VoiceRecognition.RecognitionResult.Success -> 
                    "识别成功: ${result.text}"
                is com.omnimsg.feature.voice.VoiceRecognition.RecognitionResult.Error -> 
                    "识别失败: ${result.message}"
                com.omnimsg.feature.voice.VoiceRecognition.RecognitionResult.NoMatch -> 
                    "未识别到语音"
                else -> null
            }
            
            message?.let {
                scope.launch {
                    snackbarHostState.showSnackbar(it)
                }
            }
        }
    }
    
    LaunchedEffect(uiState.commandResult) {
        uiState.commandResult?.let { result ->
            val message = if (result.isSuccess) {
                "命令执行成功: ${result.message}"
            } else {
                "命令执行失败: ${result.message}"
            }
            
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音控制") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "历史记录")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 状态指示器
                StatusIndicator(
                    isListening = uiState.isListening,
                    isProcessing = uiState.isProcessing,
                    wakeWordDetected = uiState.wakeWordDetected,
                    modifier = Modifier.padding(16.dp)
                )
                
                // 语音输入显示
                VoiceInputDisplay(
                    recognitionResult = uiState.recognitionResult,
                    commandResult = uiState.commandResult,
                    isListening = uiState.isListening,
                    volumeLevel = animatedVolume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                )
                
                // 麦克风按钮
                MicrophoneButton(
                    isListening = uiState.isListening,
                    isProcessing = uiState.isProcessing,
                    onStartListening = {
                        scope.launch {
                            viewModel.startVoiceRecognition()
                        }
                    },
                    onStopListening = {
                        scope.launch {
                            viewModel.stopVoiceRecognition()
                        }
                    },
                    modifier = Modifier.padding(bottom = 48.dp)
                )
                
                // 快速命令
                QuickCommands(
                    commands = uiState.availableCommands.take(4),
                    onCommandClick = { command ->
                        scope.launch {
                            viewModel.executeCommand(command)
                        }
                    },
                    modifier = Modifier.padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                )
            }
            
            // 唤醒词提示
            AnimatedVisibility(
                visible = uiState.wakeWordDetected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .height(160.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "唤醒词检测成功",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "请说出您的命令",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // 历史记录对话框
            if (showHistory) {
                VoiceHistoryDialog(
                    history = uiState.commandHistory,
                    onDismiss = { showHistory = false }
                )
            }
            
            // 设置对话框
            if (showSettings) {
                VoiceSettingsDialog(
                    currentSettings = uiState.voiceSettings,
                    onSettingsUpdated = { settings ->
                        viewModel.updateVoiceSettings(settings)
                        showSettings = false
                    },
                    onTrainingClick = {
                        showSettings = false
                        showTraining = true
                    },
                    onDismiss = { showSettings = false }
                )
            }
            
            // 训练对话框
            if (showTraining) {
                VoiceTrainingDialog(
                    onDismiss = { showTraining = false }
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    isListening: Boolean,
    isProcessing: Boolean,
    wakeWordDetected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 状态指示
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isProcessing -> Color(0xFFFF9800)
                            isListening -> Color(0xFF4CAF50)
                            else -> Color(0xFFBDBDBD)
                        }
                    )
            )
            
            Text(
                text = when {
                    isProcessing -> "正在处理..."
                    isListening -> "正在聆听"
                    wakeWordDetected -> "唤醒词激活"
                    else -> "等待指令"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 唤醒词状态
            if (wakeWordDetected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "熙熙",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceInputDisplay(
    recognitionResult: com.omnimsg.feature.voice.VoiceRecognition.RecognitionResult?,
    commandResult: com.omnimsg.feature.voice.VoiceCommandCenter.CommandResult?,
    isListening: Boolean,
    volumeLevel: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 音量可视化
        if (isListening) {
            VolumeVisualizer(
                volumeLevel = volumeLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // 识别结果显示
        when (recognitionResult) {
            is com.omnimsg.feature.voice.VoiceRecognition.RecognitionResult.Success -> {
                RecognitionResultCard(
                    text = recognitionResult.text,
                    confidence = recognitionResult.confidence,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            is com.omnimsg.feature.voice.VoiceRecognition.RecognitionResult.Partial -> {
                RecognitionResultCard(
                    text = recognitionResult.text,
                    confidence = null,
                    isPartial = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            is com.omnimsg.feature.voice.VoiceRecognition.RecognitionResult.Error -> {
                ErrorCard(
                    message = recognitionResult.message,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            else -> {
                // 默认提示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = if (isListening) {
                            "请说话..."
                        } else {
                            "点击麦克风开始说话"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "支持命令：发消息、打电话、紧急求助等",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // 命令执行结果
        commandResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            
            CommandResultCard(
                result = result,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VolumeVisualizer(
    volumeLevel: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 20
    val barWidth = 4.dp
    val maxHeight = 60.dp
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(barCount) { index ->
                val barHeight = if (index < (volumeLevel * barCount).toInt()) {
                    maxHeight * (0.3f + (index.toFloat() / barCount) * 0.7f)
                } else {
                    0.dp
                }
                
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < (volumeLevel * barCount).toInt()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun RecognitionResultCard(
    text: String,
    confidence: Float?,
    isPartial: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (isPartial) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在识别...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = if (isPartial) 8.dp else 0.dp)
            )
            
            confidence?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(confidence.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    confidence > 0.8 -> Color(0xFF4CAF50)
                                    confidence > 0.6 -> Color(0xFFFF9800)
                                    else -> Color(0xFFF44336)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.weight(1f - confidence))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CommandResultCard(
    result: com.omnimsg.feature.voice.VoiceCommandCenter.CommandResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (result.isSuccess) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (result.isSuccess) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (result.isSuccess) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

@Composable
private fun MicrophoneButton(
    isListening: Boolean,
    isProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 80.dp
    val pulseSize = animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1f,
        label = "pulseAnimation"
    ).value
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 脉冲效果
        if (isListening) {
            Box(
                modifier = Modifier
                    .size((buttonSize * pulseSize).dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
            )
        }
        
        // 麦克风按钮
        Surface(
            modifier = Modifier
                .size(buttonSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape),
            onClick = {
                if (isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            },
            color = if (isListening) {
                MaterialTheme.colorScheme.primary
            } else if (isProcessing) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isListening || isProcessing) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "停止" else "开始",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCommands(
    commands: List<com.omnimsg.feature.voice.VoiceCommandCenter.VoiceCommand>,
    onCommandClick: (com.omnimsg.feature.voice.VoiceCommandCenter.VoiceCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "快速命令",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            commands.forEach { command ->
                QuickCommandButton(
                    command = command,
                    onClick = { onCommandClick(command) }
                )
            }
        }
    }
}

@Composable
private fun QuickCommandButton(
    command: com.omnimsg.feature.voice.VoiceCommandCenter.VoiceCommand,
    onClick: () -> Unit
) {
    val commandText = when (command.type) {
        is com.omnimsg.feature.voice.VoiceCommandCenter.CommandType.SendMessage -> "发消息"
        is com.omnimsg.feature.voice.VoiceCommandCenter.CommandType.MakeCall -> "打电话"
        is com.omnimsg.feature.voice.VoiceCommandCenter.CommandType.EmergencyAlert -> "紧急"
        is com.omnimsg.feature.voice.VoiceCommandCenter.CommandType.SearchContact -> "搜索"
        else -> command.description
    }
    
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp)
    ) {
        Text(
            text = commandText,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun VoiceHistoryDialog(
    history: List<com.omnimsg.feature.voice.CommandExecution>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("语音命令历史") },
        text = {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无历史记录")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(history) { execution ->
                        HistoryItem(execution = execution)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun HistoryItem(
    execution: com.omnimsg.feature.voice.CommandExecution
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (execution.result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (execution.result.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = execution.inputText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Date(execution.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (execution.result.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = execution.result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VoiceSettingsDialog(
    currentSettings: com.omnimsg.feature.voice.VoiceSettings,
    onSettingsUpdated: (com.omnimsg.feature.voice.VoiceSettings) -> Unit,
    onTrainingClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var wakeWordEnabled by remember { mutableStateOf(currentSettings.wakeWordEnabled) }
    var feedbackEnabled by remember { mutableStateOf(currentSettings.feedbackEnabled) }
    var speechRate by remember { mutableFloatStateOf(currentSettings.speechRate) }
    var sensitivity by remember { mutableFloatStateOf(currentSettings.sensitivity) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("语音设置") },
        text = {
            Column {
                // 唤醒词设置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用唤醒词")
                        Text(
                            "使用\"熙熙\"唤醒语音助手",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = { wakeWordEnabled = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 语音反馈
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用语音反馈")
                        Text(
                            "执行命令后语音确认",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = feedbackEnabled,
                        onCheckedChange = { feedbackEnabled = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 语速设置
                Text(
                    "语速调节",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = speechRate,
                        onValueChange = { speechRate = it },
                        valueRange = 0.5f..2f,
                        steps = 14,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "%.1f".format(speechRate),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 灵敏度设置
                Text(
                    "识别灵敏度",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.1f..1f,
                        steps = 8,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(sensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 训练按钮
                OutlinedButton(
                    onClick = onTrainingClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("训练个性化唤醒词")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newSettings = currentSettings.copy(
                        wakeWordEnabled = wakeWordEnabled,
                        feedbackEnabled = feedbackEnabled,
                        speechRate = speechRate,
                        sensitivity = sensitivity
                    )
                    onSettingsUpdated(newSettings)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun VoiceTrainingDialog(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingCount by remember { mutableStateOf(0) }
    val totalSteps = 3
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("训练个性化唤醒词") },
        text = {
            Column {
                // 步骤指示器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(totalSteps) { index ->
                        StepIndicator(
                            title = "步骤${index + 1}",
                            isActive = index == currentStep,
                            isCompleted = index < currentStep,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                when (currentStep) {
                    0 -> {
                        Text(
                            text = "请准备录制您的个性化唤醒词",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "建议：\n" +
                                  "• 在安静的环境中录制\n" +
                                  "• 用自然的语调说出\"熙熙\"\n" +
                                  "• 需要录制3次不同的语音样本",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    1 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "请说出 \"熙熙\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // 录音按钮
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRecording) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    .clickable {
                                        isRecording = !isRecording
                                        if (isRecording) {
                                            recordingCount++
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = if (isRecording) "正在录音..." else "点击开始录音",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "已录制：$recordingCount/3",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    2 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "训练完成！",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "个性化唤醒词已保存",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "下次可以说\"熙熙\"来唤醒语音助手",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (currentStep) {
                0 -> {
                    Button(
                        onClick = { currentStep = 1 }
                    ) {
                        Text("开始训练")
                    }
                }
                1 -> {
                    Button(
                        onClick = {
                            if (recordingCount >= 3) {
                                currentStep = 2
                            }
                        },
                        enabled = recordingCount >= 3
                    ) {
                        Text(if (recordingCount >= 3) "下一步" else "继续录制")
                    }
                }
                2 -> {
                    Button(
                        onClick = onDismiss
                    ) {
                        Text("完成")
                    }
                }
            }
        },
        dismissButton = {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("上一步")
                }
            }
        }
    )
}

// 缺少的简单组件
@Composable
private fun StepIndicator(
    title: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    color = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isCompleted -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                isActive -> Text(
                    text = title.first().toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                else -> Text(
                    text = (title.indexOf(title) + 1).toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive || isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}