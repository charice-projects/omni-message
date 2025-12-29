package com.omnimsg.domain.usecases.message

import com.omnimsg.domain.models.Message
import com.omnimsg.domain.repositories.MessageRepository
import com.omnimsg.domain.usecases.BaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) : BaseUseCase<GetMessagesUseCase.Params, Flow<List<Message>>>() {
    
    data class Params(
        val conversationId: Long,
        val limit: Int = 50,
        val offset: Int = 0,
        val includeDeleted: Boolean = false
    )
    
    override suspend fun execute(params: Params): Result<Flow<List<Message>>> {
        return try {
            val messagesFlow = if (params.includeDeleted) {
                messageRepository.getMessagesByConversationIncludingDeleted(params.conversationId)
            } else {
                messageRepository.getMessagesByConversation(params.conversationId)
            }
            
            // 应用分页逻辑
            val paginatedFlow = messagesFlow.map { messages ->
                messages
                    .sortedByDescending { it.timestamp }
                    .drop(params.offset)
                    .take(params.limit)
                    .sortedBy { it.timestamp } // 按时间升序显示
            }
            
            Result.success(paginatedFlow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SearchMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) : BaseUseCase<String, Flow<List<Message>>>() {
    
    override suspend fun execute(params: String): Result<Flow<List<Message>>> {
        return try {
            val query = params.trim()
            
            if (query.isEmpty()) {
                Result.failure(IllegalArgumentException("Search query cannot be empty"))
            } else {
                val searchResults = messageRepository.searchMessages(query)
                Result.success(searchResults)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MarkMessageAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) : BaseUseCase<Long, Boolean>() {
    
    override suspend fun execute(params: Long): Result<Boolean> {
        return try {
            // 1. 获取消息
            val message = messageRepository.getMessageById(params)
                ?: return Result.failure(IllegalArgumentException("Message not found"))
            
            // 2. 如果消息已经读过，直接返回
            if (message.isRead) {
                return Result.success(true)
            }
            
            // 3. 标记消息为已读
            val updatedMessage = message.markAsRead()
            val messageUpdated = messageRepository.updateMessage(updatedMessage)
            
            // 4. 更新会话的未读计数
            if (messageUpdated) {
                conversationRepository.markConversationAsRead(message.conversationId)
            }
            
            Result.success(messageUpdated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}