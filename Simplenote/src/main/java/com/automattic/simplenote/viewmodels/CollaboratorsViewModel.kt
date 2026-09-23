package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.repositories.CollaboratorsActionResult
import com.automattic.simplenote.repositories.CollaboratorsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollaboratorsViewModel @Inject constructor(
    private val collaboratorsRepository: CollaboratorsRepository
) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<Event>()
    val event: LiveData<Event> = _event

    private lateinit var noteId: String

    private var jobCollaborators: Job? = null

    fun loadCollaborators(noteId: String) {
        this.noteId = noteId
        viewModelScope.launch {
            updateUiState(noteId)
        }
    }

    private suspend fun updateUiState(noteId: String, searchUpdate: Boolean = false, searchQuery: String? = null) {
        when (val result = collaboratorsRepository.getCollaborators(noteId, searchQuery)) {
            is CollaboratorsActionResult.CollaboratorsList ->
                _uiState.value = when (result.collaborators.isEmpty()) {
                    true -> UiState.EmptyCollaborators(allCollaboratorsRemoved = searchQuery.isNullOrEmpty(), searchUpdate)
                    false -> UiState.CollaboratorsList(result.collaborators, searchUpdate, searchQuery)
                }
            is CollaboratorsActionResult.NoteDeleted -> _uiState.value = UiState.NoteDeleted
            is CollaboratorsActionResult.NoteInTrash -> _uiState.value = UiState.NoteInTrash
        }
    }

    fun startListeningChanges() {
        jobCollaborators = viewModelScope.launch {
            collaboratorsRepository.collaboratorsChanged(noteId).collect {
                updateUiState(noteId)
            }
        }
    }

    fun stopListeningChanges() {
        jobCollaborators?.cancel()
    }

    fun clickAddCollaborator() {
        _event.value = Event.AddCollaboratorEvent(noteId)
    }

    fun longClickAddCollaborator() {
        _event.postValue(Event.LongAddCollaboratorEvent)
    }

    fun clickRemoveCollaborator(collaborator: String) {
        _event.value = Event.RemoveCollaboratorEvent(collaborator)
    }

    fun longClickRemoveCollaborator() {
        _event.postValue(Event.LongRemoveCollaboratorEvent)
    }

    fun close() {
        _event.value = Event.CloseCollaboratorsEvent
    }

    fun closeSearch() {
        viewModelScope.launch {
            updateUiState(noteId, searchUpdate = false)
        }
    }

    fun search(searchQuery: String) {
        viewModelScope.launch {
            updateUiState(noteId, searchUpdate = true, searchQuery)
        }
    }

    fun removeCollaborator(collaborator: String) {
        viewModelScope.launch {
            when (val result = collaboratorsRepository.removeCollaborator(noteId, collaborator)) {
                is CollaboratorsActionResult.CollaboratorsList -> {
                    _uiState.value = when (result.collaborators.isEmpty()) {
                        true -> UiState.EmptyCollaborators(allCollaboratorsRemoved = true)
                        false -> UiState.CollaboratorsList(result.collaborators)
                    }

                    AnalyticsTracker.track(
                        AnalyticsTracker.Stat.COLLABORATOR_REMOVED,
                        AnalyticsTracker.CATEGORY_NOTE,
                        "collaborator_removed_from_note",
                        mapOf("source" to "collaborators")
                    )
                }
                is CollaboratorsActionResult.NoteDeleted -> _uiState.value = UiState.NoteDeleted
                is CollaboratorsActionResult.NoteInTrash -> _uiState.value = UiState.NoteInTrash
            }
        }
    }

    sealed class UiState {
        object NoteInTrash : UiState()
        object NoteDeleted : UiState()
        data class EmptyCollaborators(val allCollaboratorsRemoved: Boolean, val searchUpdate: Boolean = false) : UiState()
        data class CollaboratorsList(val collaborators: List<String>, val searchUpdate: Boolean = false, val searchQuery: String? = null) : UiState()
    }

    sealed class Event {
        data class AddCollaboratorEvent(val noteId: String) : Event()
        object CloseCollaboratorsEvent : Event()
        object LongAddCollaboratorEvent : Event()
        object LongRemoveCollaboratorEvent : Event()
        data class RemoveCollaboratorEvent(val collaborator: String) : Event()
    }
}
