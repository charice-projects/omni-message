package com.omnimsg.domain.usecases.contact

import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.repositories.ContactRepository
import com.omnimsg.domain.usecases.BaseUseCase
import javax.inject.Inject

class AddContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) : BaseUseCase<AddContactUseCase.Params, Long>() {
    
    data class Params(
        val contact: Contact
    )
    
    override suspend fun execute(params: Params): Result<Long> {
        return try {
            // 验证联系人数据
            validateContact(params.contact)
            
            // 检查重复联系人
            val duplicate = contactRepository.findDuplicate(
                phoneNumber = params.contact.phoneNumber,
                email = params.contact.email
            )
            
            if (duplicate != null) {
                return Result.failure(ContactValidationException("Duplicate contact found: ${duplicate.displayName}"))
            }
            
            // 插入联系人
            val contactId = contactRepository.insertContact(params.contact)
            Result.success(contactId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateContact(contact: Contact) {
        require(contact.displayName.isNotBlank()) { "Display name cannot be empty" }
        
        if (contact.phoneNumber != null) {
            require(contact.phoneNumber.matches(Regex("^[+]?[0-9]{10,15}$"))) {
                "Invalid phone number format"
            }
        }
        
        if (contact.email != null) {
            require(contact.email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
                "Invalid email format"
            }
        }
    }
    
    class ContactValidationException(message: String) : Exception(message)
}