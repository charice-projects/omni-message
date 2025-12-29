package com.omnimsg.app.di

import android.content.Context
import com.omnimsg.OmniMessageApplication
import com.omnimsg.core.di.AppModule
import com.omnimsg.core.di.DatabaseModule
import com.omnimsg.core.di.NetworkModule
import com.omnimsg.core.di.RepositoryModule
import com.omnimsg.core.di.ServiceModule
import com.omnimsg.core.di.ViewModelModule
import dagger.BindsInstance
import dagger.Component
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        DatabaseModule::class,
        NetworkModule::class,
        RepositoryModule::class,
        ServiceModule::class,
        ViewModelModule::class
    ]
)
interface AppComponent {

    fun inject(application: OmniMessageApplication)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}