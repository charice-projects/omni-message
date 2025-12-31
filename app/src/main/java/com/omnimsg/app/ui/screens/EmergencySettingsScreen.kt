// 📁 app/src/main/java/com/omnimsg/app/ui/screens/EmergencySettingsScreen.kt
package com.omnimsg.app.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
fun EmergencySettingsScreen(
    viewModel: EmergencyViewModel = hiltViewModel(),
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
    
    // 抽屉状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 添加联系人对话框状态
    var showAddContactDialog by rememberSaveable { mutableStateOf(false) }
    var newContactName by rememberSaveable { mutableStateOf("") }
    var newContactPhone by rememberSaveable { mutableStateOf("") }
    var newContactRelationship by rememberSaveable { mutableStateOf("") }
    
    // 测试警报对话框状态
    var showTestAlertDialog by rememberSaveable { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("紧急设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { drawerState.open() }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 帮助 */ }) {
                        Icon(Icons.Outlined.Help, contentDescription = "帮助")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 测试按钮
                FloatingActionButton(
                    onClick = { showTestAlertDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Outlined.TestTube, contentDescription = "测试警报")
                }
                
                // 紧急按钮
                FloatingActionButton(
                    onClick = { viewModel.triggerEmergencyAlert() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = "紧急警报")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 状态概览卡片
            item {
                EmergencyStatusCard(state = state)
            }
            
            // 紧急联系人管理
            item {
                EmergencyContactsSection(
                    contacts = state.emergencyContacts,
                    onAddContact = { showAddContactDialog = true },
                    onRemoveContact = viewModel::removeEmergencyContact,
                    onUpdatePriority = viewModel::updateContactPriority
                )
            }
            
            // 触发方式设置
            item {
                TriggerMethodsSection(
                    triggerMethods = state.triggerMethods,
                    onUpdateTriggerMethods = viewModel::updateTriggerMethods
                )
            }
            
            // 警报内容设置
            item {
                AlertContentSection(
                    includeLocation = state.includeLocation,
                    includeAudio = state.includeAudio,
                    includePhotos = state.includePhotos,
                    onUpdateLocationSharing = viewModel::updateLocationSharing,
                    onUpdateMediaSharing = viewModel::updateMediaSharing
                )
            }
            
            // 高级设置
            item {
                AdvancedSettingsSection(
                    stealthMode = state.stealthMode,
                    autoEscalate = state.autoEscalate,
                    onUpdateStealthMode = viewModel::updateStealthMode,
                    onUpdateAutoEscalate = viewModel::updateAutoEscalate
                )
            }
            
            // 警报历史
            item {
                AlertHistorySection(
                    alertHistory = state.alertHistory,
                    onCancelAlert = viewModel::cancelEmergencyAlert
                )
            }
        }
        
        // 添加联系人对话框
        if (showAddContactDialog) {
            AddEmergencyContactDialog(
                name = newContactName,
                phone = newContactPhone,
                relationship = newContactRelationship,
                onNameChange = { newContactName = it },
                onPhoneChange = { newContactPhone = it },
                onRelationshipChange = { newContactRelationship = it },
                onDismiss = { showAddContactDialog = false },
                onConfirm = {
                    if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                        viewModel.addEmergencyContact(
                            EmergencyContact(
                                id = "new_${System.currentTimeMillis()}",
                                name = newContactName,
                                phone = newContactPhone,
                                relationship = newContactRelationship.ifBlank { "未指定" }
                            )
                        )
                        newContactName = ""
                        newContactPhone = ""
                        newContactRelationship = ""
                        showAddContactDialog = false
                    } else {
                        onShowSnackbar("请填写姓名和电话号码")
                    }
                }
            )
        }
        
        // 测试警报对话框
        if (showTestAlertDialog) {
            TestAlertDialog(
                onDismiss = { showTestAlertDialog = false },
                onTest = {
                    viewModel.testEmergencyAlert()
                    showTestAlertDialog = false
                }
            )
        }
    }
}

@Composable
private fun EmergencyStatusCard(state: EmergencyState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
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
                    text = "紧急系统状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = if (state.isEmergencyEnabled) "已启用" else "已禁用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isEmergencyEnabled) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${state.emergencyContacts.size} 个紧急联系人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 启用/禁用开关
            Switch(
                checked = state.isEmergencyEnabled,
                onCheckedChange = { /* 更新启用状态 */ },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.error,
                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    }
}

