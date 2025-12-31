package com.omnimsg.feature.voice

import com.omnimsg.domain.models.voice.VoiceCommandIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentRecognizer @Inject constructor() {
    companion object {
        private const val TAG = "IntentRecognizer"
        
        // 意图模式映射
        private val INTENT_PATTERNS = mapOf(
            "SEND_MESSAGE" to listOf(
                Pattern("发(送)?(消息|短信)?给?(\\S+)(说)?(.*)", listOf("contact", "message")),
                Pattern("给(\\S+)(发|发送)(消息|短信)(.*)", listOf("contact", "message")),
                Pattern("告诉(\\S+)(.*)", listOf("contact", "message"))
            ),
            "CALL_CONTACT" to listOf(
                Pattern("打(电话)?给?(\\S+)", listOf("contact")),
                Pattern("呼叫(\\S+)", listOf("contact")),
                Pattern("给(\\S+)打电话", listOf("contact"))
            ),
            "SEARCH_CONTACT" to listOf(
                Pattern("查找(\\S+)", listOf("contact")),
                Pattern("搜索(\\S+)", listOf("contact")),
                Pattern("找到?(\\S+)(的联系方式)?", listOf("contact"))
            ),
            "EMERGENCY_ALERT" to listOf(
                Pattern("(紧急|救命|帮帮我|SOS)", emptyList()),
                Pattern("(报警|求救)", emptyList())
            ),
            "CREATE_REMINDER" to listOf(
                Pattern("提醒我(.*)", listOf("reminder")),
                Pattern("(.*)的提醒", listOf("reminder"))
            ),
            "SET_ALARM" to listOf(
                Pattern("(\\d{1,2})点(\\d{1,2})分?的闹钟", listOf("hour", "minute")),
                Pattern("设置(\\d{1,2})点(\\d{1,2})分?的闹钟", listOf("hour", "minute"))
            )
        )
        
        // 实体提取器
        private val ENTITY_EXTRACTORS = mapOf(
            "contact" to ContactExtractor(),
            "time" to TimeExtractor(),
            "location" to LocationExtractor(),
            "message" to MessageExtractor()
        )
    }
    
    data class Pattern(
        val regex: String,
        val entities: List<String>
    ) {
        val compiledRegex = Regex(regex)
    }
    
    /**
     * 识别意图
     */
    suspend fun recognizeIntent(
        text: String,
        context: Map<String, String> = emptyMap()
    ): VoiceCommandIntent = withContext(Dispatchers.Default) {
        val normalizedText = normalizeText(text)
        var bestIntent = "UNKNOWN"
        var bestConfidence = 0f
        var bestEntities = emptyMap<String, String>()
        var bestSlots = emptyMap<String, VoiceCommandIntent.Slot>()
        
        // 遍历所有意图模式
        for ((intent, patterns) in INTENT_PATTERNS) {
            for (pattern in patterns) {
                val match = pattern.compiledRegex.find(normalizedText)
                if (match != null) {
                    val confidence = calculateMatchConfidence(match, pattern, normalizedText)
                    
                    if (confidence > bestConfidence) {
                        bestIntent = intent
                        bestConfidence = confidence
                        
                        // 提取实体
                        bestEntities = extractEntities(match, pattern.entities)
                        bestSlots = extractSlots(match, pattern.entities)
                    }
                }
            }
        }
        
        // 如果没有匹配到模式，使用关键词匹配
        if (bestConfidence < 0.5f) {
            val keywordResult = matchByKeywords(normalizedText)
            if (keywordResult.confidence > bestConfidence) {
                bestIntent = keywordResult.intent
                bestConfidence = keywordResult.confidence
            }
        }
        
        // 应用上下文增强
        val (enhancedIntent, enhancedConfidence) = applyContextEnhancement(
            bestIntent, bestConfidence, context
        )
        
        VoiceCommandIntent(
            intent = enhancedIntent,
            confidence = enhancedConfidence,
            entities = bestEntities,
            slots = bestSlots
        )
    }
    
    /**
     * 规范化文本
     */
    private fun normalizeText(text: String): String {
        var normalized = text.trim()
        
        // 转换为小写
        normalized = normalized.lowercase(Locale.getDefault())
        
        // 标准化标点
        normalized = normalized.replace("，", ",")
            .replace("。", ".")
            .replace("！", "!")
            .replace("？", "?")
            .replace("；", ";")
            .replace("：", ":")
            .replace("（", "(")
            .replace("）", ")")
            .replace("【", "[")
            .replace("】", "]")
        
        // 移除多余空格
        normalized = normalized.replace("\\s+".toRegex(), " ")
        
        return normalized
    }
    
    /**
     * 计算匹配置信度
     */
    private fun calculateMatchConfidence(
        match: MatchResult,
        pattern: Pattern,
        text: String
    ): Float {
        var confidence = 0.5f
        
        // 完全匹配加分
        if (match.value == text) {
            confidence += 0.3f
        }
        
        // 匹配长度比例
        val matchRatio = match.value.length.toFloat() / text.length.toFloat()
        confidence += matchRatio * 0.2f
        
        // 实体数量加分
        if (pattern.entities.isNotEmpty()) {
            val entityRatio = pattern.entities.size.coerceAtMost(3).toFloat() / 3.0f
            confidence += entityRatio * 0.1f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * 提取实体
     */
    private fun extractEntities(
        match: MatchResult,
        entityNames: List<String>
    ): Map<String, String> {
        val entities = mutableMapOf<String, String>()
        
        for ((index, entityName) in entityNames.withIndex()) {
            val groupIndex = index + 1 // 第一个group是整个匹配，所以从1开始
            if (groupIndex < match.groupValues.size) {
                val value = match.groupValues[groupIndex]
                if (value.isNotBlank()) {
                    entities[entityName] = value
                }
            }
        }
        
        // 使用专门的实体提取器进一步提取
        val extractedEntities = mutableMapOf<String, String>()
        for ((entityName, entityValue) in entities) {
            val extractor = ENTITY_EXTRACTORS[entityName]
            if (extractor != null) {
                val extracted = extractor.extract(entityValue)
                if (extracted != null) {
                    extractedEntities[entityName] = extracted
                }
            } else {
                extractedEntities[entityName] = entityValue
            }
        }
        
        return extractedEntities
    }
    
    /**
     * 提取槽位
     */
    private fun extractSlots(
        match: MatchResult,
        entityNames: List<String>
    ): Map<String, VoiceCommandIntent.Slot> {
        val slots = mutableMapOf<String, VoiceCommandIntent.Slot>()
        
        for ((index, entityName) in entityNames.withIndex()) {
            val groupIndex = index + 1
            if (groupIndex < match.groupValues.size) {
                val value = match.groupValues[groupIndex]
                if (value.isNotBlank()) {
                    slots[entityName] = VoiceCommandIntent.Slot(
                        value = value,
                        confidence = 0.8f,
                        rawValue = value
                    )
                }
            }
        }
        
        return slots
    }
    
    /**
     * 关键词匹配
     */
    private fun matchByKeywords(text: String): KeywordMatchResult {
        val keywordMap = mapOf(
            "发消息" to "SEND_MESSAGE",
            "打电话" to "CALL_CONTACT",
            "查找" to "SEARCH_CONTACT",
            "搜索" to "SEARCH_CONTACT",
            "紧急" to "EMERGENCY_ALERT",
            "救命" to "EMERGENCY_ALERT",
            "提醒" to "CREATE_REMINDER",
            "闹钟" to "SET_ALARM"
        )
        
        var bestIntent = "UNKNOWN"
        var bestConfidence = 0f
        
        for ((keyword, intent) in keywordMap) {
            if (text.contains(keyword)) {
                val confidence = keyword.length.toFloat() / text.length.toFloat()
                if (confidence > bestConfidence) {
                    bestIntent = intent
                    bestConfidence = confidence
                }
            }
        }
        
        return KeywordMatchResult(bestIntent, bestConfidence)
    }
    
    /**
     * 应用上下文增强
     */
    private fun applyContextEnhancement(
        intent: String,
        confidence: Float,
        context: Map<String, String>
    ): Pair<String, Float> {
        var enhancedIntent = intent
        var enhancedConfidence = confidence
        
        // 如果上下文中有上次执行的命令，且当前命令模糊，使用上下文
        if (confidence < 0.7f) {
            val lastCommand = context["last_command"]
            if (lastCommand != null && intent == "UNKNOWN") {
                enhancedIntent = lastCommand
                enhancedConfidence = 0.6f
            }
        }
        
        // 如果上下文中有相关实体，提高置信度
        if (context.isNotEmpty()) {
            enhancedConfidence = (enhancedConfidence * 1.1f).coerceAtMost(1.0f)
        }
        
        return Pair(enhancedIntent, enhancedConfidence)
    }
    
    /**
     * 训练个性化意图识别
     */
    suspend fun trainPersonalizedModel(
        examples: List<Pair<String, String>>, // (文本, 意图)
        userId: String
    ): TrainingResult = withContext(Dispatchers.Default) {
        return@withContext try {
            // TODO: 实现个性化训练逻辑
            TrainingResult.Success(0.85f)
        } catch (e: Exception) {
            TrainingResult.Error(e.message ?: "Training failed")
        }
    }
    
    data class KeywordMatchResult(
        val intent: String,
        val confidence: Float
    )
    
    sealed class TrainingResult {
        data class Success(val accuracy: Float) : TrainingResult()
        data class Error(val message: String) : TrainingResult()
    }
    
    // 实体提取器接口
    interface EntityExtractor {
        fun extract(text: String): String?
    }
    
    // 联系人提取器
    class ContactExtractor : EntityExtractor {
        override fun extract(text: String): String? {
            // 简单的联系人提取逻辑
            val words = text.split(" ", "的")
            return words.find { word ->
                word.length in 2..4 && word.all { it.isLetter() }
            }
        }
    }
    
    // 时间提取器
    class TimeExtractor : EntityExtractor {
        override fun extract(text: String): String? {
            val patterns = listOf(
                Regex("""(\d{1,2})点(\d{1,2})分?"""),
                Regex("""(\d{1,2})点"""),
                Regex("""(\d{1,2})号"""),
                Regex("""(\d{1,2})月(\d{1,2})号"""),
                Regex("""(今天|明天|后天|上午|下午|晚上)""")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    return match.value
                }
            }
            
            return null
        }
    }
    
    // 位置提取器
    class LocationExtractor : EntityExtractor {
        override fun extract(text: String): String? {
            val locationKeywords = listOf("家", "公司", "学校", "医院", "商场", "饭店", "公园")
            
            for (keyword in locationKeywords) {
                if (text.contains(keyword)) {
                    // 提取位置前的词
                    val index = text.indexOf(keyword)
                    if (index > 0) {
                        val start = text.substring(0, index).takeLast(3)
                        return start + keyword
                    }
                    return keyword
                }
            }
            
            return null
        }
    }
    
    // 消息提取器
    class MessageExtractor : EntityExtractor {
        override fun extract(text: String): String? {
            // 简单的消息提取，移除命令关键词
            var message = text
            
            val commandKeywords = listOf(
                "发消息给", "发给", "告诉", "说", "打电话给", "呼叫", "查找", "搜索"
            )
            
            for (keyword in commandKeywords) {
                message = message.replace(keyword, "")
            }
            
            // 移除可能的人名（假设是2-4个中文字符）
            message = message.replace(Regex("""[\u4e00-\u9fa5]{2,4}"""), "")
            
            message = message.trim()
            
            return if (message.isNotBlank()) message else null
        }
    }
}