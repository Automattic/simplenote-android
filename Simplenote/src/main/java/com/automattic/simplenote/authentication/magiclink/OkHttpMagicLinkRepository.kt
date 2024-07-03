package com.automattic.simplenote.authentication.magiclink

import com.automattic.simplenote.networking.SimpleHttp
import com.automattic.simplenote.repositories.MagicLinkRepository
import com.automattic.simplenote.repositories.MagicLinkResponseResult
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

class OkHttpMagicLinkRepository @Inject constructor(private val simpleHttp: SimpleHttp,) : MagicLinkRepository {
    @Throws(IOException::class)
    override suspend fun completeLogin(authKey: String, authCode: String): MagicLinkResponseResult {
        simpleHttp.firePostRequest(
            "account/complete-login",
            mapOf(Pair("auth_key", authKey), Pair("auth_code", authCode))
        ).use { response ->
            if (response.isSuccessful) {
                val body = response.body()?.string()
                val json = JSONObject(body ?: "")
                val syncToken = json.getString("sync_token")
                val username = json.getString("username")
                return MagicLinkResponseResult.MagicLinkCompleteSuccess(username, syncToken)
            }
            return MagicLinkResponseResult.MagicLinkError(response.code())
        }
    }

    @Throws(IOException::class)
    override suspend fun requestLogin(username: String): MagicLinkResponseResult {
        simpleHttp.firePostRequest(
            "account/request-login",
            mapOf(Pair("username", username), Pair("request_source", "android"))
        ).use { response ->
            if (response.isSuccessful) {
                return MagicLinkResponseResult.MagicLinkRequestSuccess(response.code())
            } else {
                return MagicLinkResponseResult.MagicLinkError(response.code())
            }
        }
    }
}
