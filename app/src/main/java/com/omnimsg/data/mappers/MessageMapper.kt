package com.omnimsg.data.mappers

import com.omnimsg.data.local.database.entities.MessageEntity
import com.omnimsg.domain.models.Message
import com.omnimsg.domain.models.MessageType
import java.util.*

object MessageMapper {
    
    fun toEntity(message: Message): MessageEntity {
        return MessageEntity(
            id = message.id,
            messageId = message.messageId,
            conversationId = message.conversationId,
            senderId = message.senderId,
            senderName = message.senderName,
            recipientId = message.recipientId,
            recipientName = message.recipientName,
            content = message.content,
            contentType = message.contentType,
            attachmentIds = message.attachments.map { it.id },
            metadata = message.metadata,
            timestamp = message.timestamp.time,
            status = message.status,
            readAt = message.readAt?.time,
            deliveredAt = message.deliveredAt?.time,
            isEncrypted = message.isEncrypted,
            encryptionKeyId = message.encryptionKeyId,
            isStarred = message.isStarred,
            isDeleted = message.isDeleted,
            syncStatus = message.syncStatus,
            syncToken = message.syncToken,
            createdAt = message.createdAt.time,
            updatedAt = message.updatedAt.time
        )
    }
    
    fun fromEntity(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            messageId = entity.messageId,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            recipientId = entity.recipientId,
            recipientName = entity.recipientName,
            content = entity.content,
            contentType = entity.contentType,
            attachments = emptyList(), // 需要从附件DAO获取
            metadata = entity.metadata,
            timestamp = Date(entity.timestamp),
            status = entity.status,
            readAt = entity.readAt?.let { Date(it) },
            deliveredAt = entity.deliveredAt?.let { Date(it) },
            isEncrypted = entity.isEncrypted,
            encryptionKeyId = entity.encryptionKeyId,
            isStarred = entity.isStarred,
            isDeleted = entity.isDeleted,
            syncStatus = entity.syncStatus,
            syncToken = entity.syncToken,
            createdAt = Date(entity.createdAt),
            updatedAt = Date(entity.updatedAt)
        )
    }
    
    fun fromEntities(entities: List<MessageEntity>): List<Message> {
        return entities.map { fromEntity(it) }
    }
    
    fun toEntities(messages: List<Message>): List<MessageEntity> {
        return messages.map { toEntity(it) }
    }
}