package com.automattic.simplenote.networking

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

private const val HTTP_SCHEME = "https"
// TODO: Do not commit into production with test URL
//private const val BASE_URL = "app.simplenote.com"
private const val BASE_URL = "passkey-dev-dot-simple-note-hrd.appspot.com"
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
    ): Response = firePostRequest(path, buildJsonBodyFromMap(bodyAsMap))

    fun firePostRequest(
        path: String,
        json: String,
    ): Response {
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .url(buildUrl(path))
            .post(RequestBody.create(jsonMediaType, json))
            .build()
        return client
            .newCall(request)
            .execute()
    }

    fun firePostFormRequest(
        path: String,
        bodyAsMap: Map<String, Any>
    ): Response {
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .url(buildUrl(path))
            .post(buildFormRequestBodyFromMap(bodyAsMap))
            .build()
        return client
            .newCall(request)
            .execute()
    }

    /**
     * Helper used to build a [RequestBody] from a JsonObject.
     */
    private fun buildJsonBodyFromMap(bodyAsMap: Map<String, Any>): String {
        val json = JSONObject()
        try {
            bodyAsMap.forEach { json.put(it.key, it.value) }
        } catch (e: JSONException) {
            throw IllegalArgumentException("Cannot construct json with supplied map: $bodyAsMap")
        }
        return json.toString()
    }

    private fun buildFormRequestBodyFromMap(bodyAsMap: Map<String, Any>): FormBody {
        val formBodyBuilder = FormBody.Builder()
        bodyAsMap.forEach {
            formBodyBuilder.add(it.key, it.value.toString())
        }
        return formBodyBuilder.build()
    }
}
