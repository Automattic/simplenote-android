package com.automattic.simplenote.authentication.passkey

import android.util.Log
import com.automattic.simplenote.networking.OkHttpEndpoints
import com.automattic.simplenote.networking.SimpleHttp
import com.automattic.simplenote.repositories.PasskeyRepository
import com.automattic.simplenote.repositories.PasskeyResponseResult
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

const val EMAIL_KEY = "email"
const val PASSWORD_KEY = "password"
const val WEB_AUTHN_KEY = "webauthn"

class OkHttpPasskeyRepository @Inject constructor(private val simpleHttp: SimpleHttp,) : PasskeyRepository {
    @Throws(IOException::class)
    override suspend fun requestChallenge(username: String, password: String): PasskeyResponseResult {
        simpleHttp.firePostFormRequest(
            OkHttpEndpoints.PASSKEY_REQUEST_CHALLENGE,
            mapOf(Pair(EMAIL_KEY, username), Pair(PASSWORD_KEY, password), Pair(WEB_AUTHN_KEY, true))
        ).use { response ->
            val contentType = response.header("content-type")
            if (response.isSuccessful && contentType?.contains("application/json") == true) {
                val body = response.body()?.string()
                return if (!body.isNullOrBlank()) {
                    PasskeyResponseResult.PasskeyChallengeRequestSuccess(json = body)
                } else {
                    PasskeyResponseResult.PasskeyError(response.code())
                }
            }
            return PasskeyResponseResult.PasskeyError(response.code())
        }
    }

    override suspend fun addCredential(username: String, json: String): PasskeyResponseResult {
        // We need to manually add the email, I think...
        val jsonObj = JSONObject(json)
        jsonObj.put(EMAIL_KEY, username)
        simpleHttp.firePostRequest(OkHttpEndpoints.PASSKEY_ADD_CREDENTIAL, jsonObj.toString()).use { response ->
            if (response.isSuccessful) {
                return PasskeyResponseResult.PasskeyAddSuccess(response.code())
            }
            Log.e(TAG, "Error adding credential occurred - ${response.code()} ${response.message()}")
            return PasskeyResponseResult.PasskeyError(response.code())
        }
    }
}
