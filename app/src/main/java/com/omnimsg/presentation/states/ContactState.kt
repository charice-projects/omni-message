package com.omnimsg.presentation.states

import com.omnimsg.domain.models.Contact

sealed class ContactState {
    object Loading : ContactState()
    data class Success(val contacts: List<Contact>) : ContactState()
    data class Error(val message: String) : ContactState()
    object Empty : ContactState()
}

sealed class ContactDetailState {
    object Loading : ContactDetailState()
    data class Success(val contact: Contact) : ContactDetailState()
    data class Error(val message: String) : ContactDetailState()
}

sealed class ContactOperationState {
    object Idle : ContactOperationState()
    object Loading : ContactOperationState()
    data class Success(val message: String) : ContactOperationState()
    data class Error(val message: String) : ContactOperationState()
}

data class ContactUiState(
    val contactState: ContactState = ContactState.Loading,
    val searchQuery: String = "",
    val selectedContacts: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val filterType: ContactFilterType = ContactFilterType.ALL,
    val sortOrder: ContactSortOrder = ContactSortOrder.NAME_ASC
)

enum class ContactFilterType {
    ALL, FAVORITES, RECENT, BLOCKED, WITH_PHONE, WITH_EMAIL
}

enum class ContactSortOrder {
    NAME_ASC, NAME_DESC, RECENT_ASC, RECENT_DESC
}