package com.omnimsg.domain.usecases.excel

import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.models.excel.*
import com.omnimsg.domain.repositories.ContactRepository
import com.omnimsg.domain.repositories.ExcelImportRepository
import com.omnimsg.domain.usecases.BaseUseCase
import com.omnimsg.feature.excelimport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportExcelUseCase @Inject constructor(
    private val excelImportEngine: ExcelImportEngine,
    private val excelImportRepository: ExcelImportRepository,
    private val contactRepository: ContactRepository,
    private val duplicateDetector: ContactDuplicateDetector,
    private val dataValidator: ContactDataValidator
) : BaseUseCase<ImportExcelUseCase.Params, ImportExcelUseCase.Result>() {
    
    data class Params(
        val request: ExcelImportRequest,
        val fieldMappings: Map<String, String>, // Excel列 -> 系统字段
        val duplicateStrategy: DuplicateResolutionStrategy = DuplicateResolutionStrategy.SMART_MERGE,
        val batchSize: Int = 50,
        val userId: String
    )
    
    data class DuplicateResolutionStrategy(
        val defaultAction: DuplicateResolution = DuplicateResolution.SKIP,
        val specificResolutions: Map<String, DuplicateResolution> = emptyMap() // contactId -> action
    )
    
    sealed class ImportEvent {
        data class Progress(
            val importId: String,
            val currentStep: ImportStep,
            val progress: Float,
            val processedRows: Int,
            val totalRows: Int,
            val importedRows: Int,
            val failedRows: Int,
            val duplicateRows: Int,
            val skippedRows: Int,
            val statusMessage: String
        ) : ImportEvent()
        
        data class ValidationResult(
            val validRows: List<ValidatedRow>,
            val invalidRows: List<InvalidRow>,
            val totalRows: Int
        ) : ImportEvent()
        
        data class DuplicateDetectionResult(
            val duplicates: List<DuplicateRow>,
            val totalRows: Int
        ) : ImportEvent()
        
        data class ImportStarted(val importId: String) : ImportEvent()
        data class ImportCompleted(val result: ImportResult) : ImportEvent()
        data class ImportFailed(val error: String) : ImportEvent()
    }
    
    data class ValidatedRow(
        val rowIndex: Int,
        val data: Map<String, String>,
        val isValid: Boolean,
        val errors: List<String>
    )
    
    data class InvalidRow(
        val rowIndex: Int,
        val data: Map<String, String>,
        val errors: List<String>
    )
    
    data class DuplicateRow(
        val rowIndex: Int,
        val data: Map<String, String>,
        val existingContactId: String,
        val existingContactName: String,
        val similarityScore: Float,
        val matchingFields: List<String>
    )
    
    sealed class Result {
        data class Success(val importId: String) : Result()
        data class Error(val message: String) : Result()
    }
    
    // 事件通道
    private val _events = Channel<ImportEvent>(Channel.BUFFERED)
    val events: Flow<ImportEvent> = _events.receiveAsFlow()
    
    // 当前导入状态
    private val _importState = MutableStateFlow<ImportState?>(null)
    val importState: StateFlow<ImportState?> = _importState
    
    data class ImportState(
        val importId: String,
        val status: ImportStatus,
        val progress: Float,
        val processedRows: Int,
        val totalRows: Int,
        val importedRows: Int,
        val failedRows: Int,
        val duplicateRows: Int,
        val skippedRows: Int,
        val currentStep: String,
        val error: String? = null,
        val startedAt: Date? = null,
        val completedAt: Date? = null
    )
    
    enum class ImportStatus {
        IDLE, PREPARING, VALIDATING, DETECTING_DUPLICATES, IMPORTING, COMPLETED, FAILED, CANCELLED
    }
    
    private var currentImportJob: Job? = null
    
    override suspend fun execute(params: Params): kotlin.Result<Result> {
        return withContext(Dispatchers.IO) {
            try {
                // 启动导入
                startImport(params)
                kotlin.Result.success(Result.Success("import_${System.currentTimeMillis()}"))
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
    
    /**
     * 启动导入过程
     */
    fun startImport(params: Params): String {
        // 取消之前的导入
        cancelImport()
        
        val importId = "import_${UUID.randomUUID()}"
        
        currentImportJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                performImport(importId, params)
            } catch (e: Exception) {
                handleImportError(importId, e)
            }
        }
        
        return importId
    }
    
    /**
     * 取消当前导入
     */
    fun cancelImport() {
        currentImportJob?.cancel()
        currentImportJob = null
        
        _importState.value?.let { state ->
            _importState.value = state.copy(
                status = ImportStatus.CANCELLED,
                completedAt = Date()
            )
        }
        
        _events.trySend(ImportEvent.ImportFailed("Import cancelled by user"))
    }
    
    /**
     * 执行导入过程
     */
    private suspend fun performImport(importId: String, params: Params) {
        updateImportState(
            importId = importId,
            status = ImportStatus.PREPARING,
            progress = 0f,
            currentStep = "准备导入..."
        )
        
        _events.trySend(ImportEvent.ImportStarted(importId))
        
        // 1. 解析Excel文件
        val parser = ExcelParser(android.content.Context())
        val parseResult = parser.parseExcelFile(
            uri = android.net.Uri.parse(params.request.fileUri),
            sheetIndex = 0,
            hasHeaders = true,
            previewOnly = false
        )
        
        if (!parseResult.success) {
            throw IllegalStateException("Failed to parse Excel file: ${parseResult.error}")
        }
        
        val sheetData = parseResult.sheetData.values.firstOrNull() ?: emptyList()
        val totalRows = sheetData.size
        
        updateImportState(
            importId = importId,
            status = ImportStatus.PREPARING,
            progress = 0.1f,
            totalRows = totalRows,
            currentStep = "文件解析完成，共发现 $totalRows 行数据"
        )
        
        // 2. 数据验证
        updateImportState(
            importId = importId,
            status = ImportStatus.VALIDATING,
            progress = 0.2f,
            currentStep = "验证数据..."
        )
        
        val validationResults = validateData(sheetData, params.fieldMappings)
        
        val validRows = validationResults.filter { it.isValid }
        val invalidRows = validationResults.filterNot { it.isValid }
        
        _events.trySend(
            ImportEvent.ValidationResult(
                validRows = validRows,
                invalidRows = invalidRows.map { InvalidRow(it.rowIndex, it.data, it.errors) },
                totalRows = totalRows
            )
        )
        
        updateImportState(
            importId = importId,
            status = ImportStatus.VALIDATING,
            progress = 0.4f,
            processedRows = totalRows,
            currentStep = "数据验证完成，有效行: ${validRows.size}, 无效行: ${invalidRows.size}"
        )
        
        // 3. 重复检测
        updateImportState(
            importId = importId,
            status = ImportStatus.DETECTING_DUPLICATES,
            progress = 0.5f,
            currentStep = "检测重复联系人..."
        )
        
        val duplicateResults = detectDuplicates(validRows, params.fieldMappings)
        
        _events.trySend(
            ImportEvent.DuplicateDetectionResult(
                duplicates = duplicateResults.map {
                    DuplicateRow(
                        rowIndex = it.rowIndex,
                        data = it.data,
                        existingContactId = it.existingContactId,
                        existingContactName = it.existingContactName,
                        similarityScore = it.similarityScore,
                        matchingFields = it.matchingFields
                    )
                },
                totalRows = validRows.size
            )
        )
        
        updateImportState(
            importId = importId,
            status = ImportStatus.DETECTING_DUPLICATES,
            progress = 0.7f,
            currentStep = "重复检测完成，发现 ${duplicateResults.size} 个重复联系人"
        )
        
        // 4. 导入数据
        updateImportState(
            importId = importId,
            status = ImportStatus.IMPORTING,
            progress = 0.8f,
            currentStep = "开始导入联系人..."
        )
        
        val importResult = importContacts(
            validRows = validRows,
            duplicateResults = duplicateResults,
            fieldMappings = params.fieldMappings,
            duplicateStrategy = params.duplicateStrategy,
            batchSize = params.batchSize,
            userId = params.userId
        )
        
        // 5. 完成导入
        updateImportState(
            importId = importId,
            status = ImportStatus.COMPLETED,
            progress = 1.0f,
            importedRows = importResult.importedRows,
            failedRows = importResult.failedRows,
            duplicateRows = importResult.duplicateRows,
            skippedRows = importResult.skippedRows,
            currentStep = "导入完成",
            completedAt = Date()
        )
        
        _events.trySend(ImportEvent.ImportCompleted(importResult))
        
        // 保存导入记录
        saveImportRecord(importId, params, importResult)
    }
    
    /**
     * 验证数据
     */
    private suspend fun validateData(
        sheetData: List<Map<String, String>>,
        fieldMappings: Map<String, String>
    ): List<ValidatedRow> {
        return withContext(Dispatchers.Default) {
            sheetData.mapIndexed { index, row ->
                // 映射数据到系统字段
                val mappedData = mutableMapOf<String, String>()
                for ((excelColumn, systemField) in fieldMappings) {
                    mappedData[systemField] = row[excelColumn] ?: ""
                }
                
                // 验证数据
                val errors = dataValidator.validateContactData(mappedData)
                val isValid = errors.isEmpty()
                
                ValidatedRow(
                    rowIndex = index,
                    data = mappedData,
                    isValid = isValid,
                    errors = errors
                )
            }
        }
    }
    
    /**
     * 检测重复
     */
    private suspend fun detectDuplicates(
        validRows: List<ValidatedRow>,
        fieldMappings: Map<String, String>
    ): List<DuplicateDetectionResult> {
        return withContext(Dispatchers.Default) {
            validRows.mapNotNull { row ->
                val phone = row.data["phone_number"]
                val email = row.data["email"]
                
                if (phone != null || email != null) {
                    val duplicate = duplicateDetector.findDuplicate(phone, email)
                    
                    if (duplicate != null) {
                        val score = duplicateDetector.calculateSimilarityScore(row.data, duplicate)
                        val matchingFields = getMatchingFields(row.data, duplicate)
                        
                        DuplicateDetectionResult(
                            rowIndex = row.rowIndex,
                            data = row.data,
                            existingContactId = duplicate.contactId,
                            existingContactName = duplicate.displayName,
                            similarityScore = score,
                            matchingFields = matchingFields
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }
    
    /**
     * 导入联系人
     */
    private suspend fun importContacts(
        validRows: List<ValidatedRow>,
        duplicateResults: List<DuplicateDetectionResult>,
        fieldMappings: Map<String, String>,
        duplicateStrategy: DuplicateResolutionStrategy,
        batchSize: Int,
        userId: String
    ): ImportResult {
        var importedRows = 0
        var failedRows = 0
        var duplicateRows = 0
        var skippedRows = 0
        
        val batches = validRows.chunked(batchSize)
        val totalBatches = batches.size
        
        for ((batchIndex, batch) in batches.withIndex()) {
            // 检查是否取消
            if (!currentImportJob?.isActive == true) {
                break
            }
            
            // 处理批次
            for (row in batch) {
                try {
                    // 检查是否为重复
                    val duplicateResult = duplicateResults.find { it.rowIndex == row.rowIndex }
                    
                    if (duplicateResult != null) {
                        duplicateRows++
                        
                        // 根据策略处理重复
                        val resolution = getResolutionAction(
                            duplicateResult.existingContactId,
                            duplicateStrategy
                        )
                        
                        when (resolution) {
                            DuplicateResolution.SKIP -> {
                                skippedRows++
                                continue
                            }
                            DuplicateResolution.MERGE -> {
                                // 合并联系人
                                val mergedContact = mergeContact(
                                    existingContactId = duplicateResult.existingContactId,
                                    newData = row.data,
                                    userId = userId
                                )
                                contactRepository.updateContact(mergedContact)
                                importedRows++
                            }
                            DuplicateResolution.REPLACE -> {
                                // 替换联系人
                                val contact = createContact(row.data, userId)
                                    .copy(id = duplicateResult.existingContactId.toLong())
                                contactRepository.updateContact(contact)
                                importedRows++
                            }
                            DuplicateResolution.KEEP_BOTH -> {
                                // 保留两者
                                val contact = createContact(row.data, userId)
                                contactRepository.insertContact(contact)
                                importedRows++
                            }
                        }
                    } else {
                        // 新增联系人
                        val contact = createContact(row.data, userId)
                        contactRepository.insertContact(contact)
                        importedRows++
                    }
                } catch (e: Exception) {
                    failedRows++
                    // 记录错误
                }
            }
            
            // 更新进度
            val progress = 0.8f + (batchIndex + 1).toFloat() / totalBatches * 0.2f
            updateImportState(
                importId = "current",
                status = ImportStatus.IMPORTING,
                progress = progress,
                processedRows = (batchIndex + 1) * batchSize,
                importedRows = importedRows,
                failedRows = failedRows,
                duplicateRows = duplicateRows,
                skippedRows = skippedRows,
                currentStep = "导入批次 ${batchIndex + 1}/$totalBatches"
            )
            
            // 发送进度事件
            _events.trySend(
                ImportEvent.Progress(
                    importId = "current",
                    currentStep = ImportStep.IMPORTING,
                    progress = progress,
                    processedRows = (batchIndex + 1) * batchSize,
                    totalRows = validRows.size,
                    importedRows = importedRows,
                    failedRows = failedRows,
                    duplicateRows = duplicateRows,
                    skippedRows = skippedRows,
                    statusMessage = "正在导入批次 ${batchIndex + 1}/$totalBatches"
                )
            )
            
            // 短暂延迟避免阻塞
            delay(50)
        }
        
        return ImportResult(
            importId = "import_${System.currentTimeMillis()}",
            totalRows = validRows.size,
            importedRows = importedRows,
            failedRows = failedRows,
            duplicateRows = duplicateRows,
            skippedRows = skippedRows,
            successRate = if (validRows.isNotEmpty()) {
                importedRows.toFloat() / validRows.size.toFloat() * 100f
            } else 0f,
            duration = 0,
            fieldMappings = fieldMappings,
            validationResults = emptyMap(),
            duplicateResolutions = emptyList(),
            failedRowsDetails = emptyList()
        )
    }
    
    /**
     * 获取处理重复的动作
     */
    private fun getResolutionAction(
        contactId: String,
        strategy: DuplicateResolutionStrategy
    ): DuplicateResolution {
        return strategy.specificResolutions[contactId] ?: strategy.defaultAction
    }
    
    /**
     * 创建联系人
     */
    private fun createContact(data: Map<String, String>, userId: String): Contact {
        return Contact(
            displayName = data["display_name"] ?: "",
            phoneNumber = data["phone_number"],
            email = data["email"],
            company = data["company"],
            position = data["position"],
            address = data["address"],
            notes = data["notes"],
            tags = data["tags"]?.split(",")?.map { it.trim() } ?: emptyList(),
            source = ContactSource.IMPORT,
            importBatchId = userId,
            customFields = data.filterKeys { key ->
                !Contact::class.java.declaredFields.any { field -> field.name == key }
            }
        )
    }
    
    /**
     * 合并联系人
     */
    private suspend fun mergeContact(
        existingContactId: String,
        newData: Map<String, String>,
        userId: String
    ): Contact {
        val existingContact = contactRepository.getContactById(existingContactId.toLong())
            ?: throw IllegalStateException("Contact not found: $existingContactId")
        
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
     * 获取匹配的字段
     */
    private fun getMatchingFields(data: Map<String, String>, contact: Contact): List<String> {
        val matchingFields = mutableListOf<String>()
        
        if (data["phone_number"] == contact.phoneNumber) {
            matchingFields.add("phone_number")
        }
        
        if (data["email"] == contact.email) {
            matchingFields.add("email")
        }
        
        // 可以添加更多字段匹配逻辑
        
        return matchingFields
    }
    
    /**
     * 保存导入记录
     */
    private suspend fun saveImportRecord(
        importId: String,
        params: Params,
        result: ImportResult
    ) {
        val record = ExcelImportRecord(
            importId = importId,
            userId = params.userId,
            fileName = params.request.fileName,
            fileUri = params.request.fileUri,
            fileSize = params.request.fileSize,
            fileFormat = params.request.fileFormat,
            totalRows = result.totalRows,
            importedRows = result.importedRows,
            failedRows = result.failedRows,
            duplicateRows = result.duplicateRows,
            skippedRows = result.skippedRows,
            status = if (result.failedRows == result.totalRows) {
                ExcelImportStatus.FAILED
            } else {
                ExcelImportStatus.COMPLETED
            },
            importStrategy = params.request.importStrategy,
            fieldMappings = result.fieldMappings,
            validationRules = params.request.validationRules,
            duplicateResolution = params.duplicateStrategy.specificResolutions.mapValues { it.value.name },
            errorMessages = result.failedRowsDetails.map { it.error },
            importDuration = result.duration,
            startedAt = Date(System.currentTimeMillis() - result.duration),
            completedAt = Date(),
            metadata = result.metadata
        )
        
        excelImportRepository.saveImportRecord(record)
    }
    
    /**
     * 处理导入错误
     */
    private suspend fun handleImportError(importId: String, error: Exception) {
        updateImportState(
            importId = importId,
            status = ImportStatus.FAILED,
            progress = 1.0f,
            currentStep = "导入失败",
            error = error.message ?: "Unknown error",
            completedAt = Date()
        )
        
        _events.trySend(ImportEvent.ImportFailed(error.message ?: "Unknown error"))
    }
    
    /**
     * 更新导入状态
     */
    private fun updateImportState(
        importId: String,
        status: ImportStatus,
        progress: Float,
        processedRows: Int = 0,
        totalRows: Int = 0,
        importedRows: Int = 0,
        failedRows: Int = 0,
        duplicateRows: Int = 0,
        skippedRows: Int = 0,
        currentStep: String,
        error: String? = null,
        startedAt: Date? = null,
        completedAt: Date? = null
    ) {
        val currentState = _importState.value
        
        val newState = ImportState(
            importId = importId,
            status = status,
            progress = progress.coerceIn(0f, 1f),
            processedRows = processedRows,
            totalRows = totalRows,
            importedRows = importedRows,
            failedRows = failedRows,
            duplicateRows = duplicateRows,
            skippedRows = skippedRows,
            currentStep = currentStep,
            error = error,
            startedAt = startedAt ?: currentState?.startedAt,
            completedAt = completedAt ?: currentState?.completedAt
        )
        
        _importState.value = newState
    }
    
    data class DuplicateDetectionResult(
        val rowIndex: Int,
        val data: Map<String, String>,
        val existingContactId: String,
        val existingContactName: String,
        val similarityScore: Float,
        val matchingFields: List<String>
    )
}