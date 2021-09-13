package com.automattic.simplenote.utils.crashlogging

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.simplenote.utils.locale.ContextBasedLocaleProvider
import com.automattic.simplenote.utils.locale.LocaleProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CrashLoggingModule {
    companion object {
        @Provides
        @Singleton
        fun provideCrashLogging(@ApplicationContext context: Context, crashLoggingDataProvider: CrashLoggingDataProvider): CrashLogging {
            return CrashLoggingProvider.createInstance(context, crashLoggingDataProvider)
        }
    }

    @Binds
    abstract fun bindCrashLoggingDataProvider(dataProvider: SimplenoteCrashLoggingDataProvider): CrashLoggingDataProvider

    @Binds
    abstract fun bindLocaleProvider(contextBasedLocaleProvider: ContextBasedLocaleProvider): LocaleProvider
}
