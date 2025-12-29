package com.omnimsg.data.repositories

import com.omnimsg.data.local.database.daos.ExcelImportRecordDao
import com.omnimsg.data.mappers.ExcelImportMapper
import com.omnimsg.domain.models.excel.ExcelImportRecord
import com.omnimsg.domain.models.excel.ExcelImportStatus
import com.omnimsg.domain.repositories.ExcelImportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelImportRepositoryImpl @Inject constructor(
    private val excelImportRecordDao: ExcelImportRecordDao
) : ExcelImportRepository {
    
    override suspend fun saveImportRecord(record: ExcelImportRecord): ExcelImportRecord {
        val entity = ExcelImportMapper.toEntity(record)
        val id = excelImportRecordDao.insert(entity)
        return getImportRecordById(id) ?: record
    }
    
    override suspend fun updateImportRecord(record: ExcelImportRecord): Boolean {
        val entity = ExcelImportMapper.toEntity(record)
        return excelImportRecordDao.update(entity) > 0
    }
    
    override suspend fun deleteImportRecord(importId: String): Boolean {
        return excelImportRecordDao.deleteByImportId(importId) > 0
    }
    
    override suspend fun getImportRecordById(id: Long): ExcelImportRecord? {
        return excelImportRecordDao.getById(id)?.let { ExcelImportMapper.fromEntity(it) }
    }
    
    override suspend fun getImportRecordByImportId(importId: String): ExcelImportRecord? {
        return excelImportRecordDao.getByImportId(importId)?.let { ExcelImportMapper.fromEntity(it) }
    }
    
    override fun getAllImportRecords(): Flow<List<ExcelImportRecord>> {
        return excelImportRecordDao.getAllRecordsStream()
            .map { entities -> ExcelImportMapper.fromEntities(entities) }
    }
    
    override fun getImportRecordsByUser(userId: String): Flow<List<ExcelImportRecord>> {
        return excelImportRecordDao.getRecordsByUserStream(userId)
            .map { entities -> ExcelImportMapper.fromEntities(entities) }
    }
    
    override fun getImportRecordsByStatus(status: ExcelImportStatus): Flow<List<ExcelImportRecord>> {
        return excelImportRecordDao.getRecordsByStatusStream(status)
            .map { entities -> ExcelImportMapper.fromEntities(entities) }
    }
    
    override suspend fun updateImportStatus(
        importId: String,
        status: ExcelImportStatus,
        completedAt: Date?
    ): Boolean {
        return excelImportRecordDao.updateRecordStatus(
            importId = importId,
            status = status,
            timestamp = System.currentTimeMillis()
        ) > 0
    }
    
    override suspend fun updateImportCounts(
        importId: String,
        importedRows: Int,
        failedRows: Int,
        duplicateRows: Int,
        skippedRows: Int
    ): Boolean {
        return excelImportRecordDao.updateRecordCounts(
            importId = importId,
            importedRows = importedRows,
            failedRows = failedRows,
            duplicateRows = duplicateRows,
            skippedRows = skippedRows,
            timestamp = System.currentTimeMillis()
        ) > 0
    }
    
    override suspend fun getImportStats(userId: String): ExcelImportRepository.ImportStats {
        val stats = excelImportRecordDao.getImportStats(userId)
        return ExcelImportRepository.ImportStats(
            total = stats.total,
            completed = stats.completed,
            failed = stats.failed,
            pending = stats.pending
        )
    }
    
    override suspend fun saveImportDetails(details: List<ExcelImportDetail>): List<Long> {
        val entities = details.map { ExcelImportMapper.toDetailEntity(it) }
        return excelImportRecordDao.insertDetails(entities)
    }
    
    override fun getImportDetails(importId: String): Flow<List<ExcelImportDetail>> {
        return excelImportRecordDao.getDetailsByImportIdStream(importId)
            .map { entities -> ExcelImportMapper.fromDetailEntities(entities) }
    }
    
    override suspend fun updateImportDetail(detail: ExcelImportDetail): Boolean {
        val entity = ExcelImportMapper.toDetailEntity(detail)
        return excelImportRecordDao.updateDetail(entity) > 0
    }
    
    override suspend fun cleanupOldRecords(olderThanDays: Int): Int {
        val cutoff = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000)
        return excelImportRecordDao.cleanupOldRecords(cutoff)
    }
    
    override suspend fun getImportProgress(importId: String): ExcelImportRepository.ImportProgress {
        val record = getImportRecordByImportId(importId) ?: return ExcelImportRepository.ImportProgress()
        
        return ExcelImportRepository.ImportProgress(
            importId = importId,
            totalRows = record.totalRows,
            importedRows = record.importedRows,
            failedRows = record.failedRows,
            duplicateRows = record.duplicateRows,
            skippedRows = record.skippedRows,
            progress = record.progress,
            status = record.status,
            statusMessage = getStatusMessage(record)
        )
    }
    
    private fun getStatusMessage(record: ExcelImportRecord): String {
        return when (record.status) {
            ExcelImportStatus.PENDING -> "等待导入"
            ExcelImportStatus.PROCESSING -> "正在导入..."
            ExcelImportStatus.COMPLETED -> "导入完成"
            ExcelImportStatus.FAILED -> "导入失败"
            ExcelImportStatus.CANCELLED -> "已取消"
        }
    }
}