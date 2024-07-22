package com.automattic.simplenote.authentication.passkey

import android.util.Log
import com.automattic.simplenote.networking.OkHttpEndpoints
import com.automattic.simplenote.networking.SimpleHttp
import com.automattic.simplenote.repositories.PasskeyRepository
import com.automattic.simplenote.repositories.PasskeyResponseResult
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

const val EMAIL_KEY = "email"
const val PASSWORD_KEY = "password"
const val WEB_AUTHN_KEY = "webauthn"

@Singleton
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

    override suspend fun prepareAuthChallenge(username: String): PasskeyResponseResult {
        simpleHttp.firePostRequest(
            OkHttpEndpoints.PASSKEY_PREPARE_AUTH_CHALLENGE,
            mapOf(Pair("email", username))
        ).use { response ->
            if (response.isSuccessful) {
                val body = response.body()?.string()
                Log.e(TAG, "prepareAuthChallenge - ($body)")
                if (!body.isNullOrBlank()) {
                    return PasskeyResponseResult.PasskeyPrepareResult(body)
                }
                return PasskeyResponseResult.PasskeyError(response.code())
            }
            return PasskeyResponseResult.PasskeyError(response.code())
        }
    }

    override suspend fun prepareDiscoverableAuthChallenge(): PasskeyResponseResult {
        simpleHttp.firePostRequest(
            OkHttpEndpoints.PASSKEY_DISCOVERABLE_CHALLENGE,
            ""
        ).use { response ->
            if (response.isSuccessful) {
                val body = response.body()?.string()
                Log.e(TAG, body ?: "")
                if (!body.isNullOrBlank()) {
                    return PasskeyResponseResult.PasskeyPrepareResult(body)
                }
                return PasskeyResponseResult.PasskeyError(response.code())
            }
            return PasskeyResponseResult.PasskeyError(response.code())
        }
    }

    override suspend fun verifyAuthChallenge(username: String, json: String): PasskeyResponseResult {
        val jsonObj = JSONObject(json)
        jsonObj.put(EMAIL_KEY, username)

        simpleHttp.firePostRequest(
            OkHttpEndpoints.PASSKEY_VERIFY_LOGIN_CREDENTIAL,
            jsonObj.toString()
        ).use { response ->
            if (response.isSuccessful) {
                try {
                    val bodyString = response.body()?.string()
                    val resultJsonObj = JSONObject(bodyString ?: "")
                    val token = resultJsonObj.getString("access_token")
                    val email = resultJsonObj.getString("username")
                    if (!token.isNullOrBlank() && !email.isNullOrBlank()) {
                        return PasskeyResponseResult.PasskeyVerifyResult(token, email)
                    } else {
                        Log.e(TAG, "The resulting email or token were null")
                    }
                } catch (e: JSONException) {
                    Log.e(TAG, "Parsing the verified challenge caused error", e)
                }
                return PasskeyResponseResult.PasskeyError(response.code())
            }
            return PasskeyResponseResult.PasskeyError(response.code())
        }
    }
}
