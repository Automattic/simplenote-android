package com.automattic.simplenote.di

import com.automattic.simplenote.networking.HeadersInterceptor
import com.automattic.simplenote.networking.SimpleHttp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private const val TIMEOUT_SECS = 30

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideOkHttp(): OkHttpClient = OkHttpClient().newBuilder()
        .addNetworkInterceptor(HeadersInterceptor())
        .readTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
        .build()

    @Provides
    fun provideSimpleHttp(okHttpClient: OkHttpClient): SimpleHttp = SimpleHttp(okHttpClient)
}
