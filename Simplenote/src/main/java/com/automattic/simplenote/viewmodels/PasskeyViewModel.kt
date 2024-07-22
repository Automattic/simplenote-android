package com.automattic.simplenote.viewmodels

import android.util.Log
import androidx.annotation.StringRes
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
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

const val TAG = "PasskeyViewModel"

@HiltViewModel
class PasskeyViewModel @Inject constructor(
    private val passkeyRepository: PasskeyRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _passkeyUiState = MutableLiveData<PasskeyUiState>()
    val passkeyUiState: LiveData<PasskeyUiState> get() = _passkeyUiState

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
                _passkeyUiState.postValue(PasskeyUiState.PasskeyError(message = R.string.add_passkey_error_label))
            }
        }
    }

    fun handleCreateCredentialException(exception: CreateCredentialException) {
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
}

sealed class PasskeyUiState {
    data class PasskeyLoading(@StringRes val msg: Int) : PasskeyUiState()
    data class PasskeyChallengeRequestSuccess(val json: String): PasskeyUiState()
    data object PasskeyAddedSuccessfully : PasskeyUiState()
    data object PasskeyUserCancelledProcess : PasskeyUiState()
    data class PasskeyError(@StringRes val message: Int): PasskeyUiState()
}
