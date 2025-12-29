package com.omnimsg.data.repositories

import com.omnimsg.data.local.database.daos.ContactDao
import com.omnimsg.data.mappers.ContactMapper
import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.models.ContactSource
import com.omnimsg.domain.models.SyncStatus
import com.omnimsg.domain.repositories.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {
    
    override suspend fun insertContact(contact: Contact): Long {
        val entity = ContactMapper.toEntity(contact)
        return contactDao.insert(entity)
    }
    
    override suspend fun insertContacts(contacts: List<Contact>): List<Long> {
        val entities = ContactMapper.toEntities(contacts)
        return contactDao.insertAll(entities)
    }
    
    override suspend fun updateContact(contact: Contact): Boolean {
        val entity = ContactMapper.toEntity(contact)
        return contactDao.update(entity) > 0
    }
    
    override suspend fun deleteContact(id: Long): Boolean {
        return contactDao.delete(id) > 0
    }
    
    override suspend fun softDeleteContact(id: Long): Boolean {
        return contactDao.softDelete(id) > 0
    }
    
    override suspend fun getContactById(id: Long): Contact? {
        return contactDao.getById(id)?.let { ContactMapper.fromEntity(it) }
    }
    
    override suspend fun getContactByPhone(phoneNumber: String): Contact? {
        return contactDao.getByPhoneNumber(phoneNumber)?.let { ContactMapper.fromEntity(it) }
    }
    
    override suspend fun getContactByEmail(email: String): Contact? {
        return contactDao.getByEmail(email)?.let { ContactMapper.fromEntity(it) }
    }
    
    override fun getAllContactsStream(): Flow<List<Contact>> {
        return contactDao.getAllContactsStream()
            .map { entities -> ContactMapper.fromEntities(entities) }
    }
    
    override fun getFavoriteContactsStream(): Flow<List<Contact>> {
        return contactDao.getFavoriteContactsStream()
            .map { entities -> ContactMapper.fromEntities(entities) }
    }
    
    override fun getBlockedContactsStream(): Flow<List<Contact>> {
        return contactDao.getBlockedContactsStream()
            .map { entities -> ContactMapper.fromEntities(entities) }
    }
    
    override fun getContactsByGroupStream(groupId: Long): Flow<List<Contact>> {
        return contactDao.getContactsByGroupStream(groupId)
            .map { entities -> ContactMapper.fromEntities(entities) }
    }
    
    override fun searchContacts(query: String): Flow<List<Contact>> {
        return contactDao.searchContacts(query)
            .map { entities -> ContactMapper.fromEntities(entities) }
    }
    
    override suspend fun updateSyncStatus(ids: List<Long>, syncStatus: SyncStatus, syncToken: String?): Boolean {
        return contactDao.updateSyncStatus(ids, syncStatus, syncToken) > 0
    }
    
    override suspend fun getContactsBySyncStatus(syncStatus: SyncStatus, limit: Int): List<Contact> {
        val entities = contactDao.getContactsBySyncStatus(syncStatus, limit)
        return ContactMapper.fromEntities(entities)
    }
    
    override suspend fun getTotalCount(): Int {
        return contactDao.getTotalCount()
    }
    
    override suspend fun getFavoriteCount(): Int {
        return contactDao.getFavoriteCount()
    }
    
    override suspend fun getUnsyncedCount(): Int {
        return contactDao.getCountBySyncStatus(SyncStatus.PENDING) +
               contactDao.getCountBySyncStatus(SyncStatus.FAILED)
    }
    
    override suspend fun findDuplicate(phoneNumber: String?, email: String?, excludeId: Long): Contact? {
        return contactDao.findDuplicate(phoneNumber, email, excludeId)?.let { ContactMapper.fromEntity(it) }
    }
    
    override suspend fun purgeSoftDeleted(olderThanDays: Int): Int {
        val cutoff = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000)
        return contactDao.purgeSoftDeleted(cutoff)
    }
    
    override suspend fun importContacts(contacts: List<Contact>, source: ContactSource): ContactRepository.ImportResult {
        val failedEntries = mutableListOf<ContactRepository.ImportResult.FailedEntry>()
        var successCount = 0
        var duplicateCount = 0
        
        // 批量处理联系人
        contacts.forEach { contact ->
            try {
                // 检查重复
                val duplicate = findDuplicate(contact.phoneNumber, contact.email)
                
                if (duplicate != null) {
                    duplicateCount++
                    failedEntries.add(
                        ContactRepository.ImportResult.FailedEntry(
                            contact = contact,
                            reason = "Duplicate contact found: ${duplicate.displayName}"
                        )
                    )
                } else {
                    // 设置导入来源
                    val contactToInsert = contact.copy(
                        source = source,
                        importBatchId = UUID.randomUUID().toString()
                    )
                    
                    // 插入联系人
                    insertContact(contactToInsert)
                    successCount++
                }
            } catch (e: Exception) {
                failedEntries.add(
                    ContactRepository.ImportResult.FailedEntry(
                        contact = contact,
                        reason = e.message ?: "Unknown error"
                    )
                )
            }
        }
        
        return ContactRepository.ImportResult(
            successCount = successCount,
            failedCount = failedEntries.size,
            duplicateCount = duplicateCount,
            failedEntries = failedEntries
        )
    }
}