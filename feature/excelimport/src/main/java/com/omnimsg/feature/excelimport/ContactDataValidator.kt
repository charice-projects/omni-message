// 📁 feature/excelimport/ContactDataValidator.kt
package com.omnimsg.feature.excelimport

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 联系人数据验证器
 * 验证导入数据的完整性和正确性
 */
@Singleton
class ContactDataValidator @Inject constructor(
    private val context: Context
) {
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val suggestions: List<String> = emptyList(),
        val score: Float = 0f // 数据质量评分 0-100
    )
    
    data class FieldValidation(
        val fieldName: String,
        val value: String?,
        val isValid: Boolean,
        val error: String? = null,
        val warning: String? = null
    )
    
    /**
     * 验证单个联系人数据
     */
    suspend fun validateContact(
        contact: Contact,
        rules: List<ValidationRule> = getDefaultValidationRules()
    ): ValidationResult {
        return withContext(Dispatchers.Default) {
            try {
                val fieldValidations = mutableListOf<FieldValidation>()
                var totalScore = 0f
                var maxScore = 0f
                
                // 对每个字段应用验证规则
                rules.forEach { rule ->
                    val value = getFieldValue(contact, rule.fieldName)
                    val validation = validateField(rule, value)
                    fieldValidations.add(validation)
                    
                    // 计算分数
                    maxScore += rule.weight
                    if (validation.isValid) {
                        totalScore += rule.weight
                    } else if (validation.warning != null) {
                        totalScore += rule.weight * 0.5f // 警告减半分数
                    }
                }
                
                // 收集错误和警告
                val errors = fieldValidations
                    .filter { !it.isValid && it.error != null }
                    .map { "${it.fieldName}: ${it.error}" }
                
                val warnings = fieldValidations
                    .filter { it.warning != null }
                    .map { "${it.fieldName}: ${it.warning}" }
                
                // 数据完整性检查
                val completenessCheck = checkCompleteness(contact)
                errors.addAll(completenessCheck.errors)
                warnings.addAll(completenessCheck.warnings)
                
                // 数据一致性检查
                val consistencyCheck = checkConsistency(contact)
                warnings.addAll(consistencyCheck)
                
                // 计算最终分数
                val finalScore = if (maxScore > 0) (totalScore / maxScore) * 100 else 100f
                
                // 生成建议
                val suggestions = generateSuggestions(fieldValidations, contact)
                
                ValidationResult(
                    isValid = errors.isEmpty(),
                    errors = errors,
                    warnings = warnings,
                    suggestions = suggestions,
                    score = finalScore
                )
            } catch (e: Exception) {
                logger.e("ContactDataValidator", "验证联系人数据失败", e)
                ValidationResult(
                    isValid = false,
                    errors = listOf("验证过程出错: ${e.message}"),
                    score = 0f
                )
            }
        }
    }
    
    /**
     * 批量验证联系人数据
     */
    suspend fun batchValidateContacts(
        contacts: List<Contact>,
        rules: List<ValidationRule> = getDefaultValidationRules()
    ): List<Pair<Contact, ValidationResult>> {
        return withContext(Dispatchers.Default) {
            contacts.map { contact ->
                val result = validateContact(contact, rules)
                Pair(contact, result)
            }
        }
    }
    
    /**
     * 获取字段值
     */
    private fun getFieldValue(contact: Contact, fieldName: String): String? {
        return when (fieldName) {
            "displayName" -> contact.displayName
            "phoneNumber" -> contact.phoneNumber
            "email" -> contact.email
            "company" -> contact.company
            "position" -> contact.position
            "address" -> contact.address
            else -> contact.customFields[fieldName]
        }
    }
    
    /**
     * 验证单个字段
     */
    private fun validateField(rule: ValidationRule, value: String?): FieldValidation {
        // 检查必填字段
        if (rule.required && value.isNullOrBlank()) {
            return FieldValidation(
                fieldName = rule.fieldName,
                value = value,
                isValid = false,
                error = "必填字段不能为空"
            )
        }
        
        // 如果字段为空且不是必填，直接返回有效
        if (value.isNullOrBlank()) {
            return FieldValidation(
                fieldName = rule.fieldName,
                value = value,
                isValid = true
            )
        }
        
        // 检查格式
        if (rule.pattern != null && !Pattern.matches(rule.pattern, value)) {
            return FieldValidation(
                fieldName = rule.fieldName,
                value = value,
                isValid = false,
                error = "格式不正确，应为: ${rule.patternDescription ?: rule.pattern}"
            )
        }
        
        // 检查最小长度
        if (rule.minLength != null && value.length < rule.minLength) {
            return FieldValidation(
                fieldName = rule.fieldName,
                value = value,
                isValid = false,
                error = "长度不能少于${rule.minLength}个字符"
            )
        }
        
        // 检查最大长度
        if (rule.maxLength != null && value.length > rule.maxLength) {
            return FieldValidation(
                fieldName = rule.fieldName,
                value = value,
                isValid = false,
                error = "长度不能超过${rule.maxLength}个字符"
            )
        }
        
        // 检查是否在允许的值列表中
        if (rule.allowedValues != null && !rule.allowedValues.contains(value)) {
            return FieldValidation(
                fieldName = rule.fieldName,
                value = value,
                isValid = false,
                error = "值不在允许范围内: ${rule.allowedValues.joinToString()}"
            )
        }
        
        // 应用自定义验证函数
        if (rule.customValidator != null) {
            val customResult = rule.customValidator(value)
            if (!customResult.isValid) {
                return FieldValidation(
                    fieldName = rule.fieldName,
                    value = value,
                    isValid = false,
                    error = customResult.errorMessage
                )
            }
        }
        
        // 检查是否有警告条件
        val warning = checkForWarnings(rule, value)
        
        return FieldValidation(
            fieldName = rule.fieldName,
            value = value,
            isValid = true,
            warning = warning
        )
    }
    
    /**
     * 检查警告条件
     */
    private fun checkForWarnings(rule: ValidationRule, value: String): String? {
        // 检查可疑内容
        if (rule.fieldName == "email" && value.contains("test") || value.contains("example")) {
            return "疑似测试邮箱地址"
        }
        
        if (rule.fieldName == "phoneNumber" && value.startsWith("123") || value.startsWith("000")) {
            return "疑似测试手机号码"
        }
        
        // 检查非常规字符
        if (value.contains(Regex("[\\x00-\\x1F\\x7F]"))) {
            return "包含不可见字符"
        }
        
        // 检查重复字符
        if (value.matches(Regex("(.)\\1{3,}"))) { // 连续4个相同字符
            return "包含重复字符，可能是测试数据"
        }
        
        return null
    }
    
    /**
     * 检查数据完整性
     */
    private fun checkCompleteness(contact: Contact): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 基本完整性检查
        if (contact.displayName.isBlank()) {
            errors.add("姓名不能为空")
        }
        
        // 检查至少有一个联系方式
        val hasContactInfo = !contact.phoneNumber.isNullOrBlank() || 
                            !contact.email.isNullOrBlank()
        if (!hasContactInfo) {
            warnings.add("没有有效的联系方式（手机或邮箱）")
        }
        
        // 检查手机号和邮箱格式（如果存在）
        contact.phoneNumber?.let { phone ->
            if (!isValidPhoneNumber(phone)) {
                errors.add("手机号格式不正确: $phone")
            }
        }
        
        contact.email?.let { email ->
            if (!isValidEmail(email)) {
                errors.add("邮箱格式不正确: $email")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 检查数据一致性
     */
    private fun checkConsistency(contact: Contact): List<String> {
        val warnings = mutableListOf<String>()
        
        // 检查姓名和公司的一致性
        contact.company?.let { company ->
            if (contact.displayName.contains(company) || company.contains(contact.displayName)) {
                warnings.add("姓名和公司名称可能混淆")
            }
        }
        
        // 检查手机号和邮箱的关联性
        if (!contact.phoneNumber.isNullOrBlank() && !contact.email.isNullOrBlank()) {
            val phonePrefix = contact.phoneNumber!!.take(3)
            val emailPrefix = contact.email!!.split("@").first()
            
            // 检查手机号后几位是否出现在邮箱中
            if (emailPrefix.contains(phonePrefix) || 
                emailPrefix.contains(contact.phoneNumber!!.takeLast(4))) {
                warnings.add("手机号和邮箱可能关联")
            }
        }
        
        // 检查职位和公司的一致性
        contact.position?.let { position ->
            contact.company?.let { company ->
                val commonTitles = listOf("经理", "总监", "工程师", "主管", "主任")
                val isHighPosition = commonTitles.any { position.contains(it) }
                
                if (isHighPosition && company.length < 5) {
                    warnings.add("高级职位但公司名称过短")
                }
            }
        }
        
        return warnings
    }
    
    /**
     * 生成改进建议
     */
    private fun generateSuggestions(
        fieldValidations: List<FieldValidation>,
        contact: Contact
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 检查缺失的重要字段
        val importantFields = listOf("displayName", "phoneNumber", "email")
        val missingFields = importantFields.filter { fieldName ->
            fieldValidations.none { it.fieldName == fieldName && it.value.isNullOrBlank() }
        }
        
        if (missingFields.isNotEmpty()) {
            suggestions.add("建议补充以下信息: ${missingFields.joinToString()}")
        }
        
        // 检查数据标准化
        contact.phoneNumber?.let { phone ->
            if (!phone.startsWith("+86") && phone.length == 11) {
                suggestions.add("建议将手机号格式化为国际格式: +86 $phone")
            }
        }
        
        // 检查备注信息
        if (contact.notes.isNullOrBlank() && contact.tags.isEmpty()) {
            suggestions.add("建议添加备注或标签以便分类管理")
        }
        
        // 检查生日信息
        if (contact.birthday == null) {
            suggestions.add("建议添加生日信息以便发送祝福")
        }
        
        return suggestions
    }
    
    /**
     * 验证手机号格式
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        
        // 中国手机号: 13x, 14x, 15x, 16x, 17x, 18x, 19x
        val chinaMobilePattern = Regex("^(\\+86)?1[3-9]\\d{9}$")
        
        // 国际号码（简化验证）
        val internationalPattern = Regex("^\\+[1-9]\\d{1,14}$")
        
        return chinaMobilePattern.matches(cleaned) || internationalPattern.matches(cleaned)
    }
    
    /**
     * 验证邮箱格式
     */
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailPattern.matches(email.trim())
    }
    
    /**
     * 获取默认验证规则
     */
    private fun getDefaultValidationRules(): List<ValidationRule> {
        return listOf(
            ValidationRule(
                fieldName = "displayName",
                fieldLabel = "姓名",
                required = true,
                minLength = 2,
                maxLength = 50,
                pattern = "^[\\p{L}\\s·.]+$", // 允许字母、空格、点、中文间隔符
                patternDescription = "只能包含字母、空格和点",
                weight = 30f
            ),
            ValidationRule(
                fieldName = "phoneNumber",
                fieldLabel = "手机号",
                required = false,
                minLength = 11,
                maxLength = 20,
                pattern = "^[+0-9\\s\\-\\(\\)]+$",
                patternDescription = "只能包含数字、空格、括号和加号",
                weight = 25f,
                customValidator = { value ->
                    if (value.isNotBlank() && !isValidPhoneNumber(value)) {
                        ValidationResult(false, "手机号格式不正确")
                    } else {
                        ValidationResult(true)
                    }
                }
            ),
            ValidationRule(
                fieldName = "email",
                fieldLabel = "邮箱",
                required = false,
                maxLength = 100,
                pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                patternDescription = "标准邮箱格式",
                weight = 20f,
                customValidator = { value ->
                    if (value.isNotBlank() && !isValidEmail(value)) {
                        ValidationResult(false, "邮箱格式不正确")
                    } else {
                        ValidationResult(true)
                    }
                }
            ),
            ValidationRule(
                fieldName = "company",
                fieldLabel = "公司",
                required = false,
                maxLength = 100,
                weight = 15f
            ),
            ValidationRule(
                fieldName = "position",
                fieldLabel = "职位",
                required = false,
                maxLength = 50,
                weight = 10f
            )
        )
    }
    
    /**
     * 验证Excel列名映射
     */
    fun validateFieldMapping(
        excelHeaders: List<String>,
        fieldMappings: Map<String, String>
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 检查是否有未映射的列
        val unmappedColumns = excelHeaders.filter { header ->
            !fieldMappings.containsKey(header) || fieldMappings[header] == "unknown"
        }
        
        if (unmappedColumns.isNotEmpty()) {
            warnings.add("以下列未映射到系统字段: ${unmappedColumns.joinToString()}")
        }
        
        // 检查是否有重复映射
        val mappedFields = fieldMappings.values.filter { it != "unknown" }
        val duplicateMappings = mappedFields.groupingBy { it }.eachCount()
            .filter { it.value > 1 }
            .keys
        
        if (duplicateMappings.isNotEmpty()) {
            errors.add("以下字段被多次映射: ${duplicateMappings.joinToString()}")
        }
        
        // 检查必填字段是否被映射
        val requiredFields = listOf("displayName", "phoneNumber", "email")
        val missingRequired = requiredFields.filter { requiredField ->
            !mappedFields.contains(requiredField)
        }
        
        if (missingRequired.isNotEmpty()) {
            warnings.add("以下重要字段未被映射: ${missingRequired.joinToString()}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            score = if (excelHeaders.isNotEmpty()) {
                (fieldMappings.size - unmappedColumns.size).toFloat() / excelHeaders.size * 100
            } else 100f
        )
    }
    
    /**
     * 计算数据质量评分
     */
    fun calculateDataQualityScore(contacts: List<Contact>): DataQualityReport {
        if (contacts.isEmpty()) {
            return DataQualityReport(
                totalContacts = 0,
                averageScore = 100f,
                qualityDistribution = emptyMap(),
                commonIssues = emptyList(),
                recommendations = emptyList()
            )
        }
        
        val validationResults = contacts.map { validateContact(it) }
        val scores = validationResults.map { it.score }
        
        // 计算质量分布
        val qualityDistribution = mapOf(
            "优秀(90-100)" to scores.count { it >= 90 },
            "良好(70-89)" to scores.count { it in 70.0..89.9 },
            "一般(50-69)" to scores.count { it in 50.0..69.9 },
            "较差(0-49)" to scores.count { it < 50 }
        )
        
        // 收集常见问题
        val allErrors = validationResults.flatMap { it.errors }
        val errorFrequency = allErrors.groupingBy { it }.eachCount()
        val commonIssues = errorFrequency.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { CommonIssue(it.key, it.value) }
        
        // 生成建议
        val recommendations = generateBulkRecommendations(validationResults, contacts)
        
        return DataQualityReport(
            totalContacts = contacts.size,
            averageScore = scores.average().toFloat(),
            minScore = scores.minOrNull() ?: 0f,
            maxScore = scores.maxOrNull() ?: 100f,
            qualityDistribution = qualityDistribution,
            commonIssues = commonIssues,
            recommendations = recommendations
        )
    }
    
    /**
     * 生成批量建议
     */
    private fun generateBulkRecommendations(
        validationResults: List<ValidationResult>,
        contacts: List<Contact>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 统计缺失字段
        val missingFieldsCount = mutableMapOf<String, Int>()
        contacts.forEach { contact ->
            if (contact.phoneNumber.isNullOrBlank()) missingFieldsCount["phoneNumber"] = 
                missingFieldsCount.getOrDefault("phoneNumber", 0) + 1
            if (contact.email.isNullOrBlank()) missingFieldsCount["email"] = 
                missingFieldsCount.getOrDefault("email", 0) + 1
            if (contact.company.isNullOrBlank()) missingFieldsCount["company"] = 
                missingFieldsCount.getOrDefault("company", 0) + 1
        }
        
        missingFieldsCount.forEach { (field, count) ->
            if (count > contacts.size * 0.3) { // 超过30%的联系人缺失
                val fieldName = when (field) {
                    "phoneNumber" -> "手机号"
                    "email" -> "邮箱"
                    "company" -> "公司"
                    else -> field
                }
                recommendations.add("${count}个联系人（${(count.toFloat() / contacts.size * 100).toInt()}%）缺少$fieldName")
            }
        }
        
        // 检查数据格式问题
        val formatErrorCount = validationResults.sumOf { it.errors.count { err -> err.contains("格式") } }
        if (formatErrorCount > 0) {
            recommendations.add("发现$formatErrorCount处数据格式问题，建议统一格式")
        }
        
        // 检查重复数据模式
        val duplicatePatterns = findDuplicatePatterns(contacts)
        if (duplicatePatterns.isNotEmpty()) {
            recommendations.add("检测到可能的重复数据模式，建议进行重复检查")
        }
        
        return recommendations
    }
    
    /**
     * 查找重复数据模式
     */
    private fun findDuplicatePatterns(contacts: List<Contact>): List<String> {
        val patterns = mutableListOf<String>()
        
        // 检查相同公司相似姓名
        val companyGroups = contacts.filter { !it.company.isNullOrBlank() }
            .groupBy { it.company }
        
        companyGroups.forEach { (company, companyContacts) ->
            if (companyContacts.size > 3) {
                // 检查姓名相似度
                val nameGroups = companyContacts.groupBy { it.displayName.firstOrNull() }
                nameGroups.forEach { (firstChar, nameContacts) ->
                    if (nameContacts.size > 2) {
                        patterns.add("公司[$company]中有${nameContacts.size}个姓名以'$firstChar'开头的联系人")
                    }
                }
            }
        }
        
        return patterns
    }
}

// 数据类和枚举
data class ValidationRule(
    val fieldName: String,
    val fieldLabel: String,
    val required: Boolean = false,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val patternDescription: String? = null,
    val allowedValues: List<String>? = null,
    val customValidator: ((String) -> ValidationResult)? = null,
    val weight: Float = 10f // 验证权重，用于计算总分
)

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class CommonIssue(
    val issue: String,
    val count: Int
)

data class DataQualityReport(
    val totalContacts: Int,
    val averageScore: Float,
    val minScore: Float = 0f,
    val maxScore: Float = 100f,
    val qualityDistribution: Map<String, Int>,
    val commonIssues: List<CommonIssue>,
    val recommendations: List<String>
)