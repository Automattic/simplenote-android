package com.automattic.simplenote.networking

import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject

private const val HTTP_SCHEME = "https"
private const val BASE_URL = "app.simplenote.com"
open class SimpleHttp @Inject constructor(val client: OkHttpClient) {
    private val jsonMediaType = MediaType.parse("application/json; charset=utf-8")

    private fun buildUrl(path: String): HttpUrl = HttpUrl.Builder()
        .scheme(HTTP_SCHEME)
        .host(BASE_URL)
        .addPathSegments(path)
        .build()

    fun firePostRequest(
        path: String,
        bodyAsMap: Map<String, Any>
    ): Response {
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader("Accept-Language", Locale.getDefault().toString())
            .url(buildUrl(path))
            .post(buildRequestBodyFromMap(bodyAsMap))
            .build()
        return client
            .newCall(request)
            .execute()
    }

    /**
     * Helper used to build a [RequestBody] from a JsonObject.
     */
    private fun buildRequestBodyFromMap(bodyAsMap: Map<String, Any>): RequestBody {
        val json = JSONObject()
        try {
            bodyAsMap.forEach { json.put(it.key, it.value) }
        } catch (e: JSONException) {
            throw IllegalArgumentException("Cannot construct json with supplied map: $bodyAsMap")
        }
        return RequestBody.create(jsonMediaType, json.toString())
    }
}
