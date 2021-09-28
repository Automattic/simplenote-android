package com.automattic.simplenote.authentication

import com.simperium.Simperium
import com.simperium.client.User
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class SessionManagerTest {
    private val simperium = Mockito.mock(Simperium::class.java)
    private val sessionManager = SessionManager(simperium)

    @Test
    fun whenUserHasTokenShouldReturnAuthorizedUser() {
        val user = User().apply {
            email = "test@test.com"
            accessToken = "124556"
        }
        whenever(simperium.user).thenReturn(user)

        val result = sessionManager.getCurrentUser()

        assertEquals(UserSession.AuthorizedUser(user), result)
    }

    @Test
    fun whenUserDoesNotHasTokenShouldReturnUnauthorizedUser() {
        val user = User().apply {
            email = "test@test.com"
            accessToken = null
        }
        whenever(simperium.user).thenReturn(user)

        val result = sessionManager.getCurrentUser()

        assertEquals(UserSession.UnauthorizedUser, result)
    }

    @Test
    fun whenUserIsNullShouldReturnUnauthorizedUser() {
        whenever(simperium.user).thenReturn(null)

        val result = sessionManager.getCurrentUser()

        assertEquals(UserSession.UnauthorizedUser, result)
    }
}