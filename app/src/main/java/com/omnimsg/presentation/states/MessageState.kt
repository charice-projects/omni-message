package com.omnimsg.presentation.states

import com.omnimsg.domain.models.Conversation
import com.omnimsg.domain.models.Message

sealed class ConversationState {
    object Loading : ConversationState()
    data class Success(val conversations: List<Conversation>) : ConversationState()
    data class Error(val message: String) : ConversationState()
    object Empty : ConversationState()
}

sealed class MessageState {
    object Loading : MessageState()
    data class Success(val messages: List<Message>) : MessageState()
    data class Error(val message: String) : MessageState()
    object Empty : MessageState()
}

sealed class MessageOperationState {
    object Idle : MessageOperationState()
    object Sending : MessageOperationState()
    data class Success(val message: Message) : MessageOperationState()
    data class Error(val message: String) : MessageOperationState()
}

data class ConversationUiState(
    val conversationState: ConversationState = ConversationState.Loading,
    val searchQuery: String = "",
    val selectedConversations: Set<Long> = emptySet(),
    val filterType: ConversationFilterType = ConversationFilterType.ALL,
    val sortOrder: ConversationSortOrder = ConversationSortOrder.RECENT_DESC,
    val unreadCount: Int = 0
)

data class ChatUiState(
    val conversation: Conversation? = null,
    val messageState: MessageState = MessageState.Loading,
    val operationState: MessageOperationState = MessageOperationState.Idle,
    val inputText: String = "",
    val isRecording: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val replyToMessage: Message? = null,
    val isTyping: Boolean = false,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

data class Attachment(
    val id: String,
    val type: AttachmentType,
    val uri: String,
    val name: String,
    val size: Long,
    val thumbnailUri: String? = null
)

enum class AttachmentType {
    IMAGE, VIDEO, AUDIO, FILE, LOCATION, CONTACT
}

enum class ConversationFilterType {
    ALL, UNREAD, PINNED, MUTED, ARCHIVED
}

enum class ConversationSortOrder {
    RECENT_DESC, RECENT_ASC, UNREAD_DESC, NAME_ASC
}