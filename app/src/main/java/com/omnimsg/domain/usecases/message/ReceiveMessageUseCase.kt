package com.omnimsg.domain.usecases.message

import com.omnimsg.domain.models.Message
import com.omnimsg.domain.models.MessageStatus
import com.omnimsg.domain.repositories.ConversationRepository
import com.omnimsg.domain.repositories.MessageRepository
import com.omnimsg.domain.usecases.BaseUseCase
import javax.inject.Inject

class ReceiveMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) : BaseUseCase<Message, Message>() {
    
    override suspend fun execute(params: Message): Result<Message> {
        return try {
            // 1. 验证消息
            validateIncomingMessage(params)
            
            // 2. 检查消息是否已存在（防止重复接收）
            val existingMessage = messageRepository.getMessageByMessageId(params.messageId)
            if (existingMessage != null) {
                return Result.success(existingMessage)
            }
            
            // 3. 确保会话存在
            val conversation = ensureConversationExists(params)
            
            // 4. 保存消息
            val savedMessage = messageRepository.saveMessage(params.copy(status = MessageStatus.SENT))
            
            // 5. 更新会话信息
            conversationRepository.updateLastMessage(
                conversationId = conversation.id,
                messageId = savedMessage.messageId,
                content = savedMessage.content,
                senderId = savedMessage.senderId,
                senderName = savedMessage.senderName
            )
            
            // 6. 触发消息通知（如果会话没有静音）
            if (!conversation.isMuted) {
                triggerNotification(savedMessage, conversation)
            }
            
            Result.success(savedMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateIncomingMessage(message: Message) {
        require(message.senderId.isNotBlank()) { "Sender ID cannot be empty" }
        require(message.recipientId.isNotBlank()) { "Recipient ID cannot be empty" }
        require(message.content.isNotBlank()) { "Message content cannot be empty" }
        require(message.conversationId > 0) { "Conversation ID is invalid" }
    }
    
    private suspend fun ensureConversationExists(message: Message): Conversation {
        // 查找现有会话
        val existingConversation = conversationRepository.getConversationById(message.conversationId)
        
        return existingConversation ?: run {
            // 创建新会话
            // 这里需要根据消息类型创建相应的会话
            val conversation = when (message.conversationId) {
                // 对于直接消息，创建直接会话
                else -> Conversation.createDirectConversation(
                    participant1 = Participant(
                        userId = message.senderId,
                        displayName = message.senderName
                    ),
                    participant2 = Participant(
                        userId = message.recipientId,
                        displayName = message.recipientName
                    )
                )
            }
            
            conversationRepository.saveConversation(conversation)
        }
    }
    
    private fun triggerNotification(message: Message, conversation: Conversation) {
        // 这里实现消息通知逻辑
        // 可以集成系统的通知服务
        // 包括显示通知、播放声音等
        
        val notificationData = mapOf(
            "messageId" to message.messageId,
            "conversationId" to conversation.conversationId,
            "senderName" to message.senderName,
            "content" to message.content,
            "timestamp" to message.timestamp.toString()
        )
        
        // 调用通知管理器
        // notificationManager.showMessageNotification(notificationData)
    }
}