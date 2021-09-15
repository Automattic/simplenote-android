package com.automattic.simplenote.di

import android.content.Context
import com.automattic.simplenote.Simplenote
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideSimplenote(@ApplicationContext appContext: Context): Simplenote = appContext as Simplenote
}
