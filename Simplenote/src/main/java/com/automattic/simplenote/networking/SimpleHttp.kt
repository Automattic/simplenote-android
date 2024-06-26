package com.automattic.simplenote.networking

import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SimpleHttp {
    private val JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8")
    private const val TIMEOUT_SECS = 30
    private const val HTTP_SCHEME = "https"
    private const val BASE_URL = "app.simplenote.com"

    private val client: OkHttpClient by lazy {
        OkHttpClient().newBuilder()
            .readTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
            .build()
    }

    private fun buildUrl(path: String): HttpUrl = HttpUrl.Builder()
        .scheme(HTTP_SCHEME)
        .host(BASE_URL)
        .addPathSegments(path)
        .build()

    fun firePostRequest(
        path: String,
        body: RequestBody,
    ): Response {
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .url(buildUrl(path))
            .post(body)
            .build()
        return client
            .newCall(request)
            .execute()
    }

    /**
     * Helper used to build a [RequestBody] from a JsonObject.
     */
    fun buildRequestBodyFromMap(mapRequest: Map<String, Any>): RequestBody {
        val json = JSONObject()
        try {
            mapRequest.forEach { json.put(it.key, it.value) }
        } catch (e: JSONException) {
            throw IllegalArgumentException("Cannot construct json with supplied map: $mapRequest")
        }
        return RequestBody.create(JSON_MEDIA_TYPE, json.toString())
    }
}
