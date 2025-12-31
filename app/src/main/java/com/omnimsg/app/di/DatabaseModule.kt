package com.omnimsg.app.di

import android.content.Context
import com.omnimsg.data.local.database.OmniMessageDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OmniMessageDatabase {
        return OmniMessageDatabase.getInstance(context)
    }
    
    @Provides
    fun provideContactDao(database: OmniMessageDatabase) = database.contactDao()
    
    @Provides
    fun provideMessageDao(database: OmniMessageDatabase) = database.messageDao()
    
    @Provides
    fun provideConversationDao(database: OmniMessageDatabase) = database.conversationDao()
    
    @Provides
    fun provideGroupDao(database: OmniMessageDatabase) = database.groupDao()
    
    @Provides
    fun provideAttachmentDao(database: OmniMessageDatabase) = database.attachmentDao()
    
    @Provides
    fun provideNotificationDao(database: OmniMessageDatabase) = database.notificationDao()
}