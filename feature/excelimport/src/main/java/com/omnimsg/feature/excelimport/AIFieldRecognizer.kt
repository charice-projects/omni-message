package com.omnimsg.feature.excelimport

import com.omnimsg.domain.models.excel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIFieldRecognizer @Inject constructor() {
    
    companion object {
        // 系统字段与关键词的映射
        private val FIELD_KEYWORDS = mapOf(
            "display_name" to listOf("name", "姓名", "名字", "联系人", "contact", "full name"),
            "phone_number" to listOf("phone", "mobile", "电话", "手机", "telephone", "cell"),
            "email" to listOf("email", "mail", "邮箱", "电子邮件"),
            "company" to listOf("company", "公司", "organization", "org", "单位"),
            "position" to listOf("position", "title", "职位", "职务", "job"),
            "address" to listOf("address", "地址", "location", "住址"),
            "birthday" to listOf("birthday", "birth", "生日", "出生日期", "dob"),
            "notes" to listOf("notes", "备注", "comment", "说明", "note"),
            "tags" to listOf("tags", "标签", "category", "分类")
        )
        
        // 字段优先级
        private val FIELD_PRIORITY = listOf(
            "display_name",
            "phone_number", 
            "email",
            "company",
            "position",
            "address",
            "birthday"
        )
    }
    
    data class RecognitionResult(
        val fieldMappings: List<FieldMapping>,
        val confidence: Float,
        val suggestions: List<MappingSuggestion>,
        val unrecognizedColumns: List<String>
    )
    
    data class MappingSuggestion(
        val excelColumn: String,
        val suggestedField: String,
        val confidence: Float,
        val reason: String
    )
    
    /**
     * 智能识别Excel列到系统字段的映射
     */
    suspend fun recognizeFields(
        excelPreview: ExcelPreview,
        sampleData: List<Map<String, String>> = emptyList()
    ): RecognitionResult = withContext(Dispatchers.Default) {
        val headers = excelPreview.headers
        val columnStats = excelPreview.columnStats
        val dataTypes = excelPreview.dataTypes
        
        val fieldMappings = mutableListOf<FieldMapping>()
        val suggestions = mutableListOf<MappingSuggestion>()
        val unrecognizedColumns = mutableListOf<String>()
        
        // 第一步：基于列名关键词匹配
        val nameBasedMappings = recognizeByName(headers)
        
        // 第二步：基于数据类型匹配
        val typeBasedMappings = recognizeByDataType(headers, dataTypes)
        
        // 第三步：基于数据模式匹配（如果提供了样本数据）
        val patternBasedMappings = if (sampleData.isNotEmpty()) {
            recognizeByPattern(headers, sampleData)
        } else {
            emptyMap()
        }
        
        // 合并识别结果
        for (header in headers) {
            val nameMatch = nameBasedMappings[header]
            val typeMatch = typeBasedMappings[header]
            val patternMatch = patternBasedMappings[header]
            
            val candidates = listOfNotNull(nameMatch, typeMatch, patternMatch)
            
            if (candidates.isEmpty()) {
                unrecognizedColumns.add(header)
                continue
            }
            
            // 选择最佳匹配
            val bestMatch = selectBestMatch(header, candidates, columnStats[header])
            
            fieldMappings.add(bestMatch.mapping)
            suggestions.add(bestMatch.suggestion)
        }
        
        // 计算整体置信度
        val confidence = calculateOverallConfidence(fieldMappings)
        
        // 确保关键字段有映射
        ensureCriticalFields(fieldMappings, headers, columnStats, suggestions)
        
        RecognitionResult(
            fieldMappings = fieldMappings,
            confidence = confidence,
            suggestions = suggestions,
            unrecognizedColumns = unrecognizedColumns
        )
    }
    
    /**
     * 基于列名关键词识别
     */
    private fun recognizeByName(headers: List<String>): Map<String, FieldMapping> {
        val mappings = mutableMapOf<String, FieldMapping>()
        
        for (header in headers) {
            val normalizedHeader = header.lowercase(Locale.getDefault()).trim()
            
            for ((field, keywords) in FIELD_KEYWORDS) {
                if (keywords.any { keyword ->
                        normalizedHeader.contains(keyword, ignoreCase = true)
                    }) {
                    
                    val confidence = calculateNameConfidence(normalizedHeader, keywords)
                    mappings[header] = FieldMapping(
                        excelColumn = header,
                        systemField = field,
                        confidence = confidence
                    )
                    break
                }
            }
        }
        
        return mappings
    }
    
    /**
     * 基于数据类型识别
     */
    private fun recognizeByDataType(
        headers: List<String>,
        dataTypes: Map<String, DataType>
    ): Map<String, FieldMapping> {
        val mappings = mutableMapOf<String, FieldMapping>()
        
        // 数据类型到系统字段的映射
        val typeToField = mapOf(
            DataType.PHONE to "phone_number",
            DataType.EMAIL to "email",
            DataType.DATE to "birthday",
            DataType.NUMBER to listOf("phone_number") // 数字可能是电话
        )
        
        for (header in headers) {
            val dataType = dataTypes[header] ?: continue
            
            when (dataType) {
                DataType.PHONE -> {
                    mappings[header] = FieldMapping(
                        excelColumn = header,
                        systemField = "phone_number",
                        confidence = 0.8f
                    )
                }
                DataType.EMAIL -> {
                    mappings[header] = FieldMapping(
                        excelColumn = header,
                        systemField = "email",
                        confidence = 0.9f
                    )
                }
                DataType.DATE -> {
                    mappings[header] = FieldMapping(
                        excelColumn = header,
                        systemField = "birthday",
                        confidence = 0.7f
                    )
                }
                else -> {
                    // 其他类型不进行自动映射
                }
            }
        }
        
        return mappings
    }
    
    /**
     * 基于数据模式识别
     */
    private fun recognizeByPattern(
        headers: List<String>,
        sampleData: List<Map<String, String>>
    ): Map<String, FieldMapping> {
        if (sampleData.isEmpty()) return emptyMap()
        
        val mappings = mutableMapOf<String, FieldMapping>()
        
        // 分析每列的数据模式
        for (header in headers) {
            val columnData = sampleData.mapNotNull { it[header] }
            if (columnData.isEmpty()) continue
            
            // 检测是否为姓名列（包含中文或英文名字特征）
            if (isLikelyNameColumn(columnData)) {
                mappings[header] = FieldMapping(
                    excelColumn = header,
                    systemField = "display_name",
                    confidence = 0.85f
                )
                continue
            }
            
            // 检测是否为电话列
            if (isLikelyPhoneColumn(columnData)) {
                mappings[header] = FieldMapping(
                    excelColumn = header,
                    systemField = "phone_number",
                    confidence = 0.9f
                )
                continue
            }
            
            // 检测是否为邮箱列
            if (isLikelyEmailColumn(columnData)) {
                mappings[header] = FieldMapping(
                    excelColumn = header,
                    systemField = "email",
                    confidence = 0.95f
                )
                continue
            }
        }
        
        return mappings
    }
    
    /**
     * 选择最佳匹配
     */
    private fun selectBestMatch(
        header: String,
        candidates: List<FieldMapping>,
        columnStats: ColumnStats?
    ): BestMatchResult {
        // 如果没有候选，创建默认映射
        if (candidates.isEmpty()) {
            val defaultMapping = FieldMapping(
                excelColumn = header,
                systemField = "custom_${header.lowercase().replace(" ", "_")}",
                confidence = 0.1f
            )
            
            return BestMatchResult(
                mapping = defaultMapping,
                suggestion = MappingSuggestion(
                    excelColumn = header,
                    suggestedField = defaultMapping.systemField,
                    confidence = defaultMapping.confidence,
                    reason = "No strong match found, using custom field"
                )
            )
        }
        
        // 如果有多个候选，选择置信度最高的
        val bestCandidate = candidates.maxByOrNull { it.confidence } ?: candidates.first()
        
        // 根据列统计信息调整置信度
        val adjustedConfidence = adjustConfidence(bestCandidate, columnStats)
        
        val adjustedMapping = bestCandidate.copy(confidence = adjustedConfidence)
        
        return BestMatchResult(
            mapping = adjustedMapping,
            suggestion = MappingSuggestion(
                excelColumn = header,
                suggestedField = adjustedMapping.systemField,
                confidence = adjustedConfidence,
                reason = generateSuggestionReason(header, adjustedMapping, columnStats)
            )
        )
    }
    
    /**
     * 调整置信度
     */
    private fun adjustConfidence(
        mapping: FieldMapping,
        columnStats: ColumnStats?
    ): Float {
        var confidence = mapping.confidence
        
        columnStats?.let { stats ->
            // 基于数据质量调整置信度
            val dataQuality = stats.nonEmptyCount.toFloat() / stats.totalCount.toFloat()
            confidence *= dataQuality
            
            // 基于唯一性调整（对于某些字段，高唯一性是好的）
            when (mapping.systemField) {
                "phone_number", "email" -> {
                    val uniqueness = stats.uniqueCount.toFloat() / stats.nonEmptyCount.toFloat()
                    confidence *= (0.5f + uniqueness * 0.5f)
                }
                "display_name" -> {
                    // 姓名可以有重复，所以不调整
                }
            }
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * 生成建议原因
     */
    private fun generateSuggestionReason(
        header: String,
        mapping: FieldMapping,
        columnStats: ColumnStats?
    ): String {
        val reasons = mutableListOf<String>()
        
        // 基于关键词匹配
        val keywords = FIELD_KEYWORDS[mapping.systemField] ?: emptyList()
        val normalizedHeader = header.lowercase()
        val matchedKeyword = keywords.find { normalizedHeader.contains(it) }
        if (matchedKeyword != null) {
            reasons.add("Column name contains keyword '$matchedKeyword'")
        }
        
        // 基于数据类型
        columnStats?.let { stats ->
            stats.dataPattern?.let { pattern ->
                reasons.add("Data pattern detected: $pattern")
            }
            
            if (stats.nonEmptyCount == stats.totalCount) {
                reasons.add("All rows have data")
            }
        }
        
        // 基于置信度
        when {
            mapping.confidence >= 0.9f -> reasons.add("High confidence match")
            mapping.confidence >= 0.7f -> reasons.add("Good confidence match")
            else -> reasons.add("Low confidence match")
        }
        
        return if (reasons.isNotEmpty()) {
            reasons.joinToString(", ")
        } else {
            "Automatic field detection"
        }
    }
    
    /**
     * 计算名称匹配置信度
     */
    private fun calculateNameConfidence(header: String, keywords: List<String>): Float {
        val normalizedHeader = header.lowercase()
        
        // 检查完全匹配
        if (keywords.any { it == normalizedHeader }) {
            return 0.95f
        }
        
        // 检查包含关系
        if (keywords.any { normalizedHeader.contains(it) }) {
            return 0.8f
        }
        
        // 检查相似度（简化实现）
        return 0.6f
    }
    
    /**
     * 计算整体置信度
     */
    private fun calculateOverallConfidence(fieldMappings: List<FieldMapping>): Float {
        if (fieldMappings.isEmpty()) return 0f
        
        val totalConfidence = fieldMappings.sumOf { it.confidence.toDouble() }
        return (totalConfidence / fieldMappings.size).toFloat()
    }
    
    /**
     * 确保关键字段有映射
     */
    private fun ensureCriticalFields(
        fieldMappings: MutableList<FieldMapping>,
        headers: List<String>,
        columnStats: Map<String, ColumnStats>,
        suggestions: MutableList<MappingSuggestion>
    ) {
        val criticalFields = listOf("display_name", "phone_number", "email")
        val existingFields = fieldMappings.map { it.systemField }.toSet()
        
        for (criticalField in criticalFields) {
            if (criticalField in existingFields) continue
            
            // 尝试为关键字段找到最佳列
            val bestColumn = findBestColumnForField(criticalField, headers, columnStats)
            
            if (bestColumn != null) {
                val mapping = FieldMapping(
                    excelColumn = bestColumn,
                    systemField = criticalField,
                    confidence = 0.7f
                )
                
                fieldMappings.add(mapping)
                
                suggestions.add(MappingSuggestion(
                    excelColumn = bestColumn,
                    suggestedField = criticalField,
                    confidence = 0.7f,
                    reason = "Auto-selected for critical field '$criticalField'"
                ))
            }
        }
    }
    
    /**
     * 为字段找到最佳列
     */
    private fun findBestColumnForField(
        field: String,
        headers: List<String>,
        columnStats: Map<String, ColumnStats>
    ): String? {
        // 基于字段类型寻找合适的列
        return when (field) {
            "display_name" -> {
                headers.find { header ->
                    val stats = columnStats[header]
                    stats?.dataPattern == DataPattern.TEXT &&
                            stats.nonEmptyCount > 0 &&
                            stats.uniqueCount > stats.totalCount * 0.5
                }
            }
            "phone_number" -> {
                headers.find { header ->
                    val stats = columnStats[header]
                    stats?.dataPattern == DataPattern.PHONE_NUMBER ||
                            stats?.suggestedDataType == DataType.PHONE
                }
            }
            "email" -> {
                headers.find { header ->
                    val stats = columnStats[header]
                    stats?.dataPattern == DataPattern.EMAIL ||
                            stats?.suggestedDataType == DataType.EMAIL
                }
            }
            else -> null
        }
    }
    
    /**
     * 检测是否为姓名列
     */
    private fun isLikelyNameColumn(data: List<String>): Boolean {
        if (data.isEmpty()) return false
        
        val sample = data.take(10)
        var nameScore = 0f
        
        for (value in sample) {
            // 检查是否包含常见姓氏（中文）
            val chineseSurnames = listOf("张", "王", "李", "赵", "刘", "陈", "杨", "黄", "周", "吴")
            if (chineseSurnames.any { value.startsWith(it) }) {
                nameScore += 0.2f
            }
            
            // 检查长度（中文姓名通常2-4个字符）
            if (value.length in 2..4 && value.all { it.isLetter() }) {
                nameScore += 0.1f
            }
            
            // 检查是否包含空格（英文姓名）
            if (value.contains(" ")) {
                nameScore += 0.15f
            }
        }
        
        return nameScore / sample.size >= 0.3f
    }
    
    /**
     * 检测是否为电话列
     */
    private fun isLikelyPhoneColumn(data: List<String>): Boolean {
        if (data.isEmpty()) return false
        
        val phoneRegex = Regex("^[+]?[0-9]{10,15}$")
        val sample = data.take(10)
        
        val phoneCount = sample.count { it.matches(phoneRegex) }
        return phoneCount.toFloat() / sample.size >= 0.7f
    }
    
    /**
     * 检测是否为邮箱列
     */
    private fun isLikelyEmailColumn(data: List<String>): Boolean {
        if (data.isEmpty()) return false
        
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        val sample = data.take(10)
        
        val emailCount = sample.count { it.matches(emailRegex) }
        return emailCount.toFloat() / sample.size >= 0.8f
    }
    
    private data class BestMatchResult(
        val mapping: FieldMapping,
        val suggestion: MappingSuggestion
    )
}