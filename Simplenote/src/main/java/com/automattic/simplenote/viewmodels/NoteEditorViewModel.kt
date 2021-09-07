package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.analytics.AnalyticsTracker.Stat
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.usecases.GetTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val getTagsUseCase: GetTagsUseCase,
) : ViewModel() {
    private var _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun update(note: Note) {
        val tags = getTagsUseCase.getTags(note)
        _uiState.value = UiState(tags)
    }

    fun addTag(tagName: String, note: Note) {
        note.addTag(tagName)

        update(note)

        AnalyticsTracker.track(
            Stat.EDITOR_TAG_ADDED,
            AnalyticsTracker.CATEGORY_NOTE,
            "tag_added_to_note"
        )
    }

    fun removeTag(tagName: String, note: Note) {
        note.removeTag(tagName)

        update(note)

        AnalyticsTracker.track(
            Stat.EDITOR_TAG_REMOVED,
            AnalyticsTracker.CATEGORY_NOTE,
            "tag_removed_from_note"
        )
    }

    data class UiState(val tags: List<String>)
}