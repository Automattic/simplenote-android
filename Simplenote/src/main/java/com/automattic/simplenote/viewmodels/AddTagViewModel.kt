package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.R
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.usecases.ValidateTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddTagViewModel @Inject constructor(
    private val tagsRepository: TagsRepository,
    private val validateTagUseCase: ValidateTagUseCase
) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<Event>()
    val event: LiveData<Event> = _event

    fun updateUiState(tagName: String) =  when (validateTagUseCase.isTagValid(tagName)) {
        ValidateTagUseCase.TagValidationResult.TagEmpty ->
            _uiState.value = UiState(tagName, R.string.tag_error_empty)
        ValidateTagUseCase.TagValidationResult.TagWithSpaces ->
            _uiState.value = UiState(tagName, R.string.tag_error_spaces)
        ValidateTagUseCase.TagValidationResult.TagTooLong ->
            _uiState.value = UiState(tagName, R.string.tag_error_length)
        ValidateTagUseCase.TagValidationResult.TagExists ->
            _uiState.value = UiState(tagName, R.string.tag_error_exists)
        ValidateTagUseCase.TagValidationResult.TagIsCollaborator ->
            _uiState.value = UiState(tagName, R.string.tag_error_collaborator)
        ValidateTagUseCase.TagValidationResult.TagValid ->
            _uiState.value = UiState(tagName)
    }

    fun saveTag() {
        val tagName = _uiState.value?.tagName ?: ""
        // Keyboard should be closed
        _uiState.value = UiState(tagName)

        val result = tagsRepository.saveTag(tagName)
        _event.postValue(if (result) Event.FINISH else Event.SHOW_ERROR)
    }

    fun start() {
        // Show keyboard at startup
        _uiState.value = UiState("")
        _event.postValue(Event.START)
    }

    fun close() {
        _event.postValue(Event.CLOSE)
    }

    data class UiState(val tagName: String, val errorMsg: Int? = null)

    enum class Event {
        START,
        CLOSE,
        FINISH,
        SHOW_ERROR
    }
}
