// 📁 app/src/main/java/com/omnimsg/app/ui/screens/ExcelImportScreen.kt
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
fun ExcelImportScreen(
    viewModel: ExcelImportViewModel = hiltViewModel(),
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
                title = { Text("Excel导入") },
                navigationIcon = {
                    IconButton(onClick = { /* 返回 */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 导入历史 */ }) {
                        Icon(Icons.Default.History, contentDescription = "导入历史")
                    }
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
            when (state.currentStep) {
                ExcelImportStep.FILE_SELECTION -> {
                    FloatingActionButton(
                        onClick = { /* 触发文件选择器 */ },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "选择文件")
                    }
                }
                ExcelImportStep.COMPLETE -> {
                    FloatingActionButton(
                        onClick = { viewModel.resetImport() },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新开始")
                    }
                }
                else -> {
                    // 其他步骤不显示FAB或显示其他操作
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 导入步骤指示器
            ImportStepsIndicator(currentStep = state.currentStep)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (state.currentStep) {
                ExcelImportStep.FILE_SELECTION -> {
                    FileSelectionStep(
                        onFileSelected = viewModel::selectExcelFile,
                        isLoading = state.isLoading
                    )
                }
                ExcelImportStep.FILE_SELECTED,
                ExcelImportStep.PARSING_COMPLETE -> {
                    FileInfoStep(
                        excelData = state.excelData,
                        isLoading = state.isLoading
                    )
                }
                ExcelImportStep.FIELD_MAPPING -> {
                    FieldMappingStep(
                        fieldRecognition = state.fieldRecognition,
                        excelData = state.excelData,
                        onFieldMappingUpdated = viewModel::updateFieldMapping,
                        onAnalyzeDuplicates = viewModel::analyzeDuplicates,
                        isLoading = state.isLoading
                    )
                }
                ExcelImportStep.DUPLICATE_CHECK -> {
                    DuplicateCheckStep(
                        duplicateAnalysis = state.duplicateAnalysis,
                        duplicateStrategy = state.duplicateStrategy,
                        onStrategyChanged = viewModel::updateDuplicateStrategy,
                        onStartImport = viewModel::startImport,
                        isLoading = state.isLoading
                    )
                }
                ExcelImportStep.IMPORTING -> {
                    ImportProgressStep(
                        importProgress = state.importProgress,
                        isImporting = state.isImporting
                    )
                }
                ExcelImportStep.COMPLETE -> {
                    ImportCompleteStep(
                        importResult = state.importResult,
                        onReset = viewModel::resetImport
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 导入历史预览
            if (state.importHistory.isNotEmpty()) {
                ImportHistoryPreview(
                    importHistory = state.importHistory,
                    onViewAll = { /* 显示历史记录对话框 */ }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ImportStepsIndicator(currentStep: ExcelImportStep) {
    val steps = listOf(
        "选择文件",
        "字段映射",
        "重复检查",
        "导入完成"
    )
    
    val currentStepIndex = when (currentStep) {
        ExcelImportStep.FILE_SELECTION -> 0
        ExcelImportStep.FILE_SELECTED,
        ExcelImportStep.PARSING_COMPLETE -> 0
        ExcelImportStep.FIELD_MAPPING -> 1
        ExcelImportStep.DUPLICATE_CHECK -> 2
        ExcelImportStep.IMPORTING -> 2
        ExcelImportStep.COMPLETE -> 3
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "导入向导",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 步骤指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, stepName ->
                    StepIndicator(
                        stepNumber = index + 1,
                        stepName = stepName,
                        isActive = index == currentStepIndex,
                        isCompleted = index < currentStepIndex,
                        isLast = index == steps.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    stepNumber: Int,
    stepName: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isLast: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 步骤圆圈
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已完成",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = stepName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 连接线（除了最后一个步骤）
        if (!isLast) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun FileSelectionStep(
    onFileSelected: (File) -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "正在解析Excel文件...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    Icons.Outlined.UploadFile,
                    contentDescription = "上传文件",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "选择Excel文件",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "支持 .xlsx 和 .xls 格式文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 文件选择按钮
                Button(
                    onClick = { /* TODO: 触发文件选择器 */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = "选择文件",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("选择Excel文件")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 或拖放提示
                Text(
                    text = "或将文件拖放到此处",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 支持的文件格式说明
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "支持的文件格式:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "支持",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(".xlsx (Excel 2007及以上)")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "支持",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(".xls (Excel 97-2003)")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "建议",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("建议使用最新Excel格式以获得最佳兼容性")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileInfoStep(
    excelData: ExcelData?,
    isLoading: Boolean
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
                    text = "文件信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (excelData != null) {
                    Chip(
                        onClick = { /* 重新选择文件 */ },
                        colors = ChipDefaults.secondaryChipColors(),
                        border = ChipDefaults.outlinedChipBorder()
                    ) {
                        Text("重新选择")
                    }
                }
            }
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在解析Excel文件...",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (excelData != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 文件基本信息
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FileInfoItem(
                        icon = Icons.Default.Description,
                        title = "文件名",
                        value = excelData.fileName
                    )
                    
                    FileInfoItem(
                        icon = Icons.Default.TableChart,
                        title = "工作表",
                        value = excelData.sheetName
                    )
                    
                    FileInfoItem(
                        icon = Icons.Default.ViewList,
                        title = "数据行数",
                        value = "${excelData.totalRows} 行"
                    )
                    
                    FileInfoItem(
                        icon = Icons.Default.ViewColumn,
                        title = "数据列数",
                        value = "${excelData.headers.size} 列"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 数据预览
                Text(
                    text = "数据预览",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 显示前几行数据
                if (excelData.rows.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            // 表头
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    excelData.headers.forEachIndexed { index, header ->
                                        Text(
                                            text = header,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            
                            // 数据行（最多显示5行）
                            items(excelData.rows.take(5)) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    row.forEachIndexed { index, cell ->
                                        Text(
                                            text = cell,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            
                            // 更多数据提示
                            if (excelData.rows.size > 5) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "... 还有 ${excelData.rows.size - 5} 行数据未显示",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FieldMappingStep(
    fieldRecognition: FieldRecognition?,
    excelData: ExcelData?,
    onFieldMappingUpdated: (String, ContactField) -> Unit,
    onAnalyzeDuplicates: () -> Unit,
    isLoading: Boolean
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
                    text = "字段映射",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (fieldRecognition != null) {
                    Text(
                        text = "${String.format("%.0f", fieldRecognition.overallConfidence * 100)}% 匹配",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在智能识别字段...",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (fieldRecognition != null && excelData != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "将Excel列映射到联系人字段",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 字段映射列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(excelData.headers) { header ->
                        FieldMappingItem(
                            header = header,
                            currentMapping = fieldRecognition.mappings[header],
                            confidence = fieldRecognition.confidenceScores[header] ?: 0f,
                            onMappingChanged = { newField ->
                                onFieldMappingUpdated(header, newField)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 下一步按钮
                Button(
                    onClick = onAnalyzeDuplicates,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("检查重复数据")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "下一步",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldMappingItem(
    header: String,
    currentMapping: ContactField?,
    confidence: Float,
    onMappingChanged: (ContactField) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 列名
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 置信度指示器
                if (confidence > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = confidence,
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp),
                            color = when {
                                confidence > 0.8 -> MaterialTheme.colorScheme.primary
                                confidence > 0.5 -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.tertiary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "${String.format("%.0f", confidence * 100)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 字段选择下拉框
            Box {
                Button(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (currentMapping) {
                            ContactField.UNMAPPED -> MaterialTheme.colorScheme.surfaceVariant
                            null -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = when (currentMapping) {
                            ContactField.UNMAPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentMapping?.let { getContactFieldDisplayName(it) } ?: "未映射",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "选择字段"
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(200.dp)
                ) {
                    ContactField.values().forEach { field ->
                        DropdownMenuItem(
                            text = {
                                Text(getContactFieldDisplayName(field))
                            },
                            onClick = {
                                onMappingChanged(field)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateCheckStep(
    duplicateAnalysis: DuplicateAnalysis?,
    duplicateStrategy: DuplicateStrategy,
    onStrategyChanged: (DuplicateStrategy) -> Unit,
    onStartImport: () -> Unit,
    isLoading: Boolean
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
                text = "重复检查",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在分析重复数据...",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (duplicateAnalysis != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 重复统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DuplicateStatItem(
                        title = "总记录数",
                        value = duplicateAnalysis.totalContacts.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    DuplicateStatItem(
                        title = "唯一记录",
                        value = duplicateAnalysis.uniqueContacts.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    DuplicateStatItem(
                        title = "重复记录",
                        value = duplicateAnalysis.duplicateCount.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 重复处理策略
                Text(
                    text = "重复处理策略",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DuplicateStrategy.values().forEach { strategy ->
                        item {
                            DuplicateStrategyOption(
                                strategy = strategy,
                                isSelected = duplicateStrategy == strategy,
                                onClick = { onStrategyChanged(strategy) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 重复分组详情（如果有）
                if (duplicateAnalysis.duplicateGroups.isNotEmpty()) {
                    Text(
                        text = "重复分组",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(duplicateAnalysis.duplicateGroups.take(5)) { group ->
                            DuplicateGroupItem(group = group)
                        }
                        
                        if (duplicateAnalysis.duplicateGroups.size > 5) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "... 还有 ${duplicateAnalysis.duplicateGroups.size - 5} 个重复组",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 开始导入按钮
                Button(
                    onClick = onStartImport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("开始导入")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "开始导入",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateStatItem(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DuplicateStrategyOption(
    strategy: DuplicateStrategy,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        border = if (isSelected) BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getDuplicateStrategyDisplayName(strategy),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = getDuplicateStrategyDescription(strategy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupItem(group: DuplicateGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
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
                Text(
                    text = "重复组 ${group.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Chip(
                    onClick = { /* 查看详情 */ },
                    colors = ChipDefaults.secondaryChipColors(),
                    border = ChipDefaults.outlinedChipBorder()
                ) {
                    Text("${group.contacts.size} 条")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "重复类型: ${getDuplicateTypeDisplayName(group.duplicateType)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 显示前几个联系人
            group.contacts.take(3).forEach { contact ->
                Text(
                    text = "• ${contact.name ?: "未命名"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            if (group.contacts.size > 3) {
                Text(
                    text = "... 还有 ${group.contacts.size - 3} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImportProgressStep(
    importProgress: Float,
    isImporting: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = importProgress,
                modifier = Modifier.size(100.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "正在导入数据",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "${String.format("%.0f", importProgress * 100)}% 完成",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = importProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isImporting) {
                Text(
                    text = "请勿关闭应用或离开此页面",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImportCompleteStep(
    importResult: ImportProgress.Completed?,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "完成",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "导入完成！",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (importResult != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ImportResultItem(
                        title = "总记录数",
                        value = importResult.totalRecords.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    ImportResultItem(
                        title = "成功导入",
                        value = importResult.importedRecords.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    ImportResultItem(
                        title = "跳过记录",
                        value = importResult.skippedRecords.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    ImportResultItem(
                        title = "重复记录",
                        value = importResult.duplicateRecords.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("导入新文件")
                }
                
                OutlinedButton(
                    onClick = { /* 查看导入结果 */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("查看详情")
                }
            }
        }
    }
}

@Composable
private fun ImportResultItem(
    title: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
    
    Divider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun ImportHistoryPreview(
    importHistory: List<ImportRecord>,
    onViewAll: () -> Unit
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
                    text = "最近导入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                TextButton(onClick = onViewAll) {
                    Text("查看全部")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 显示最近的3条记录
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(importHistory.take(3)) { record ->
                    ImportHistoryItem(record = record)
                }
            }
        }
    }
}

@Composable
private fun ImportHistoryItem(record: ImportRecord) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val dateStr = remember(record.importDate) {
        dateFormat.format(Date(record.importDate))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = "导入记录",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${record.importedRecords} 条",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (record.skippedRecords > 0) {
                    Text(
                        text = "跳过 ${record.skippedRecords} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 辅助函数：获取枚举值的显示名称
private fun getContactFieldDisplayName(field: ContactField): String = when (field) {
    ContactField.NAME -> "姓名"
    ContactField.PHONE -> "电话"
    ContactField.EMAIL -> "邮箱"
    ContactField.COMPANY -> "公司"
    ContactField.POSITION -> "职位"
    ContactField.ADDRESS -> "地址"
    ContactField.BIRTHDAY -> "生日"
    ContactField.NOTES -> "备注"
    ContactField.TAGS -> "标签"
    ContactField.CUSTOM_FIELD_1 -> "自定义字段1"
    ContactField.CUSTOM_FIELD_2 -> "自定义字段2"
    ContactField.CUSTOM_FIELD_3 -> "自定义字段3"
    ContactField.UNMAPPED -> "未映射"
}

private fun getDuplicateStrategyDisplayName(strategy: DuplicateStrategy): String = when (strategy) {
    DuplicateStrategy.KEEP_ALL -> "保留所有"
    DuplicateStrategy.KEEP_FIRST -> "保留第一条"
    DuplicateStrategy.KEEP_LAST -> "保留最后一条"
    DuplicateStrategy.MERGE -> "合并数据"
    DuplicateStrategy.SKIP_ALL -> "跳过所有重复"
    DuplicateStrategy.PROMPT -> "手动选择"
}

private fun getDuplicateStrategyDescription(strategy: DuplicateStrategy): String = when (strategy) {
    DuplicateStrategy.KEEP_ALL -> "保留所有记录，包括重复项"
    DuplicateStrategy.KEEP_FIRST -> "仅保留每组重复中的第一条记录"
    DuplicateStrategy.KEEP_LAST -> "仅保留每组重复中的最后一条记录"
    DuplicateStrategy.MERGE -> "智能合并重复记录中的不同字段"
    DuplicateStrategy.SKIP_ALL -> "跳过所有重复记录，仅导入唯一记录"
    DuplicateStrategy.PROMPT -> "手动选择如何处理每个重复组"
}

private fun getDuplicateTypeDisplayName(type: DuplicateType): String = when (type) {
    DuplicateType.EXACT_MATCH -> "完全匹配"
    DuplicateType.SIMILAR_NAME -> "相似姓名"
    DuplicateType.SAME_PHONE -> "相同电话"
    DuplicateType.SAME_EMAIL -> "相同邮箱"
    DuplicateType.FUZZY_MATCH -> "模糊匹配"
}