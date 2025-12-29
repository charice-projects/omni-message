package com.omnimsg.service

import android.content.Context
import androidx.work.*
import com.omnimsg.domain.models.SyncStatus
import com.omnimsg.domain.repositories.ContactRepository
import com.omnimsg.domain.repositories.ConversationRepository
import com.omnimsg.domain.repositories.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncService @Inject constructor(
    private val context: Context,
    private val contactRepository: ContactRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val workManager: WorkManager
) {
    
    companion object {
        private const val TAG = "SyncService"
        
        // 工作请求标签
        private const val SYNC_WORK_TAG = "omnimessage_sync"
        private const val PERIODIC_SYNC_TAG = "omnimessage_periodic_sync"
        
        // 同步间隔
        private const val PERIODIC_SYNC_INTERVAL_HOURS = 1L
    }
    
    data class SyncConfig(
        val syncContacts: Boolean = true,
        val syncMessages: Boolean = true,
        val syncConversations: Boolean = true,
        val forceFullSync: Boolean = false,
        val networkType: NetworkType = NetworkType.CONNECTED
    )
    
    enum class NetworkType {
        ANY, CONNECTED, UNMETERED, NOT_ROAMING
    }
    
    data class SyncResult(
        val success: Boolean,
        val syncedContacts: Int = 0,
        val syncedMessages: Int = 0,
        val syncedConversations: Int = 0,
        val failedItems: Int = 0,
        val error: String? = null
    )
    
    /**
     * 开始数据同步
     */
    fun startSync(config: SyncConfig = SyncConfig()) {
        if (!BuildConfig.ENABLE_SERVER_FEATURES) {
            Timber.d("Server features disabled, skipping sync")
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                when (config.networkType) {
                    NetworkType.ANY -> NetworkType.CONNECTED
                    NetworkType.CONNECTED -> NetworkType.CONNECTED
                    NetworkType.UNMETERED -> NetworkType.UNMETERED
                    NetworkType.NOT_ROAMING -> NetworkType.NOT_ROAMING
                }
            )
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SYNC_WORK_TAG)
            .setInputData(
                workDataOf(
                    "sync_contacts" to config.syncContacts,
                    "sync_messages" to config.syncMessages,
                    "sync_conversations" to config.syncConversations,
                    "force_full_sync" to config.forceFullSync
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueue(syncWorkRequest)
        
        Timber.d("Sync work enqueued: contacts=${config.syncContacts}, messages=${config.syncMessages}")
    }
    
    /**
     * 启动定期同步
     */
    fun schedulePeriodicSync() {
        if (!BuildConfig.ENABLE_SERVER_FEATURES) {
            Timber.d("Server features disabled, skipping periodic sync")
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val periodicSyncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_SYNC_TAG)
            .setInputData(
                workDataOf(
                    "sync_contacts" to true,
                    "sync_messages" to true,
                    "sync_conversations" to true,
                    "force_full_sync" to false
                )
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncWorkRequest
        )
        
        Timber.d("Periodic sync scheduled every $PERIODIC_SYNC_INTERVAL_HOURS hours")
    }
    
    /**
     * 取消所有同步任务
     */
    fun cancelAllSync() {
        workManager.cancelAllWorkByTag(SYNC_WORK_TAG)
        workManager.cancelAllWorkByTag(PERIODIC_SYNC_TAG)
        Timber.d("All sync work cancelled")
    }
    
    /**
     * 手动触发立即同步
     */
    fun syncNow(): SyncResult {
        return CoroutineScope(Dispatchers.IO).run {
            try {
                performSync(
                    syncContacts = true,
                    syncMessages = true,
                    syncConversations = true,
                    forceFullSync = false
                )
            } catch (e: Exception) {
                SyncResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * 执行实际的同步逻辑
     */
    private suspend fun performSync(
        syncContacts: Boolean,
        syncMessages: Boolean,
        syncConversations: Boolean,
        forceFullSync: Boolean
    ): SyncResult {
        Timber.d("Starting sync: contacts=$syncContacts, messages=$syncMessages")
        
        var syncedContacts = 0
        var syncedMessages = 0
        var syncedConversations = 0
        var failedItems = 0
        
        try {
            // 1. 同步联系人
            if (syncContacts) {
                val contactsResult = syncContacts(forceFullSync)
                syncedContacts = contactsResult.syncedCount
                failedItems += contactsResult.failedCount
            }
            
            // 2. 同步会话
            if (syncConversations) {
                val conversationsResult = syncConversations(forceFullSync)
                syncedConversations = conversationsResult.syncedCount
                failedItems += conversationsResult.failedCount
            }
            
            // 3. 同步消息
            if (syncMessages) {
                val messagesResult = syncMessages(forceFullSync)
                syncedMessages = messagesResult.syncedCount
                failedItems += messagesResult.failedCount
            }
            
            return SyncResult(
                success = true,
                syncedContacts = syncedContacts,
                syncedMessages = syncedMessages,
                syncedConversations = syncedConversations,
                failedItems = failedItems
            )
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            return SyncResult(
                success = false,
                syncedContacts = syncedContacts,
                syncedMessages = syncedMessages,
                syncedConversations = syncedConversations,
                failedItems = failedItems,
                error = e.message
            )
        }
    }
    
    private suspend fun syncContacts(forceFullSync: Boolean): SyncItemResult {
        // 获取需要同步的联系人
        val pendingContacts = contactRepository.getContactsBySyncStatus(SyncStatus.PENDING, 100)
        val failedContacts = contactRepository.getContactsBySyncStatus(SyncStatus.FAILED, 50)
        
        val contactsToSync = if (forceFullSync) {
            // 强制全量同步的逻辑
            emptyList() // 实际实现中这里需要获取所有联系人
        } else {
            pendingContacts + failedContacts
        }
        
        Timber.d("Syncing ${contactsToSync.size} contacts")
        
        // 这里实现实际的同步逻辑
        // 1. 调用服务器API
        // 2. 处理响应
        // 3. 更新本地数据库
        
        return SyncItemResult(
            syncedCount = contactsToSync.size,
            failedCount = 0
        )
    }
    
    private suspend fun syncConversations(forceFullSync: Boolean): SyncItemResult {
        // 获取需要同步的会话
        val pendingConversations = conversationRepository.getConversationsBySyncStatus(SyncStatus.PENDING, 100)
        val failedConversations = conversationRepository.getConversationsBySyncStatus(SyncStatus.FAILED, 50)
        
        val conversationsToSync = if (forceFullSync) {
            // 强制全量同步
            emptyList()
        } else {
            pendingConversations + failedConversations
        }
        
        Timber.d("Syncing ${conversationsToSync.size} conversations")
        
        // 实现会话同步逻辑
        
        return SyncItemResult(
            syncedCount = conversationsToSync.size,
            failedCount = 0
        )
    }
    
    private suspend fun syncMessages(forceFullSync: Boolean): SyncItemResult {
        // 获取需要同步的消息
        val pendingMessages = messageRepository.getMessagesBySyncStatus(SyncStatus.PENDING, 200)
        val failedMessages = messageRepository.getMessagesBySyncStatus(SyncStatus.FAILED, 100)
        
        val messagesToSync = if (forceFullSync) {
            // 强制全量同步
            emptyList()
        } else {
            pendingMessages + failedMessages
        }
        
        Timber.d("Syncing ${messagesToSync.size} messages")
        
        // 实现消息同步逻辑
        // 特别注意：消息同步需要处理顺序和去重
        
        return SyncItemResult(
            syncedCount = messagesToSync.size,
            failedCount = 0
        )
    }
    
    private data class SyncItemResult(
        val syncedCount: Int,
        val failedCount: Int
    )
}

/**
 * WorkManager的工作器
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    @Inject
    lateinit var syncService: SyncService
    
    init {
        // 这里需要注入依赖，可以通过Hilt的WorkerFactory实现
        // 为了简化，我们暂时直接创建
    }
    
    override suspend fun doWork(): Result {
        Timber.d("SyncWorker started")
        
        return try {
            val syncContacts = inputData.getBoolean("sync_contacts", true)
            val syncMessages = inputData.getBoolean("sync_messages", true)
            val syncConversations = inputData.getBoolean("sync_conversations", true)
            val forceFullSync = inputData.getBoolean("force_full_sync", false)
            
            // 创建临时的SyncService实例
            // 在实际应用中，应该通过依赖注入获取
            val result = syncService.syncNow()
            
            if (result.success) {
                Timber.d("Sync completed successfully: ${result.syncedMessages} messages, ${result.syncedContacts} contacts")
                Result.success()
            } else {
                Timber.w("Sync completed with errors: ${result.error}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker failed")
            Result.failure()
        }
    }
}