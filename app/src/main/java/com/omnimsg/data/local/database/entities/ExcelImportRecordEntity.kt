package com.omnimsg.data.local.database.entities

import androidx.room.*
import com.omnimsg.domain.models.ExcelImportStatus
import com.omnimsg.domain.models.ExcelImportStrategy
import java.util.*

@Entity(
    tableName = "excel_import_records",
    indices = [
        Index(value = ["import_id"], unique = true),
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["user_id"])
    ]
)
data class ExcelImportRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "import_id")
    val importId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "file_format")
    val fileFormat: String, // xlsx, xls, csv
    
    @ColumnInfo(name = "total_rows")
    val totalRows: Int = 0,
    
    @ColumnInfo(name = "imported_rows")
    val importedRows: Int = 0,
    
    @ColumnInfo(name = "failed_rows")
    val failedRows: Int = 0,
    
    @ColumnInfo(name = "duplicate_rows")
    val duplicateRows: Int = 0,
    
    @ColumnInfo(name = "skipped_rows")
    val skippedRows: Int = 0,
    
    @ColumnInfo(name = "status")
    val status: ExcelImportStatus = ExcelImportStatus.PENDING,
    
    @ColumnInfo(name = "import_strategy")
    val importStrategy: ExcelImportStrategy = ExcelImportStrategy.SMART_MERGE,
    
    @ColumnInfo(name = "field_mappings")
    val fieldMappings: Map<String, String>, // Excel列名 -> 系统字段名
    
    @ColumnInfo(name = "ai_suggested_mappings")
    val aiSuggestedMappings: Map<String, String>, // AI建议的映射
    
    @ColumnInfo(name = "validation_rules")
    val validationRules: List<String>, // 使用的验证规则
    
    @ColumnInfo(name = "duplicate_resolution")
    val duplicateResolution: Map<String, String>, // 重复处理方式
    
    @ColumnInfo(name = "error_messages")
    val errorMessages: List<String>, // 错误信息
    
    @ColumnInfo(name = "import_duration")
    val importDuration: Long = 0, // 导入耗时（毫秒）
    
    @ColumnInfo(name = "started_at")
    val startedAt: Long?,
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "metadata")
    val metadata: Map<String, String> = emptyMap()
) {
    val successRate: Float
        get() = if (totalRows > 0) {
            importedRows.toFloat() / totalRows.toFloat() * 100f
        } else {
            0f
        }
    
    val processingTime: Long
        get() = if (startedAt != null && completedAt != null) {
            completedAt - startedAt
        } else {
            0
        }
    
    fun getProgress(): Float {
        return if (totalRows > 0) {
            (importedRows + failedRows + duplicateRows + skippedRows).toFloat() / totalRows.toFloat()
        } else {
            0f
        }
    }
}

@Entity(
    tableName = "excel_import_details",
    indices = [
        Index(value = ["import_id"]),
        Index(value = ["row_index"]),
        Index(value = ["status"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExcelImportRecordEntity::class,
            parentColumns = ["import_id"],
            childColumns = ["import_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExcelImportDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "import_id")
    val importId: String,
    
    @ColumnInfo(name = "row_index")
    val rowIndex: Int,
    
    @ColumnInfo(name = "original_data")
    val originalData: Map<String, String>, // Excel原始数据
    
    @ColumnInfo(name = "mapped_data")
    val mappedData: Map<String, String>, // 映射后的数据
    
    @ColumnInfo(name = "validation_errors")
    val validationErrors: List<String>,
    
    @ColumnInfo(name = "duplicate_contact_id")
    val duplicateContactId: String?, // 重复的联系人ID
    
    @ColumnInfo(name = "duplicate_score")
    val duplicateScore: Float = 0f, // 重复匹配分数
    
    @ColumnInfo(name = "resolution_action")
    val resolutionAction: String, // skip, merge, replace, keep_both
    
    @ColumnInfo(name = "imported_contact_id")
    val importedContactId: String?, // 导入后的联系人ID
    
    @ColumnInfo(name = "status")
    val status: ExcelImportStatus = ExcelImportStatus.PENDING,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
    
    @ColumnInfo(name = "processed_at")
    val processedAt: Long?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)