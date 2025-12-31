// 📁 app/src/main/java/com/omnimsg/app/ui/viewmodels/ExcelImportViewModel.kt
package com.omnimsg.app.ui.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnimsg.app.data.repository.ExcelImportRepository
import com.omnimsg.app.domain.usecases.excel.*
import com.omnimsg.app.ui.events.UiEvent
import com.omnimsg.app.ui.states.ExcelImportState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExcelImportViewModel @Inject constructor(
    private val parseExcelFileUseCase: ParseExcelFileUseCase,
    private val recognizeFieldsUseCase: RecognizeFieldsUseCase,
    private val resolveDuplicatesUseCase: ResolveDuplicatesUseCase,
    private val importContactsUseCase: ImportContactsUseCase,
    private val excelImportRepository: ExcelImportRepository
) : ViewModel() {

    // UI状态
    var state by mutableStateOf(ExcelImportState())
        private set

    // 事件通道
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // 初始化时加载历史记录
    init {
        loadImportHistory()
    }

    // 加载导入历史
    private fun loadImportHistory() {
        viewModelScope.launch {
            try {
                val history = excelImportRepository.getImportHistory()
                state = state.copy(importHistory = history)
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("加载导入历史失败: ${e.message}"))
            }
        }
    }

    // 选择Excel文件
    fun selectExcelFile(file: File) {
        viewModelScope.launch {
            try {
                state = state.copy(
                    selectedFile = file,
                    currentStep = ExcelImportStep.FILE_SELECTED,
                    isLoading = true
                )

                // 解析Excel文件
                parseExcelFileUseCase(file).onSuccess { excelData ->
                    state = state.copy(
                        excelData = excelData,
                        currentStep = ExcelImportStep.PARSING_COMPLETE,
                        isLoading = false
                    )
                    
                    // 自动识别字段
                    recognizeFieldsAutomatically(excelData)
                }.onFailure { error ->
                    state = state.copy(isLoading = false)
                    sendUiEvent(UiEvent.ShowSnackbar("解析Excel文件失败: ${error.message}"))
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false)
                sendUiEvent(UiEvent.ShowSnackbar("处理文件时出错"))
            }
        }
    }

    // 自动识别字段
    private fun recognizeFieldsAutomatically(excelData: ExcelData) {
        viewModelScope.launch {
            try {
                state = state.copy(isLoading = true)
                
                recognizeFieldsUseCase(excelData).onSuccess { fieldRecognition ->
                    state = state.copy(
                        fieldRecognition = fieldRecognition,
                        currentStep = ExcelImportStep.FIELD_MAPPING,
                        isLoading = false
                    )
                }.onFailure { error ->
                    state = state.copy(isLoading = false)
                    sendUiEvent(UiEvent.ShowSnackbar("字段识别失败: ${error.message}"))
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false)
                sendUiEvent(UiEvent.ShowSnackbar("字段识别过程中出错"))
            }
        }
    }

    // 手动更新字段映射
    fun updateFieldMapping(sourceColumn: String, targetField: ContactField) {
        val currentMappings = state.fieldRecognition?.mappings?.toMutableMap() ?: mutableMapOf()
        currentMappings[sourceColumn] = targetField
        
        state.fieldRecognition?.let { currentRecognition ->
            state = state.copy(
                fieldRecognition = currentRecognition.copy(mappings = currentMappings)
            )
        }
    }

    // 分析重复联系人
    fun analyzeDuplicates() {
        viewModelScope.launch {
            try {
                val excelData = state.excelData
                val fieldRecognition = state.fieldRecognition
                
                if (excelData == null || fieldRecognition == null) {
                    sendUiEvent(UiEvent.ShowSnackbar("请先选择文件和映射字段"))
                    return@launch
                }
                
                state = state.copy(isLoading = true)
                
                // 转换Excel数据为联系人列表
                val contacts = convertToContacts(excelData, fieldRecognition.mappings)
                
                // 分析重复
                val duplicateAnalysis = resolveDuplicatesUseCase.analyze(contacts)
                
                state = state.copy(
                    duplicateAnalysis = duplicateAnalysis,
                    currentStep = ExcelImportStep.DUPLICATE_CHECK,
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false)
                sendUiEvent(UiEvent.ShowSnackbar("分析重复数据失败"))
            }
        }
    }

    // 更新重复处理策略
    fun updateDuplicateStrategy(strategy: DuplicateStrategy) {
        state = state.copy(duplicateStrategy = strategy)
    }

    // 开始导入
    fun startImport() {
        viewModelScope.launch {
            try {
                val excelData = state.excelData
                val fieldRecognition = state.fieldRecognition
                val duplicateStrategy = state.duplicateStrategy
                
                if (excelData == null || fieldRecognition == null) {
                    sendUiEvent(UiEvent.ShowSnackbar("请先完成字段映射"))
                    return@launch
                }
                
                state = state.copy(
                    isImporting = true,
                    importProgress = 0f,
                    currentStep = ExcelImportStep.IMPORTING,
                    importResult = null
                )
                
                // 转换数据
                val contacts = convertToContacts(excelData, fieldRecognition.mappings)
                
                // 执行导入
                importContactsUseCase(
                    contacts = contacts,
                    duplicateStrategy = duplicateStrategy
                ).collect { progress ->
                    when (progress) {
                        is ImportProgress.Processing -> {
                            state = state.copy(importProgress = progress.progress)
                        }
                        is ImportProgress.Completed -> {
                            // 保存导入记录
                            val importRecord = ImportRecord(
                                id = System.currentTimeMillis().toString(),
                                fileName = state.selectedFile?.name ?: "未知文件",
                                importDate = System.currentTimeMillis(),
                                totalRecords = progress.totalRecords,
                                importedRecords = progress.importedRecords,
                                skippedRecords = progress.skippedRecords,
                                duplicateRecords = progress.duplicateRecords
                            )
                            
                            excelImportRepository.saveImportRecord(importRecord)
                            
                            state = state.copy(
                                isImporting = false,
                                importProgress = 1f,
                                currentStep = ExcelImportStep.COMPLETE,
                                importResult = progress,
                                importHistory = listOf(importRecord) + state.importHistory
                            )
                            
                            sendUiEvent(UiEvent.ShowSnackbar("导入完成: ${progress.importedRecords} 条记录已导入"))
                        }
                        is ImportProgress.Error -> {
                            state = state.copy(isImporting = false)
                            sendUiEvent(UiEvent.ShowSnackbar("导入失败: ${progress.errorMessage}"))
                        }
                    }
                }
            } catch (e: Exception) {
                state = state.copy(isImporting = false)
                sendUiEvent(UiEvent.ShowSnackbar("导入过程中出错"))
            }
        }
    }

    // 重置导入状态
    fun resetImport() {
        state = ExcelImportState(importHistory = state.importHistory)
    }

    // 导出映射模板
    fun exportMappingTemplate() {
        viewModelScope.launch {
            try {
                val template = excelImportRepository.generateMappingTemplate()
                sendUiEvent(UiEvent.ShowSnackbar("映射模板已导出"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("导出模板失败"))
            }
        }
    }

    // 保存字段映射配置
    fun saveFieldMappingConfig(configName: String) {
        viewModelScope.launch {
            try {
                val mappings = state.fieldRecognition?.mappings ?: emptyMap()
                excelImportRepository.saveFieldMappingConfig(configName, mappings)
                sendUiEvent(UiEvent.ShowSnackbar("字段映射配置已保存: $configName"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("保存配置失败"))
            }
        }
    }

    // 加载字段映射配置
    fun loadFieldMappingConfig(configName: String) {
        viewModelScope.launch {
            try {
                val mappings = excelImportRepository.loadFieldMappingConfig(configName)
                state.fieldRecognition?.let { currentRecognition ->
                    state = state.copy(
                        fieldRecognition = currentRecognition.copy(mappings = mappings)
                    )
                }
                sendUiEvent(UiEvent.ShowSnackbar("配置已加载: $configName"))
            } catch (e: Exception) {
                sendUiEvent(UiEvent.ShowSnackbar("加载配置失败"))
            }
        }
    }

    // 发送UI事件
    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    // 辅助函数：将Excel数据转换为联系人列表
    private fun convertToContacts(
        excelData: ExcelData,
        fieldMappings: Map<String, ContactField>
    ): List<Contact> {
        // TODO: 实现数据转换逻辑
        return emptyList()
    }
}

// Excel数据模型
data class ExcelData(
    val fileName: String,
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val totalRows: Int,
    val sheetCount: Int
)

// 字段识别结果
data class FieldRecognition(
    val mappings: Map<String, ContactField>, // 源列名 -> 目标字段
    val confidenceScores: Map<String, Float>, // 每个映射的置信度
    val suggestions: List<FieldSuggestion>,
    val overallConfidence: Float
)

// 字段建议
data class FieldSuggestion(
    val sourceColumn: String,
    val suggestedField: ContactField,
    val confidence: Float,
    val reason: String
)

// 联系人字段枚举
enum class ContactField {
    NAME,
    PHONE,
    EMAIL,
    COMPANY,
    POSITION,
    ADDRESS,
    BIRTHDAY,
    NOTES,
    TAGS,
    CUSTOM_FIELD_1,
    CUSTOM_FIELD_2,
    CUSTOM_FIELD_3,
    UNMAPPED // 未映射字段
}

// 重复分析结果
data class DuplicateAnalysis(
    val totalContacts: Int,
    val uniqueContacts: Int,
    val duplicateGroups: List<DuplicateGroup>,
    val duplicateCount: Int,
    val confidence: Float
)

// 重复组
data class DuplicateGroup(
    val id: String,
    val contacts: List<Contact>,
    val duplicateType: DuplicateType,
    val confidence: Float,
    val suggestedResolution: DuplicateResolution?
)

// 重复类型
enum class DuplicateType {
    EXACT_MATCH,        // 完全匹配
    SIMILAR_NAME,       // 相似姓名
    SAME_PHONE,         // 相同电话
    SAME_EMAIL,         // 相同邮箱
    FUZZY_MATCH         // 模糊匹配
}

// 重复解决策略
enum class DuplicateStrategy {
    KEEP_ALL,           // 保留所有
    KEEP_FIRST,         // 保留第一条
    KEEP_LAST,          // 保留最后一条
    MERGE,              // 合并数据
    SKIP_ALL,           // 跳过所有重复
    PROMPT              // 手动选择
}

// 重复解决方案
data class DuplicateResolution(
    val groupId: String,
    val action: DuplicateAction,
    val selectedContactId: String? = null,
    val mergedContact: Contact? = null
)

// 重复处理动作
enum class DuplicateAction {
    KEEP_FIRST,
    KEEP_LAST,
    MERGE,
    SKIP,
    KEEP_BOTH
}

// 导入进度
sealed class ImportProgress {
    data class Processing(val progress: Float) : ImportProgress()
    data class Completed(
        val totalRecords: Int,
        val importedRecords: Int,
        val skippedRecords: Int,
        val duplicateRecords: Int
    ) : ImportProgress()
    data class Error(val errorMessage: String) : ImportProgress()
}

// 导入记录
data class ImportRecord(
    val id: String,
    val fileName: String,
    val importDate: Long,
    val totalRecords: Int,
    val importedRecords: Int,
    val skippedRecords: Int,
    val duplicateRecords: Int
)

// Excel导入步骤
enum class ExcelImportStep {
    FILE_SELECTION,     // 选择文件
    FILE_SELECTED,      // 文件已选择
    PARSING_COMPLETE,   // 解析完成
    FIELD_MAPPING,      // 字段映射
    DUPLICATE_CHECK,    // 重复检查
    IMPORTING,          // 导入中
    COMPLETE            // 完成
}