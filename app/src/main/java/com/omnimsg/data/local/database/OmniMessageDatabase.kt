package com.omnimsg.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.omnimsg.data.local.database.converters.*
import com.omnimsg.data.local.database.daos.*
import com.omnimsg.data.local.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Database(
    entities = [
        ContactEntity::class,
        MessageEntity::class,
        ConversationEntity::class,
        GroupEntity::class,
        AttachmentEntity::class,
        NotificationEntity::class,
        WorkflowEntity::class,
        EmergencyContactEntity::class,
        VoiceCommandEntity::class,
        ExcelImportRecordEntity::class,
        CommandHistoryEntity::class,
        PrivacyAuditLogEntity::class,
        QuickActionSettingEntity::class,
        SceneContextEntity::class,
        EmergencySessionEntity::class,
        NotificationGroupEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    DateConverter::class,
    ListConverter::class,
    MapConverter::class,
    UriConverter::class
)
abstract class OmniMessageDatabase : RoomDatabase() {
    
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun groupDao(): GroupDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun voiceCommandDao(): VoiceCommandDao
    abstract fun excelImportRecordDao(): ExcelImportRecordDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun privacyAuditLogDao(): PrivacyAuditLogDao
    abstract fun quickActionSettingDao(): QuickActionSettingDao
    abstract fun sceneContextDao(): SceneContextDao
    abstract fun emergencySessionDao(): EmergencySessionDao
    abstract fun notificationGroupDao(): NotificationGroupDao
    
    companion object {
        @Volatile
        private var INSTANCE: OmniMessageDatabase? = null
        
        private const val DATABASE_NAME = "omnimessage_database.db"
        
        fun getInstance(context: Context): OmniMessageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmniMessageDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(DatabaseCallback())
                .addMigrations()
                .setQueryExecutor(Executors.newFixedThreadPool(4))
                .setJournalMode(JournalMode.TRUNCATE)
                .enableMultiInstanceInvalidation()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
    
    class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // 在数据库创建时执行初始化操作
            CoroutineScope(Dispatchers.IO).launch {
                // 这里可以插入一些初始数据或执行初始化脚本
            }
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            
            // 在数据库打开时执行操作
            db.execSQL("PRAGMA foreign_keys = ON;")
            db.execSQL("PRAGMA journal_mode = WAL;")
            db.execSQL("PRAGMA synchronous = NORMAL;")
        }
    }
}

// 如果需要SQLCipher加密，可以使用以下配置
/*
.setOpenHelperFactory(SQLCipherHelperFactory(getEncryptionKey()))

private fun getEncryptionKey(): ByteArray {
    // 从安全存储获取加密密钥
    // 例如从Android Keystore或用户输入的密码派生
    return KeyStoreManager.getDatabaseKey()
}
*/