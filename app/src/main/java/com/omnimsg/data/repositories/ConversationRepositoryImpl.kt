package com.omnimsg.data.repositories

import com.omnimsg.data.local.database.daos.ConversationDao
import com.omnimsg.data.mappers.ConversationMapper
import com.omnimsg.domain.models.Conversation
import com.omnimsg.domain.models.SyncStatus
import com.omnimsg.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao
) : ConversationRepository {
    
    override suspend fun saveConversation(conversation: Conversation): Conversation {
        val entity = ConversationMapper.toEntity(conversation)
        val id = conversationDao.insert(entity)
        return getConversationById(id) ?: conversation
    }
    
    override suspend fun updateConversation(conversation: Conversation): Boolean {
        val entity = ConversationMapper.toEntity(conversation)
        return conversationDao.update(entity) > 0
    }
    
    override suspend fun deleteConversation(id: Long): Boolean {
        return conversationDao.softDelete(id) > 0
    }
    
    override suspend fun getConversationById(id: Long): Conversation? {
        return conversationDao.getById(id)?.let { ConversationMapper.fromEntity(it) }
    }
    
    override suspend fun getConversationByConversationId(conversationId: String): Conversation? {
        return conversationDao.getByConversationId(conversationId)?.let { ConversationMapper.fromEntity(it) }
    }
    
    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversationsStream()
            .map { entities -> ConversationMapper.fromEntities(entities) }
    }
    
    override fun getPinnedConversations(): Flow<List<Conversation>> {
        return conversationDao.getPinnedConversationsStream()
            .map { entities -> ConversationMapper.fromEntities(entities) }
    }
    
    override fun getArchivedConversations(): Flow<List<Conversation>> {
        return conversationDao.getArchivedConversationsStream()
            .map { entities -> ConversationMapper.fromEntities(entities) }
    }
    
    override fun searchConversations(query: String): Flow<List<Conversation>> {
        return conversationDao.searchConversations(query)
            .map { entities -> ConversationMapper.fromEntities(entities) }
    }
    
    override suspend fun updateLastMessage(
        conversationId: Long,
        messageId: String,
        content: String,
        senderId: String,
        senderName: String
    ): Boolean {
        val timestamp = System.currentTimeMillis()
        val unreadIncrement = if (senderId == Conversation.currentUserId) 0 else 1
        
        return conversationDao.updateLastMessage(
            id = conversationId,
            messageId = messageId,
            content = content,
            senderId = senderId,
            senderName = senderName,
            timestamp = timestamp,
            unreadIncrement = unreadIncrement
        ) > 0
    }
    
    override suspend fun markConversationAsRead(conversationId: Long): Boolean {
        return conversationDao.markAsRead(conversationId) > 0
    }
    
    override suspend fun pinConversation(conversationId: Long, isPinned: Boolean): Boolean {
        return conversationDao.setPinned(conversationId, isPinned) > 0
    }
    
    override suspend fun muteConversation(conversationId: Long, isMuted: Boolean): Boolean {
        return conversationDao.setMuted(conversationId, isMuted) > 0
    }
    
    override suspend fun archiveConversation(conversationId: Long, isArchived: Boolean): Boolean {
        return conversationDao.setArchived(conversationId, isArchived) > 0
    }
    
    override suspend fun findOrCreateDirectConversation(
        currentUserId: String,
        otherUserId: String,
        currentUserName: String,
        otherUserName: String
    ): Conversation {
        val entity = conversationDao.findOrCreateDirectConversation(
            currentUserId = currentUserId,
            otherUserId = otherUserId,
            currentUserName = currentUserName,
            otherUserName = otherUserName
        )
        
        return ConversationMapper.fromEntity(entity)
    }
    
    override suspend fun getTotalUnreadCount(): Int {
        return conversationDao.getTotalUnreadCount()
    }
    
    override suspend fun getConversationsBySyncStatus(syncStatus: SyncStatus, limit: Int): List<Conversation> {
        val entities = conversationDao.getConversationsBySyncStatus(syncStatus, limit)
        return ConversationMapper.fromEntities(entities)
    }
    
    override suspend fun updateSyncStatus(ids: List<Long>, syncStatus: SyncStatus, syncToken: String?): Boolean {
        return conversationDao.updateSyncStatus(ids, syncStatus, syncToken) > 0
    }
}