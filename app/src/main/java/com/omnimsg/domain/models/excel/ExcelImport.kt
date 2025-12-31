package com.omnimsg.domain.models.excel

import java.util.*

data class ExcelImportRecord(
    val id: Long = 0,
    val importId: String = UUID.randomUUID().toString(),
    val userId: String,
    val fileName: String,
    val fileUri: String,
    val fileSize: Long,
    val fileFormat: ExcelFileFormat,
    val totalRows: Int = 0,
    val importedRows: Int = 0,
    val failedRows: Int = 0,
    val duplicateRows: Int = 0,
    val skippedRows: Int = 0,
    val status: ExcelImportStatus = ExcelImportStatus.PENDING,
    val importStrategy: ExcelImportStrategy = ExcelImportStrategy.SMART_MERGE,
    val fieldMappings: Map<String, String> = emptyMap(),
    val aiSuggestedMappings: Map<String, String> = emptyMap(),
    val validationRules: List<ValidationRule> = emptyList(),
    val duplicateResolution: Map<String, DuplicateResolution> = emptyMap(),
    val errorMessages: List<String> = emptyList(),
    val importDuration: Long = 0,
    val startedAt: Date? = null,
    val completedAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val metadata: Map<String, String> = emptyMap()
) {
    val successRate: Float
        get() = if (totalRows > 0) {
            importedRows.toFloat() / totalRows.toFloat() * 100f
        } else {
            0f
        }
    
    val progress: Float
        get() = if (totalRows > 0) {
            (importedRows + failedRows + duplicateRows + skippedRows).toFloat() / totalRows.toFloat()
        } else {
            0f
        }
    
    val isInProgress: Boolean
        get() = status == ExcelImportStatus.PROCESSING
    
    val isCompleted: Boolean
        get() = status == ExcelImportStatus.COMPLETED || status == ExcelImportStatus.FAILED
    
    fun startProcessing(): ExcelImportRecord {
        return copy(
            status = ExcelImportStatus.PROCESSING,
            startedAt = Date(),
            updatedAt = Date()
        )
    }
    
    fun completeProcessing(
        importedRows: Int,
        failedRows: Int,
        duplicateRows: Int,
        skippedRows: Int,
        duration: Long
    ): ExcelImportRecord {
        val finalStatus = if (failedRows == totalRows) {
            ExcelImportStatus.FAILED
        } else {
            ExcelImportStatus.COMPLETED
        }
        
        return copy(
            status = finalStatus,
            importedRows = importedRows,
            failedRows = failedRows,
            duplicateRows = duplicateRows,
            skippedRows = skippedRows,
            importDuration = duration,
            completedAt = Date(),
            updatedAt = Date()
        )
    }
    
    fun failProcessing(errorMessages: List<String>): ExcelImportRecord {
        return copy(
            status = ExcelImportStatus.FAILED,
            errorMessages = errorMessages,
            completedAt = Date(),
            updatedAt = Date()
        )
    }
}

data class ExcelImportDetail(
    val id: Long = 0,
    val importId: String,
    val rowIndex: Int,
    val originalData: Map<String, String> = emptyMap(),
    val mappedData: Map<String, String> = emptyMap(),
    val validationErrors: List<String> = emptyList(),
    val duplicateContactId: String? = null,
    val duplicateScore: Float = 0f,
    val resolutionAction: DuplicateResolution = DuplicateResolution.SKIP,
    val importedContactId: String? = null,
    val status: ExcelImportStatus = ExcelImportStatus.PENDING,
    val errorMessage: String? = null,
    val processedAt: Date? = null,
    val createdAt: Date = Date()
) {
    val isValid: Boolean
        get() = validationErrors.isEmpty()
    
    val isDuplicate: Boolean
        get() = duplicateContactId != null
    
    val isImported: Boolean
        get() = importedContactId != null
    
    fun markAsValid(): ExcelImportDetail {
        return copy(validationErrors = emptyList())
    }
    
    fun addValidationError(error: String): ExcelImportDetail {
        return copy(validationErrors = validationErrors + error)
    }
    
    fun markAsDuplicate(contactId: String, score: Float): ExcelImportDetail {
        return copy(
            duplicateContactId = contactId,
            duplicateScore = score
        )
    }
    
    fun markAsProcessed(
        action: DuplicateResolution,
        contactId: String? = null
    ): ExcelImportDetail {
        return copy(
            resolutionAction = action,
            importedContactId = contactId,
            status = ExcelImportStatus.COMPLETED,
            processedAt = Date()
        )
    }
    
    fun markAsFailed(error: String): ExcelImportDetail {
        return copy(
            status = ExcelImportStatus.FAILED,
            errorMessage = error,
            processedAt = Date()
        )
    }
}

