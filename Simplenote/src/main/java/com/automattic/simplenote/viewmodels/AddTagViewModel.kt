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
        if (tagName.isEmpty()) {
            _uiState.value = UiState(tagName, Status.ERROR, R.string.tag_error_empty)
            return
        }

        if (tagName.contains(" ")) {
            _uiState.value = UiState(tagName, Status.ERROR, R.string.tag_error_spaces)
            return
        }

        if (!tagsRepository.isTagValid(tagName)) {
            _uiState.value = UiState(tagName, Status.ERROR, R.string.tag_error_length)
            return
        }

        if (!tagsRepository.isTagMissing(tagName)) {
            _uiState.value = UiState(tagName, Status.ERROR, R.string.tag_error_exists)
            return
        }

        _uiState.value = UiState(tagName, Status.VALID)
    }

    fun saveTag() {
        val tagName = _uiState.value?.tagName ?: ""
        _uiState.value = UiState(tagName, Status.SAVING)

        val result = tagsRepository.saveTag(tagName)
        _event.postValue(if (result) Event.FINISH else Event.SHOW_ERROR)
    }

    fun start() {
        _uiState.value = UiState("", Status.STARTED)
    }

    fun close() {
        _event.postValue(Event.CLOSE)
    }

    data class UiState(val tagName: String, val status: Status, val errorMsg: Int = -1)

    enum class Status {
        STARTED,
        VALID,
        SAVING,
        ERROR,
    }

    enum class Event {
        CLOSE,
        FINISH,
        SHOW_ERROR
    }
}
