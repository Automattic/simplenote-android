package com.automattic.simplenote.viewmodels

import android.util.Log
import androidx.annotation.StringRes
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.domerrors.AbortError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.domerrors.TimeoutError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.R
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.repositories.PasskeyRepository
import com.automattic.simplenote.repositories.PasskeyResponseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "PasskeyViewModel"

@HiltViewModel
class PasskeyViewModel @Inject constructor(
    private val passkeyRepository: PasskeyRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private var autoFillRequested: Boolean = false

    private val _passkeyUiState = MutableLiveData<PasskeyUiState>(PasskeyUiState.Waiting)
    val passkeyUiState: LiveData<PasskeyUiState> get() = _passkeyUiState

    // ADD PASSKEYS
    fun requestAuthChallenge(email: String, password: String) = viewModelScope.launch(ioDispatcher) {
        _passkeyUiState.postValue(PasskeyUiState.PasskeyLoading(msg = R.string.add_passkey_loading_request_challenge_label))
        when (val result = passkeyRepository.requestChallenge(email, password)) {
            is PasskeyResponseResult.PasskeyChallengeRequestSuccess -> {
                _passkeyUiState.postValue(PasskeyUiState.PasskeyChallengeRequestSuccess(json = result.json))
            }
            is PasskeyResponseResult.PasskeyError -> {
                if (result.code == 401) {
                    _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.add_passkey_request_challenge_error_label))
                } else {
                    _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.add_passkey_error_label))
                }
            }
            else -> {
                _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.add_passkey_error_label))
            }
        }
    }

    fun handleCreateCredentialResponse(username: String, result: CreateCredentialResponse) = viewModelScope.launch(ioDispatcher) {
        Log.d(TAG, result.type)
        when (result.type) {
            PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> {
                val responseJson = (result as CreatePublicKeyCredentialResponse).registrationResponseJson
                if (responseJson.isNotBlank()) {
                    _passkeyUiState.postValue(PasskeyUiState.PasskeyLoading(msg = R.string.add_passkey_loading_label))
                    when (passkeyRepository.addCredential(username, responseJson)) {
                        is PasskeyResponseResult.PasskeyAddSuccess -> {
                            _passkeyUiState.postValue(PasskeyUiState.PasskeyAddedSuccessfully)
                        }
                        is PasskeyResponseResult.PasskeyError -> {
                            _passkeyUiState.postValue(PasskeyUiState.PasskeyError(message = R.string.add_passkey_error_label))
                        }
                        else -> {
                            _passkeyUiState.postValue(PasskeyUiState.PasskeyError(message = R.string.add_passkey_error_label))
                        }
                    }
                }
            }
            else -> {
                // This case shouldn't be possible since we only support passkeys with CredentialManager
                Log.d(TAG, "We ran into the else...")
                _passkeyUiState.postValue(PasskeyUiState.PasskeyError(message = R.string.add_passkey_error_label))
            }
        }
    }

    fun handleCreateCredentialException(exception: CreateCredentialException) {
        Log.e(TAG, "handleCreateCredentialException", exception)
        when (exception) {
            is CreatePublicKeyCredentialDomException -> {
                Log.d(TAG, "CreatePublicKeyCredentialDomException (${exception.domError.type})")
                when (exception.domError) {
                    is NotAllowedError, is AbortError -> {
                        _passkeyUiState.postValue(PasskeyUiState.PasskeyUserCancelledProcess)
                    }
                    is TimeoutError -> {
                        _passkeyUiState.postValue(PasskeyUiState.PasskeyError(message = R.string.add_passkey_error_timeout_label))
                    }
                    else -> {
                        _passkeyUiState.postValue(PasskeyUiState.PasskeyError(message = R.string.add_passkey_error_label))
                    }
                }
            }
            is CreateCredentialCancellationException -> {
                _passkeyUiState.postValue(PasskeyUiState.PasskeyUserCancelledProcess)
            }
            is CreateCredentialUnknownException, is CreateCredentialCustomException -> {
                _passkeyUiState.postValue(PasskeyUiState.PasskeyError(message = R.string.add_passkey_error_label))
            }
        }
    }

    fun handleGetCredentialException(exception: GetCredentialException) {
        Log.e(TAG, "handleGetCredentialException", exception)
        _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.passkey_login_error_label))
    }

    // LOGIN_WITH PASSKEY
    fun prepareAuthChallenge(email: String) = viewModelScope.launch(ioDispatcher) {
        _passkeyUiState.postValue(PasskeyUiState.PasskeyLoading(msg = R.string.add_passkey_loading_request_challenge_label))
        when (val result = passkeyRepository.prepareAuthChallenge(email)) {
            is PasskeyResponseResult.PasskeyPrepareResult -> {
                _passkeyUiState.postValue(PasskeyUiState.PasskeyPrepareAuthChallengeRequest(challengeJson = result.json))
            }
            is PasskeyResponseResult.PasskeyError -> {
                _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.passkey_login_error_label))
            }
            else -> {
                _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.passkey_login_error_label))
            }
        }
    }

    fun verifyAuthChallenge(username: String, response: GetCredentialResponse) = viewModelScope.launch(ioDispatcher) {
        val credential = response.credential
        if (credential is PublicKeyCredential) {
            _passkeyUiState.postValue(PasskeyUiState.PasskeyLoading(msg = R.string.passkey_login_verification))
            when (val result = passkeyRepository.verifyAuthChallenge(username, credential.authenticationResponseJson)) {
                is PasskeyResponseResult.PasskeyVerifyResult -> {
                    if (result.token.isNotBlank()) {
                        Log.d(TAG, "Successfully verified passkey challenge. Token: ${result.token}")
                    }
                    _passkeyUiState.postValue(PasskeyUiState.PasskeyVerifyAuthChallengeRequest(result.username, result.token))
                }

                is PasskeyResponseResult.PasskeyError -> {
                    Log.e(TAG, "PasskeyError - ${result.code}")
                    _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.passkey_login_error_label))
                }
                else -> {
                    Log.e(TAG, "Ran into PasskeyRepository Response we have no implemented")
                    _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.passkey_login_error_label))
                }
            }
        } else {
            Log.e(
                TAG,
                "GetCredentialResponse was not PublicKeyCredential. Shouldn't be possible. But if you see this, let Firebird know"
            )
            _passkeyUiState.postValue(PasskeyUiState.PasskeyError(R.string.passkey_login_error_label))
        }
    }

    /**
     * Used to reset the state back to Waiting. Required to avoid refiring certain events on orientation change.
     */
    fun resetState() {
        _passkeyUiState.postValue(PasskeyUiState.Waiting)
    }

    fun attemptAutofill() {
        // This is special functionality we only ever want to do ONE time on Sign In Load.
        if (!autoFillRequested) {
            autoFillRequested = true
            viewModelScope.launch(ioDispatcher) {
                when (val result = passkeyRepository.prepareDiscoverableAuthChallenge()) {
                    is PasskeyResponseResult.PasskeyPrepareResult -> {
                        _passkeyUiState.postValue(PasskeyUiState.PasskeyPrepareAuthChallengeRequest(challengeJson = result.json))
                    }
                    is PasskeyResponseResult.PasskeyError -> {
                        Log.e(TAG, "Something went wrong attempting to autofill passkey (${result.code}). Carry on.")
                    }
                    else -> {
                        Log.e(TAG, "Something went wrong attempting to autofill passkey. Carry on.")
                    }
                }
            }
        }
    }
}

sealed class PasskeyUiState {
    data object Waiting: PasskeyUiState()
    data class PasskeyLoading(@StringRes val msg: Int) : PasskeyUiState()
    data object PasskeyUserCancelledProcess : PasskeyUiState()
    data class PasskeyError(@StringRes val message: Int): PasskeyUiState()
    // Create Passkey
    data class PasskeyChallengeRequestSuccess(val json: String): PasskeyUiState()
    data object PasskeyAddedSuccessfully : PasskeyUiState()
    // Login With Passkey
    data class PasskeyPrepareAuthChallengeRequest(val challengeJson: String): PasskeyUiState()
    data class PasskeyVerifyAuthChallengeRequest(val username: String, val token: String): PasskeyUiState()
}
