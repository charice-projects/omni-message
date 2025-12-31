package com.omnimsg.feature.excelimport

import com.omnimsg.data.repositories.ContactRepository
import com.omnimsg.data.repositories.ExcelImportRepository
import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.models.ContactSource
import com.omnimsg.domain.models.excel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelImportEngine @Inject constructor(
    private val excelParser: ExcelParser,
    private val aiFieldRecognizer: AIFieldRecognizer,
    private val contactRepository: ContactRepository,
    private val excelImportRepository: ExcelImportRepository,
    private val duplicateDetector: ContactDuplicateDetector,
    private val dataValidator: ContactDataValidator
) {
    
    companion object {
        private const val TAG = "ExcelImportEngine"
        private const val BATCH_SIZE = 50
    }
    
    data class ImportConfig(
        val fileUri: String,
        val fileName: String,
        val fileSize: Long,
        val fileFormat: ExcelFileFormat,
        val sheetIndex: Int = 0,
        val hasHeaders: Boolean = true,
        val importStrategy: ExcelImportStrategy = ExcelImportStrategy.SMART_MERGE,
        val fieldMappings: Map<String, String>? = null, // null表示使用AI识别
        val validationRules: List<ValidationRule> = defaultValidationRules(),
        val duplicateResolutions: Map<String, DuplicateResolution> = emptyMap(),
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
    
    data class ImportProgress(
        val importId: String,
        val currentStep: ImportStep,
        val progress: Float, // 0-1
        val processedRows: Int,
        val totalRows: Int,
        val importedRows: Int,
        val failedRows: Int,
        val duplicateRows: Int,
        val skippedRows: Int,
        val currentBatch: Int,
        val totalBatches: Int,
        val statusMessage: String
    ) {
        val isComplete: Boolean
            get() = currentStep == ImportStep.COMPLETED || currentStep == ImportStep.FAILED
    }
    
    enum class ImportStep {
        INITIALIZING,
        PARSING_FILE,
        ANALYZING_DATA,
        RECOGNIZING_FIELDS,
        VALIDATING_DATA,
        DETECTING_DUPLICATES,
        IMPORTING_DATA,
        COMPLETED,
        FAILED
    }
    
    // 进度状态流
    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress
    
    private var currentImportJob: Job? = null
    
    /**
     * 开始导入Excel文件
     */
    fun startImport(config: ImportConfig): String {
        // 取消任何正在进行的导入
        cancelCurrentImport()
        
        val importId = UUID.randomUUID().toString()
        
        currentImportJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                performImport(importId, config)
            } catch (e: Exception) {
                updateProgress(
                    importId = importId,
                    step = ImportStep.FAILED,
                    progress = 1f,
                    statusMessage = "Import failed: ${e.message}"
                )
                
                // 保存失败记录
                saveFailedImport(importId, config, e)
            }
        }
        
        return importId
    }
    
    /**
     * 取消当前导入
     */
    fun cancelCurrentImport() {
        currentImportJob?.cancel()
        currentImportJob = null
    }
    
    /**
     * 执行导入过程
     */
    private suspend fun performImport(importId: String, config: ImportConfig) {
        updateProgress(
            importId = importId,
            step = ImportStep.INITIALIZING,
            progress = 0f,
            statusMessage = "Initializing import..."
        )
        
        // 1. 创建导入记录
        val importRecord = createImportRecord(importId, config)
        excelImportRepository.saveImportRecord(importRecord)
        
        // 2. 解析Excel文件
        updateProgress(
            importId = importId,
            step = ImportStep.PARSING_FILE,
            progress = 0.1f,
            statusMessage = "Parsing Excel file..."
        )
        
        val parseResult = excelParser.parseExcelFile(
            uri = android.net.Uri.parse(config.fileUri),
            sheetIndex = config.sheetIndex,
            hasHeaders = config.hasHeaders,
            previewOnly = false
        )
        
        if (!parseResult.success) {
            throw ExcelImportException("Failed to parse Excel file: ${parseResult.error}")
        }
        
        val fileInfo = parseResult.fileInfo!!
        val sheetData = parseResult.sheetData.values.firstOrNull() ?: emptyList()
        
        if (sheetData.isEmpty()) {
            throw ExcelImportException("No data found in Excel file")
        }
        
        // 3. 分析数据
        updateProgress(
            importId = importId,
            step = ImportStep.ANALYZING_DATA,
            progress = 0.2f,
            statusMessage = "Analyzing data..."
        )
        
        val previewData = parseResult.previewData!!
        
        // 4. 识别字段映射
        updateProgress(
            importId = importId,
            step = ImportStep.RECOGNIZING_FIELDS,
            progress = 0.3f,
            statusMessage = "Recognizing field mappings..."
        )
        
        val fieldMappings = if (config.fieldMappings != null) {
            // 使用用户提供的映射
            config.fieldMappings.map { (excelColumn, systemField) ->
                FieldMapping(
                    excelColumn = excelColumn,
                    systemField = systemField,
                    confidence = 1.0f
                )
            }
        } else {
            // 使用AI识别
            val recognitionResult = aiFieldRecognizer.recognizeFields(
                excelPreview = previewData,
                sampleData = sheetData.take(100)
            )
            
            recognitionResult.fieldMappings
        }
        
        // 5. 准备导入数据
        val totalRows = sheetData.size
        val batches = sheetData.chunked(BATCH_SIZE)
        
        // 6. 验证数据
        updateProgress(
            importId = importId,
            step = ImportStep.VALIDATING_DATA,
            progress = 0.4f,
            statusMessage = "Validating data..."
        )
        
        val validationResults = validateData(
            sheetData = sheetData,
            fieldMappings = fieldMappings,
            validationRules = config.validationRules
        )
        
        // 7. 检测重复
        updateProgress(
            importId = importId,
            step = ImportStep.DETECTING_DUPLICATES,
            progress = 0.5f,
            statusMessage = "Detecting duplicates..."
        )
        
        val duplicateResults = detectDuplicates(
            sheetData = sheetData,
            fieldMappings = fieldMappings,
            validationResults = validationResults
        )
        
        // 8. 导入数据
        updateProgress(
            importId = importId,
            step = ImportStep.IMPORTING_DATA,
            progress = 0.6f,
            statusMessage = "Importing data..."
        )
        
        val importResults = mutableListOf<RowImportResult>()
        var importedCount = 0
        var failedCount = 0
        var duplicateCount = 0
        var skippedCount = 0
        
        for ((batchIndex, batch) in batches.withIndex()) {
            // 检查是否取消
            if (!currentImportJob?.isActive == true) {
                break
            }
            
            val batchResults = importBatch(
                batch = batch,
                batchIndex = batchIndex,
                fieldMappings = fieldMappings,
                validationResults = validationResults,
                duplicateResults = duplicateResults,
                importStrategy = config.importStrategy,
                duplicateResolutions = config.duplicateResolutions,
                userId = config.userId
            )
            
            importResults.addAll(batchResults)
            
            // 更新计数
            importedCount += batchResults.count { it.success && !it.isDuplicate }
            failedCount += batchResults.count { !it.success }
            duplicateCount += batchResults.count { it.isDuplicate }
            skippedCount += batchResults.count { it.skipped }
            
            // 更新进度
            val progress = 0.6f + (batchIndex + 1).toFloat() / batches.size * 0.3f
            updateProgress(
                importId = importId,
                step = ImportStep.IMPORTING_DATA,
                progress = progress,
                processedRows = (batchIndex + 1) * BATCH_SIZE,
                totalRows = totalRows,
                importedRows = importedCount,
                failedRows = failedCount,
                duplicateRows = duplicateCount,
                skippedRows = skippedCount,
                currentBatch = batchIndex + 1,
                totalBatches = batches.size,
                statusMessage = "Importing batch ${batchIndex + 1}/${batches.size}..."
            )
            
            // 小延迟以避免阻塞
            delay(50)
        }
        
        // 9. 完成导入
        updateProgress(
            importId = importId,
            step = ImportStep.COMPLETED,
            progress = 1f,
            processedRows = totalRows,
            totalRows = totalRows,
            importedRows = importedCount,
            failedRows = failedCount,
            duplicateRows = duplicateCount,
            skippedRows = skippedCount,
            statusMessage = "Import completed successfully"
        )
        
        // 保存最终结果
        saveImportResult(
            importId = importId,
            config = config,
            totalRows = totalRows,
            importedCount = importedCount,
            failedCount = failedCount,
            duplicateCount = duplicateCount,
            skippedCount = skippedCount,
            fieldMappings = fieldMappings.associate { it.excelColumn to it.systemField },
            validationResults = validationResults,
            importResults = importResults
        )
    }
    
    /**
     * 创建导入记录
     */
    private fun createImportRecord(importId: String, config: ImportConfig): ExcelImportRecord {
        return ExcelImportRecord(
            importId = importId,
            userId = config.userId,
            fileName = config.fileName,
            fileUri = config.fileUri,
            fileSize = config.fileSize,
            fileFormat = config.fileFormat,
            importStrategy = config.importStrategy,
            validationRules = config.validationRules,
            duplicateResolution = config.duplicateResolutions.mapValues { it.value.name },
            startedAt = Date()
        )
    }
    
    /**
     * 验证数据
     */
    private suspend fun validateData(
        sheetData: List<Map<String, String>>,
        fieldMappings: List<FieldMapping>,
        validationRules: List<ValidationRule>
    ): Map<Int, List<String>> {
        val validationResults = mutableMapOf<Int, List<String>>()
        
        for ((index, row) in sheetData.withIndex()) {
            val errors = mutableListOf<String>()
            
            // 将Excel数据映射到系统字段
            val mappedData = mapRowData(row, fieldMappings)
            
            // 应用验证规则
            for (rule in validationRules) {
                when (rule) {
                    ValidationRule.REQUIRED_FIELDS -> {
                        // 检查必填字段
                        val requiredFields = listOf("display_name")
                        for (field in requiredFields) {
                            if (mappedData[field].isNullOrBlank()) {
                                errors.add("Required field '$field' is empty")
                            }
                        }
                    }
                    
                    ValidationRule.VALID_PHONE_FORMAT -> {
                        val phone = mappedData["phone_number"]
                        if (!phone.isNullOrBlank()) {
                            if (!dataValidator.isValidPhoneNumber(phone)) {
                                errors.add("Invalid phone number format: $phone")
                            }
                        }
                    }
                    
                    ValidationRule.VALID_EMAIL_FORMAT -> {
                        val email = mappedData["email"]
                        if (!email.isNullOrBlank()) {
                            if (!dataValidator.isValidEmail(email)) {
                                errors.add("Invalid email format: $email")
                            }
                        }
                    }
                    
                    else -> {
                        // 其他验证规则
                    }
                }
            }
            
            if (errors.isNotEmpty()) {
                validationResults[index] = errors
            }
        }
        
        return validationResults
    }
    
    /**
     * 检测重复联系人
     */
    private suspend fun detectDuplicates(
        sheetData: List<Map<String, String>>,
        fieldMappings: List<FieldMapping>,
        validationResults: Map<Int, List<String>>
    ): Map<Int, DuplicateDetectionResult> {
        val duplicateResults = mutableMapOf<Int, DuplicateDetectionResult>()
        
        for ((index, row) in sheetData.withIndex()) {
            // 如果数据验证失败，跳过重复检测
            if (validationResults.containsKey(index)) {
                continue
            }
            
            val mappedData = mapRowData(row, fieldMappings)
            val phone = mappedData["phone_number"]
            val email = mappedData["email"]
            
            if (phone != null || email != null) {
                val duplicate = duplicateDetector.findDuplicate(phone, email)
                
                if (duplicate != null) {
                    val score = duplicateDetector.calculateSimilarityScore(
                        mappedData = mappedData,
                        existingContact = duplicate
                    )
                    
                    duplicateResults[index] = DuplicateDetectionResult(
                        existingContactId = duplicate.contactId,
                        existingContactName = duplicate.displayName,
                        similarityScore = score,
                        matchingFields = getMatchingFields(mappedData, duplicate)
                    )
                }
            }
        }
        
        return duplicateResults
    }
    
    /**
     * 导入批次数据
     */
    private suspend fun importBatch(
        batch: List<Map<String, String>>,
        batchIndex: Int,
        fieldMappings: List<FieldMapping>,
        validationResults: Map<Int, List<String>>,
        duplicateResults: Map<Int, DuplicateDetectionResult>,
        importStrategy: ExcelImportStrategy,
        duplicateResolutions: Map<String, DuplicateResolution>,
        userId: String
    ): List<RowImportResult> {
        val results = mutableListOf<RowImportResult>()
        
        for ((localIndex, row) in batch.withIndex()) {
            val globalIndex = batchIndex * BATCH_SIZE + localIndex
            
            val result = try {
                importRow(
                    rowIndex = globalIndex,
                    rowData = row,
                    fieldMappings = fieldMappings,
                    validationErrors = validationResults[globalIndex],
                    duplicateResult = duplicateResults[globalIndex],
                    importStrategy = importStrategy,
                    duplicateResolutions = duplicateResolutions,
                    userId = userId
                )
            } catch (e: Exception) {
                RowImportResult(
                    rowIndex = globalIndex,
                    success = false,
                    error = e.message ?: "Unknown error",
                    isDuplicate = false,
                    skipped = false
                )
            }
            
            results.add(result)
            
            // 保存导入详情
            saveImportDetail(globalIndex, row, result, userId)
        }
        
        return results
    }
    
    /**
     * 导入单行数据
     */
    private suspend fun importRow(
        rowIndex: Int,
        rowData: Map<String, String>,
        fieldMappings: List<FieldMapping>,
        validationErrors: List<String>?,
        duplicateResult: DuplicateDetectionResult?,
        importStrategy: ExcelImportStrategy,
        duplicateResolutions: Map<String, DuplicateResolution>,
        userId: String
    ): RowImportResult {
        // 如果有验证错误，跳过
        if (!validationErrors.isNullOrEmpty()) {
            return RowImportResult(
                rowIndex = rowIndex,
                success = false,
                error = validationErrors.joinToString(", "),
                isDuplicate = false,
                skipped = false
            )
        }
        
        // 映射数据
        val mappedData = mapRowData(rowData, fieldMappings)
        
        // 处理重复
        if (duplicateResult != null) {
            return handleDuplicate(
                rowIndex = rowIndex,
                mappedData = mappedData,
                duplicateResult = duplicateResult,
                importStrategy = importStrategy,
                duplicateResolutions = duplicateResolutions,
                userId = userId
            )
        }
        
        // 创建联系人
        val contact = createContactFromMappedData(mappedData, userId)
        
        // 保存联系人
        return try {
            val contactId = contactRepository.insertContact(contact)
            
            RowImportResult(
                rowIndex = rowIndex,
                success = true,
                contactId = contactId.toString(),
                isDuplicate = false,
                skipped = false
            )
        } catch (e: Exception) {
            RowImportResult(
                rowIndex = rowIndex,
                success = false,
                error = e.message ?: "Failed to save contact",
                isDuplicate = false,
                skipped = false
            )
        }
    }
    
    /**
     * 处理重复联系人
     */
    private suspend fun handleDuplicate(
        rowIndex: Int,
        mappedData: Map<String, String>,
        duplicateResult: DuplicateDetectionResult,
        importStrategy: ExcelImportStrategy,
        duplicateResolutions: Map<String, DuplicateResolution>,
        userId: String
    ): RowImportResult {
        val existingContactId = duplicateResult.existingContactId
        
        // 检查是否有特定的重复处理规则
        val resolution = duplicateResolutions[existingContactId] ?: when (importStrategy) {
            ExcelImportStrategy.SMART_MERGE -> DuplicateResolution.MERGE
            ExcelImportStrategy.REPLACE_ALL -> DuplicateResolution.REPLACE
            ExcelImportStrategy.KEEP_BOTH -> DuplicateResolution.KEEP_BOTH
            ExcelImportStrategy.SKIP_DUPLICATES -> DuplicateResolution.SKIP
        }
        
        return when (resolution) {
            DuplicateResolution.SKIP -> {
                RowImportResult(
                    rowIndex = rowIndex,
                    success = true,
                    isDuplicate = true,
                    skipped = true,
                    resolution = resolution
                )
            }
            
            DuplicateResolution.MERGE -> {
                // 合并联系人
                val mergedContact = mergeContacts(
                    existingContactId = existingContactId,
                    newData = mappedData,
                    userId = userId
                )
                
                try {
                    contactRepository.updateContact(mergedContact)
                    
                    RowImportResult(
                        rowIndex = rowIndex,
                        success = true,
                        contactId = existingContactId,
                        isDuplicate = true,
                        skipped = false,
                        resolution = resolution
                    )
                } catch (e: Exception) {
                    RowImportResult(
                        rowIndex = rowIndex,
                        success = false,
                        error = "Failed to merge contact: ${e.message}",
                        isDuplicate = true,
                        skipped = false,
                        resolution = resolution
                    )
                }
            }
            
            DuplicateResolution.REPLACE -> {
                // 替换联系人
                val newContact = createContactFromMappedData(mappedData, userId)
                    .copy(id = existingContactId.toLongOrNull() ?: 0L)
                
                try {
                    contactRepository.updateContact(newContact)
                    
                    RowImportResult(
                        rowIndex = rowIndex,
                        success = true,
                        contactId = existingContactId,
                        isDuplicate = true,
                        skipped = false,
                        resolution = resolution
                    )
                } catch (e: Exception) {
                    RowImportResult(
                        rowIndex = rowIndex,
                        success = false,
                        error = "Failed to replace contact: ${e.message}",
                        isDuplicate = true,
                        skipped = false,
                        resolution = resolution
                    )
                }
            }
            
            DuplicateResolution.KEEP_BOTH -> {
                // 创建新联系人
                val contact = createContactFromMappedData(mappedData, userId)
                
                try {
                    val contactId = contactRepository.insertContact(contact)
                    
                    RowImportResult(
                        rowIndex = rowIndex,
                        success = true,
                        contactId = contactId.toString(),
                        isDuplicate = true,
                        skipped = false,
                        resolution = resolution
                    )
                } catch (e: Exception) {
                    RowImportResult(
                        rowIndex = rowIndex,
                        success = false,
                        error = "Failed to create new contact: ${e.message}",
                        isDuplicate = true,
                        skipped = false,
                        resolution = resolution
                    )
                }
            }
        }
    }
    
    /**
     * 映射行数据
     */
    private fun mapRowData(
        rowData: Map<String, String>,
        fieldMappings: List<FieldMapping>
    ): Map<String, String> {
        val mappedData = mutableMapOf<String, String>()
        
        for (mapping in fieldMappings) {
            val excelValue = rowData[mapping.excelColumn] ?: ""
            mappedData[mapping.systemField] = excelValue
        }
        
        return mappedData
    }
    
    /**
     * 创建联系人
     */
    private fun createContactFromMappedData(
        mappedData: Map<String, String>,
        userId: String
    ): Contact {
        return Contact(
            displayName = mappedData["display_name"] ?: "",
            phoneNumber = mappedData["phone_number"],
            email = mappedData["email"],
            company = mappedData["company"],
            position = mappedData["position"],
            address = mappedData["address"],
            notes = mappedData["notes"],
            tags = mappedData["tags"]?.split(",")?.map { it.trim() } ?: emptyList(),
            source = ContactSource.IMPORT,
            importBatchId = userId,
            customFields = mappedData.filterKeys { !Contact::class.java.declaredFields.any { field -> field.name == it } }
        )
    }
    
    /**
     * 合并联系人
     */
    private suspend fun mergeContacts(
        existingContactId: String,
        newData: Map<String, String>,
        userId: String
    ): Contact {
        val existingContact = contactRepository.getContactById(existingContactId.toLong())
            ?: throw ExcelImportException("Existing contact not found: $existingContactId")
        
        // 创建合并策略：新数据覆盖空字段，保留非空字段
        return existingContact.copy(
            displayName = newData["display_name"] ?: existingContact.displayName,
            phoneNumber = existingContact.phoneNumber ?: newData["phone_number"],
            email = existingContact.email ?: newData["email"],
            company = existingContact.company ?: newData["company"],
            position = existingContact.position ?: newData["position"],
            address = existingContact.address ?: newData["address"],
            notes = existingContact.notes ?: newData["notes"],
            tags = (existingContact.tags + (newData["tags"]?.split(",")?.map { it.trim() } ?: emptyList())).distinct(),
            updatedAt = Date(),
            customFields = existingContact.customFields + newData.filterKeys { key ->
                !Contact::class.java.declaredFields.any { field -> field.name == key }
            }
        )
    }
    
    /**
     * 更新进度
     */
    private fun updateProgress(
        importId: String,
        step: ImportStep,
        progress: Float,
        processedRows: Int = 0,
        totalRows: Int = 0,
        importedRows: Int = 0,
        failedRows: Int = 0,
        duplicateRows: Int = 0,
        skippedRows: Int = 0,
        currentBatch: Int = 0,
        totalBatches: Int = 0,
        statusMessage: String = ""
    ) {
        _importProgress.value = ImportProgress(
            importId = importId,
            currentStep = step,
            progress = progress.coerceIn(0f, 1f),
            processedRows = processedRows,
            totalRows = totalRows,
            importedRows = importedRows,
            failedRows = failedRows,
            duplicateRows = duplicateRows,
            skippedRows = skippedRows,
            currentBatch = currentBatch,
            totalBatches = totalBatches,
            statusMessage = statusMessage
        )
    }
    
    /**
     * 保存导入详情
     */
    private suspend fun saveImportDetail(
        rowIndex: Int,
        rowData: Map<String, String>,
        result: RowImportResult,
        userId: String
    ) {
        try {
            val importDetail = ExcelImportDetail(
                rowIndex = rowIndex,
                excelData = rowData,
                success = result.success,
                error = result.error,
                contactId = result.contactId?.toLongOrNull(),
                isDuplicate = result.isDuplicate,
                skipped = result.skipped,
                duplicateResolution = result.resolution?.name,
                processedAt = Date(),
                userId = userId
            )
            
            excelImportRepository.saveImportDetail(importDetail)
        } catch (e: Exception) {
            // 记录错误但不中断导入过程
            println("Failed to save import detail for row $rowIndex: ${e.message}")
        }
    }
    
    /**
     * 保存导入结果
     */
    private suspend fun saveImportResult(
        importId: String,
        config: ImportConfig,
        totalRows: Int,
        importedCount: Int,
        failedCount: Int,
        duplicateCount: Int,
        skippedCount: Int,
        fieldMappings: Map<String, String>,
        validationResults: Map<Int, List<String>>,
        importResults: List<RowImportResult>
    ) {
        try {
            // 1. 更新导入记录的状态和统计信息
            val importRecord = excelImportRepository.getImportRecordById(importId)
            
            if (importRecord != null) {
                val updatedRecord = importRecord.copy(
                    status = ImportStatus.COMPLETED,
                    completedAt = Date(),
                    totalRows = totalRows,
                    importedRows = importedCount,
                    failedRows = failedCount,
                    duplicateRows = duplicateCount,
                    skippedRows = skippedCount,
                    fieldMappings = fieldMappings,
                    validationResults = validationResults.mapValues { it.value.joinToString("; ") },
                    successRate = if (totalRows > 0) importedCount.toFloat() / totalRows else 0f,
                    errorRate = if (totalRows > 0) failedCount.toFloat() / totalRows else 0f
                )
                
                excelImportRepository.updateImportRecord(updatedRecord)
            }
            
            // 2. 生成导入报告摘要
            val report = generateImportReport(
                importId = importId,
                config = config,
                totalRows = totalRows,
                importedCount = importedCount,
                failedCount = failedCount,
                duplicateCount = duplicateCount,
                skippedCount = skippedCount,
                fieldMappings = fieldMappings,
                validationResults = validationResults,
                importResults = importResults
            )
            
            // 3. 保存报告（如果需要）
            saveImportReport(importId, report)
            
            // 4. 触发导入完成事件
            triggerImportCompletedEvent(importId, report)
            
        } catch (e: Exception) {
            println("Failed to save import result: ${e.message}")
            // 尝试保存错误信息
            try {
                val importRecord = excelImportRepository.getImportRecordById(importId)
                if (importRecord != null) {
                    val errorRecord = importRecord.copy(
                        status = ImportStatus.COMPLETED_WITH_ERRORS,
                        completedAt = Date(),
                        errorMessage = "Failed to save results: ${e.message}"
                    )
                    excelImportRepository.updateImportRecord(errorRecord)
                }
            } catch (innerE: Exception) {
                println("Failed to save error record: ${innerE.message}")
            }
        }
    }
    
    /**
     * 保存失败的导入
     */
    private suspend fun saveFailedImport(
        importId: String,
        config: ImportConfig,
        exception: Exception
    ) {
        try {
            // 获取已有的导入记录或创建新的失败记录
            val importRecord = excelImportRepository.getImportRecordById(importId)
            
            if (importRecord != null) {
                // 更新现有记录
                val failedRecord = importRecord.copy(
                    status = ImportStatus.FAILED,
                    completedAt = Date(),
                    failedRows = 0,
                    totalRows = 0,
                    errorMessage = exception.message ?: "Unknown error",
                    stackTrace = exception.stackTraceToString()
                )
                excelImportRepository.updateImportRecord(failedRecord)
            } else {
                // 创建新的失败记录
                val failedRecord = ExcelImportRecord(
                    importId = importId,
                    userId = config.userId,
                    fileName = config.fileName,
                    fileUri = config.fileUri,
                    fileSize = config.fileSize,
                    fileFormat = config.fileFormat,
                    importStrategy = config.importStrategy,
                    validationRules = config.validationRules,
                    duplicateResolution = config.duplicateResolutions.mapValues { it.value.name },
                    status = ImportStatus.FAILED,
                    startedAt = Date(),
                    completedAt = Date(),
                    totalRows = 0,
                    importedRows = 0,
                    failedRows = 0,
                    duplicateRows = 0,
                    skippedRows = 0,
                    errorMessage = exception.message ?: "Unknown error",
                    stackTrace = exception.stackTraceToString()
                )
                excelImportRepository.saveImportRecord(failedRecord)
            }
            
            // 记录失败详情
            saveFailureDetail(importId, exception, config.userId)
            
        } catch (e: Exception) {
            println("Failed to save failed import: ${e.message}")
            // 这里我们可以记录到系统日志或发送到监控系统
            Log.e(TAG, "Critical: Failed to save import failure record", e)
        }
    }
    
    /**
     * 获取匹配的字段
     */
    private fun getMatchingFields(
        mappedData: Map<String, String>,
        existingContact: Contact
    ): List<String> {
        val matchingFields = mutableListOf<String>()
        
        if (mappedData["phone_number"] == existingContact.phoneNumber) {
            matchingFields.add("phone_number")
        }
        
        if (mappedData["email"] == existingContact.email) {
            matchingFields.add("email")
        }
        
        // 可以添加更多字段的匹配逻辑
        if (mappedData["display_name"] == existingContact.displayName) {
            matchingFields.add("display_name")
        }
        
        return matchingFields
    }
    
    /**
     * 生成导入报告
     */
    private fun generateImportReport(
        importId: String,
        config: ImportConfig,
        totalRows: Int,
        importedCount: Int,
        failedCount: Int,
        duplicateCount: Int,
        skippedCount: Int,
        fieldMappings: Map<String, String>,
        validationResults: Map<Int, List<String>>,
        importResults: List<RowImportResult>
    ): ImportReport {
        val successRate = if (totalRows > 0) importedCount.toFloat() / totalRows else 0f
        
        // 分析常见错误
        val commonErrors = analyzeCommonErrors(validationResults)
        
        // 分析重复处理情况
        val duplicateAnalysis = analyzeDuplicates(importResults)
        
        return ImportReport(
            importId = importId,
            fileName = config.fileName,
            totalRows = totalRows,
            importedRows = importedCount,
            failedRows = failedCount,
            duplicateRows = duplicateCount,
            skippedRows = skippedCount,
            successRate = successRate,
            fieldMappings = fieldMappings,
            commonErrors = commonErrors,
            duplicateAnalysis = duplicateAnalysis,
            timestamp = Date(),
            duration = calculateImportDuration(importId)
        )
    }
    
    /**
     * 保存导入报告
     */
    private suspend fun saveImportReport(
        importId: String,
        report: ImportReport
    ) {
        // 这里可以扩展为保存到文件系统或云存储
        // 目前只记录到日志
        println("Import report generated: $report")
        
        // 也可以保存到数据库或本地文件
        val reportJson = convertReportToJson(report)
        // excelImportRepository.saveImportReport(importId, reportJson)
    }
    
    /**
     * 触发导入完成事件
     */
    private fun triggerImportCompletedEvent(
        importId: String,
        report: ImportReport
    ) {
        // 这里可以触发事件总线或回调
        // 例如：eventBus.publish(ImportCompletedEvent(importId, report))
    }
    
    /**
     * 保存失败详情
     */
    private suspend fun saveFailureDetail(
        importId: String,
        exception: Exception,
        userId: String
    ) {
        val failureDetail = ExcelImportDetail(
            rowIndex = -1,
            excelData = emptyMap(),
            success = false,
            error = exception.message ?: "Unknown error",
            contactId = null,
            isDuplicate = false,
            skipped = false,
            processedAt = Date(),
            userId = userId,
            additionalInfo = mapOf(
                "exception_type" to exception::class.java.simpleName,
                "stack_trace" to exception.stackTraceToString()
            )
        )
        
        excelImportRepository.saveImportDetail(failureDetail)
    }
    
    /**
     * 分析常见错误
     */
    private fun analyzeCommonErrors(
        validationResults: Map<Int, List<String>>
    ): Map<String, Int> {
        val errorCounts = mutableMapOf<String, Int>()
        
        validationResults.values.forEach { errors ->
            errors.forEach { error ->
                errorCounts[error] = errorCounts.getOrDefault(error, 0) + 1
            }
        }
        
        return errorCounts
    }
    
    /**
     * 分析重复处理
     */
    private fun analyzeDuplicates(
        importResults: List<RowImportResult>
    ): Map<DuplicateResolution, Int> {
        val duplicateCounts = mutableMapOf<DuplicateResolution, Int>()
        
        importResults.filter { it.isDuplicate && it.resolution != null }.forEach { result ->
            val resolution = result.resolution!!
            duplicateCounts[resolution] = duplicateCounts.getOrDefault(resolution, 0) + 1
        }
        
        return duplicateCounts
    }
    
    /**
     * 计算导入持续时间
     */
    private suspend fun calculateImportDuration(importId: String): Long {
        val importRecord = excelImportRepository.getImportRecordById(importId)
        
        return if (importRecord != null && importRecord.completedAt != null && importRecord.startedAt != null) {
            importRecord.completedAt!!.time - importRecord.startedAt.time
        } else {
            0L
        }
    }
    
    /**
     * 转换报告为JSON
     */
    private fun convertReportToJson(report: ImportReport): String {
        // 使用Gson或其他JSON库进行转换
        // 这里简化为返回字符串表示
        return report.toString()
    }
    
    data class RowImportResult(
        val rowIndex: Int,
        val success: Boolean,
        val contactId: String? = null,
        val error: String? = null,
        val isDuplicate: Boolean,
        val skipped: Boolean,
        val resolution: DuplicateResolution? = null
    )
    
    data class DuplicateDetectionResult(
        val existingContactId: String,
        val existingContactName: String,
        val similarityScore: Float,
        val matchingFields: List<String>
    )
    
    data class ImportReport(
        val importId: String,
        val fileName: String,
        val totalRows: Int,
        val importedRows: Int,
        val failedRows: Int,
        val duplicateRows: Int,
        val skippedRows: Int,
        val successRate: Float,
        val fieldMappings: Map<String, String>,
        val commonErrors: Map<String, Int>,
        val duplicateAnalysis: Map<DuplicateResolution, Int>,
        val timestamp: Date,
        val duration: Long
    )
    
    class ExcelImportException(message: String) : Exception(message)
    
    // 辅助日志类
    private object Log {
        fun e(tag: String, message: String, e: Exception) {
            // 这里可以使用Android Log或自定义日志系统
            println("ERROR [$tag]: $message - ${e.message}")
        }
    }
}