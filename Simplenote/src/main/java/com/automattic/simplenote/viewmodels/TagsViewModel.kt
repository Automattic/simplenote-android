package com.automattic.simplenote.viewmodels

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.TagsRepository
import kotlinx.coroutines.launch

class TagsViewModel(private val tagsRepository: TagsRepository) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<TagsEvent>()
    val event: LiveData<TagsEvent> = _event

    fun start() {
        viewModelScope.launch {
            val tagItems = tagsRepository.allTags()
            _uiState.value = UiState(tagItems)
        }
    }

    fun clickAddTag() {
        _event.postValue(TagsEvent.AddTagEvent)
    }

    fun longClickAddTag() {
        _event.postValue(TagsEvent.LongAddTagEvent)
    }

    fun close() {
        _event.postValue(TagsEvent.FinishEvent)
    }

    fun clickEditTag(tagItem: TagItem) {
        _event.postValue(TagsEvent.EditTagEvent(tagItem))
    }

    fun clickDeleteTag(tagItem: TagItem) {
        _event.postValue(TagsEvent.DeleteTagEvent(tagItem))
    }

    fun longClickDeleteTag(view: View) {
        _event.postValue(TagsEvent.LongDeleteTagEvent(view))
    }

    fun deleteTag(TagItem: TagItem) {

    }

    data class UiState(val tagItems: List<TagItem>, val searchQuery: String? = null)
}

sealed class TagsEvent {
    object AddTagEvent : TagsEvent()
    object LongAddTagEvent : TagsEvent()
    object ShowSearch : TagsEvent()
    object CloseSearch : TagsEvent()
    object ResultEvent : TagsEvent()
    object FinishEvent: TagsEvent()
    data class EditTagEvent(val tagItem: TagItem) : TagsEvent()
    data class DeleteTagEvent(val tagItem: TagItem) : TagsEvent()
    data class LongDeleteTagEvent(val view: View) : TagsEvent()
}
