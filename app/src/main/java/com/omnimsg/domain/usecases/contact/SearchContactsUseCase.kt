package com.omnimsg.domain.usecases.contact

import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.repositories.ContactRepository
import com.omnimsg.domain.usecases.BaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) : BaseUseCase<String, Flow<List<Contact>>>() {
    
    override suspend fun execute(params: String): Result<Flow<List<Contact>>> {
        return try {
            val query = params.trim()
            
            if (query.isEmpty()) {
                // 如果查询为空，返回所有联系人
                val allContacts = contactRepository.getAllContactsStream()
                Result.success(allContacts)
            } else {
                // 搜索联系人
                val searchResults = contactRepository.searchContacts(query)
                Result.success(searchResults)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}