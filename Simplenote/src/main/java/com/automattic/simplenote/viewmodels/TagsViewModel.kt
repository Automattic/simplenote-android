package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.TagsRepository

class TagsViewModel(private val tagsRepository: TagsRepository) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<TagsEvent>()
    val event: LiveData<TagsEvent> = _event

    fun clickAddTag() {
        _event.postValue(TagsEvent.AddTagEvent)
    }

    fun longClickAddTag() {
        _event.postValue(TagsEvent.LongAddTagEvent)
    }

    fun close() {
        _event.postValue(TagsEvent.FinishEvent)
    }

    data class UiState(private val tags: List<TagItem>, private val searchQuery: String)
}

sealed class TagsEvent {
    object AddTagEvent : TagsEvent()
    object LongAddTagEvent : TagsEvent()
    object ShowSearch : TagsEvent()
    object CloseSearch : TagsEvent()
    object ResultEvent : TagsEvent()
    object FinishEvent: TagsEvent()
    data class EditTagEvent(val tag: Tag) : TagsEvent()
}
