package com.automattic.simplenote.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.R
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.networking.SimpleHttp
import com.automattic.simplenote.repositories.MagicLinkRepository
import com.automattic.simplenote.repositories.MagicLinkResponseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class RequestMagicLinkViewModel @Inject constructor(
    private val magicLinkRepository: MagicLinkRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _magicLinkRequestUiState = MutableLiveData<MagicLinkRequestUiState>()
    val magicLinkRequestUiState: LiveData<MagicLinkRequestUiState> get() = _magicLinkRequestUiState

    fun requestLogin(username: String) = viewModelScope.launch(ioDispatcher) {
        _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Loading(
            messageRes = R.string.magic_link_request_login_loading_message)
        )
        try {
            when (magicLinkRepository.requestLogin(username)) {
                is MagicLinkResponseResult.MagicLinkRequestSuccess -> {
                    _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Success(username = username))
                }
                is MagicLinkResponseResult.MagicLinkError -> {
                    _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Error())
                }
                else -> _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Error())
            }
        } catch (exception: IOException) {
            _magicLinkRequestUiState.postValue(MagicLinkRequestUiState.Error())
        }
    }

}

sealed class MagicLinkRequestUiState {
    data class Loading(@StringRes val messageRes: Int): MagicLinkRequestUiState()
    data class Error(val message: String? = null): MagicLinkRequestUiState()
    data class Success(val username: String) : MagicLinkRequestUiState()
}
