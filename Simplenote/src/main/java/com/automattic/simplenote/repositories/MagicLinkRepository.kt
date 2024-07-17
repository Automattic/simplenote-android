package com.automattic.simplenote.repositories

interface MagicLinkRepository {
    suspend fun completeLogin(username: String, authCode: String): MagicLinkResponseResult
    suspend fun requestLogin(username: String): MagicLinkResponseResult
}

sealed class MagicLinkResponseResult {
    data class MagicLinkCompleteSuccess(val username: String, val syncToken: String) : MagicLinkResponseResult()
    data class MagicLinkRequestSuccess(val code: Int) : MagicLinkResponseResult()
    data class MagicLinkError(val code: Int) : MagicLinkResponseResult()
}
