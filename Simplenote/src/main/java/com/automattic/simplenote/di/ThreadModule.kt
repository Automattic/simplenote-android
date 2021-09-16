package com.automattic.simplenote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named

const val IO_THREAD = "IO_THREAD"

@Module
@InstallIn(SingletonComponent::class)
abstract class ThreadModule {
    companion object {
        @Provides
        @Named(IO_THREAD)
        fun provideIODispatcher(): CoroutineDispatcher {
            return Dispatchers.IO
        }
    }
}
