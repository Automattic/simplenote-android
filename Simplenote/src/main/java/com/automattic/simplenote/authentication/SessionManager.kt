package com.automattic.simplenote.authentication

import com.simperium.Simperium
import com.simperium.client.User
import javax.inject.Inject

sealed class UserSession {
    object UnauthorizedUser : UserSession()
    data class AuthorizedUser(val user: User) : UserSession()
}

class SessionManager @Inject constructor(private val simperium: Simperium) {
    fun getCurrentUser(): UserSession {
        val currentUser = simperium.user ?: return UserSession.UnauthorizedUser

        return when (currentUser.email != null && !currentUser.needsAuthorization()) {
            true -> UserSession.AuthorizedUser(currentUser)
            false -> UserSession.UnauthorizedUser
        }
    }
}