@Composable
private fun EmergencyContactsSection(
    contacts: List<EmergencyContact>,
    onAddContact: () -> Unit,
    onRemoveContact: (String) -> Unit,
    onUpdatePriority: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "紧急联系人",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = onAddContact,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加联系人",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (contacts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.PersonOff,
                        contentDescription = "无联系人",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无紧急联系人",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右上角添加按钮添加联系人",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contacts.sortedBy { it.priority }) { contact ->
                        EmergencyContactItem(
                            contact = contact,
                            onRemove = { onRemoveContact(contact.id) },
                            onPriorityChange = { newPriority ->
                                onUpdatePriority(contact.id, newPriority)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "注意：紧急联系人将在紧急警报触发时收到通知",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun EmergencyContactItem(
    contact: EmergencyContact,
    onRemove: () -> Unit,
    onPriorityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 优先级指示器
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when (contact.priority) {
                                    1 -> MaterialTheme.colorScheme.error
                                    2 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.priority.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row {
                    Text(
                        text = contact.relationship,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 位置共享状态
                    if (contact.canReceiveLocation) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = "可接收位置",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // 媒体共享状态
                    if (contact.canReceiveMedia) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = "可接收媒体",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // 优先级调节器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "优先级",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (priority in 1..3) {
                    FilterChip(
                        selected = contact.priority == priority,
                        onClick = { onPriorityChange(priority) },
                        label = {
                            Text(
                                text = when (priority) {
                                    1 -> "高"
                                    2 -> "中"
                                    3 -> "低"
                                    else -> priority.toString()
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (priority) {
                                1 -> MaterialTheme.colorScheme.error
                                2 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = when (priority) {
                                1 -> MaterialTheme.colorScheme.error
                                2 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TriggerMethodsSection(
    triggerMethods: Set<TriggerMethod>,
    onUpdateTriggerMethods: (Set<TriggerMethod>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "触发方式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 电源键三击
            TriggerMethodOption(
                title = "电源键三击",
                description = "快速按下电源键三次",
                icon = Icons.Outlined.PowerSettingsNew,
                isSelected = triggerMethods.contains(TriggerMethod.POWER_BUTTON_TRIPLE),
                onToggle = {
                    val newSet = triggerMethods.toMutableSet()
                    if (it) {
                        newSet.add(TriggerMethod.POWER_BUTTON_TRIPLE)
                    } else {
                        newSet.remove(TriggerMethod.POWER_BUTTON_TRIPLE)
                    }
                    onUpdateTriggerMethods(newSet)
                }
            )
            
            // 音量键组合
            TriggerMethodOption(
                title = "音量键组合",
                description = "同时按下音量+和音量-键",
                icon = Icons.Outlined.VolumeUp,
                isSelected = triggerMethods.contains(TriggerMethod.VOLUME_COMBO),
                onToggle = {
                    val newSet = triggerMethods.toMutableSet()
                    if (it) {
                        newSet.add(TriggerMethod.VOLUME_COMBO)
                    } else {
                        newSet.remove(TriggerMethod.VOLUME_COMBO)
                    }
                    onUpdateTriggerMethods(newSet)
                }
            )
            
            // 手势识别
            TriggerMethodOption(
                title = "手势识别",
                description = "画出预设的紧急手势",
                icon = Icons.Outlined.Gesture,
                isSelected = triggerMethods.contains(TriggerMethod.GESTURE),
                onToggle = {
                    val newSet = triggerMethods.toMutableSet()
                    if (it) {
                        newSet.add(TriggerMethod.GESTURE)
                    } else {
                        newSet.remove(TriggerMethod.GESTURE)
                    }
                    onUpdateTriggerMethods(newSet)
                }
            )
            
            // 语音命令
            TriggerMethodOption(
                title = "语音命令",
                description = "说出预设的紧急短语",
                icon = Icons.Outlined.Mic,
                isSelected = triggerMethods.contains(TriggerMethod.VOICE_COMMAND),
                onToggle = {
                    val newSet = triggerMethods.toMutableSet()
                    if (it) {
                        newSet.add(TriggerMethod.VOICE_COMMAND)
                    } else {
                        newSet.remove(TriggerMethod.VOICE_COMMAND)
                    }
                    onUpdateTriggerMethods(newSet)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "注意：启用多个触发方式可增加紧急情况下的可靠性",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TriggerMethodOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                  else MaterialTheme.colorScheme.onSurfaceVariant
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
            checked = isSelected,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun AlertContentSection(
    includeLocation: Boolean,
    includeAudio: Boolean,
    includePhotos: Boolean,
    onUpdateLocationSharing: (Boolean) -> Unit,
    onUpdateMediaSharing: (MediaType, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "警报内容",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 位置共享
            AlertContentOption(
                title = "共享位置信息",
                description = "自动发送您的实时位置",
                icon = Icons.Outlined.LocationOn,
                isSelected = includeLocation,
                onToggle = onUpdateLocationSharing
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 音频录制
            AlertContentOption(
                title = "录制环境音频",
                description = "录制并发送10秒环境音频",
                icon = Icons.Outlined.Mic,
                isSelected = includeAudio,
                onToggle = { onUpdateMediaSharing(MediaType.AUDIO, it) }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 照片拍摄
            AlertContentOption(
                title = "拍摄环境照片",
                description = "前后摄像头各拍摄一张照片",
                icon = Icons.Outlined.PhotoCamera,
                isSelected = includePhotos,
                onToggle = { onUpdateMediaSharing(MediaType.PHOTOS, it) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "注意：共享媒体内容可能会增加数据使用量",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlertContentOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                  else MaterialTheme.colorScheme.onSurfaceVariant
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
            checked = isSelected,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun AdvancedSettingsSection(
    stealthMode: Boolean,
    autoEscalate: Boolean,
    onUpdateStealthMode: (Boolean) -> Unit,
    onUpdateAutoEscalate: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "高级设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 隐身模式
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.VisibilityOff,
                    contentDescription = "隐身模式",
                    modifier = Modifier.size(24.dp),
                    tint = if (stealthMode) MaterialTheme.colorScheme.primary 
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "隐身模式",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "隐藏警报发送界面，避免被发现",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = stealthMode,
                    onCheckedChange = onUpdateStealthMode
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 自动升级
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.EscalatorWarning,
                    contentDescription = "自动升级",
                    modifier = Modifier.size(24.dp),
                    tint = if (autoEscalate) MaterialTheme.colorScheme.primary 
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "自动升级警报",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "未响应时自动提高警报级别",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = autoEscalate,
                    onCheckedChange = onUpdateAutoEscalate
                )
            }
        }
    }
}

@Composable
private fun AlertHistorySection(
    alertHistory: List<EmergencyAlert>,
    onCancelAlert: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "警报历史",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "最近 ${alertHistory.size} 条",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (alertHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = "无历史记录",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无警报历史",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alertHistory.take(5)) { alert ->
                        AlertHistoryItem(
                            alert = alert,
                            onCancel = { onCancelAlert(alert.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertHistoryItem(
    alert: EmergencyAlert,
    onCancel: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val dateStr = remember(alert.timestamp) {
        dateFormat.format(Date(alert.timestamp))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.status) {
                AlertStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                AlertStatus.RESPONDED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (alert.triggerMethod) {
                            TriggerMethod.POWER_BUTTON_TRIPLE -> "电源键触发"
                            TriggerMethod.VOLUME_COMBO -> "音量键触发"
                            TriggerMethod.GESTURE -> "手势触发"
                            TriggerMethod.VOICE_COMMAND -> "语音触发"
                            TriggerMethod.MANUAL -> "手动触发"
                            TriggerMethod.TEST -> "测试警报"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 状态标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (alert.status) {
                                AlertStatus.SENT -> MaterialTheme.colorScheme.secondaryContainer
                                AlertStatus.DELIVERED -> MaterialTheme.colorScheme.primaryContainer
                                AlertStatus.RESPONDED -> MaterialTheme.colorScheme.tertiaryContainer
                                AlertStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (alert.status) {
                            AlertStatus.SENT -> "已发送"
                            AlertStatus.DELIVERED -> "已送达"
                            AlertStatus.RESPONDED -> "已响应"
                            AlertStatus.CANCELLED -> "已取消"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when (alert.status) {
                            AlertStatus.SENT -> MaterialTheme.colorScheme.onSecondaryContainer
                            AlertStatus.DELIVERED -> MaterialTheme.colorScheme.onPrimaryContainer
                            AlertStatus.RESPONDED -> MaterialTheme.colorScheme.onTertiaryContainer
                            AlertStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 收件人信息
                Column {
                    Text(
                        text = "收件人: ${alert.recipientCount}人",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (alert.status == AlertStatus.RESPONDED) {
                        Text(
                            text = "已响应: ${alert.respondedCount}人",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // 内容标志
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (alert.locationIncluded) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = "包含位置",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (alert.mediaIncluded) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = "包含媒体",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 取消按钮（仅对已发送但未完成的警报显示）
            if (alert.status == AlertStatus.SENT || alert.status == AlertStatus.DELIVERED) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "取消警报",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("取消警报")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEmergencyContactDialog(
    name: String,
    phone: String,
    relationship: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRelationshipChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("添加紧急联系人")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("姓名") },
                    placeholder = { Text("请输入姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("电话号码") },
                    placeholder = { Text("+86 138 0000 0000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )
                
                OutlinedTextField(
                    value = relationship,
                    onValueChange = onRelationshipChange,
                    label = { Text("关系") },
                    placeholder = { Text("家人、朋友、同事等") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank() && phone.isNotBlank()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestAlertDialog(
    onDismiss: () -> Unit,
    onTest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("测试紧急警报")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "将发送测试警报给所有紧急联系人",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "测试内容：",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "• 测试消息（非真实紧急情况）",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• 当前位置（如已启用）",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• 环境音频/照片（如已启用）",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "注意：接收方将收到明确的测试标记",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onTest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("发送测试警报")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}