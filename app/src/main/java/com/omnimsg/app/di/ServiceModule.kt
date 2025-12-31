package com.omnimsg.app.di

import android.content.Context
import androidx.work.WorkManager
import com.omnimsg.feature.excelimport.*
import com.omnimsg.service.SyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideSyncService(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): SyncService {
        return SyncService(
            context = context,
            workManager = workManager
        )
    }
    
    @Provides
    @Singleton
    fun provideExcelParser(@ApplicationContext context: Context): ExcelParser {
        return ExcelParser(context)
    }
    
    @Provides
    @Singleton
    fun provideAIFieldRecognizer(): AIFieldRecognizer {
        return AIFieldRecognizer()
    }
    
    @Provides
    @Singleton
    fun provideContactDataValidator(): ContactDataValidator {
        return ContactDataValidator()
    }
    
    @Provides
    @Singleton
    fun provideContactDuplicateDetector(
        contactRepository: ContactRepository
    ): ContactDuplicateDetector {
        return ContactDuplicateDetector(contactRepository)
    }
    
    @Provides
    @Singleton
    fun provideExcelImportEngine(
        excelParser: ExcelParser,
        aiFieldRecognizer: AIFieldRecognizer,
        contactRepository: ContactRepository,
        excelImportRepository: ExcelImportRepository,
        duplicateDetector: ContactDuplicateDetector,
        dataValidator: ContactDataValidator
    ): ExcelImportEngine {
        return ExcelImportEngine(
            excelParser = excelParser,
            aiFieldRecognizer = aiFieldRecognizer,
            contactRepository = contactRepository,
            excelImportRepository = excelImportRepository,
            duplicateDetector = duplicateDetector,
            dataValidator = dataValidator
        )
    }
}