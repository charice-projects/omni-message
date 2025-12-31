package com.omnimsg.app.di

import androidx.lifecycle.ViewModel
import com.omnimsg.presentation.factories.ViewModelFactory
import com.omnimsg.presentation.factories.ViewModelKey
import com.omnimsg.presentation.viewmodels.ContactViewModel
import com.omnimsg.presentation.viewmodels.ConversationViewModel
import com.omnimsg.presentation.viewmodels.ExcelImportViewModel
import com.omnimsg.presentation.viewmodels.MessageViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {
    
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
    
    @Binds
    @IntoMap
    @ViewModelKey(ContactViewModel::class)
    abstract fun bindContactViewModel(viewModel: ContactViewModel): ViewModel
    
    @Binds
    @IntoMap
    @ViewModelKey(MessageViewModel::class)
    abstract fun bindMessageViewModel(viewModel: MessageViewModel): ViewModel
    
    @Binds
    @IntoMap
    @ViewModelKey(ConversationViewModel::class)
    abstract fun bindConversationViewModel(viewModel: ConversationViewModel): ViewModel
    
    @Binds
    @IntoMap
    @ViewModelKey(ExcelImportViewModel::class)
    abstract fun bindExcelImportViewModel(viewModel: ExcelImportViewModel): ViewModel
    
    // 添加更多ViewModel绑定
}