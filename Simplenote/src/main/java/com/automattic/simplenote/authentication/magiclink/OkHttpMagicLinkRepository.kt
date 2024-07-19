package com.automattic.simplenote.authentication.magiclink

import android.util.Log
import com.automattic.simplenote.R
import com.automattic.simplenote.networking.SimpleHttp
import com.automattic.simplenote.repositories.MagicLinkRepository
import com.automattic.simplenote.repositories.MagicLinkResponseResult
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

private const val TAG = "OkHttpMagicLinkRepository"

private const val ERROR_FIELD = "error"
class OkHttpMagicLinkRepository @Inject constructor(private val simpleHttp: SimpleHttp,) : MagicLinkRepository {
    @Throws(IOException::class)
    override suspend fun completeLogin(username: String, authCode: String): MagicLinkResponseResult {
        simpleHttp.firePostRequest(
            "account/complete-login",
            mapOf(Pair("username", username), Pair("auth_code", authCode))
        ).use { response ->
            val body = response.body()?.string()
            if (response.isSuccessful) {
                val json = JSONObject(body ?: "")
                val syncToken = json.getString("sync_token")
                val user = json.getString("username")
                return MagicLinkResponseResult.MagicLinkCompleteSuccess(user, syncToken)
            }
            Log.d(TAG, "Error completing login: $body")
            return MagicLinkResponseResult.MagicLinkError(response.code(), handleErrorMessage(body))
        }
    }

    private fun handleErrorMessage(body: String?): Int  {
        try {
            val errorJson = JSONObject(body ?: "")
            val errorString = errorJson.getString(ERROR_FIELD)
            if (errorString == "invalid-code") {
                return R.string.magic_link_complete_login_invalid_code_error_message
            } else if (errorString == "request-not-found") {
                return R.string.magic_link_complete_login_expired_code_error_message
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing unsuccessful response", e)
        }
        return R.string.magic_link_general_error
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
