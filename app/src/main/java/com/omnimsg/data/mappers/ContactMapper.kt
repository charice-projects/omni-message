package com.omnimsg.data.mappers

import com.omnimsg.data.local.database.entities.ContactEntity
import com.omnimsg.domain.models.Contact
import java.util.*

object ContactMapper {
    
    fun toEntity(contact: Contact): ContactEntity {
        return ContactEntity(
            id = contact.id,
            contactId = contact.contactId,
            displayName = contact.displayName,
            phoneNumber = contact.phoneNumber,
            secondaryPhone = contact.secondaryPhone,
            email = contact.email,
            company = contact.company,
            position = contact.position,
            address = contact.address,
            birthday = contact.birthday?.time,
            isLunarBirthday = contact.isLunarBirthday,
            notes = contact.notes,
            avatarUri = contact.avatarUri,
            relationship = contact.relationship,
            tags = contact.tags,
            groupId = contact.groupId,
            isFavorite = contact.isFavorite,
            isBlocked = contact.isBlocked,
            lastContacted = contact.lastContacted?.time,
            interactionCount = contact.interactionCount,
            interactionScore = contact.interactionScore,
            customFields = contact.customFields,
            source = contact.source,
            importBatchId = contact.importBatchId,
            createdAt = contact.createdAt.time,
            updatedAt = contact.updatedAt.time,
            syncStatus = contact.syncStatus,
            syncToken = contact.syncToken,
            isDeleted = contact.isDeleted
        )
    }
    
    fun fromEntity(entity: ContactEntity): Contact {
        return Contact(
            id = entity.id,
            contactId = entity.contactId,
            displayName = entity.displayName,
            phoneNumber = entity.phoneNumber,
            secondaryPhone = entity.secondaryPhone,
            email = entity.email,
            company = entity.company,
            position = entity.position,
            address = entity.address,
            birthday = entity.birthday?.let { Date(it) },
            isLunarBirthday = entity.isLunarBirthday,
            notes = entity.notes,
            avatarUri = entity.avatarUri,
            relationship = entity.relationship,
            tags = entity.tags,
            groupId = entity.groupId,
            isFavorite = entity.isFavorite,
            isBlocked = entity.isBlocked,
            lastContacted = entity.lastContacted?.let { Date(it) },
            interactionCount = entity.interactionCount,
            interactionScore = entity.interactionScore,
            customFields = entity.customFields,
            source = entity.source,
            importBatchId = entity.importBatchId,
            createdAt = Date(entity.createdAt),
            updatedAt = Date(entity.updatedAt),
            syncStatus = entity.syncStatus,
            syncToken = entity.syncToken,
            isDeleted = entity.isDeleted
        )
    }
    
    fun fromEntities(entities: List<ContactEntity>): List<Contact> {
        return entities.map { fromEntity(it) }
    }
    
    fun toEntities(contacts: List<Contact>): List<ContactEntity> {
        return contacts.map { toEntity(it) }
    }
    
    fun updateEntityFromDomain(entity: ContactEntity, contact: Contact): ContactEntity {
        return entity.copy(
            displayName = contact.displayName,
            phoneNumber = contact.phoneNumber,
            secondaryPhone = contact.secondaryPhone,
            email = contact.email,
            company = contact.company,
            position = contact.position,
            address = contact.address,
            birthday = contact.birthday?.time,
            isLunarBirthday = contact.isLunarBirthday,
            notes = contact.notes,
            avatarUri = contact.avatarUri,
            relationship = contact.relationship,
            tags = contact.tags,
            groupId = contact.groupId,
            isFavorite = contact.isFavorite,
            isBlocked = contact.isBlocked,
            lastContacted = contact.lastContacted?.time,
            interactionCount = contact.interactionCount,
            interactionScore = contact.interactionScore,
            customFields = contact.customFields,
            updatedAt = contact.updatedAt.time,
            syncStatus = contact.syncStatus,
            syncToken = contact.syncToken,
            isDeleted = contact.isDeleted
        )
    }
}