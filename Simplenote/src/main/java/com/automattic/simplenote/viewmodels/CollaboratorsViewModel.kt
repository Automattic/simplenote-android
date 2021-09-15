package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.repositories.CollaboratorsActionResult
import com.automattic.simplenote.repositories.CollaboratorsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
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

    private lateinit var _noteId: String

    private var jobTagsFlow: Job? = null

    fun loadCollaborators(noteId: String) {
        _noteId = noteId
        viewModelScope.launch {
            updateUiState(noteId)
        }
    }

    private suspend fun updateUiState(noteId: String) {
        when (val result = collaboratorsRepository.getCollaborators(noteId)) {
            is CollaboratorsActionResult.CollaboratorsList ->
                _uiState.value = if (result.collaborators.isEmpty()) UiState.EmptyCollaborators else
                    UiState.CollaboratorsList(result.collaborators)
            CollaboratorsActionResult.NoteDeleted -> _uiState.value = UiState.NoteDeleted
            CollaboratorsActionResult.NoteInTrash -> _uiState.value = UiState.NoteInTrash
        }
    }

    fun startListeningChanges() {
        jobTagsFlow = viewModelScope.launch {
            collaboratorsRepository.collaboratorsChanged(_noteId).collect {
                updateUiState(_noteId)
            }
        }
    }

    fun stopListeningChanges() {
        jobTagsFlow?.cancel()
    }

    fun clickAddCollaborator() {
        _event.value = Event.AddCollaboratorEvent(_noteId)
    }

    fun clickRemoveCollaborator(collaborator: String) {
        _event.value = Event.RemoveCollaboratorEvent(collaborator)
    }

    fun close() {
        _event.value = Event.CloseCollaboratorsEvent
    }

    fun removeCollaborator(collaborator: String) {
        viewModelScope.launch {
            when (collaboratorsRepository.removeCollaborator(_noteId, collaborator)) {
                CollaboratorsActionResult.NoteDeleted -> _uiState.value = UiState.NoteDeleted
                CollaboratorsActionResult.NoteInTrash -> _uiState.value = UiState.NoteInTrash
                is CollaboratorsActionResult.CollaboratorsList -> Unit // do not do anything
            }
        }
    }

    sealed class UiState {
        object NoteInTrash : UiState()
        object NoteDeleted : UiState()
        object EmptyCollaborators: UiState()
        data class CollaboratorsList(val collaborators: List<String>) : UiState()
    }

    sealed class Event {
        data class AddCollaboratorEvent(val noteId: String) : Event()
        object CloseCollaboratorsEvent : Event()
        data class RemoveCollaboratorEvent(val collaborator: String) : Event()
    }
}
