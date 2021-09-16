package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.analytics.AnalyticsTracker.Stat
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.usecases.GetTagsUseCase
import com.automattic.simplenote.usecases.ValidateTagUseCase
import com.automattic.simplenote.usecases.ValidateTagUseCase.TagValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val getTagsUseCase: GetTagsUseCase,
    private val validateTagUseCase: ValidateTagUseCase
) : ViewModel() {
    private var _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private var _event = MutableLiveData<NoteEditorEvent>()
    val event: LiveData<NoteEditorEvent> = _event

    fun update(note: Note) {
        val tags = getTagsUseCase.getTags(note)
        _uiState.value = UiState(tags)
    }

    fun addTag(tagName: String, note: Note) {
        when (val result = validateTagUseCase.isTagValid(tagName)) {
            TagValidationResult.TagEmpty,
            TagValidationResult.TagTooLong,
            TagValidationResult.TagWithSpaces -> _event.value = NoteEditorEvent.InvalidTag
            TagValidationResult.TagIsCollaborator -> {
                addTagName(tagName, note, result)
                _event.value = NoteEditorEvent.TagAsCollaborator(tagName)
            }
            TagValidationResult.TagExists,
            TagValidationResult.TagValid -> addTagName(tagName, note, result)
        }
    }

    private fun addTagName(tagName: String, note: Note, validationResult: TagValidationResult) {
        note.addTag(tagName)

        update(note)


        if (validationResult == TagValidationResult.TagIsCollaborator) {
            AnalyticsTracker.track(
                Stat.COLLABORATOR_ADDED,
                AnalyticsTracker.CATEGORY_NOTE,
                "collaborator_added_to_note",
                mapOf("source" to "editor")
            )
        } else {
            AnalyticsTracker.track(
                Stat.EDITOR_TAG_ADDED,
                AnalyticsTracker.CATEGORY_NOTE,
                "tag_added_to_note"
            )
        }
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

    sealed class NoteEditorEvent {
        object InvalidTag : NoteEditorEvent()
        data class TagAsCollaborator(val collaborator: String) : NoteEditorEvent()
    }

}