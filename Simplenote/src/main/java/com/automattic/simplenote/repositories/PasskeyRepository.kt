package com.automattic.simplenote.repositories

interface PasskeyRepository {
    suspend fun requestChallenge(email: String, password: String): PasskeyResponseResult
    suspend fun addCredential(username: String, json: String): PasskeyResponseResult

    suspend fun prepareAuthChallenge(username: String): PasskeyResponseResult
    suspend fun prepareDiscoverableAuthChallenge(): PasskeyResponseResult
    suspend fun verifyAuthChallenge(username: String, json: String): PasskeyResponseResult
}

sealed class PasskeyResponseResult {
    data class PasskeyChallengeRequestSuccess(val json: String) : PasskeyResponseResult()
    data class PasskeyAddSuccess(val code: Int) : PasskeyResponseResult()
    data class PasskeyError(val code: Int) : PasskeyResponseResult()

    data class PasskeyPrepareResult(val json: String): PasskeyResponseResult()
    data class PasskeyVerifyResult(val token: String, val username: String): PasskeyResponseResult()
}
