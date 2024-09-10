package com.automattic.simplenote.networking

import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

/**
 * Interceptor used to add headers we expect on all OkHttp calls.
 */
class HeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .header("Accept-Language", Locale.getDefault().toLanguageTag())
            .build()
        return chain.proceed(newRequest)
    }
}
