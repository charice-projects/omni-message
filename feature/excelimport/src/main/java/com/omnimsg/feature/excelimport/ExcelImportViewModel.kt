// 📁 feature/excelimport/ExcelImportViewModel.kt
package com.omnimsg.feature.excelimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnimsg.feature.excelimport.data.ExcelImportRecord
import com.omnimsg.feature.excelimport.data.ExcelPreview
import com.omnimsg.feature.excelimport.data.FieldMapping
import com.omnimsg.feature.excelimport.data.ImportConfig
import com.omnimsg.feature.excelimport.data.ImportStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExcelImportViewModel @Inject constructor(
    private val context: Context,
    private val excelImportEngine: ExcelImportEngine,
    private val excelImportRecordDao: ExcelImportRecordDao
) : ViewModel() {
    
    data class UiState(
        val isLoading: Boolean = false,
        val excelPreview: ExcelPreview? = null,
        val fieldMappings: Map<String, String> = emptyMap(),
        val importConfig: ImportConfig = ImportConfig(),
        val importProgress: Float = 0f,
        val isImporting: Boolean = false,
        val importResult: ExcelImportRecord? = null,
        val importHistory: List<ExcelImportRecord> = emptyList(),
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * 加载Excel文件并预览
     */
    fun loadExcelFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val preview = excelImportEngine.parseExcelFile(uri, _uiState.value.importConfig)
                val autoMappings = excelImportEngine.recognizeFields(preview)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        excelPreview = preview,
                        fieldMappings = autoMappings
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载Excel文件失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 更新字段映射
     */
    fun updateFieldMapping(excelHeader: String, systemField: String) {
        _uiState.update { state ->
            val newMappings = state.fieldMappings.toMutableMap()
            newMappings[excelHeader] = systemField
            state.copy(fieldMappings = newMappings)
        }
    }
    
    /**
     * 更新导入配置
     */
    fun updateImportConfig(config: ImportConfig) {
        _uiState.update { it.copy(importConfig = config) }
    }
    
    /**
     * 执行导入
     */
    fun performImport(config: ImportConfig) {
        viewModelScope.launch {
            val preview = _uiState.value.excelPreview
            val fieldMappings = _uiState.value.fieldMappings
            
            if (preview == null || fieldMappings.isEmpty()) {
                _uiState.update { it.copy(error = "请先加载Excel文件并配置字段映射") }
                return@launch
            }
            
            _uiState.update { it.copy(isImporting = true, importProgress = 0f) }
            
            try {
                // 创建导入记录
                val importRecord = ExcelImportRecord(
                    id = 0,
                    importId = generateImportId(),
                    fileName = preview.fileName,
                    totalRows = preview.totalRows,
                    status = ImportStatus.IN_PROGRESS,
                    fieldMappings = fieldMappings,
                    config = config,
                    createdAt = System.currentTimeMillis()
                )
                
                val recordId = excelImportRecordDao.insert(importRecord)
                
                // 执行导入
                val result = excelImportEngine.performImport(
                    importId = importRecord.importId,
                    preview = preview,
                    fieldMappings = fieldMappings,
                    config = config,
                    onProgress = { progress ->
                        _uiState.update { it.copy(importProgress = progress) }
                    }
                )
                
                // 更新UI状态
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = 1f,
                        importResult = result,
                        excelPreview = null,
                        fieldMappings = emptyMap()
                    )
                }
                
                // 重新加载历史
                loadImportHistory()
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "导入失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 加载导入历史
     */
    fun loadImportHistory() {
        viewModelScope.launch {
            try {
                val history = excelImportRecordDao.getAllRecords()
                    .sortedByDescending { it.createdAt }
                
                _uiState.update { it.copy(importHistory = history) }
            } catch (e: Exception) {
                // 历史加载失败不影响主流程
            }
        }
    }
    
    /**
     * 重置预览
     */
    fun resetPreview() {
        _uiState.update {
            it.copy(
                excelPreview = null,
                fieldMappings = emptyMap(),
                importProgress = 0f
            )
        }
    }
    
    /**
     * 重置导入结果
     */
    fun resetImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }
    
    /**
     * 下载模板文件
     */
    fun downloadTemplate() {
        viewModelScope.launch {
            try {
                excelImportEngine.generateTemplateFile()
                // 这里应该触发文件下载
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "下载模板失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 生成导入ID
     */
    private fun generateImportId(): String {
        return "import_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}