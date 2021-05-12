package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.R
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.TagsRepository

class TagDialogViewModel(private val tagsRepository: TagsRepository) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<TagDialogEvent>()
    val event: LiveData<TagDialogEvent> = _event

    fun start(tag: Tag) {
        _uiState.value = UiState(tag.name, tag)
    }

    fun close() {
        _event.postValue(CloseEvent)
    }

    fun updateUiState(tagName: String) {
        if (tagName.isEmpty()) {
            _uiState.value = _uiState.value!!.copy(tagName = tagName, errorMsg = R.string.tag_error_empty)
            return
        }

        if (tagName.contains(" ")) {
            _uiState.value = _uiState.value!!.copy(tagName = tagName, errorMsg = R.string.tag_error_spaces)
            return
        }

        if (!tagsRepository.isTagValid(tagName)) {
            _uiState.value = _uiState.value!!.copy(tagName = tagName, errorMsg = R.string.tag_error_length)
            return
        }

        _uiState.value = _uiState.value!!.copy(tagName = tagName, errorMsg = null)
    }

    fun renameTagIfValid() {
        val currentState = uiState.value

        if (currentState == null) {
            _event.postValue(ShowErrorEvent)
            return
        }

        // If the tag did not changed, do not anything
        if (currentState.tagName == currentState.oldTag.name) {
            _event.postValue(FinishEvent)
            return
        }

        // If there is another tag with the same name
        if (!tagsRepository.isTagMissing(currentState.tagName)) {
            // get canonical name
            val canonicalTagName = tagsRepository.getCanonicalTagName(currentState.tagName)
            _event.postValue(ConflictEvent(canonicalTagName, currentState.oldTag.name))
            return
        }

        renameTag()
    }

    fun renameTag() {
        val currentState = _uiState.value

        if (currentState == null) {
            _event.postValue(ShowErrorEvent)
            return
        }

        val result = tagsRepository.renameTag(currentState.tagName, currentState.oldTag)
        if (result) {
            _event.postValue(FinishEvent)
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.TAG_EDITOR_ACCESSED,
                    AnalyticsTracker.CATEGORY_TAG,
                    "tag_alert_edit_box"
            )
        } else {
            _event.postValue(ShowErrorEvent)
        }
    }

    data class UiState(val tagName: String, val oldTag: Tag, val errorMsg: Int? = null)
}

sealed class TagDialogEvent
object CloseEvent : TagDialogEvent()
object FinishEvent : TagDialogEvent()
object ShowErrorEvent : TagDialogEvent()
data class ConflictEvent(val canonicalTagName: String, val oldTagName: String): TagDialogEvent()
