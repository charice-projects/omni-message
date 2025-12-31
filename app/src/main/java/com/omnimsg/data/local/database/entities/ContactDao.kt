package com.omnimsg.data.local.database.daos

import androidx.room.*
import com.omnimsg.data.local.database.entities.ContactEntity
import com.omnimsg.domain.models.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    
    // 插入操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>): List<Long>
    
    // 更新操作
    @Update
    suspend fun update(contact: ContactEntity): Int
    
    @Query("UPDATE contacts SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun delete(id: Long): Int
    
    @Query("DELETE FROM contacts WHERE is_deleted = 1")
    suspend fun deleteAllSoftDeleted(): Int
    
    // 查询操作
    @Query("SELECT * FROM contacts WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: Long): ContactEntity?
    
    @Query("SELECT * FROM contacts WHERE contact_id = :contactId AND is_deleted = 0")
    suspend fun getByContactId(contactId: String): ContactEntity?
    
    @Query("SELECT * FROM contacts WHERE phone_number = :phoneNumber AND is_deleted = 0 LIMIT 1")
    suspend fun getByPhoneNumber(phoneNumber: String): ContactEntity?
    
    @Query("SELECT * FROM contacts WHERE email = :email AND is_deleted = 0 LIMIT 1")
    suspend fun getByEmail(email: String): ContactEntity?
    
    // 流式查询
    @Query("SELECT * FROM contacts WHERE is_deleted = 0 ORDER BY display_name COLLATE NOCASE ASC")
    fun getAllContactsStream(): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE is_favorite = 1 AND is_deleted = 0 ORDER BY display_name COLLATE NOCASE ASC")
    fun getFavoriteContactsStream(): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE is_blocked = 1 AND is_deleted = 0 ORDER BY display_name COLLATE NOCASE ASC")
    fun getBlockedContactsStream(): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE group_id = :groupId AND is_deleted = 0 ORDER BY display_name COLLATE NOCASE ASC")
    fun getContactsByGroupStream(groupId: Long): Flow<List<ContactEntity>>
    
    // 搜索查询
    @Query("""
        SELECT * FROM contacts 
        WHERE (display_name LIKE '%' || :query || '%' 
               OR phone_number LIKE '%' || :query || '%' 
               OR email LIKE '%' || :query || '%'
               OR company LIKE '%' || :query || '%')
          AND is_deleted = 0
        ORDER BY 
            CASE 
                WHEN display_name LIKE :query || '%' THEN 1
                WHEN phone_number LIKE :query || '%' THEN 2
                WHEN email LIKE :query || '%' THEN 3
                ELSE 4
            END,
            display_name COLLATE NOCASE ASC
    """)
    fun searchContacts(query: String): Flow<List<ContactEntity>>
    
    // 批量操作
    @Query("UPDATE contacts SET sync_status = :syncStatus, sync_token = :syncToken WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<Long>, syncStatus: SyncStatus, syncToken: String?): Int
    
    @Query("SELECT * FROM contacts WHERE sync_status = :syncStatus AND is_deleted = 0 ORDER BY updated_at ASC LIMIT :limit")
    suspend fun getContactsBySyncStatus(syncStatus: SyncStatus, limit: Int = 100): List<ContactEntity>
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM contacts WHERE is_deleted = 0")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM contacts WHERE is_favorite = 1 AND is_deleted = 0")
    suspend fun getFavoriteCount(): Int
    
    @Query("SELECT COUNT(*) FROM contacts WHERE sync_status = :syncStatus AND is_deleted = 0")
    suspend fun getCountBySyncStatus(syncStatus: SyncStatus): Int
    
    // 重复检测
    @Query("""
        SELECT * FROM contacts 
        WHERE (phone_number = :phoneNumber OR email = :email) 
          AND id != :excludeId 
          AND is_deleted = 0
        LIMIT 1
    """)
    suspend fun findDuplicate(phoneNumber: String?, email: String?, excludeId: Long = 0): ContactEntity?
    
    // 数据清理
    @Query("SELECT COUNT(*) FROM contacts WHERE is_deleted = 1")
    suspend fun getSoftDeletedCount(): Int
    
    @Query("DELETE FROM contacts WHERE is_deleted = 1 AND updated_at < :timestamp")
    suspend fun purgeSoftDeleted(timestamp: Long): Int
}