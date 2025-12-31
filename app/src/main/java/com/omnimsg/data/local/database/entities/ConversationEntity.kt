package com.omnimsg.data.local.database.entities

import androidx.room.*
import com.omnimsg.domain.models.ConversationType
import com.omnimsg.domain.models.SyncStatus
import java.util.*

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["conversation_id"], unique = true),
        Index(value = ["type"]),
        Index(value = ["last_message_at"]),
        Index(value = ["is_pinned"]),
        Index(value = ["is_muted"]),
        Index(value = ["sync_status"])
    ]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "conversation_id")
    val conversationId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "title")
    val title: String?,
    
    @ColumnInfo(name = "type")
    val type: ConversationType = ConversationType.DIRECT,
    
    @ColumnInfo(name = "participant_ids")
    val participantIds: List<String>,
    
    @ColumnInfo(name = "participant_names")
    val participantNames: Map<String, String>,
    
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String?,
    
    @ColumnInfo(name = "last_message_id")
    val lastMessageId: String?,
    
    @ColumnInfo(name = "last_message_content")
    val lastMessageContent: String?,
    
    @ColumnInfo(name = "last_message_sender_id")
    val lastMessageSenderId: String?,
    
    @ColumnInfo(name = "last_message_sender_name")
    val lastMessageSenderName: String?,
    
    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Long,
    
    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,
    
    @ColumnInfo(name = "total_message_count")
    val totalMessageCount: Int = 0,
    
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    
    @ColumnInfo(name = "is_muted")
    val isMuted: Boolean = false,
    
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "metadata")
    val metadata: Map<String, String> = emptyMap(),
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    
    @ColumnInfo(name = "sync_token")
    val syncToken: String?
) {
    fun getDisplayName(defaultName: String = "Unknown"): String {
        return title ?: when (type) {
            ConversationType.DIRECT -> {
                // 对于单聊，显示对方的名字
                val otherParticipants = participantNames.values.filter { it.isNotBlank() }
                if (otherParticipants.isNotEmpty()) otherParticipants.first() else defaultName
            }
            ConversationType.GROUP -> {
                // 对于群聊，显示参与者名字组合
                val names = participantNames.values.take(3)
                if (names.isNotEmpty()) {
                    names.joinToString(", ")
                } else {
                    defaultName
                }
            }
            ConversationType.CHANNEL -> {
                title ?: defaultName
            }
        }
    }
}