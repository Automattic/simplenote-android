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
            val authError = getErrorFromJson(body)
            val errorStringRes = when (authError) {
                MagicLinkAuthError.INVALID_CODE -> R.string.magic_link_complete_login_invalid_code_error_message
                MagicLinkAuthError.REQUEST_NOT_FOUND -> R.string.magic_link_complete_login_expired_code_error_message
                MagicLinkAuthError.UNKNOWN_ERROR -> R.string.magic_link_general_error
            }
            return MagicLinkResponseResult.MagicLinkError(response.code(), authError, errorStringRes)
        }
    }

    private fun getErrorFromJson(body: String?): MagicLinkAuthError {
        try {
            val errorJson = JSONObject(body ?: "")
            val errorString = errorJson.getString(ERROR_FIELD)
            return when(errorString) {
                MagicLinkAuthError.INVALID_CODE.str -> {
                    MagicLinkAuthError.INVALID_CODE
                }
                MagicLinkAuthError.REQUEST_NOT_FOUND.str -> {
                    MagicLinkAuthError.REQUEST_NOT_FOUND
                }
                else -> MagicLinkAuthError.UNKNOWN_ERROR
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing error json", e)
        }
        return MagicLinkAuthError.UNKNOWN_ERROR
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
