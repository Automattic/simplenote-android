package com.automattic.simplenote.viewmodels

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.usecases.GetTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagsRepository: TagsRepository,
    private val getTagsUseCase: GetTagsUseCase
) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<TagsEvent>()
    val event: LiveData<TagsEvent> = _event

    private var jobTagsFlow: Job? = null

    fun start() {
        viewModelScope.launch {
            val tagItems = getTagsUseCase.allTags()
            _uiState.value = UiState(tagItems)
        }
    }

    fun startListeningTagChanges() {
        jobTagsFlow = viewModelScope.launch {
            tagsRepository.tagsChanged().collect {
                val searchQuery = _uiState.value?.searchQuery
                updateUiState(searchQuery)
            }
        }
    }

    private suspend fun updateUiState(searchQuery: String?, searchUpdate: Boolean = false) {
        val tagItems = if (searchQuery == null) getTagsUseCase.allTags()
            else getTagsUseCase.searchTags(searchQuery)

        _uiState.value = UiState(tagItems, searchUpdate, searchQuery)
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

    fun closeSearch() {
        _uiState.value = _uiState.value?.copy(searchUpdate = true, searchQuery = null)
    }

    fun search(searchQuery: String) {
        viewModelScope.launch {
            updateUiState(searchQuery, true)
        }
    }

    fun stopListeningTagChanges() {
        // When the job for tagsFlow is cancelled, the awaitClose block is called
        // This remove the listeners for the tags bucket
        jobTagsFlow?.cancel()
    }

    fun updateOnResult() {
        viewModelScope.launch {
            val searchQuery = _uiState.value?.searchQuery
            updateUiState(searchQuery)
        }
    }

    fun clickEditTag(tagItem: TagItem) {
        _event.postValue(TagsEvent.EditTagEvent(tagItem))
    }

    fun clickDeleteTag(tagItem: TagItem) {
        if (tagItem.noteCount > 0) {
            _event.postValue(TagsEvent.DeleteTagEvent(tagItem))
        } else {
            deleteTag(tagItem)
        }
    }

    fun longClickDeleteTag(view: View) {
        _event.postValue(TagsEvent.LongDeleteTagEvent(view))
    }

    fun deleteTag(tagItem: TagItem) {
        viewModelScope.launch {
            tagsRepository.deleteTag(tagItem.tag)
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.TAG_MENU_DELETED,
                    AnalyticsTracker.CATEGORY_TAG,
                    "list_trash_button"
            )
        }
    }

    data class UiState(val tagItems: List<TagItem>, val searchUpdate: Boolean = false, val searchQuery: String? = null)
}

sealed class TagsEvent {
    object AddTagEvent : TagsEvent()
    object LongAddTagEvent : TagsEvent()
    object FinishEvent : TagsEvent()
    data class EditTagEvent(val tagItem: TagItem) : TagsEvent()
    data class DeleteTagEvent(val tagItem: TagItem) : TagsEvent()
    data class LongDeleteTagEvent(val view: View) : TagsEvent()
}
