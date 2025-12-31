package com.omnimsg.data.local.database.entities

import androidx.room.*
import com.omnimsg.domain.models.MessageStatus
import com.omnimsg.domain.models.MessageType
import java.util.*

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["message_id"], unique = true),
        Index(value = ["conversation_id"]),
        Index(value = ["sender_id"]),
        Index(value = ["recipient_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["is_encrypted"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "message_id")
    val messageId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "conversation_id")
    val conversationId: Long,
    
    @ColumnInfo(name = "sender_id")
    val senderId: String,
    
    @ColumnInfo(name = "sender_name")
    val senderName: String,
    
    @ColumnInfo(name = "recipient_id")
    val recipientId: String,
    
    @ColumnInfo(name = "recipient_name")
    val recipientName: String,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "content_type")
    val contentType: MessageType = MessageType.TEXT,
    
    @ColumnInfo(name = "attachment_ids")
    val attachmentIds: List<String> = emptyList(),
    
    @ColumnInfo(name = "metadata")
    val metadata: Map<String, String> = emptyMap(),
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "status")
    val status: MessageStatus = MessageStatus.SENDING,
    
    @ColumnInfo(name = "read_at")
    val readAt: Long?,
    
    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Long?,
    
    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean = true,
    
    @ColumnInfo(name = "encryption_key_id")
    val encryptionKeyId: String?,
    
    @ColumnInfo(name = "is_starred")
    val isStarred: Boolean = false,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    
    @ColumnInfo(name = "sync_token")
    val syncToken: String?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)