package com.omnimsg.data.repositories

import com.omnimsg.data.local.database.daos.MessageDao
import com.omnimsg.data.mappers.MessageMapper
import com.omnimsg.domain.models.Message
import com.omnimsg.domain.models.MessageStatus
import com.omnimsg.domain.models.SyncStatus
import com.omnimsg.domain.repositories.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {
    
    override suspend fun saveMessage(message: Message): Message {
        val entity = MessageMapper.toEntity(message)
        val id = messageDao.insert(entity)
        return getMessageById(id) ?: message
    }
    
    override suspend fun saveMessages(messages: List<Message>): List<Message> {
        val entities = MessageMapper.toEntities(messages)
        val ids = messageDao.insertAll(entities)
        return ids.mapNotNull { getMessageById(it) }
    }
    
    override suspend fun updateMessage(message: Message): Boolean {
        val entity = MessageMapper.toEntity(message)
        return messageDao.update(entity) > 0
    }
    
    override suspend fun deleteMessage(id: Long): Boolean {
        return messageDao.delete(id) > 0
    }
    
    override suspend fun softDeleteMessage(id: Long): Boolean {
        return messageDao.softDelete(id) > 0
    }
    
    override suspend fun getMessageById(id: Long): Message? {
        return messageDao.getById(id)?.let { MessageMapper.fromEntity(it) }
    }
    
    override suspend fun getMessageByMessageId(messageId: String): Message? {
        return messageDao.getByMessageId(messageId)?.let { MessageMapper.fromEntity(it) }
    }
    
    override fun getMessagesByConversation(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessagesByConversationStream(conversationId)
            .map { entities -> MessageMapper.fromEntities(entities) }
    }
    
    override fun getMessagesByConversationIncludingDeleted(conversationId: Long): Flow<List<Message>> {
        // 注意：这里需要扩展DAO来支持包含已删除的消息
        // 暂时返回不包含已删除的消息
        return getMessagesByConversation(conversationId)
    }
    
    override fun getMessagesBySender(senderId: String): Flow<List<Message>> {
        return messageDao.getMessagesBySenderStream(senderId)
            .map { entities -> MessageMapper.fromEntities(entities) }
    }
    
    override fun searchMessages(query: String): Flow<List<Message>> {
        return messageDao.searchMessages(query)
            .map { entities -> MessageMapper.fromEntities(entities) }
    }
    
    override fun searchMessagesInConversation(conversationId: Long, query: String): Flow<List<Message>> {
        return messageDao.searchMessagesInConversation(conversationId, query)
            .map { entities -> MessageMapper.fromEntities(entities) }
    }
    
    override suspend fun updateMessageStatus(id: Long, status: MessageStatus): Boolean {
        return messageDao.updateStatus(id, status) > 0
    }
    
    override suspend fun markMessageAsRead(id: Long): Boolean {
        return messageDao.markAsRead(id) > 0
    }
    
    override suspend fun markMessageAsDelivered(id: Long): Boolean {
        return messageDao.markAsDelivered(id) > 0
    }
    
    override suspend fun updateSyncStatus(ids: List<Long>, syncStatus: SyncStatus, syncToken: String?): Boolean {
        return messageDao.updateSyncStatus(ids, syncStatus, syncToken) > 0
    }
    
    override suspend fun getMessagesBySyncStatus(syncStatus: SyncStatus, limit: Int): List<Message> {
        val entities = messageDao.getMessagesBySyncStatus(syncStatus, limit)
        return MessageMapper.fromEntities(entities)
    }
    
    override suspend fun getMessageCountByConversation(conversationId: Long): Int {
        return messageDao.getMessageCountByConversation(conversationId)
    }
    
    override suspend fun getUnreadCount(conversationId: Long, userId: String): Int {
        return messageDao.getUnreadCount(conversationId, userId)
    }
    
    override suspend fun getMessagesPaginated(conversationId: Long, limit: Int, offset: Int): List<Message> {
        val entities = messageDao.getMessagesPaginated(conversationId, limit, offset)
        return MessageMapper.fromEntities(entities)
    }
    
    override suspend fun getLatestMessage(conversationId: Long): Message? {
        return messageDao.getLatestMessage(conversationId)?.let { MessageMapper.fromEntity(it) }
    }
    
    override suspend fun purgeSoftDeleted(olderThanDays: Int): Int {
        val cutoff = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000)
        return messageDao.purgeSoftDeleted(cutoff)
    }
}