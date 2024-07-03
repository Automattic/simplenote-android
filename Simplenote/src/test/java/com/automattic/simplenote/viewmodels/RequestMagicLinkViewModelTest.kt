package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.repositories.MagicLinkRepository
import com.automattic.simplenote.repositories.MagicLinkResponseResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.mockito.Mockito

import org.junit.Assert.assertEquals
import org.junit.Rule

const val email = "test@email.com"

@ExperimentalCoroutinesApi
class RequestMagicLinkViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val repository: MagicLinkRepository = Mockito.mock(MagicLinkRepository::class.java)

    @Test
    fun instantiateViewModel() = runBlockingTest {
        val viewModel = RequestMagicLinkViewModel(repository, TestCoroutineDispatcher())
        assertEquals(null, viewModel.magicLinkRequestUiState.value)
    }

    @Test
    fun firingPostRequestShouldLeadToSuccess() = runBlockingTest {
        Mockito.`when`(
            repository.requestLogin(email)
        ).thenReturn(MagicLinkResponseResult.MagicLinkRequestSuccess(code = 200))

        val viewModel = RequestMagicLinkViewModel(repository, TestCoroutineDispatcher())
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
        Mockito.`when`(
            repository.requestLogin(email)
        ).thenReturn(MagicLinkResponseResult.MagicLinkError(code = 400))

        val viewModel = RequestMagicLinkViewModel(repository, TestCoroutineDispatcher())
        val states = mutableListOf<MagicLinkRequestUiState>()
        viewModel.magicLinkRequestUiState.observeForever {
            states.add(it)
        }
        viewModel.requestLogin(email)
        assertEquals(MagicLinkRequestUiState.Loading::class.java, states[0]::class.java)
        assertEquals(MagicLinkRequestUiState.Error::class.java, states[1]::class.java)
    }
}
