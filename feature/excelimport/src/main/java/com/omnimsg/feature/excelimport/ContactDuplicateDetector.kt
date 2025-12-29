// 📁 feature/excelimport/ContactDuplicateDetector.kt
package com.omnimsg.feature.excelimport

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 联系人重复检测器
 * 使用多种策略检测重复联系人
 */
@Singleton
class ContactDuplicateDetector @Inject constructor(
    private val context: Context,
    private val contactRepository: ContactRepository
) {
    
    data class DuplicateCheckResult(
        val isDuplicate: Boolean,
        val duplicateContactId: String? = null,
        val confidence: Float = 0f, // 0-1的置信度
        val matchType: MatchType? = null,
        val differences: List<String> = emptyList()
    )
    
    enum class MatchType {
        EXACT_PHONE,        // 完全相同手机号
        EXACT_EMAIL,        // 完全相同邮箱
        SIMILAR_NAME,       // 相似姓名
        SIMILAR_PHONE,      // 相似手机号（如区号不同）
        SIMILAR_COMPANY,    // 相同公司+相似姓名
        WEAK_MATCH          // 弱匹配
    }
    
    /**
     * 检测联系人是否重复
     */
    suspend fun checkDuplicate(
        contact: Contact,
        duplicateStrategy: DuplicateStrategy
    ): DuplicateCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 获取所有现有联系人进行比对
                val existingContacts = contactRepository.getAllContacts()
                
                // 2. 应用不同的检测策略
                val checkResults = listOf(
                    checkExactPhoneMatch(contact, existingContacts),
                    checkExactEmailMatch(contact, existingContacts),
                    checkSimilarNameMatch(contact, existingContacts),
                    checkSimilarPhoneMatch(contact, existingContacts),
                    checkCompanyAndNameMatch(contact, existingContacts)
                ).filter { it.isDuplicate }
                
                // 3. 根据策略决定如何处理
                return@withContext when {
                    checkResults.isEmpty() -> DuplicateCheckResult(
                        isDuplicate = false,
                        confidence = 0f
                    )
                    
                    duplicateStrategy == DuplicateStrategy.SKIP -> {
                        // 找到最高置信度的重复项
                        val bestMatch = checkResults.maxByOrNull { it.confidence }!!
                        DuplicateCheckResult(
                            isDuplicate = true,
                            duplicateContactId = bestMatch.duplicateContactId,
                            confidence = bestMatch.confidence,
                            matchType = bestMatch.matchType,
                            differences = bestMatch.differences
                        )
                    }
                    
                    duplicateStrategy == DuplicateStrategy.MERGE -> {
                        // 智能合并：找到最适合合并的重复项
                        findBestMergeCandidate(contact, checkResults)
                    }
                    
                    else -> DuplicateCheckResult(isDuplicate = false)
                }
            } catch (e: Exception) {
                logger.e("ContactDuplicateDetector", "重复检测失败", e)
                DuplicateCheckResult(isDuplicate = false)
            }
        }
    }
    
    /**
     * 精确手机号匹配
     */
    private fun checkExactPhoneMatch(
        contact: Contact,
        existingContacts: List<Contact>
    ): DuplicateCheckResult {
        if (contact.phoneNumber.isNullOrBlank()) {
            return DuplicateCheckResult(isDuplicate = false)
        }
        
        // 标准化手机号（移除空格、横线等）
        val normalizedPhone = normalizePhoneNumber(contact.phoneNumber)
        
        existingContacts.forEach { existing ->
            val existingPhone = normalizePhoneNumber(existing.phoneNumber)
            if (normalizedPhone == existingPhone && normalizedPhone.isNotBlank()) {
                return DuplicateCheckResult(
                    isDuplicate = true,
                    duplicateContactId = existing.id,
                    confidence = 0.95f,
                    matchType = MatchType.EXACT_PHONE,
                    differences = calculateDifferences(contact, existing)
                )
            }
        }
        
        return DuplicateCheckResult(isDuplicate = false)
    }
    
    /**
     * 精确邮箱匹配
     */
    private fun checkExactEmailMatch(
        contact: Contact,
        existingContacts: List<Contact>
    ): DuplicateCheckResult {
        if (contact.email.isNullOrBlank()) {
            return DuplicateCheckResult(isDuplicate = false)
        }
        
        val normalizedEmail = contact.email.trim().lowercase()
        
        existingContacts.forEach { existing ->
            val existingEmail = existing.email?.trim()?.lowercase()
            if (normalizedEmail == existingEmail && normalizedEmail.isNotBlank()) {
                return DuplicateCheckResult(
                    isDuplicate = true,
                    duplicateContactId = existing.id,
                    confidence = 0.90f,
                    matchType = MatchType.EXACT_EMAIL,
                    differences = calculateDifferences(contact, existing)
                )
            }
        }
        
        return DuplicateCheckResult(isDuplicate = false)
    }
    
    /**
     * 相似姓名匹配（使用字符串相似度算法）
     */
    private fun checkSimilarNameMatch(
        contact: Contact,
        existingContacts: List<Contact>
    ): DuplicateCheckResult {
        if (contact.displayName.isBlank()) {
            return DuplicateCheckResult(isDuplicate = false)
        }
        
        val name1 = contact.displayName.trim()
        
        existingContacts.forEach { existing ->
            val name2 = existing.displayName.trim()
            
            // 计算姓名相似度
            val similarity = calculateNameSimilarity(name1, name2)
            
            if (similarity >= 0.85) { // 85%相似度阈值
                return DuplicateCheckResult(
                    isDuplicate = true,
                    duplicateContactId = existing.id,
                    confidence = similarity * 0.8f, // 降低权重
                    matchType = MatchType.SIMILAR_NAME,
                    differences = calculateDifferences(contact, existing)
                )
            }
        }
        
        return DuplicateCheckResult(isDuplicate = false)
    }
    
    /**
     * 相似手机号匹配（考虑区号、国家代码）
     */
    private fun checkSimilarPhoneMatch(
        contact: Contact,
        existingContacts: List<Contact>
    ): DuplicateCheckResult {
        if (contact.phoneNumber.isNullOrBlank()) {
            return DuplicateCheckResult(isDuplicate = false)
        }
        
        val phone1 = normalizePhoneNumber(contact.phoneNumber)
        
        existingContacts.forEach { existing ->
            val phone2 = normalizePhoneNumber(existing.phoneNumber)
            
            if (phone1.isNotBlank() && phone2.isNotBlank()) {
                // 移除国家代码和区号后比较
                val basePhone1 = extractBasePhoneNumber(phone1)
                val basePhone2 = extractBasePhoneNumber(phone2)
                
                if (basePhone1 == basePhone2 && basePhone1.length >= 7) {
                    return DuplicateCheckResult(
                        isDuplicate = true,
                        duplicateContactId = existing.id,
                        confidence = 0.75f,
                        matchType = MatchType.SIMILAR_PHONE,
                        differences = calculateDifferences(contact, existing)
                    )
                }
            }
        }
        
        return DuplicateCheckResult(isDuplicate = false)
    }
    
    /**
     * 公司+姓名组合匹配
     */
    private fun checkCompanyAndNameMatch(
        contact: Contact,
        existingContacts: List<Contact>
    ): DuplicateCheckResult {
        if (contact.displayName.isBlank() || contact.company.isNullOrBlank()) {
            return DuplicateCheckResult(isDuplicate = false)
        }
        
        val name1 = contact.displayName.trim()
        val company1 = contact.company!!.trim()
        
        existingContacts.forEach { existing ->
            val name2 = existing.displayName.trim()
            val company2 = existing.company?.trim()
            
            if (company2 != null) {
                // 公司名称相似度
                val companySimilarity = calculateStringSimilarity(company1, company2)
                val nameSimilarity = calculateNameSimilarity(name1, name2)
                
                if (companySimilarity >= 0.9 && nameSimilarity >= 0.7) {
                    return DuplicateCheckResult(
                        isDuplicate = true,
                        duplicateContactId = existing.id,
                        confidence = (companySimilarity * 0.6f + nameSimilarity * 0.4f) * 0.7f,
                        matchType = MatchType.SIMILAR_COMPANY,
                        differences = calculateDifferences(contact, existing)
                    )
                }
            }
        }
        
        return DuplicateCheckResult(isDuplicate = false)
    }
    
    /**
     * 找到最适合合并的重复项
     */
    private fun findBestMergeCandidate(
        contact: Contact,
        duplicateResults: List<DuplicateCheckResult>
    ): DuplicateCheckResult {
        // 优先选择高置信度、信息更完整的联系人
        val scoredResults = duplicateResults.map { result ->
            val score = calculateMergeScore(contact, result)
            Pair(result, score)
        }.sortedByDescending { it.second }
        
        return scoredResults.firstOrNull()?.first ?: duplicateResults.first()
    }
    
    /**
     * 计算合并得分
     */
    private fun calculateMergeScore(
        contact: Contact,
        duplicateResult: DuplicateCheckResult
    ): Float {
        var score = duplicateResult.confidence
        
        // 根据匹配类型调整分数
        when (duplicateResult.matchType) {
            MatchType.EXACT_PHONE -> score *= 1.2f
            MatchType.EXACT_EMAIL -> score *= 1.1f
            MatchType.SIMILAR_NAME -> score *= 0.9f
            MatchType.SIMILAR_PHONE -> score *= 0.8f
            else -> score *= 0.7f
        }
        
        // 差异数量越少，得分越高
        score *= (1.0f - duplicateResult.differences.size * 0.05f).coerceAtLeast(0.5f)
        
        return score
    }
    
    /**
     * 计算两个联系人的差异
     */
    private fun calculateDifferences(contact1: Contact, contact2: Contact): List<String> {
        val differences = mutableListOf<String>()
        
        // 比较姓名
        if (contact1.displayName != contact2.displayName) {
            differences.add("姓名: ${contact1.displayName} -> ${contact2.displayName}")
        }
        
        // 比较手机号
        if (contact1.phoneNumber != contact2.phoneNumber) {
            differences.add("手机号: ${contact1.phoneNumber} -> ${contact2.phoneNumber}")
        }
        
        // 比较邮箱
        if (contact1.email != contact2.email) {
            differences.add("邮箱: ${contact1.email} -> ${contact2.email}")
        }
        
        // 比较公司
        if (contact1.company != contact2.company) {
            differences.add("公司: ${contact1.company} -> ${contact2.company}")
        }
        
        return differences
    }
    
    /**
     * 标准化手机号
     */
    private fun normalizePhoneNumber(phone: String?): String {
        return phone?.replace(Regex("[\\s\\-\\(\\)]"), "") ?: ""
    }
    
    /**
     * 提取基础手机号（移除国家代码和区号）
     */
    private fun extractBasePhoneNumber(phone: String): String {
        // 简单实现：取最后10位（假设是标准手机号）
        return if (phone.length >= 10) {
            phone.substring(phone.length - 10)
        } else {
            phone
        }
    }
    
    /**
     * 计算姓名相似度
     */
    private fun calculateNameSimilarity(name1: String, name2: String): Float {
        // 简单的相似度计算，实际可以使用更复杂的算法
        if (name1 == name2) return 1.0f
        
        // 移除空格
        val cleanName1 = name1.replace(" ", "")
        val cleanName2 = name2.replace(" ", "")
        
        if (cleanName1 == cleanName2) return 0.95f
        
        // 使用编辑距离
        val distance = calculateLevenshteinDistance(cleanName1, cleanName2)
        val maxLength = maxOf(cleanName1.length, cleanName2.length)
        
        return 1.0f - distance.toFloat() / maxLength
    }
    
    /**
     * 计算字符串相似度
     */
    private fun calculateStringSimilarity(str1: String, str2: String): Float {
        if (str1 == str2) return 1.0f
        
        // 转换为小写比较
        val lower1 = str1.lowercase()
        val lower2 = str2.lowercase()
        
        if (lower1 == lower2) return 0.95f
        
        // 检查包含关系
        if (lower1.contains(lower2) || lower2.contains(lower1)) {
            return 0.8f
        }
        
        // 使用编辑距离
        val distance = calculateLevenshteinDistance(lower1, lower2)
        val maxLength = maxOf(lower1.length, lower2.length)
        
        return 1.0f - distance.toFloat() / maxLength
    }
    
    /**
     * 计算Levenshtein距离（编辑距离）
     */
    private fun calculateLevenshteinDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,     // 删除
                    dp[i][j - 1] + 1,     // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * 批量检测重复
     */
    suspend fun batchCheckDuplicates(
        contacts: List<Contact>,
        duplicateStrategy: DuplicateStrategy
    ): List<Pair<Contact, DuplicateCheckResult>> {
        return withContext(Dispatchers.Default) {
            contacts.map { contact ->
                val result = checkDuplicate(contact, duplicateStrategy)
                Pair(contact, result)
            }
        }
    }
    
    /**
     * 智能合并联系人
     */
    suspend fun mergeContacts(
        sourceContact: Contact,
        targetContactId: String,
        mergeStrategy: MergeStrategy = MergeStrategy.PRESERVE_BEST
    ): Contact {
        return withContext(Dispatchers.IO) {
            try {
                val targetContact = contactRepository.getContactById(targetContactId)
                    ?: throw IllegalArgumentException("目标联系人不存在: $targetContactId")
                
                // 根据合并策略合并联系人信息
                val mergedContact = when (mergeStrategy) {
                    MergeStrategy.PRESERVE_BEST -> mergePreserveBest(sourceContact, targetContact)
                    MergeStrategy.PRESERVE_TARGET -> mergePreserveTarget(sourceContact, targetContact)
                    MergeStrategy.PRESERVE_SOURCE -> mergePreserveSource(sourceContact, targetContact)
                    MergeStrategy.MANUAL -> mergeManual(sourceContact, targetContact)
                }
                
                // 更新联系人
                contactRepository.updateContact(mergedContact)
                
                // 记录合并历史
                recordMergeHistory(sourceContact.id, targetContactId, mergeStrategy)
                
                mergedContact
            } catch (e: Exception) {
                logger.e("ContactDuplicateDetector", "合并联系人失败", e)
                throw e
            }
        }
    }
    
    /**
     * 保留最优信息合并
     */
    private fun mergePreserveBest(
        source: Contact,
        target: Contact
    ): Contact {
        return target.copy(
            displayName = selectBestValue(source.displayName, target.displayName),
            phoneNumber = selectBestPhone(source.phoneNumber, target.phoneNumber),
            secondaryPhone = selectBestPhone(source.secondaryPhone, target.secondaryPhone),
            email = selectBestEmail(source.email, target.email),
            company = selectBestValue(source.company, target.company),
            position = selectBestValue(source.position, target.position),
            address = selectBestValue(source.address, target.address),
            notes = mergeNotes(source.notes, target.notes),
            tags = (source.tags + target.tags).distinct(),
            customFields = mergeCustomFields(source.customFields, target.customFields),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 选择最优值
     */
    private fun selectBestValue(value1: String?, value2: String?): String? {
        return when {
            value2.isNullOrBlank() -> value1
            value1.isNullOrBlank() -> value2
            value1.length > value2.length -> value1 // 假设更长的信息更完整
            else -> value2
        }
    }
    
    /**
     * 选择最优手机号
     */
    private fun selectBestPhone(phone1: String?, phone2: String?): String? {
        return when {
            phone2.isNullOrBlank() -> phone1
            phone1.isNullOrBlank() -> phone2
            phone1.length >= 11 && phone2.length < 11 -> phone1 // 优先完整手机号
            phone2.length >= 11 && phone1.length < 11 -> phone2
            else -> phone2 // 默认保留目标手机号
        }
    }
    
    /**
     * 选择最优邮箱
     */
    private fun selectBestEmail(email1: String?, email2: String?): String? {
        return when {
            email2.isNullOrBlank() -> email1
            email1.isNullOrBlank() -> email2
            email1.contains("@") && !email2.contains("@") -> email1
            email2.contains("@") && !email1.contains("@") -> email2
            else -> email2 // 默认保留目标邮箱
        }
    }
    
    /**
     * 合并备注
     */
    private fun mergeNotes(notes1: String?, notes2: String?): String? {
        return when {
            notes1.isNullOrBlank() && notes2.isNullOrBlank() -> null
            notes1.isNullOrBlank() -> notes2
            notes2.isNullOrBlank() -> notes1
            else -> "$notes2\n---\n$notes1"
        }
    }
    
    /**
     * 合并自定义字段
     */
    private fun mergeCustomFields(
        fields1: Map<String, String>,
        fields2: Map<String, String>
    ): Map<String, String> {
        val merged = mutableMapOf<String, String>()
        merged.putAll(fields2) // 先添加目标字段
        fields1.forEach { (key, value) ->
            if (!merged.containsKey(key) && value.isNotBlank()) {
                merged[key] = value
            }
        }
        return merged
    }
    
    private fun mergePreserveTarget(source: Contact, target: Contact): Contact {
        // 保留目标，仅添加源中目标没有的信息
        return target.copy(
            secondaryPhone = target.secondaryPhone ?: source.secondaryPhone,
            notes = target.notes ?: source.notes,
            tags = (target.tags + source.tags).distinct(),
            customFields = mergeCustomFields(source.customFields, target.customFields),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun mergePreserveSource(source: Contact, target: Contact): Contact {
        // 使用源信息替换目标信息
        return source.copy(id = target.id, createdAt = target.createdAt)
    }
    
    private fun mergeManual(source: Contact, target: Contact): Contact {
        // 需要用户手动选择，这里返回目标联系人（实际应该由UI处理）
        return target
    }
    
    /**
     * 记录合并历史
     */
    private suspend fun recordMergeHistory(
        sourceId: String,
        targetId: String,
        strategy: MergeStrategy
    ) {
        // 实现合并历史记录逻辑
        // 这里可以保存到数据库或日志文件
    }
}

// 枚举定义
enum class DuplicateStrategy {
    SKIP,           // 跳过重复项
    MERGE,          // 合并重复项
    REPLACE,        // 替换重复项
    KEEP_BOTH       // 保留两者
}

enum class MergeStrategy {
    PRESERVE_BEST,  // 保留最优信息
    PRESERVE_TARGET,// 保留目标信息为主
    PRESERVE_SOURCE,// 保留源信息为主
    MANUAL          // 手动选择
}

enum class Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class FieldMappingAnalysis(
    val totalFields: Int,
    val autoMapped: Int,
    val manualMapped: Int,
    val ambiguous: Int,
    val confidence: Float,
    val suggestions: List<String>
)

data class CommonIssue(
    val type: String,
    val description: String,
    val affectedRows: Int,
    val severity: Severity
)

data class ImportStatistics(
    val totalRows: Int,
    val successfulRows: Int,
    val failedRows: Int,
    val duplicateRows: Int,
    val skippedRows: Int,
    val successRate: Float,
    val averageProcessingTimeMs: Long,
    val fastestRowMs: Long,
    val slowestRowMs: Long
)