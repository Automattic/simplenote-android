package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.R
import com.automattic.simplenote.repositories.TagsRepository

class AddTagViewModel(private val tagsRepository: TagsRepository) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<Event>()
    val event: LiveData<Event> = _event

    fun updateUiState(tagName: String) {
        val currentIsKeyboardShowing = _uiState.value?.isKeyboardShowing ?: true
        if (tagName.isEmpty()) {
            _uiState.value = UiState(tagName, currentIsKeyboardShowing, R.string.tag_error_empty)
            return
        }

        if (tagName.contains(" ")) {
            _uiState.value = UiState(tagName, currentIsKeyboardShowing, R.string.tag_error_spaces)
            return
        }

        if (!tagsRepository.isTagValid(tagName)) {
            _uiState.value = UiState(tagName, currentIsKeyboardShowing, R.string.tag_error_length)
            return
        }

        if (!tagsRepository.isTagMissing(tagName)) {
            _uiState.value = UiState(tagName, currentIsKeyboardShowing, R.string.tag_error_exists)
            return
        }

        _uiState.value = UiState(tagName, currentIsKeyboardShowing)
    }

    fun saveTag() {
        val tagName = _uiState.value?.tagName ?: ""
        // Keyboard should be closed
        _uiState.value = UiState(tagName, false)

        val result = tagsRepository.saveTag(tagName)
        _event.postValue(if (result) Event.FINISH else Event.SHOW_ERROR)
    }

    fun start() {
        // Show keyboard at startup
        _uiState.value = UiState("", true)
        _event.postValue(Event.START)
    }

    fun close() {
        _event.postValue(Event.CLOSE)
    }

    data class UiState(val tagName: String, val isKeyboardShowing: Boolean, val errorMsg: Int? = null)

    enum class Event {
        START,
        CLOSE,
        FINISH,
        SHOW_ERROR
    }
}
