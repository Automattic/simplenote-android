package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import com.automattic.simplenote.repositories.PasskeyRepository
import com.automattic.simplenote.repositories.PasskeyResponseResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

import org.junit.Assert.assertEquals

@ExperimentalCoroutinesApi
class PasskeyViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository: PasskeyRepository = Mockito.mock(PasskeyRepository::class.java)

    val testResponse = "{\"Hello\": \"World\"}"

    @Test
    fun defaultViewModelState() = runTest {
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        assertEquals(PasskeyUiState.Waiting, viewModel.passkeyUiState.value)
    }

    @Test
    fun requestAuthChallengeAndSucceed() = runTest {
        Mockito.`when`(repository.requestChallenge("email", "password")).thenReturn(
            PasskeyResponseResult.PasskeyChallengeRequestSuccess(json = "")
        )
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        viewModel.requestAuthChallenge("email", "password")
        assertEquals(3, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyLoading::class.java, state[1]::class.java)
        assertEquals(PasskeyUiState.PasskeyChallengeRequestSuccess::class.java, state[2]::class.java)
    }

    @Test
    fun requestAuthChallengeAndFail() = runTest {
        Mockito.`when`(repository.requestChallenge("email", "password")).thenReturn(
            PasskeyResponseResult.PasskeyError(code = 400)
        )
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        viewModel.requestAuthChallenge("email", "password")
        assertEquals(3, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyLoading::class.java, state[1]::class.java)
        assertEquals(PasskeyUiState.PasskeyError::class.java, state[2]::class.java)
    }

    @Test
    fun extractChallengeFromCreateCredentialResponseAndAddPasskeySuccessfully() = runTest {
        Mockito.`when`(repository.addCredential("email", testResponse)).thenReturn(
            PasskeyResponseResult.PasskeyAddSuccess(code = 200)
        )
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        val credentialResponseMock = Mockito.mock(CreatePublicKeyCredentialResponse::class.java)
        Mockito.`when`(credentialResponseMock.type).thenReturn(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        Mockito.`when`(credentialResponseMock.registrationResponseJson).thenReturn(testResponse)

        viewModel.handleCreateCredentialResponse("email", credentialResponseMock)
        assertEquals(3, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyLoading::class.java, state[1]::class.java)
        assertEquals(PasskeyUiState.PasskeyAddedSuccessfully::class.java, state[2]::class.java)
    }

    @Test
    fun extractChallengeFromCreateCredentialResponseAndAddPasskeyWithFailure() = runTest {
        val testResponse = "{\"Hello\": \"World\""
        Mockito.`when`(repository.addCredential("email", testResponse)).thenReturn(
            PasskeyResponseResult.PasskeyError(code = 401)
        )
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        val credentialResponseMock = Mockito.mock(CreatePublicKeyCredentialResponse::class.java)
        Mockito.`when`(credentialResponseMock.type).thenReturn(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        Mockito.`when`(credentialResponseMock.registrationResponseJson).thenReturn(testResponse)

        viewModel.handleCreateCredentialResponse("email", credentialResponseMock)
        assertEquals(3, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyLoading::class.java, state[1]::class.java)
        assertEquals(PasskeyUiState.PasskeyError::class.java, state[2]::class.java)
    }

    @Test
    fun checkThatViewModelHandlesDomExceptionProperly() {
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        val domErrorMock = Mockito.mock(NotAllowedError::class.java)
        val domExceptionMock = Mockito.mock(CreatePublicKeyCredentialDomException::class.java)
        Mockito.`when`(domErrorMock.type).thenReturn("some_error") // Actual value shouldn't matter.
        Mockito.`when`(domExceptionMock.domError).thenReturn(domErrorMock)
        viewModel.handleCreateCredentialException(domExceptionMock)

        assertEquals(2, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyUserCancelledProcess::class.java, state[1]::class.java)
    }

    @Test
    fun checkThatViewModelHandlesCancellationExceptionProperly() {
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        val exceptionMock = Mockito.mock(CreateCredentialCancellationException::class.java)
        viewModel.handleCreateCredentialException(exceptionMock)

        assertEquals(2, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyUserCancelledProcess::class.java, state[1]::class.java)
    }

    @Test
    fun checkThatViewModelHandlesUnknownExceptionProperly() {
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        val exceptionMock = Mockito.mock(CreateCredentialUnknownException::class.java)
        viewModel.handleCreateCredentialException(exceptionMock)

        assertEquals(2, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyError::class.java, state[1]::class.java)
    }

    @Test
    fun requestAuthChallengeForLoginWithPasskey() = runTest {
        Mockito.`when`(repository.prepareAuthChallenge("email")).thenReturn(
            PasskeyResponseResult.PasskeyPrepareResult(json = testResponse)
        )
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        viewModel.prepareAuthChallenge("email")
        assertEquals(3, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyLoading::class.java, state[1]::class.java)
        assertEquals(PasskeyUiState.PasskeyPrepareAuthChallengeRequest::class.java, state[2]::class.java)
    }

    @Test
    fun requestAuthChallengeForLoginWithPasskeyWithError() = runTest {
        Mockito.`when`(repository.prepareAuthChallenge("email")).thenReturn(
            PasskeyResponseResult.PasskeyError(code = 400)
        )
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }
        viewModel.prepareAuthChallenge("email")
        assertEquals(3, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyLoading::class.java, state[1]::class.java)
        assertEquals(PasskeyUiState.PasskeyError::class.java, state[2]::class.java)
    }

    @Test
    fun verifyAuthChallengeForLoginWithPasskey() = runTest {
        val getCredentialResponse = Mockito.mock(GetCredentialResponse::class.java)
        val credential = Mockito.mock(PublicKeyCredential::class.java)
        Mockito.`when`(credential.authenticationResponseJson).thenReturn(testResponse)
        Mockito.`when`(getCredentialResponse.credential).thenReturn(credential)

        Mockito.`when`(repository.verifyAuthChallenge("email", testResponse)).thenReturn(
            PasskeyResponseResult.PasskeyVerifyResult("testToken", "email")
        )
        val viewModel = PasskeyViewModel(repository, testDispatcher)
        val state = mutableListOf<PasskeyUiState>()
        viewModel.passkeyUiState.observeForever {
            state.add(it)
        }

        viewModel.verifyAuthChallenge("email", getCredentialResponse)
        assertEquals(3, state.size)
        assertEquals(PasskeyUiState.Waiting::class.java, state[0]::class.java)
        assertEquals(PasskeyUiState.PasskeyLoading::class.java, state[1]::class.java)
        assertEquals(PasskeyUiState.PasskeyVerifyAuthChallengeRequest::class.java, state[2]::class.java)
    }
}
