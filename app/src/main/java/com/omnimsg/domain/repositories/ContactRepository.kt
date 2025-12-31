package com.omnimsg.domain.repositories

import com.omnimsg.domain.models.Contact
import com.omnimsg.domain.models.ContactSource
import com.omnimsg.domain.models.SyncStatus
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    
    // 基础CRUD操作
    suspend fun insertContact(contact: Contact): Long
    suspend fun insertContacts(contacts: List<Contact>): List<Long>
    suspend fun updateContact(contact: Contact): Boolean
    suspend fun deleteContact(id: Long): Boolean
    suspend fun softDeleteContact(id: Long): Boolean
    suspend fun getContactById(id: Long): Contact?
    suspend fun getContactByPhone(phoneNumber: String): Contact?
    suspend fun getContactByEmail(email: String): Contact?
    
    // 流式查询
    fun getAllContactsStream(): Flow<List<Contact>>
    fun getFavoriteContactsStream(): Flow<List<Contact>>
    fun getBlockedContactsStream(): Flow<List<Contact>>
    fun getContactsByGroupStream(groupId: Long): Flow<List<Contact>>
    
    // 搜索
    fun searchContacts(query: String): Flow<List<Contact>>
    
    // 批量操作
    suspend fun updateSyncStatus(ids: List<Long>, syncStatus: SyncStatus, syncToken: String?): Boolean
    suspend fun getContactsBySyncStatus(syncStatus: SyncStatus, limit: Int): List<Contact>
    
    // 统计
    suspend fun getTotalCount(): Int
    suspend fun getFavoriteCount(): Int
    suspend fun getUnsyncedCount(): Int
    
    // 重复检测
    suspend fun findDuplicate(phoneNumber: String?, email: String?, excludeId: Long = 0): Contact?
    
    // 数据清理
    suspend fun purgeSoftDeleted(olderThanDays: Int = 30): Int
    
    // 导入相关
    suspend fun importContacts(contacts: List<Contact>, source: ContactSource = ContactSource.IMPORT): ImportResult
    
    data class ImportResult(
        val successCount: Int,
        val failedCount: Int,
        val duplicateCount: Int,
        val failedEntries: List<FailedEntry> = emptyList()
    ) {
        data class FailedEntry(
            val contact: Contact,
            val reason: String
        )
    }
}