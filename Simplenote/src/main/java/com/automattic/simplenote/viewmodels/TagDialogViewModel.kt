package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.R
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.usecases.ValidateTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TagDialogViewModel @Inject constructor(
    private val tagsRepository: TagsRepository,
    private val validateTagUseCase: ValidateTagUseCase
) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<TagDialogEvent>()
    val event: LiveData<TagDialogEvent> = _event

    fun start(tag: Tag) {
        _uiState.value = UiState(tag.name, tag)
    }

    fun close() {
        _event.postValue(TagDialogEvent.CloseEvent)
    }

    fun updateUiState(tagName: String) {
        // Make sure the UI state exists
        val currentUiState = _uiState.value
        if (currentUiState == null) {
            _event.postValue(TagDialogEvent.ShowErrorEvent)
            return
        }

        when (validateTagUseCase.isTagValid(tagName)) {
            ValidateTagUseCase.TagValidationResult.TagEmpty ->
                _uiState.value = currentUiState.copy(tagName = tagName, errorMsg = R.string.tag_error_empty)
            ValidateTagUseCase.TagValidationResult.TagWithSpaces ->
                _uiState.value = currentUiState.copy(tagName = tagName, errorMsg = R.string.tag_error_spaces)
            ValidateTagUseCase.TagValidationResult.TagTooLong ->
                _uiState.value = currentUiState.copy(tagName = tagName, errorMsg = R.string.tag_error_length)
            ValidateTagUseCase.TagValidationResult.TagIsCollaborator ->
                _uiState.value = currentUiState.copy(tagName = tagName, errorMsg = R.string.tag_error_collaborator)
            ValidateTagUseCase.TagValidationResult.TagExists, // If the tag exists, it will be managed in a later stage
            ValidateTagUseCase.TagValidationResult.TagValid ->
                _uiState.value = currentUiState.copy(tagName = tagName, errorMsg = null)
        }
    }

    fun renameTagIfValid() {
        val currentState = uiState.value

        if (currentState == null) {
            _event.postValue(TagDialogEvent.ShowErrorEvent)
            return
        }

        // If the tag did not changed, do not anything
        if (currentState.tagName == currentState.oldTag.name) {
            _event.postValue(TagDialogEvent.FinishEvent)
            return
        }

        // If there is another tag with the same name
        if (tagsRepository.isTagConflict(currentState.tagName, currentState.oldTag.name)) {
            // get canonical name
            val canonicalTagName = tagsRepository.getCanonicalTagName(currentState.tagName)
            _event.postValue(TagDialogEvent.ConflictEvent(canonicalTagName, currentState.oldTag.name))
            return
        }

        renameTag()
    }

    fun renameTag() {
        val currentState = _uiState.value

        if (currentState == null) {
            _event.postValue(TagDialogEvent.ShowErrorEvent)
            return
        }

        val result = tagsRepository.renameTag(currentState.tagName, currentState.oldTag)
        if (result) {
            _event.postValue(TagDialogEvent.FinishEvent)
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.TAG_EDITOR_ACCESSED,
                    AnalyticsTracker.CATEGORY_TAG,
                    "tag_alert_edit_box"
            )
        } else {
            _event.postValue(TagDialogEvent.ShowErrorEvent)
        }
    }

    data class UiState(val tagName: String, val oldTag: Tag, val errorMsg: Int? = null)
}

sealed class TagDialogEvent {
    object CloseEvent : TagDialogEvent()
    object FinishEvent : TagDialogEvent()
    object ShowErrorEvent : TagDialogEvent()
    data class ConflictEvent(val canonicalTagName: String, val oldTagName: String): TagDialogEvent()
}
