package com.omnimsg.app.di

import com.omnimsg.domain.usecases.contact.*
import com.omnimsg.domain.usecases.excel.*
import com.omnimsg.domain.usecases.message.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {
    
    // 联系人Use Cases
    @Binds
    @Singleton
    abstract fun bindAddContactUseCase(useCase: AddContactUseCase): AddContactUseCase
    
    @Binds
    @Singleton
    abstract fun bindGetContactUseCase(useCase: GetContactUseCase): GetContactUseCase
    
    @Binds
    @Singleton
    abstract fun bindUpdateContactUseCase(useCase: UpdateContactUseCase): UpdateContactUseCase
    
    @Binds
    @Singleton
    abstract fun bindSearchContactsUseCase(useCase: SearchContactsUseCase): SearchContactsUseCase
    
    @Binds 
    @Singleton
    abstract fun bindGetContactsByGroupUseCase(useCase: GetContactsByGroupUseCase): GetContactsByGroupUseCase
    
    // 消息Use Cases
    @Binds
    @Singleton
    abstract fun bindSendMessageUseCase(useCase: SendMessageUseCase): SendMessageUseCase
    
    @Binds
    @Singleton
    abstract fun bindReceiveMessageUseCase(useCase: ReceiveMessageUseCase): ReceiveMessageUseCase
    
    @Binds
    @Singleton
    abstract fun bindGetMessagesUseCase(useCase: GetMessagesUseCase): GetMessagesUseCase
    
    @Binds
    @Singleton
    abstract fun bindMarkMessageAsReadUseCase(useCase: MarkMessageAsReadUseCase): MarkMessageAsReadUseCase
    
    @Binds
    @Singleton
    abstract fun bindCreateConversationUseCase(useCase: CreateConversationUseCase): CreateConversationUseCase
    
    // Excel导入Use Cases
    @Binds
    @Singleton
    abstract fun bindPreviewExcelUseCase(useCase: PreviewExcelUseCase): PreviewExcelUseCase
    
    @Binds
    @Singleton
    abstract fun bindAnalyzeExcelUseCase(useCase: AnalyzeExcelUseCase): AnalyzeExcelUseCase
    
    @Binds
    @Singleton
    abstract fun bindImportExcelUseCase(useCase: ImportExcelUseCase): ImportExcelUseCase
}