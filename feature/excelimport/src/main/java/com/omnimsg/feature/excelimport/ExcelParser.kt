package com.omnimsg.feature.excelimport

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelParser @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ExcelParser"
        private const val PREVIEW_ROW_COUNT = 10
        private const val SAMPLE_DATA_SIZE = 50
    }
    
    data class ParseResult(
        val success: Boolean,
        val fileInfo: ExcelFileInfo? = null,
        val error: String? = null,
        val sheetData: Map<String, List<List<String>>> = emptyMap(),
        val previewData: ExcelPreview? = null
    )
    
    /**
     * 解析Excel文件
     */
    suspend fun parseExcelFile(
        uri: Uri,
        sheetIndex: Int = 0,
        hasHeaders: Boolean = true,
        previewOnly: Boolean = false
    ): ParseResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ParseResult(
                    success = false,
                    error = "Cannot open file: $uri"
                )
            
            inputStream.use { stream ->
                val workbook = createWorkbook(stream, uri)
                val fileFormat = detectFileFormat(uri)
                
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetName = workbook.getSheetName(sheetIndex)
                val sheetCount = workbook.numberOfSheets
                val sheetNames = (0 until sheetCount).map { workbook.getSheetName(it) }
                
                // 获取行数和列数
                val rowCount = sheet.physicalNumberOfRows
                val columnCount = getMaxColumnCount(sheet)
                
                // 获取表头
                val headers = if (hasHeaders && rowCount > 0) {
                    getHeaders(sheet, columnCount)
                } else {
                    (1..columnCount).map { "Column $it" }
                }
                
                // 获取数据
                val data = if (previewOnly) {
                    getPreviewData(sheet, headers, hasHeaders, PREVIEW_ROW_COUNT)
                } else {
                    getAllData(sheet, headers, hasHeaders)
                }
                
                // 分析列统计信息
                val columnStats = analyzeColumns(data, headers)
                val dataTypes = inferDataTypes(columnStats)
                
                val preview = ExcelPreview(
                    headers = headers,
                    sampleRows = data.take(PREVIEW_ROW_COUNT),
                    dataTypes = dataTypes,
                    columnStats = columnStats
                )
                
                val sheetData = mapOf(sheetName to data)
                
                val fileInfo = ExcelFileInfo(
                    uri = uri.toString(),
                    fileName = getFileName(uri),
                    fileSize = getFileSize(uri),
                    format = fileFormat,
                    sheetCount = sheetCount,
                    sheetNames = sheetNames,
                    firstSheetData = data.take(PREVIEW_ROW_COUNT).map { row ->
                        headers.map { header -> row[header] ?: "" }
                    },
                    columnCount = columnCount,
                    rowCount = rowCount,
                    headers = if (hasHeaders) headers else null
                )
                
                ParseResult(
                    success = true,
                    fileInfo = fileInfo,
                    sheetData = sheetData,
                    previewData = preview
                )
            }
        } catch (e: Exception) {
            ParseResult(
                success = false,
                error = "Failed to parse Excel file: ${e.message}"
            )
        }
    }
    
    /**
     * 获取文件信息而不解析全部数据
     */
    suspend fun getFileInfo(uri: Uri): ExcelFileInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                
                inputStream.use { stream ->
                    val workbook = createWorkbook(stream, uri)
                    val fileFormat = detectFileFormat(uri)
                    val sheetCount = workbook.numberOfSheets
                    val sheetNames = (0 until sheetCount).map { workbook.getSheetName(it) }
                    
                    // 只读取第一个sheet的基本信息
                    val sheet = workbook.getSheetAt(0)
                    val rowCount = sheet.physicalNumberOfRows
                    val columnCount = getMaxColumnCount(sheet)
                    
                    // 读取前几行来获取表头和数据样本
                    val headers = if (rowCount > 0) {
                        val headerRow = sheet.getRow(0)
                        (0 until columnCount.coerceAtMost(headerRow.lastCellNum)).map { colIndex ->
                            headerRow.getCell(colIndex)?.toString()?.trim() ?: "Column ${colIndex + 1}"
                        }
                    } else {
                        (1..columnCount).map { "Column $it" }
                    }
                    
                    // 获取前几行数据作为预览
                    val previewData = mutableListOf<List<String>>()
                    val maxPreviewRows = PREVIEW_ROW_COUNT.coerceAtMost(rowCount)
                    
                    for (rowIndex in 0 until maxPreviewRows) {
                        val row = sheet.getRow(rowIndex) ?: continue
                        val rowData = mutableListOf<String>()
                        
                        for (colIndex in 0 until columnCount.coerceAtMost(row.lastCellNum)) {
                            val cell = row.getCell(colIndex)
                            rowData.add(formatCellValue(cell))
                        }
                        previewData.add(rowData)
                    }
                    
                    ExcelFileInfo(
                        uri = uri.toString(),
                        fileName = getFileName(uri),
                        fileSize = getFileSize(uri),
                        format = fileFormat,
                        sheetCount = sheetCount,
                        sheetNames = sheetNames,
                        firstSheetData = previewData,
                        columnCount = columnCount,
                        rowCount = rowCount,
                        headers = if (rowCount > 0) headers else null
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 检测文件格式
     */
    private fun detectFileFormat(uri: Uri): ExcelFileFormat {
        val fileName = getFileName(uri).lowercase()
        return when {
            fileName.endsWith(".xlsx") -> ExcelFileFormat.XLSX
            fileName.endsWith(".xls") -> ExcelFileFormat.XLS
            fileName.endsWith(".csv") -> ExcelFileFormat.CSV
            else -> ExcelFileFormat.XLSX // 默认
        }
    }
    
    /**
     * 创建工作簿
     */
    private fun createWorkbook(stream: InputStream, uri: Uri): Workbook {
        val fileName = getFileName(uri).lowercase()
        
        return when {
            fileName.endsWith(".xlsx") -> XSSFWorkbook(stream)
            fileName.endsWith(".xls") -> HSSFWorkbook(stream)
            fileName.endsWith(".csv") -> parseCsv(stream)
            else -> throw IllegalArgumentException("Unsupported file format: $fileName")
        }
    }
    
    /**
     * 解析CSV文件（简化实现）
     */
    private fun parseCsv(stream: InputStream): Workbook {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")
        
        val lines = stream.bufferedReader().readLines()
        for ((rowIndex, line) in lines.withIndex()) {
            val row = sheet.createRow(rowIndex)
            val columns = line.split(",")
            
            for ((colIndex, value) in columns.withIndex()) {
                val cell = row.createCell(colIndex)
                cell.setCellValue(value.trim())
            }
        }
        
        return workbook
    }
    
    /**
     * 获取最大列数
     */
    private fun getMaxColumnCount(sheet: Sheet): Int {
        var maxCols = 0
        for (row in sheet) {
            val lastCellNum = row.lastCellNum
            if (lastCellNum > maxCols) {
                maxCols = lastCellNum
            }
        }
        return maxCols
    }
    
    /**
     * 获取表头
     */
    private fun getHeaders(sheet: Sheet, columnCount: Int): List<String> {
        val headerRow = sheet.getRow(0) ?: return (1..columnCount).map { "Column $it" }
        
        return (0 until columnCount.coerceAtMost(headerRow.lastCellNum)).map { colIndex ->
            val cell = headerRow.getCell(colIndex)
            val header = formatCellValue(cell).trim()
            if (header.isBlank()) "Column ${colIndex + 1}" else header
        }
    }
    
    /**
     * 获取预览数据
     */
    private fun getPreviewData(
        sheet: Sheet,
        headers: List<String>,
        hasHeaders: Boolean,
        maxRows: Int
    ): List<Map<String, String>> {
        val data = mutableListOf<Map<String, String>>()
        val startRow = if (hasHeaders) 1 else 0
        val endRow = (startRow + maxRows).coerceAtMost(sheet.physicalNumberOfRows)
        
        for (rowIndex in startRow until endRow) {
            val row = sheet.getRow(rowIndex) ?: continue
            val rowData = mutableMapOf<String, String>()
            
            for ((colIndex, header) in headers.withIndex()) {
                val cell = row.getCell(colIndex)
                rowData[header] = formatCellValue(cell)
            }
            
            data.add(rowData)
        }
        
        return data
    }
    
    /**
     * 获取所有数据
     */
    private fun getAllData(
        sheet: Sheet,
        headers: List<String>,
        hasHeaders: Boolean
    ): List<Map<String, String>> {
        val data = mutableListOf<Map<String, String>>()
        val startRow = if (hasHeaders) 1 else 0
        
        for (rowIndex in startRow until sheet.physicalNumberOfRows) {
            val row = sheet.getRow(rowIndex) ?: continue
            val rowData = mutableMapOf<String, String>()
            
            for ((colIndex, header) in headers.withIndex()) {
                val cell = row.getCell(colIndex)
                rowData[header] = formatCellValue(cell)
            }
            
            data.add(rowData)
        }
        
        return data
    }
    
    /**
     * 格式化单元格值
     */
    private fun formatCellValue(cell: Cell?): String {
        if (cell == null) return ""
        
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue.toString()
                } else {
                    // 防止显示科学计数法
                    val value = cell.numericCellValue
                    if (value == value.toLong().toDouble()) {
                        value.toLong().toString()
                    } else {
                        value.toString()
                    }
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    when (cell.cachedFormulaResultType) {
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.STRING -> cell.stringCellValue
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        else -> ""
                    }
                } catch (e: Exception) {
                    ""
                }
            }
            CellType.BLANK -> ""
            else -> ""
        }
    }
    
    /**
     * 分析列统计信息
     */
    private fun analyzeColumns(
        data: List<Map<String, String>>,
        headers: List<String>
    ): Map<String, ColumnStats> {
        val columnStats = mutableMapOf<String, ColumnStats>()
        
        for (header in headers) {
            val columnData = data.map { it[header] ?: "" }
            val nonEmptyData = columnData.filter { it.isNotBlank() }
            
            if (nonEmptyData.isEmpty()) {
                columnStats[header] = ColumnStats(
                    totalCount = columnData.size,
                    nonEmptyCount = 0,
                    uniqueCount = 0,
                    mostFrequentValue = null,
                    dataPattern = null,
                    suggestedDataType = DataType.STRING
                )
                continue
            }
            
            // 计算频率
            val frequencyMap = mutableMapOf<String, Int>()
            for (value in nonEmptyData) {
                frequencyMap[value] = frequencyMap.getOrDefault(value, 0) + 1
            }
            
            val mostFrequentEntry = frequencyMap.maxByOrNull { it.value }
            val mostFrequentValue = mostFrequentEntry?.key
            val uniqueCount = frequencyMap.size
            
            // 检测数据模式
            val dataPattern = detectDataPattern(nonEmptyData)
            val suggestedDataType = when (dataPattern) {
                DataPattern.PHONE_NUMBER -> DataType.PHONE
                DataPattern.EMAIL -> DataType.EMAIL
                DataPattern.DATE -> DataType.DATE
                DataPattern.NUMBER -> DataType.NUMBER
                else -> DataType.STRING
            }
            
            columnStats[header] = ColumnStats(
                totalCount = columnData.size,
                nonEmptyCount = nonEmptyData.size,
                uniqueCount = uniqueCount,
                mostFrequentValue = mostFrequentValue,
                dataPattern = dataPattern,
                suggestedDataType = suggestedDataType
            )
        }
        
        return columnStats
    }
    
    /**
     * 检测数据模式
     */
    private fun detectDataPattern(data: List<String>): DataPattern? {
        if (data.isEmpty()) return null
        
        // 采样检测
        val sample = data.take(10)
        
        // 检测手机号
        val phoneRegex = Regex("^[+]?[0-9]{10,15}$")
        if (sample.all { it.matches(phoneRegex) }) return DataPattern.PHONE_NUMBER
        
        // 检测邮箱
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        if (sample.all { it.matches(emailRegex) }) return DataPattern.EMAIL
        
        // 检测数字
        val numberRegex = Regex("^-?\\d+(\\.\\d+)?$")
        if (sample.all { it.matches(numberRegex) }) return DataPattern.NUMBER
        
        // 检测日期（简化）
        val dateRegexes = listOf(
            Regex("^\\d{4}-\\d{2}-\\d{2}$"),
            Regex("^\\d{2}/\\d{2}/\\d{4}$"),
            Regex("^\\d{2}-\\d{2}-\\d{4}$")
        )
        
        if (sample.any { date ->
                dateRegexes.any { regex -> date.matches(regex) }
            }) return DataPattern.DATE
        
        return DataPattern.TEXT
    }
    
    /**
     * 推断数据类型
     */
    private fun inferDataTypes(columnStats: Map<String, ColumnStats>): Map<String, DataType> {
        return columnStats.mapValues { (_, stats) ->
            stats.suggestedDataType
        }
    }
    
    /**
     * 获取文件名
     */
    private fun getFileName(uri: Uri): String {
        return uri.lastPathSegment ?: "unknown.xlsx"
    }
    
    /**
     * 获取文件大小
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            val file = context.contentResolver.openFileDescriptor(uri, "r")
            file?.statSize ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}