data class ExcelImportRequest(
    val fileUri: String,
    val fileName: String,
    val fileSize: Long,
    val fileFormat: ExcelFileFormat,
    val importStrategy: ExcelImportStrategy = ExcelImportStrategy.SMART_MERGE,
    val fieldMappings: Map<String, String>? = null, // 如果为null则使用AI自动识别
    val validationRules: List<ValidationRule> = defaultValidationRules(),
    val duplicateResolution: Map<String, DuplicateResolution> = emptyMap(),
    val batchSize: Int = 100,
    val userId: String
) {
    companion object {
        fun defaultValidationRules(): List<ValidationRule> {
            return listOf(
                ValidationRule.REQUIRED_FIELDS,
                ValidationRule.VALID_PHONE_FORMAT,
                ValidationRule.VALID_EMAIL_FORMAT
            )
        }
    }
}

data class ExcelImportResult(
    val importId: String,
    val totalRows: Int,
    val importedRows: Int,
    val failedRows: Int,
    val duplicateRows: Int,
    val skippedRows: Int,
    val successRate: Float,
    val duration: Long,
    val fieldMappings: Map<String, String>,
    val validationResults: Map<String, List<String>>, // 字段 -> 错误列表
    val duplicateResolutions: List<DuplicateResolutionResult>,
    val failedRowsDetails: List<FailedRowDetail>,
    val metadata: Map<String, String> = emptyMap()
) {
    val isSuccess: Boolean
        get() = failedRows == 0 && importedRows > 0
}

data class DuplicateResolutionResult(
    val rowIndex: Int,
    val existingContactId: String,
    val duplicateScore: Float,
    val resolution: DuplicateResolution,
    val importedContactId: String? = null
)

data class FailedRowDetail(
    val rowIndex: Int,
    val error: String,
    val data: Map<String, String>
)

data class ExcelFileInfo(
    val uri: String,
    val fileName: String,
    val fileSize: Long,
    val format: ExcelFileFormat,
    val sheetCount: Int,
    val sheetNames: List<String>,
    val firstSheetData: List<List<String>>? = null, // 预览数据
    val columnCount: Int,
    val rowCount: Int,
    val headers: List<String>? = null
)

enum class ExcelFileFormat {
    XLSX, XLS, CSV
}

enum class ExcelImportStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}

enum class ExcelImportStrategy {
    SMART_MERGE,      // 智能合并重复项
    REPLACE_ALL,      // 替换所有
    KEEP_BOTH,        // 保留两者
    SKIP_DUPLICATES   // 跳过重复项
}

enum class DuplicateResolution {
    SKIP,        // 跳过
    MERGE,       // 合并
    REPLACE,     // 替换
    KEEP_BOTH    // 保留两者
}

enum class ValidationRule {
    REQUIRED_FIELDS,      // 必填字段检查
    VALID_PHONE_FORMAT,   // 手机号格式验证
    VALID_EMAIL_FORMAT,   // 邮箱格式验证
    UNIQUE_PHONE,         // 手机号唯一性
    UNIQUE_EMAIL,         // 邮箱唯一性
    DATE_FORMAT,          // 日期格式验证
    CUSTOM                // 自定义验证
}

data class FieldMapping(
    val excelColumn: String,
    val systemField: String,
    val confidence: Float = 0f, // AI识别置信度
    val sampleData: List<String> = emptyList(),
    val dataType: DataType = DataType.STRING
)

enum class DataType {
    STRING, NUMBER, PHONE, EMAIL, DATE, BOOLEAN
}

data class ExcelPreview(
    val headers: List<String>,
    val sampleRows: List<Map<String, String>>, // 前几行数据
    val dataTypes: Map<String, DataType>, // 列数据类型推测
    val columnStats: Map<String, ColumnStats> // 列统计信息
)

data class ColumnStats(
    val totalCount: Int,
    val nonEmptyCount: Int,
    val uniqueCount: Int,
    val mostFrequentValue: String?,
    val dataPattern: DataPattern?,
    val suggestedDataType: DataType
)

enum class DataPattern {
    PHONE_NUMBER, EMAIL, DATE, URL, NUMBER, TEXT
}