package com.omnimsg.domain.usecases.message

import com.omnimsg.domain.models.Conversation
import com.omnimsg.domain.models.Participant
import com.omnimsg.domain.repositories.ConversationRepository
import com.omnimsg.domain.usecases.BaseUseCase
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) : BaseUseCase<CreateConversationUseCase.Params, Conversation>() {
    
    data class Params(
        val title: String? = null,
        val type: ConversationType,
        val participants: List<Participant>,
        val avatarUri: String? = null,
        val metadata: Map<String, String> = emptyMap()
    )
    
    override suspend fun execute(params: Params): Result<Conversation> {
        return try {
            // 验证参数
            validateParams(params)
            
            // 检查是否已存在相同的会话（对于直接消息）
            if (params.type == ConversationType.DIRECT && params.participants.size == 2) {
                val existingConversation = findExistingDirectConversation(params.participants)
                if (existingConversation != null) {
                    return Result.success(existingConversation)
                }
            }
            
            // 创建新会话
            val conversation = Conversation(
                title = params.title,
                type = params.type,
                participants = params.participants,
                avatarUri = params.avatarUri,
                metadata = params.metadata
            )
            
            val savedConversation = conversationRepository.saveConversation(conversation)
            Result.success(savedConversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateParams(params: Params) {
        require(params.participants.isNotEmpty()) { "Conversation must have at least one participant" }
        
        when (params.type) {
            ConversationType.DIRECT -> {
                require(params.participants.size == 2) { "Direct conversation must have exactly 2 participants" }
            }
            ConversationType.GROUP -> {
                require(params.participants.size >= 2) { "Group conversation must have at least 2 participants" }
            }
            ConversationType.CHANNEL -> {
                require(params.title?.isNotBlank() == true) { "Channel must have a title" }
            }
        }
    }
    
    private suspend fun findExistingDirectConversation(participants: List<Participant>): Conversation? {
        if (participants.size != 2) return null
        
        val currentUserId = Conversation.currentUserId
        val otherUserId = participants.find { it.userId != currentUserId }?.userId ?: return null
        
        return conversationRepository.findOrCreateDirectConversation(
            currentUserId = currentUserId,
            otherUserId = otherUserId,
            currentUserName = participants.find { it.userId == currentUserId }?.displayName ?: "You",
            otherUserName = participants.find { it.userId == otherUserId }?.displayName ?: "Unknown"
        )
    }
}