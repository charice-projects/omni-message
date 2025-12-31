package com.omnimsg.domain.models

import java.util.*

data class Message(
    val id: Long = 0,
    val messageId: String = UUID.randomUUID().toString(),
    val conversationId: Long,
    val conversationName: String? = null,
    val senderId: String,
    val senderName: String,
    val recipientId: String,
    val recipientName: String,
    val content: String,
    val contentType: MessageType = MessageType.TEXT,
    val attachments: List<Attachment> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Date = Date(),
    val status: MessageStatus = MessageStatus.SENDING,
    val readAt: Date? = null,
    val deliveredAt: Date? = null,
    val isEncrypted: Boolean = true,
    val encryptionKeyId: String? = null,
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncToken: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val isSent: Boolean
        get() = status == MessageStatus.SENT
    
    val isDelivered: Boolean
        get() = deliveredAt != null
    
    val isRead: Boolean
        get() = readAt != null
    
    val isFailed: Boolean
        get() = status == MessageStatus.FAILED
    
    val isPending: Boolean
        get() = status == MessageStatus.SENDING || status == MessageStatus.PENDING
    
    val hasAttachments: Boolean
        get() = attachments.isNotEmpty()
    
    val isOutgoing: Boolean
        get() = senderId == currentUserId // currentUserId 需要从上下文中获取
    
    val isIncoming: Boolean
        get() = !isOutgoing
    
    val formattedTime: String
        get() {
            // 格式化时间显示
            val now = Date()
            val diff = now.time - timestamp.time
            
            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> {
                    val dateFormat = java.text.SimpleDateFormat("MM/dd", Locale.getDefault())
                    dateFormat.format(timestamp)
                }
            }
        }
    
    fun markAsSent(): Message {
        return this.copy(
            status = MessageStatus.SENT,
            updatedAt = Date()
        )
    }
    
    fun markAsDelivered(): Message {
        return this.copy(
            deliveredAt = Date(),
            updatedAt = Date()
        )
    }
    
    fun markAsRead(): Message {
        return this.copy(
            readAt = Date(),
            updatedAt = Date()
        )
    }
    
    fun markAsFailed(): Message {
        return this.copy(
            status = MessageStatus.FAILED,
            updatedAt = Date()
        )
    }
    
    fun star(isStarred: Boolean = true): Message {
        return this.copy(
            isStarred = isStarred,
            updatedAt = Date()
        )
    }
    
    companion object {
        // 需要在应用初始化时设置
        var currentUserId: String = ""
        
        fun createTextMessage(
            conversationId: Long,
            senderId: String,
            senderName: String,
            recipientId: String,
            recipientName: String,
            content: String
        ): Message {
            return Message(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                recipientId = recipientId,
                recipientName = recipientName,
                content = content,
                contentType = MessageType.TEXT
            )
        }
        
        fun createImageMessage(
            conversationId: Long,
            senderId: String,
            senderName: String,
            recipientId: String,
            recipientName: String,
            imageUrl: String,
            caption: String? = null
        ): Message {
            val attachment = Attachment(
                type = AttachmentType.IMAGE,
                url = imageUrl,
                thumbnailUrl = imageUrl,
                caption = caption
            )
            
            return Message(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                recipientId = recipientId,
                recipientName = recipientName,
                content = caption ?: "图片",
                contentType = MessageType.IMAGE,
                attachments = listOf(attachment)
            )
        }
    }
}

enum class MessageType {
    TEXT, IMAGE, AUDIO, VIDEO, FILE, LOCATION, CONTACT, VOICE, SYSTEM
}

enum class MessageStatus {
    DRAFT, PENDING, SENDING, SENT, DELIVERED, READ, FAILED
}