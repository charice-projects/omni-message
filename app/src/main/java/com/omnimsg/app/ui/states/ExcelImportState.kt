// 📁 app/src/main/java/com/omnimsg/app/ui/states/ExcelImportState.kt
package com.omnimsg.app.ui.states

import com.omnimsg.app.ui.viewmodels.*
import java.io.File

data class ExcelImportState(
    // 文件选择
    val selectedFile: File? = null,
    val excelData: ExcelData? = null,
    
    // 字段映射
    val fieldRecognition: FieldRecognition? = null,
    val selectedSheetIndex: Int = 0,
    val hasHeaderRow: Boolean = true,
    
    // 重复处理
    val duplicateAnalysis: DuplicateAnalysis? = null,
    val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.MERGE,
    val selectedDuplicateResolutions: Map<String, DuplicateResolution> = emptyMap(),
    
    // 导入状态
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val importResult: ImportProgress.Completed? = null,
    
    // 历史记录
    val importHistory: List<ImportRecord> = emptyList(),
    val selectedHistoryRecord: ImportRecord? = null,
    
    // 操作状态
    val isLoading: Boolean = false,
    val isAnalyzingDuplicates: Boolean = false,
    val isExportingTemplate: Boolean = false,
    
    // UI状态
    val currentStep: ExcelImportStep = ExcelImportStep.FILE_SELECTION,
    val showFilePicker: Boolean = false,
    val showFieldMappingDialog: Boolean = false,
    val showDuplicateResolutionDialog: Boolean = false,
    val showImportHistoryDialog: Boolean = false,
    val showSaveMappingDialog: Boolean = false,
    val showLoadMappingDialog: Boolean = false,
    
    // 错误状态
    val errorMessage: String? = null
)