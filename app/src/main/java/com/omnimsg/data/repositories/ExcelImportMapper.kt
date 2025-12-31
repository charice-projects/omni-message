package com.omnimsg.data.mappers

import com.omnimsg.data.local.database.entities.ExcelImportDetailEntity
import com.omnimsg.data.local.database.entities.ExcelImportRecordEntity
import com.omnimsg.domain.models.excel.*
import java.util.*

object ExcelImportMapper {
    
    // 记录映射
    fun toEntity(record: ExcelImportRecord): ExcelImportRecordEntity {
        return ExcelImportRecordEntity(
            id = record.id,
            importId = record.importId,
            userId = record.userId,
            fileName = record.fileName,
            fileUri = record.fileUri,
            fileSize = record.fileSize,
            fileFormat = record.fileFormat.name,
            totalRows = record.totalRows,
            importedRows = record.importedRows,
            failedRows = record.failedRows,
            duplicateRows = record.duplicateRows,
            skippedRows = record.skippedRows,
            status = record.status,
            importStrategy = record.importStrategy,
            fieldMappings = record.fieldMappings,
            aiSuggestedMappings = record.aiSuggestedMappings,
            validationRules = record.validationRules.map { it.name },
            duplicateResolution = record.duplicateResolution.mapValues { it.value.name },
            errorMessages = record.errorMessages,
            importDuration = record.importDuration,
            startedAt = record.startedAt?.time,
            completedAt = record.completedAt?.time,
            createdAt = record.createdAt.time,
            updatedAt = record.updatedAt.time,
            metadata = record.metadata
        )
    }
    
    fun fromEntity(entity: ExcelImportRecordEntity): ExcelImportRecord {
        return ExcelImportRecord(
            id = entity.id,
            importId = entity.importId,
            userId = entity.userId,
            fileName = entity.fileName,
            fileUri = entity.fileUri,
            fileSize = entity.fileSize,
            fileFormat = ExcelFileFormat.valueOf(entity.fileFormat),
            totalRows = entity.totalRows,
            importedRows = entity.importedRows,
            failedRows = entity.failedRows,
            duplicateRows = entity.duplicateRows,
            skippedRows = entity.skippedRows,
            status = entity.status,
            importStrategy = entity.importStrategy,
            fieldMappings = entity.fieldMappings,
            aiSuggestedMappings = entity.aiSuggestedMappings,
            validationRules = entity.validationRules.map { ValidationRule.valueOf(it) },
            duplicateResolution = entity.duplicateResolution.mapValues { DuplicateResolution.valueOf(it.value) },
            errorMessages = entity.errorMessages,
            importDuration = entity.importDuration,
            startedAt = entity.startedAt?.let { Date(it) },
            completedAt = entity.completedAt?.let { Date(it) },
            createdAt = Date(entity.createdAt),
            updatedAt = Date(entity.updatedAt),
            metadata = entity.metadata
        )
    }
    
    fun fromEntities(entities: List<ExcelImportRecordEntity>): List<ExcelImportRecord> {
        return entities.map { fromEntity(it) }
    }
    
    // 详情映射
    fun toDetailEntity(detail: ExcelImportDetail): ExcelImportDetailEntity {
        return ExcelImportDetailEntity(
            id = detail.id,
            importId = detail.importId,
            rowIndex = detail.rowIndex,
            originalData = detail.originalData,
            mappedData = detail.mappedData,
            validationErrors = detail.validationErrors,
            duplicateContactId = detail.duplicateContactId,
            duplicateScore = detail.duplicateScore,
            resolutionAction = detail.resolutionAction.name,
            importedContactId = detail.importedContactId,
            status = detail.status,
            errorMessage = detail.errorMessage,
            processedAt = detail.processedAt?.time,
            createdAt = detail.createdAt.time
        )
    }
    
    fun fromDetailEntity(entity: ExcelImportDetailEntity): ExcelImportDetail {
        return ExcelImportDetail(
            id = entity.id,
            importId = entity.importId,
            rowIndex = entity.rowIndex,
            originalData = entity.originalData,
            mappedData = entity.mappedData,
            validationErrors = entity.validationErrors,
            duplicateContactId = entity.duplicateContactId,
            duplicateScore = entity.duplicateScore,
            resolutionAction = DuplicateResolution.valueOf(entity.resolutionAction),
            importedContactId = entity.importedContactId,
            status = entity.status,
            errorMessage = entity.errorMessage,
            processedAt = entity.processedAt?.let { Date(it) },
            createdAt = Date(entity.createdAt)
        )
    }
    
    fun fromDetailEntities(entities: List<ExcelImportDetailEntity>): List<ExcelImportDetail> {
        return entities.map { fromDetailEntity(it) }
    }
    
    // DTO映射
    fun fromImportRequestToRecord(request: ExcelImportRequest): ExcelImportRecord {
        return ExcelImportRecord(
            userId = request.userId,
            fileName = request.fileName,
            fileUri = request.fileUri,
            fileSize = request.fileSize,
            fileFormat = request.fileFormat,
            importStrategy = request.importStrategy,
            validationRules = request.validationRules,
            duplicateResolution = request.duplicateResolution,
            createdAt = Date(),
            updatedAt = Date()
        )
    }
    
    fun fromResultToRecord(result: ExcelImportResult, request: ExcelImportRequest): ExcelImportRecord {
        return ExcelImportRecord(
            importId = result.importId,
            userId = request.userId,
            fileName = request.fileName,
            fileUri = request.fileUri,
            fileSize = request.fileSize,
            fileFormat = request.fileFormat,
            totalRows = result.totalRows,
            importedRows = result.importedRows,
            failedRows = result.failedRows,
            duplicateRows = result.duplicateRows,
            skippedRows = result.skippedRows,
            status = if (result.isSuccess) ExcelImportStatus.COMPLETED else ExcelImportStatus.FAILED,
            importStrategy = request.importStrategy,
            fieldMappings = result.fieldMappings,
            validationRules = request.validationRules,
            duplicateResolution = request.duplicateResolution,
            errorMessages = result.failedRowsDetails.map { it.error },
            importDuration = result.duration,
            startedAt = Date(System.currentTimeMillis() - result.duration),
            completedAt = Date(),
            metadata = result.metadata
        )
    }
}