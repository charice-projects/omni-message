package com.omnimsg.data.local.database.daos

import androidx.room.*
import com.omnimsg.data.local.database.entities.ExcelImportDetailEntity
import com.omnimsg.data.local.database.entities.ExcelImportRecordEntity
import com.omnimsg.domain.models.ExcelImportStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcelImportRecordDao {
    
    // 插入操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExcelImportRecordEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: ExcelImportDetailEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<ExcelImportDetailEntity>): List<Long>
    
    // 更新操作
    @Update
    suspend fun update(record: ExcelImportRecordEntity): Int
    
    @Update
    suspend fun updateDetail(detail: ExcelImportDetailEntity): Int
    
    // 查询操作
    @Query("SELECT * FROM excel_import_records WHERE id = :id")
    suspend fun getById(id: Long): ExcelImportRecordEntity?
    
    @Query("SELECT * FROM excel_import_records WHERE import_id = :importId")
    suspend fun getByImportId(importId: String): ExcelImportRecordEntity?
    
    // 流式查询
    @Query("SELECT * FROM excel_import_records ORDER BY created_at DESC")
    fun getAllRecordsStream(): Flow<List<ExcelImportRecordEntity>>
    
    @Query("SELECT * FROM excel_import_records WHERE user_id = :userId ORDER BY created_at DESC")
    fun getRecordsByUserStream(userId: String): Flow<List<ExcelImportRecordEntity>>
    
    @Query("SELECT * FROM excel_import_records WHERE status = :status ORDER BY created_at DESC")
    fun getRecordsByStatusStream(status: ExcelImportStatus): Flow<List<ExcelImportRecordEntity>>
    
    // 详情查询
    @Query("SELECT * FROM excel_import_details WHERE import_id = :importId ORDER BY row_index ASC")
    fun getDetailsByImportIdStream(importId: String): Flow<List<ExcelImportDetailEntity>>
    
    @Query("SELECT * FROM excel_import_details WHERE import_id = :importId AND status = :status")
    suspend fun getDetailsByStatus(importId: String, status: ExcelImportStatus): List<ExcelImportDetailEntity>
    
    // 批量更新
    @Query("""
        UPDATE excel_import_records 
        SET status = :status, 
            updated_at = :timestamp,
            completed_at = CASE WHEN :status = 'COMPLETED' OR :status = 'FAILED' THEN :timestamp ELSE completed_at END
        WHERE import_id = :importId
    """)
    suspend fun updateRecordStatus(
        importId: String,
        status: ExcelImportStatus,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    @Query("""
        UPDATE excel_import_records 
        SET imported_rows = :importedRows,
            failed_rows = :failedRows,
            duplicate_rows = :duplicateRows,
            skipped_rows = :skippedRows,
            updated_at = :timestamp
        WHERE import_id = :importId
    """)
    suspend fun updateRecordCounts(
        importId: String,
        importedRows: Int,
        failedRows: Int,
        duplicateRows: Int,
        skippedRows: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    @Query("""
        UPDATE excel_import_details 
        SET status = :status,
            error_message = :errorMessage,
            imported_contact_id = :contactId,
            processed_at = :timestamp
        WHERE id = :id
    """)
    suspend fun updateDetailStatus(
        id: Long,
        status: ExcelImportStatus,
        errorMessage: String? = null,
        contactId: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM excel_import_records WHERE user_id = :userId")
    suspend fun getRecordCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM excel_import_details WHERE import_id = :importId AND status = :status")
    suspend fun getDetailCountByStatus(importId: String, status: ExcelImportStatus): Int
    
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
            SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending
        FROM excel_import_records 
        WHERE user_id = :userId
    """)
    suspend fun getImportStats(userId: String): ImportStats
    
    data class ImportStats(
        val total: Int,
        val completed: Int,
        val failed: Int,
        val pending: Int
    )
    
    // 删除操作
    @Query("DELETE FROM excel_import_records WHERE import_id = :importId")
    suspend fun deleteByImportId(importId: String): Int
    
    @Query("DELETE FROM excel_import_details WHERE import_id = :importId")
    suspend fun deleteDetailsByImportId(importId: String): Int
    
    // 清理旧记录
    @Query("DELETE FROM excel_import_records WHERE created_at < :timestamp AND status IN ('COMPLETED', 'FAILED')")
    suspend fun cleanupOldRecords(timestamp: Long): Int
}