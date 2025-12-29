package com.omnimsg.domain.models

import java.util.*

data class Contact(
    val id: Long = 0,
    val contactId: String = UUID.randomUUID().toString(),
    val displayName: String,
    val phoneNumber: String? = null,
    val secondaryPhone: String? = null,
    val email: String? = null,
    val company: String? = null,
    val position: String? = null,
    val address: String? = null,
    val birthday: Date? = null,
    val isLunarBirthday: Boolean = false,
    val notes: String? = null,
    val avatarUri: String? = null,
    val relationship: RelationshipType = RelationshipType.OTHER,
    val tags: List<String> = emptyList(),
    val groupId: Long? = null,
    val groupName: String? = null,
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val lastContacted: Date? = null,
    val interactionCount: Int = 0,
    val interactionScore: Float = 0f,
    val customFields: Map<String, String> = emptyMap(),
    val source: ContactSource = ContactSource.MANUAL,
    val importBatchId: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncToken: String? = null,
    val isDeleted: Boolean = false
) {
    val fullName: String
        get() = displayName
    
    val primaryContact: String?
        get() = phoneNumber ?: email
    
    val hasAvatar: Boolean
        get() = avatarUri != null
    
    val isRecentlyContacted: Boolean
        get() {
            val weekAgo = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
            return lastContacted?.after(weekAgo) ?: false
        }
    
    val interactionLevel: InteractionLevel
        get() = when {
            interactionScore >= 80f -> InteractionLevel.HIGH
            interactionScore >= 30f -> InteractionLevel.MEDIUM
            else -> InteractionLevel.LOW
        }
    
    fun updateInteraction(increment: Int = 1, scoreDelta: Float = 1f): Contact {
        return this.copy(
            interactionCount = this.interactionCount + increment,
            interactionScore = (this.interactionScore + scoreDelta).coerceAtMost(100f),
            lastContacted = Date(),
            updatedAt = Date()
        )
    }
    
    fun markAsFavorite(isFavorite: Boolean = true): Contact {
        return this.copy(
            isFavorite = isFavorite,
            updatedAt = Date()
        )
    }
    
    fun markAsBlocked(isBlocked: Boolean = true): Contact {
        return this.copy(
            isBlocked = isBlocked,
            updatedAt = Date()
        )
    }
    
    companion object {
        fun fromDisplayName(name: String): Contact {
            return Contact(displayName = name)
        }
    }
}

enum class RelationshipType {
    FAMILY, FRIEND, COLLEAGUE, BUSINESS, OTHER
}

enum class ContactSource {
    MANUAL, IMPORT, SYNC, SCAN, API
}

enum class SyncStatus {
    PENDING, SYNCING, SYNCED, FAILED, CONFLICT
}

enum class InteractionLevel {
    LOW, MEDIUM, HIGH
}