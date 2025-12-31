package com.omnimsg.domain.models

import java.util.*

data class Conversation(
    val id: Long = 0,
    val conversationId: String = UUID.randomUUID().toString(),
    val title: String? = null,
    val type: ConversationType = ConversationType.DIRECT,
    val participants: List<Participant> = emptyList(),
    val avatarUri: String? = null,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val totalMessageCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncToken: String? = null
) {
    val displayName: String
        get() = title ?: when (type) {
            ConversationType.DIRECT -> {
                // 对于单聊，显示对方的名字
                val otherParticipants = participants.filter { it.userId != currentUserId }
                otherParticipants.firstOrNull()?.displayName ?: "Unknown"
            }
            ConversationType.GROUP -> {
                // 对于群聊，显示参与者名字组合
                val names = participants.take(3).map { it.displayName }
                if (names.isNotEmpty()) {
                    names.joinToString(", ")
                } else {
                    "Group"
                }
            }
            ConversationType.CHANNEL -> {
                title ?: "Channel"
            }
        }
    
    val hasUnreadMessages: Boolean
        get() = unreadCount > 0 && !isMuted
    
    val formattedLastMessageTime: String
        get() {
            val timestamp = lastMessage?.timestamp ?: updatedAt
            return formatTimestamp(timestamp)
        }
    
    val lastMessagePreview: String
        get() {
            return when {
                lastMessage == null -> "No messages yet"
                lastMessage.isDeleted -> "Message deleted"
                else -> {
                    val senderPrefix = if (lastMessage.senderId == currentUserId) "You: " else ""
                    senderPrefix + when (lastMessage.contentType) {
                        MessageType.TEXT -> lastMessage.content
                        MessageType.IMAGE -> "📷 Image"
                        MessageType.AUDIO -> "🎵 Audio"
                        MessageType.VIDEO -> "🎬 Video"
                        MessageType.FILE -> "📄 File"
                        MessageType.LOCATION -> "📍 Location"
                        MessageType.CONTACT -> "👤 Contact"
                        MessageType.VOICE -> "🎤 Voice message"
                        MessageType.SYSTEM -> "System message"
                    }
                }
            }
        }
    
    fun markAsRead(): Conversation {
        return this.copy(
            unreadCount = 0,
            updatedAt = Date()
        )
    }
    
    fun updateLastMessage(message: Message): Conversation {
        return this.copy(
            lastMessage = message,
            lastMessageAt = message.timestamp,
            unreadCount = if (message.senderId == currentUserId) unreadCount else unreadCount + 1,
            totalMessageCount = totalMessageCount + 1,
            updatedAt = Date()
        )
    }
    
    fun pin(isPinned: Boolean = true): Conversation {
        return this.copy(
            isPinned = isPinned,
            updatedAt = Date()
        )
    }
    
    fun mute(isMuted: Boolean = true): Conversation {
        return this.copy(
            isMuted = isMuted,
            updatedAt = Date()
        )
    }
    
    fun archive(isArchived: Boolean = true): Conversation {
        return this.copy(
            isArchived = isArchived,
            updatedAt = Date()
        )
    }
    
    companion object {
        // 需要在应用初始化时设置
        var currentUserId: String = ""
        
        private fun formatTimestamp(timestamp: Date): String {
            val now = Date()
            val diff = now.time - timestamp.time
            
            val calendar = Calendar.getInstance()
            calendar.time = timestamp
            val messageHour = calendar.get(Calendar.HOUR_OF_DAY)
            val messageMinute = calendar.get(Calendar.MINUTE)
            
            return when {
                diff < 24 * 60 * 60 * 1000 -> {
                    // 今天
                    String.format("%02d:%02d", messageHour, messageMinute)
                }
                diff < 7 * 24 * 60 * 60 * 1000 -> {
                    // 一周内
                    val daysOfWeek = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    daysOfWeek[dayOfWeek - 1]
                }
                else -> {
                    // 更早
                    val dateFormat = java.text.SimpleDateFormat("MM/dd", Locale.getDefault())
                    dateFormat.format(timestamp)
                }
            }
        }
        
        fun createDirectConversation(participant1: Participant, participant2: Participant): Conversation {
            return Conversation(
                type = ConversationType.DIRECT,
                participants = listOf(participant1, participant2)
            )
        }
        
        fun createGroupConversation(
            title: String? = null,
            participants: List<Participant>,
            avatarUri: String? = null
        ): Conversation {
            return Conversation(
                title = title,
                type = ConversationType.GROUP,
                participants = participants,
                avatarUri = avatarUri
            )
        }
    }
}

data class Participant(
    val userId: String,
    val displayName: String,
    val avatarUri: String? = null,
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val joinedAt: Date = Date(),
    val lastSeenAt: Date? = null
)

enum class ConversationType {
    DIRECT, GROUP, CHANNEL
}

enum class ParticipantRole {
    OWNER, ADMIN, MEMBER, GUEST
}