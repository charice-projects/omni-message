package com.omnimsg.domain.usecases.message

import com.omnimsg.domain.models.Conversation
import com.omnimsg.domain.models.Message
import com.omnimsg.domain.models.MessageStatus
import com.omnimsg.domain.repositories.ConversationRepository
import com.omnimsg.domain.repositories.MessageRepository
import com.omnimsg.domain.usecases.BaseUseCase
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) : BaseUseCase<SendMessageUseCase.Params, Message>() {
    
    data class Params(
        val conversationId: Long,
        val content: String,
        val contentType: MessageType = MessageType.TEXT,
        val attachments: List<Attachment> = emptyList(),
        val metadata: Map<String, String> = emptyMap()
    )
    
    override suspend fun execute(params: Params): Result<Message> {
        return try {
            // 1. 获取会话信息
            val conversation = conversationRepository.getConversationById(params.conversationId)
                ?: return Result.failure(IllegalArgumentException("Conversation not found"))
            
            // 2. 验证消息内容
            validateMessage(params)
            
            // 3. 创建消息对象
            val message = Message(
                conversationId = params.conversationId,
                conversationName = conversation.displayName,
                senderId = Message.currentUserId,
                senderName = "You", // 可以从用户信息获取
                recipientId = conversation.participants
                    .firstOrNull { it.userId != Message.currentUserId }
                    ?.userId ?: "",
                recipientName = conversation.displayName,
                content = params.content,
                contentType = params.contentType,
                attachments = params.attachments,
                metadata = params.metadata,
                status = MessageStatus.SENDING
            )
            
            // 4. 保存消息到本地数据库
            val savedMessage = messageRepository.saveMessage(message)
            
            // 5. 更新会话的最后消息
            conversationRepository.updateLastMessage(
                conversationId = params.conversationId,
                messageId = savedMessage.messageId,
                content = params.content,
                senderId = Message.currentUserId,
                senderName = "You"
            )
            
            // 6. 尝试发送到服务器（异步）
            sendToServerAsync(savedMessage)
            
            Result.success(savedMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateMessage(params: Params) {
        require(params.content.isNotBlank()) { "Message content cannot be empty" }
        
        when (params.contentType) {
            MessageType.TEXT -> {
                // 文本消息长度限制
                require(params.content.length <= 5000) { "Message too long" }
            }
            MessageType.IMAGE, MessageType.AUDIO, MessageType.VIDEO -> {
                // 媒体消息需要有附件
                require(params.attachments.isNotEmpty()) { "Media message must have attachments" }
            }
            else -> {
                // 其他类型验证
            }
        }
    }
    
    private suspend fun sendToServerAsync(message: Message) {
        // 这里实现异步发送到服务器的逻辑
        // 1. 添加到发送队列
        // 2. 尝试发送
        // 3. 更新消息状态
        
        try {
            // 模拟发送延迟
            kotlinx.coroutines.delay(1000)
            
            // 更新消息状态为已发送
            val updatedMessage = message.markAsSent()
            messageRepository.updateMessage(updatedMessage)
            
            // 如果需要，更新为已送达
            if (shouldMarkAsDelivered()) {
                kotlinx.coroutines.delay(2000)
                val deliveredMessage = updatedMessage.markAsDelivered()
                messageRepository.updateMessage(deliveredMessage)
            }
        } catch (e: Exception) {
            // 发送失败，更新状态
            val failedMessage = message.markAsFailed()
            messageRepository.updateMessage(failedMessage)
        }
    }
    
    private fun shouldMarkAsDelivered(): Boolean {
        // 根据消息类型和收件人状态决定是否标记为已送达
        return true
    }
}