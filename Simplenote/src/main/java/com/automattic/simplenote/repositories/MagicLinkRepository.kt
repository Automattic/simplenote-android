package com.automattic.simplenote.repositories

import androidx.annotation.StringRes
import com.automattic.simplenote.authentication.magiclink.MagicLinkAuthError

interface MagicLinkRepository {
    suspend fun completeLogin(username: String, authCode: String): MagicLinkResponseResult
    suspend fun requestLogin(username: String): MagicLinkResponseResult
}

sealed class MagicLinkResponseResult {
    data class MagicLinkCompleteSuccess(val username: String, val syncToken: String) : MagicLinkResponseResult()
    data class MagicLinkRequestSuccess(val code: Int) : MagicLinkResponseResult()
    data class MagicLinkError(val code: Int, val authError: MagicLinkAuthError = MagicLinkAuthError.UNKNOWN_ERROR, @StringRes val errorMessage: Int? = null) : MagicLinkResponseResult()
}
