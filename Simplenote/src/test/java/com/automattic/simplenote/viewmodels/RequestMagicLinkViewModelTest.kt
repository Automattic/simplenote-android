package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.networking.SimpleHttp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.Response
import org.junit.Test
import org.mockito.Mockito

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule

const val email = "test@email.com"

@ExperimentalCoroutinesApi
class RequestMagicLinkViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val response: Response = Mockito.mock(Response::class.java)
    private val simpleHttp: SimpleHttp = Mockito.mock(SimpleHttp::class.java)

    @Before
    fun setup() {
        Mockito.`when`(
            simpleHttp.firePostRequest("account/request-login", mapOf(Pair("username", email), Pair("request_source", "android")))
        ).thenReturn(response)
    }

    @Test
    fun instantiateViewModel() = runBlockingTest {
        val viewModel = RequestMagicLinkViewModel(simpleHttp, TestCoroutineDispatcher())
        assertEquals(null, viewModel.magicLinkRequestUiState.value)
    }

    @Test
    fun firingPostRequestShouldLeadToSuccess() = runBlockingTest {
        Mockito.`when`(response.isSuccessful).thenReturn(true)

        val viewModel = RequestMagicLinkViewModel(simpleHttp, TestCoroutineDispatcher())
        val states = mutableListOf<MagicLinkRequestUiState>()
        viewModel.magicLinkRequestUiState.observeForever {
            states.add(it)
        }
        viewModel.requestLogin(email)
        assertEquals(MagicLinkRequestUiState.Loading::class.java, states[0]::class.java)
        assertEquals(MagicLinkRequestUiState.Success(username = email), states[1])
    }

    @Test
    fun firingPostRequestShouldLeadToError() = runBlockingTest {
        Mockito.`when`(response.isSuccessful).thenReturn(false)

        val viewModel = RequestMagicLinkViewModel(simpleHttp, TestCoroutineDispatcher())
        val states = mutableListOf<MagicLinkRequestUiState>()
        viewModel.magicLinkRequestUiState.observeForever {
            states.add(it)
        }
        viewModel.requestLogin(email)
        assertEquals(MagicLinkRequestUiState.Loading::class.java, states[0]::class.java)
        assertEquals(MagicLinkRequestUiState.Error::class.java, states[1]::class.java)
    }
}
