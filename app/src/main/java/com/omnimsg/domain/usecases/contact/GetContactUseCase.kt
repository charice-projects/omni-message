package com.omnimsg.domain.usecases.contact

import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.repositories.ContactRepository
import com.omnimsg.domain.usecases.BaseUseCase
import javax.inject.Inject

class GetContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) : BaseUseCase<Long, Contact>() {
    
    override suspend fun execute(params: Long): Result<Contact> {
        return try {
            val contact = contactRepository.getContactById(params)
                ?: return Result.failure(IllegalArgumentException("Contact not found"))
            
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetContactsByGroupUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) : BaseUseCase<GetContactsByGroupUseCase.Params, List<Contact>>() {
    
    data class Params(
        val groupId: Long,
        val includeDeleted: Boolean = false
    )
    
    override suspend fun execute(params: Params): Result<List<Contact>> {
        return try {
            val contactsFlow = contactRepository.getContactsByGroupStream(params.groupId)
            val contacts = contactsFlow.firstOrNull() ?: emptyList()
            
            val filteredContacts = if (params.includeDeleted) {
                contacts
            } else {
                contacts.filter { !it.isDeleted }
            }
            
            Result.success(filteredContacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) : BaseUseCase<UpdateContactUseCase.Params, Boolean>() {
    
    data class Params(
        val contact: Contact,
        val validateBeforeUpdate: Boolean = true
    )
    
    override suspend fun execute(params: Params): Result<Boolean> {
        return try {
            if (params.validateBeforeUpdate) {
                validateContact(params.contact)
            }
            
            val updated = contactRepository.updateContact(params.contact)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateContact(contact: Contact) {
        require(contact.displayName.isNotBlank()) { "Display name cannot be empty" }
        
        contact.phoneNumber?.let { phone ->
            require(phone.matches(Regex("^[+]?[0-9]{10,15}$"))) {
                "Invalid phone number format"
            }
        }
        
        contact.email?.let { email ->
            require(email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
                "Invalid email format"
            }
        }
    }
}