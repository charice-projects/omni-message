package com.omnimsg.domain.usecases.excel

import com.omnimsg.domain.models.excel.ExcelFileInfo
import com.omnimsg.domain.models.excel.ExcelPreview
import com.omnimsg.feature.excelimport.ExcelParser
import com.omnimsg.domain.usecases.BaseUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class PreviewExcelUseCase @Inject constructor(
    private val excelParser: ExcelParser
) : BaseUseCase<PreviewExcelUseCase.Params, PreviewExcelUseCase.Result>() {
    
    data class Params(
        val fileUri: String,
        val sheetIndex: Int = 0,
        val hasHeaders: Boolean = true,
        val previewRows: Int = 10
    )
    
    sealed class Result {
        data class Success(
            val fileInfo: ExcelFileInfo,
            val preview: ExcelPreview,
            val sheetData: List<Map<String, String>>
        ) : Result()
        
        data class Error(val message: String) : Result()
    }
    
    override suspend fun execute(params: Params): kotlin.Result<Result> {
        return withContext(Dispatchers.IO) {
            try {
                val parseResult = excelParser.parseExcelFile(
                    uri = android.net.Uri.parse(params.fileUri),
                    sheetIndex = params.sheetIndex,
                    hasHeaders = params.hasHeaders,
                    previewOnly = true
                )
                
                if (!parseResult.success || parseResult.fileInfo == null || parseResult.previewData == null) {
                    return@withContext kotlin.Result.failure(
                        IllegalStateException(parseResult.error ?: "Failed to parse Excel file")
                    )
                }
                
                // 获取预览数据
                val previewData = parseResult.sheetData.values.firstOrNull() ?: emptyList()
                
                val result = Result.Success(
                    fileInfo = parseResult.fileInfo,
                    preview = parseResult.previewData,
                    sheetData = previewData.take(params.previewRows)
                )
                
                kotlin.Result.success(result)
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
}

class AnalyzeExcelUseCase @Inject constructor(
    private val excelParser: ExcelParser,
    private val aiFieldRecognizer: AIFieldRecognizer
) : BaseUseCase<AnalyzeExcelUseCase.Params, AnalyzeExcelUseCase.Result>() {
    
    data class Params(
        val fileUri: String,
        val sheetIndex: Int = 0,
        val hasHeaders: Boolean = true,
        val sampleSize: Int = 50
    )
    
    data class AnalysisResult(
        val fileInfo: ExcelFileInfo,
        val fieldMappings: List<FieldMapping>,
        val unrecognizedColumns: List<String>,
        val overallConfidence: Float,
        val columnAnalysis: Map<String, ColumnAnalysis>,
        val suggestions: List<MappingSuggestion>
    )
    
    data class ColumnAnalysis(
        val dataType: DataType,
        val uniqueValues: Int,
        val emptyValues: Int,
        val sampleValues: List<String>,
        val suggestedField: String?,
        val confidence: Float
    )
    
    sealed class Result {
        data class Success(val analysis: AnalysisResult) : Result()
        data class Error(val message: String) : Result()
    }
    
    override suspend fun execute(params: Params): kotlin.Result<Result> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 解析Excel文件
                val parseResult = excelParser.parseExcelFile(
                    uri = android.net.Uri.parse(params.fileUri),
                    sheetIndex = params.sheetIndex,
                    hasHeaders = params.hasHeaders,
                    previewOnly = false
                )
                
                if (!parseResult.success || parseResult.fileInfo == null || parseResult.previewData == null) {
                    return@withContext kotlin.Result.failure(
                        IllegalStateException(parseResult.error ?: "Failed to parse Excel file")
                    )
                }
                
                val fileInfo = parseResult.fileInfo
                val preview = parseResult.previewData
                val sheetData = parseResult.sheetData.values.firstOrNull() ?: emptyList()
                
                // 2. AI字段识别
                val sampleData = sheetData.take(params.sampleSize)
                val recognitionResult = aiFieldRecognizer.recognizeFields(preview, sampleData)
                
                // 3. 分析列数据
                val columnAnalysis = analyzeColumns(preview, sheetData, recognitionResult.fieldMappings)
                
                // 4. 构建结果
                val analysisResult = AnalysisResult(
                    fileInfo = fileInfo,
                    fieldMappings = recognitionResult.fieldMappings,
                    unrecognizedColumns = recognitionResult.unrecognizedColumns,
                    overallConfidence = recognitionResult.confidence,
                    columnAnalysis = columnAnalysis,
                    suggestions = recognitionResult.suggestions
                )
                
                kotlin.Result.success(Result.Success(analysisResult))
            } catch (e: Exception) {
                kotlin.Result.failure(e)
            }
        }
    }
    
    private fun analyzeColumns(
        preview: ExcelPreview,
        data: List<Map<String, String>>,
        fieldMappings: List<FieldMapping>
    ): Map<String, ColumnAnalysis> {
        val analysis = mutableMapOf<String, ColumnAnalysis>()
        
        for (header in preview.headers) {
            val columnData = data.mapNotNull { it[header] }
            val fieldMapping = fieldMappings.find { it.excelColumn == header }
            
            val columnStats = preview.columnStats[header]
            
            val analysisResult = ColumnAnalysis(
                dataType = fieldMapping?.dataType ?: DataType.STRING,
                uniqueValues = columnStats?.uniqueCount ?: columnData.toSet().size,
                emptyValues = columnData.count { it.isBlank() },
                sampleValues = columnData.take(5),
                suggestedField = fieldMapping?.systemField,
                confidence = fieldMapping?.confidence ?: 0f
            )
            
            analysis[header] = analysisResult
        }
        
        return analysis
    }
}