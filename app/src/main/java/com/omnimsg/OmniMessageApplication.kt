package com.omnimsg

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.omnimsg.core.di.AppModule
import com.omnimsg.core.di.DatabaseModule
import com.omnimsg.core.di.NetworkModule
import com.omnimsg.core.kernel.CoreKernel
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * OmniMessage 应用主类
 */
@HiltAndroidApp
class OmniMessageApplication : Application() {

    @Inject
    lateinit var coreKernel: CoreKernel

    // DataStore 作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 初始化日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 初始化内核
        applicationScope.launch {
            val result = coreKernel.initialize()
            when (result) {
                is CoreKernel.InitializationResult.Success -> {
                    Timber.d("Core kernel initialized successfully")
                }
                is CoreKernel.InitializationResult.Error -> {
                    Timber.e(result.exception, "Failed to initialize core kernel")
                }
            }
        }

        // 其他初始化...
        initDataStore()
    }

    private fun initDataStore() {
        // 创建 DataStore
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = applicationScope
        ) {
            preferencesDataStoreFile("omnimessage_preferences")
        }

        // 可以在这里注入或使用 DataStore
    }

    override fun onTerminate() {
        applicationScope.launch {
            coreKernel.shutdown()
        }
        super.onTerminate()
    }
}