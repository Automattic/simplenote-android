package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.networking.SimpleHttp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Test
import org.mockito.Mockito

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule

const val AUTH_KEY_TEST = "auth_key_test"
const val AUTH_CODE_TEST = "auth_code_test"

@ExperimentalCoroutinesApi
class CompleteMagicLinkViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val response: Response = Mockito.mock(Response::class.java)
    private val responseBody: ResponseBody = Mockito.mock(ResponseBody::class.java)
    private val simpleHttp: SimpleHttp = Mockito.mock(SimpleHttp::class.java)
    private val app = Mockito.mock(Simplenote::class.java)

    @Before
    fun setup() {
        Mockito.`when`(
            simpleHttp.firePostRequest(
                "account/complete-login",
                mapOf(Pair("auth_key", AUTH_KEY_TEST), Pair("auth_code", AUTH_CODE_TEST))
            )
        ).thenReturn(response)
    }

    @Test
    fun instantiateViewModel() = runBlockingTest {
        val viewModel = CompleteMagicLinkViewModel(app, simpleHttp, TestCoroutineDispatcher())
        assertEquals(null, viewModel.magicLinkUiState.value)
    }

    @Test
    fun firingPostRequestShouldLeadToSuccess() = runBlockingTest {
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(response.body()).thenReturn(responseBody)
        Mockito.`when`(responseBody.string()).thenReturn("{\"sync_token\": \"test_token\", \"username\": \"test_username\"}")

        val viewModel = CompleteMagicLinkViewModel(app, simpleHttp, TestCoroutineDispatcher())
        val states = mutableListOf<MagicLinkUiState>()
        viewModel.magicLinkUiState.observeForever {
            states.add(it)
        }
        viewModel.completeLogin(AUTH_KEY_TEST, AUTH_CODE_TEST)

        assertEquals(MagicLinkUiState.Loading::class.java, states[0]::class.java)
        assertEquals(MagicLinkUiState.Success, states[1])
    }

    @Test
    fun firingPostRequestShouldLeadToError() = runBlockingTest {
        Mockito.`when`(response.isSuccessful).thenReturn(false)

        val viewModel = CompleteMagicLinkViewModel(app, simpleHttp, TestCoroutineDispatcher())
        val states = mutableListOf<MagicLinkUiState>()
        viewModel.magicLinkUiState.observeForever {
            states.add(it)
        }
        viewModel.completeLogin(AUTH_KEY_TEST, AUTH_CODE_TEST)
        assertEquals(MagicLinkUiState.Loading::class.java, states[0]::class.java)
        assertEquals(MagicLinkUiState.Error::class.java, states[1]::class.java)
    }
}
