// 📁 feature/excelimport/ui/ExcelImportScreen.kt
package com.omnimsg.feature.excelimport.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.omnimsg.feature.excelimport.ExcelImportViewModel
import com.omnimsg.feature.excelimport.data.ExcelImportRecord
import com.omnimsg.feature.excelimport.data.ExcelPreview
import com.omnimsg.feature.excelimport.data.FieldMapping
import com.omnimsg.feature.excelimport.data.ImportConfig
import com.omnimsg.feature.excelimport.data.ImportStatus
import com.omnimsg.shared.ui.components.ExcelPreviewTable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelImportScreen(
    navController: NavController,
    viewModel: ExcelImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 文件选择器状态
    var showFilePicker by remember { mutableStateOf(false) }
    
    // 导入配置对话框
    var showImportConfig by remember { mutableStateOf(false) }
    
    // 预览对话框
    var showPreview by remember { mutableStateOf(false) }
    
    // 导入历史对话框
    var showHistory by remember { mutableStateOf(false) }
    
    // 导入报告对话框
    var selectedReportId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        // 加载导入历史
        viewModel.loadImportHistory()
    }
    
    // 处理导入结果
    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { result ->
            val message = when (result.status) {
                ImportStatus.COMPLETED -> "导入完成：成功导入${result.importedCount}个联系人"
                ImportStatus.PARTIALLY_COMPLETED -> "部分完成：成功${result.importedCount}个，失败${result.failedCount}个"
                ImportStatus.FAILED -> "导入失败：${result.errorMessage}"
                else -> "导入状态：${result.status}"
            }
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long,
                    actionLabel = if (result.reportPath != null) "查看报告" else null
                ).let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        selectedReportId = uiState.importResult?.id
                    }
                }
            }
            
            // 重置导入结果
            viewModel.resetImportResult()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excel导入联系人") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "导入历史")
                    }
                    IconButton(onClick = { showImportConfig = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "导入设置")
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Center,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("正在处理...")
                        }
                    }
                }
                
                uiState.excelPreview != null -> {
                    // 显示预览和字段映射界面
                    PreviewAndMappingScreen(
                        preview = uiState.excelPreview!!,
                        fieldMappings = uiState.fieldMappings,
                        onFieldMappingChanged = { header, field ->
                            viewModel.updateFieldMapping(header, field)
                        },
                        onImport = { config ->
                            scope.launch {
                                viewModel.performImport(config)
                            }
                        },
                        onBack = {
                            viewModel.resetPreview()
                        },
                        importProgress = uiState.importProgress,
                        isImporting = uiState.isImporting
                    )
                }
                
                else -> {
                    // 主界面：文件选择
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 上传区域
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            onClick = { showFilePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "上传文件",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "点击选择Excel文件",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "支持 .xlsx, .xls, .csv 格式",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 快速操作
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    // 下载模板
                                    scope.launch {
                                        viewModel.downloadTemplate()
                                        snackbarHostState.showSnackbar("模板下载成功")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("下载导入模板")
                            }
                            
                            OutlinedButton(
                                onClick = { showHistory = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("查看导入历史")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 使用说明
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "使用说明",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "1. 下载模板文件，按照格式填写联系人信息\n" +
                                          "2. 点击上方区域选择填写好的Excel文件\n" +
                                          "3. 系统会自动识别字段映射关系\n" +
                                          "4. 确认映射关系后开始导入\n" +
                                          "5. 查看导入报告和处理结果",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // 文件选择器
            if (showFilePicker) {
                FilePickerDialog(
                    onDismiss = { showFilePicker = false },
                    onFileSelected = { uri ->
                        showFilePicker = false
                        scope.launch {
                            viewModel.loadExcelFile(uri)
                        }
                    }
                )
            }
            
            // 导入配置对话框
            if (showImportConfig) {
                ImportConfigDialog(
                    currentConfig = uiState.importConfig,
                    onConfigUpdated = { config ->
                        viewModel.updateImportConfig(config)
                        showImportConfig = false
                    },
                    onDismiss = { showImportConfig = false }
                )
            }
            
            // 导入历史对话框
            if (showHistory) {
                ImportHistoryDialog(
                    importRecords = uiState.importHistory,
                    onRecordSelected = { record ->
                        selectedReportId = record.id
                        showHistory = false
                    },
                    onDismiss = { showHistory = false }
                )
            }
            
            // 导入报告对话框
            selectedReportId?.let { reportId ->
                ImportReportDialog(
                    recordId = reportId,
                    onDismiss = { selectedReportId = null }
                )
            }
        }
    }
}

@Composable
private fun PreviewAndMappingScreen(
    preview: ExcelPreview,
    fieldMappings: Map<String, String>,
    onFieldMappingChanged: (String, String) -> Unit,
    onImport: (ImportConfig) -> Unit,
    onBack: () -> Unit,
    importProgress: Float,
    isImporting: Boolean
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showImportConfirm by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 步骤指示器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("预览", "字段映射", "导入").forEachIndexed { index, title ->
                StepIndicator(
                    title = title,
                    isActive = index == currentStep,
                    isCompleted = index < currentStep,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        when (currentStep) {
            0 -> {
                // 数据预览
                PreviewStep(
                    preview = preview,
                    onNext = { currentStep = 1 }
                )
            }
            1 -> {
                // 字段映射
                FieldMappingStep(
                    excelHeaders = preview.headers,
                    fieldMappings = fieldMappings,
                    onFieldMappingChanged = onFieldMappingChanged,
                    onNext = { currentStep = 2 },
                    onBack = { currentStep = 0 }
                )
            }
            2 -> {
                // 导入确认
                ImportConfirmationStep(
                    preview = preview,
                    fieldMappings = fieldMappings,
                    onImport = { showImportConfirm = true },
                    onBack = { currentStep = 1 },
                    importProgress = importProgress,
                    isImporting = isImporting
                )
            }
        }
    }
    
    // 导入确认对话框
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("确认导入") },
            text = {
                Column {
                    Text("即将导入 ${preview.totalRows} 个联系人")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("请确认字段映射正确：")
                    Spacer(modifier = Modifier.height(4.dp))
                    fieldMappings.forEach { (excelHeader, systemField) ->
                        if (systemField != "unknown") {
                            Text("• $excelHeader → $systemField")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirm = false
                        val config = ImportConfig(
                            duplicateStrategy = DuplicateStrategy.MERGE,
                            validationEnabled = true,
                            autoMapping = true,
                            batchSize = 100
                        )
                        onImport(config)
                    }
                ) {
                    Text("开始导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PreviewStep(
    preview: ExcelPreview,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "数据预览",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${preview.totalRows} 行 × ${preview.headers.size} 列",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 数据统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "总行数",
                        value = preview.totalRows.toString(),
                        icon = Icons.Default.Description
                    )
                    StatCard(
                        title = "列数",
                        value = preview.headers.size.toString(),
                        icon = Icons.Default.Preview
                    )
                    StatCard(
                        title = "数据样例",
                        value = "${preview.sampleRows.size} 行",
                        icon = Icons.Default.Visibility
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 数据预览表格
                ExcelPreviewTable(
                    headers = preview.headers,
                    sampleData = preview.sampleRows,
                    modifier = Modifier.height(300.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 导航按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onNext) {
                Text("下一步：字段映射")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldMappingStep(
    excelHeaders: List<String>,
    fieldMappings: Map<String, String>,
    onFieldMappingChanged: (String, String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val systemFields = listOf(
        "unknown" to "不导入",
        "displayName" to "姓名",
        "phoneNumber" to "手机号",
        "email" to "邮箱",
        "company" to "公司",
        "position" to "职位",
        "address" to "地址",
        "birthday" to "生日",
        "notes" to "备注",
        "tags" to "标签"
    )
    
    var expandedHeader by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "字段映射配置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "请将Excel列映射到系统字段，系统会自动识别部分字段",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 字段映射列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(excelHeaders) { header ->
                        val currentMapping = fieldMappings[header] ?: "unknown"
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Excel列名
                            Text(
                                text = header,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // 映射选择器
                            ExposedDropdownMenuBox(
                                expanded = expandedHeader == header,
                                onExpandedChange = { 
                                    expandedHeader = if (expandedHeader == header) null else header 
                                }
                            ) {
                                OutlinedTextField(
                                    value = systemFields.find { it.first == currentMapping }?.second ?: "未映射",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHeader == header) },
                                    modifier = Modifier
                                        .width(150.dp)
                                        .menuAnchor()
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = expandedHeader == header,
                                    onDismissRequest = { expandedHeader = null }
                                ) {
                                    systemFields.forEach { (fieldId, displayName) ->
                                        DropdownMenuItem(
                                            text = { Text(displayName) },
                                            onClick = {
                                                onFieldMappingChanged(header, fieldId)
                                                expandedHeader = null
                                            },
                                            trailingIcon = {
                                                if (currentMapping == fieldId) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
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
        
        // 映射统计
        val mappedCount = fieldMappings.values.count { it != "unknown" }
        val unmappedCount = excelHeaders.size - mappedCount
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "映射统计",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = mappedCount.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "已映射",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = unmappedCount.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (unmappedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "未映射",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${if (excelHeaders.isNotEmpty()) (mappedCount * 100 / excelHeaders.size) else 100}%",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "完成率",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 导航按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("上一步")
            }
            
            Button(
                onClick = onNext,
                enabled = mappedCount > 0
            ) {
                Text("下一步：导入确认")
            }
        }
    }
}

@Composable
private fun ImportConfirmationStep(
    preview: ExcelPreview,
    fieldMappings: Map<String, String>,
    onImport: () -> Unit,
    onBack: () -> Unit,
    importProgress: Float,
    isImporting: Boolean
) {
    val mappedCount = fieldMappings.values.count { it != "unknown" }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "导入确认",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 导入摘要
                ImportSummaryItem(
                    icon = Icons.Default.Description,
                    title = "文件信息",
                    description = "${preview.totalRows} 行数据，${preview.headers.size} 个字段"
                )
                
                ImportSummaryItem(
                    icon = Icons.Default.CheckCircle,
                    title = "字段映射",
                    description = "$mappedCount/${preview.headers.size} 个字段已映射"
                )
                
                ImportSummaryItem(
                    icon = Icons.Default.Settings,
                    title = "导入设置",
                    description = "智能去重，数据验证，批量处理"
                )
            }
        }
        
        // 注意事项
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "注意事项",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• 导入过程可能需要几分钟时间，请勿关闭应用\n" +
                          "• 系统会自动检测并处理重复联系人\n" +
                          "• 导入失败的行会生成详细报告\n" +
                          "• 建议在WiFi环境下导入大量数据",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // 导入进度
        if (isImporting) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "导入进度",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = importProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${(importProgress * 100).toInt()}%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 导航按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isImporting
            ) {
                Text("上一步")
            }
            
            Button(
                onClick = onImport,
                enabled = !isImporting && mappedCount > 0
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导入中...")
                } else {
                    Text("开始导入")
                }
            }
        }
    }
}

@Composable
private fun ImportSummaryItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
                .clip(RoundedCornerShape(16.dp))
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

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.weight(1f),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun FilePickerDialog(
    onDismiss: () -> Unit,
    onFileSelected: (String) -> Unit
) {
    // 这里应该使用实际的文件选择器组件
    // 由于文件选择器实现依赖于具体框架，这里只显示示意
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择Excel文件") },
        text = { Text("请选择要导入的Excel文件") },
        confirmButton = {
            Button(onClick = { 
                // 模拟文件选择
                onFileSelected("file://path/to/excel.xlsx")
            }) {
                Text("选择文件")
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
private fun ImportConfigDialog(
    currentConfig: ImportConfig,
    onConfigUpdated: (ImportConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var duplicateStrategy by remember { mutableStateOf(currentConfig.duplicateStrategy) }
    var validationEnabled by remember { mutableStateOf(currentConfig.validationEnabled) }
    var autoMapping by remember { mutableStateOf(currentConfig.autoMapping) }
    var batchSize by remember { mutableStateOf(currentConfig.batchSize) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入设置") },
        text = {
            Column {
                // 重复处理策略
                Text(
                    text = "重复联系人处理",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                DuplicateStrategy.values().forEach { strategy ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = duplicateStrategy == strategy,
                            onClick = { duplicateStrategy = strategy }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (strategy) {
                                DuplicateStrategy.SKIP -> "跳过重复项"
                                DuplicateStrategy.MERGE -> "智能合并"
                                DuplicateStrategy.REPLACE -> "替换旧数据"
                                DuplicateStrategy.KEEP_BOTH -> "保留两者"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                // 其他设置
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用数据验证")
                        Text(
                            "检查数据格式和完整性",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = validationEnabled,
                        onCheckedChange = { validationEnabled = it }
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用自动字段映射")
                        Text(
                            "系统智能识别字段",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoMapping,
                        onCheckedChange = { autoMapping = it }
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                // 批处理大小
                Text(
                    text = "批处理大小",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$batchSize 条/批",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { if (batchSize > 10) batchSize -= 10 }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "减少")
                    }
                    Text("$batchSize")
                    IconButton(
                        onClick = { if (batchSize < 1000) batchSize += 10 }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "增加")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newConfig = ImportConfig(
                        duplicateStrategy = duplicateStrategy,
                        validationEnabled = validationEnabled,
                        autoMapping = autoMapping,
                        batchSize = batchSize
                    )
                    onConfigUpdated(newConfig)
                }
            ) {
                Text("保存设置")
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
private fun ImportHistoryDialog(
    importRecords: List<ExcelImportRecord>,
    onRecordSelected: (ExcelImportRecord) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入历史") },
        text = {
            if (importRecords.isEmpty()) {
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
                        Text("暂无导入历史")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(importRecords) { record ->
                        ImportHistoryItem(
                            record = record,
                            onClick = { onRecordSelected(record) }
                        )
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
private fun ImportHistoryItem(
    record: ExcelImportRecord,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态图标
                Icon(
                    imageVector = when (record.status) {
                        ImportStatus.COMPLETED -> Icons.Default.CheckCircle
                        ImportStatus.PARTIALLY_COMPLETED -> Icons.Default.Warning
                        ImportStatus.FAILED -> Icons.Default.Error
                        else -> Icons.Default.Refresh
                    },
                    contentDescription = null,
                    tint = when (record.status) {
                        ImportStatus.COMPLETED -> Color(0xFF4CAF50)
                        ImportStatus.PARTIALLY_COMPLETED -> Color(0xFFFF9800)
                        ImportStatus.FAILED -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 基本信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.fileName ?: "未知文件",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date(record.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 统计信息
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${record.importedCount}/${record.totalRows}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (record.status) {
                            ImportStatus.COMPLETED -> "已完成"
                            ImportStatus.PARTIALLY_COMPLETED -> "部分完成"
                            ImportStatus.FAILED -> "失败"
                            else -> "进行中"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (record.status) {
                            ImportStatus.COMPLETED -> Color(0xFF4CAF50)
                            ImportStatus.PARTIALLY_COMPLETED -> Color(0xFFFF9800)
                            ImportStatus.FAILED -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportReportDialog(
    recordId: String,
    onDismiss: () -> Unit
) {
    // 这里应该加载实际的导入报告
    // 由于需要数据库查询，这里只显示示意
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入报告") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在加载报告...")
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
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

// 缺少的简单组件
@Composable
private fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}