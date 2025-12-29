package com.omnimsg.data.local.database.daos

import androidx.room.*
import com.omnimsg.data.local.database.entities.MessageEntity
import com.omnimsg.domain.models.MessageStatus
import com.omnimsg.domain.models.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface MessageDao {
    
    // 插入操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>): List<Long>
    
    // 更新操作
    @Update
    suspend fun update(message: MessageEntity): Int
    
    @Query("UPDATE messages SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long): Int
    
    // 查询操作
    @Query("SELECT * FROM messages WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: Long): MessageEntity?
    
    @Query("SELECT * FROM messages WHERE message_id = :messageId AND is_deleted = 0")
    suspend fun getByMessageId(messageId: String): MessageEntity?
    
    // 流式查询
    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getMessagesByConversationStream(conversationId: Long): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE sender_id = :senderId AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getMessagesBySenderStream(senderId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE recipient_id = :recipientId AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getMessagesByRecipientStream(recipientId: String): Flow<List<MessageEntity>>
    
    // 状态更新
    @Query("UPDATE messages SET status = :status, updated_at = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MessageStatus, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE messages SET read_at = :timestamp, updated_at = :timestamp WHERE id = :id AND read_at IS NULL")
    suspend fun markAsRead(id: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE messages SET delivered_at = :timestamp, updated_at = :timestamp WHERE id = :id AND delivered_at IS NULL")
    suspend fun markAsDelivered(id: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    // 批量状态更新
    @Query("UPDATE messages SET status = :status, updated_at = :timestamp WHERE conversation_id = :conversationId AND status = :oldStatus")
    suspend fun updateConversationStatus(conversationId: Long, oldStatus: MessageStatus, status: MessageStatus, timestamp: Long = System.currentTimeMillis()): Int
    
    // 搜索查询
    @Query("""
        SELECT * FROM messages 
        WHERE content LIKE '%' || :query || '%' 
          AND is_deleted = 0
        ORDER BY timestamp DESC
    """)
    fun searchMessages(query: String): Flow<List<MessageEntity>>
    
    @Query("""
        SELECT * FROM messages 
        WHERE conversation_id = :conversationId 
          AND content LIKE '%' || :query || '%' 
          AND is_deleted = 0
        ORDER BY timestamp DESC
    """)
    fun searchMessagesInConversation(conversationId: Long, query: String): Flow<List<MessageEntity>>
    
    // 同步相关
    @Query("UPDATE messages SET sync_status = :syncStatus, sync_token = :syncToken WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<Long>, syncStatus: SyncStatus, syncToken: String?): Int
    
    @Query("SELECT * FROM messages WHERE sync_status = :syncStatus AND is_deleted = 0 ORDER BY updated_at ASC LIMIT :limit")
    suspend fun getMessagesBySyncStatus(syncStatus: SyncStatus, limit: Int = 100): List<MessageEntity>
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id = :conversationId AND is_deleted = 0")
    suspend fun getMessageCountByConversation(conversationId: Long): Int
    
    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id = :conversationId AND status = :status AND is_deleted = 0")
    suspend fun getMessageCountByStatus(conversationId: Long, status: MessageStatus): Int
    
    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id = :conversationId AND read_at IS NULL AND recipient_id = :userId AND is_deleted = 0")
    suspend fun getUnreadCount(conversationId: Long, userId: String): Int
    
    // 分页查询
    @Query("""
        SELECT * FROM messages 
        WHERE conversation_id = :conversationId 
          AND is_deleted = 0
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getMessagesPaginated(conversationId: Long, limit: Int, offset: Int): List<MessageEntity>
    
    // 数据清理
    @Query("DELETE FROM messages WHERE is_deleted = 1 AND updated_at < :timestamp")
    suspend fun purgeSoftDeleted(timestamp: Long): Int
    
    // 获取最新消息
    @Query("""
        SELECT * FROM messages 
        WHERE conversation_id = :conversationId 
          AND is_deleted = 0
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getLatestMessage(conversationId: Long): MessageEntity?
}