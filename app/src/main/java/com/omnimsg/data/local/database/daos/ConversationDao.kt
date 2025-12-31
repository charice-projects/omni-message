package com.omnimsg.data.local.database.daos

import androidx.room.*
import com.omnimsg.data.local.database.entities.ConversationEntity
import com.omnimsg.domain.models.ConversationType
import com.omnimsg.domain.models.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    // 插入操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>): List<Long>
    
    // 更新操作
    @Update
    suspend fun update(conversation: ConversationEntity): Int
    
    @Query("UPDATE conversations SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    // 查询操作
    @Query("SELECT * FROM conversations WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: Long): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE conversation_id = :conversationId AND is_deleted = 0")
    suspend fun getByConversationId(conversationId: String): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE :participantId IN (participant_ids) AND is_deleted = 0")
    suspend fun getByParticipantId(participantId: String): List<ConversationEntity>
    
    // 流式查询
    @Query("""
        SELECT * FROM conversations 
        WHERE is_deleted = 0 AND is_archived = 0
        ORDER BY 
            is_pinned DESC,
            last_message_at DESC
    """)
    fun getAllConversationsStream(): Flow<List<ConversationEntity>>
    
    @Query("""
        SELECT * FROM conversations 
        WHERE is_deleted = 0 AND is_pinned = 1
        ORDER BY last_message_at DESC
    """)
    fun getPinnedConversationsStream(): Flow<List<ConversationEntity>>
    
    @Query("""
        SELECT * FROM conversations 
        WHERE is_deleted = 0 AND is_archived = 1
        ORDER BY last_message_at DESC
    """)
    fun getArchivedConversationsStream(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE is_deleted = 0 AND type = :type ORDER BY last_message_at DESC")
    fun getConversationsByTypeStream(type: ConversationType): Flow<List<ConversationEntity>>
    
    // 搜索查询
    @Query("""
        SELECT * FROM conversations 
        WHERE (title LIKE '%' || :query || '%' 
               OR last_message_content LIKE '%' || :query || '%')
          AND is_deleted = 0
        ORDER BY last_message_at DESC
    """)
    fun searchConversations(query: String): Flow<List<ConversationEntity>>
    
    // 更新最后消息
    @Query("""
        UPDATE conversations 
        SET last_message_id = :messageId,
            last_message_content = :content,
            last_message_sender_id = :senderId,
            last_message_sender_name = :senderName,
            last_message_at = :timestamp,
            unread_count = unread_count + :unreadIncrement,
            total_message_count = total_message_count + 1,
            updated_at = :updateTimestamp
        WHERE id = :id
    """)
    suspend fun updateLastMessage(
        id: Long,
        messageId: String,
        content: String,
        senderId: String,
        senderName: String,
        timestamp: Long,
        unreadIncrement: Int = 1,
        updateTimestamp: Long = System.currentTimeMillis()
    ): Int
    
    // 标记为已读
    @Query("UPDATE conversations SET unread_count = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun markAsRead(id: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE conversations SET unread_count = 0, updated_at = :timestamp WHERE conversation_id = :conversationId")
    suspend fun markAsReadByConversationId(conversationId: String, timestamp: Long = System.currentTimeMillis()): Int
    
    // 置顶/取消置顶
    @Query("UPDATE conversations SET is_pinned = :isPinned, updated_at = :timestamp WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean, timestamp: Long = System.currentTimeMillis()): Int
    
    // 静音/取消静音
    @Query("UPDATE conversations SET is_muted = :isMuted, updated_at = :timestamp WHERE id = :id")
    suspend fun setMuted(id: Long, isMuted: Boolean, timestamp: Long = System.currentTimeMillis()): Int
    
    // 归档/取消归档
    @Query("UPDATE conversations SET is_archived = :isArchived, updated_at = :timestamp WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, timestamp: Long = System.currentTimeMillis()): Int
    
    // 同步相关
    @Query("UPDATE conversations SET sync_status = :syncStatus, sync_token = :syncToken WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<Long>, syncStatus: SyncStatus, syncToken: String?): Int
    
    @Query("SELECT * FROM conversations WHERE sync_status = :syncStatus AND is_deleted = 0 ORDER BY updated_at ASC LIMIT :limit")
    suspend fun getConversationsBySyncStatus(syncStatus: SyncStatus, limit: Int = 100): List<ConversationEntity>
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM conversations WHERE is_deleted = 0")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM conversations WHERE unread_count > 0 AND is_deleted = 0 AND is_muted = 0")
    suspend fun getTotalUnreadCount(): Int
    
    @Query("SELECT COUNT(*) FROM conversations WHERE is_pinned = 1 AND is_deleted = 0")
    suspend fun getPinnedCount(): Int
    
    // 查找或创建直接会话
    @Transaction
    suspend fun findOrCreateDirectConversation(
        currentUserId: String,
        otherUserId: String,
        currentUserName: String,
        otherUserName: String
    ): ConversationEntity {
        // 查找现有的直接会话
        val existingConversations = getByParticipantId(currentUserId)
        val directConversation = existingConversations.find { conversation ->
            conversation.type == ConversationType.DIRECT &&
                    conversation.participantIds.size == 2 &&
                    conversation.participantIds.contains(otherUserId)
        }
        
        return directConversation ?: run {
            // 创建新的直接会话
            val newConversation = ConversationEntity(
                title = null,
                type = ConversationType.DIRECT,
                participantIds = listOf(currentUserId, otherUserId),
                participantNames = mapOf(
                    currentUserId to currentUserName,
                    otherUserId to otherUserName
                ),
                lastMessageAt = System.currentTimeMillis()
            )
            val id = insert(newConversation)
            getById(id) ?: newConversation
        }
    }
}