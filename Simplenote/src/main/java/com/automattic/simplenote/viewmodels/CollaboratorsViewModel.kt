package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.repositories.CollaboratorsActionResult
import com.automattic.simplenote.repositories.CollaboratorsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class CollaboratorsViewModel @Inject constructor(
    private val collaboratorsRepository: CollaboratorsRepository
) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<Event>()
    val event: LiveData<Event> = _event

    fun loadCollaborators(noteId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = collaboratorsRepository.getCollaborators(noteId)) {
                is CollaboratorsActionResult.CollaboratorsList ->
                    _uiState.value = if (result.collaborators.isEmpty()) UiState.EmptyCollaborators else
                        UiState.CollaboratorsList(noteId, result.collaborators)
                CollaboratorsActionResult.NoteDeleted -> _uiState.value = UiState.NoteDeleted
                CollaboratorsActionResult.NoteInTrash -> _uiState.value = UiState.NoteInTrash
            }
        }
    }

    fun clickAddCollaborator() {

    }

    fun clickRemoveCollaborator(collaborator: String) {

    }

    fun removeCollaborator(collaborator: String) {
        // Validate constraint that the UiState should have a list of collaborators
        if (uiState.value !is UiState.CollaboratorsList) {
            return
        }

        val noteId = (uiState.value as UiState.CollaboratorsList).noteId
        viewModelScope.launch {
            when (val result = collaboratorsRepository.removeCollaborator(noteId, collaborator)) {
                is CollaboratorsActionResult.CollaboratorsList ->
                    _uiState.value = if (result.collaborators.isEmpty()) UiState.EmptyCollaborators else
                        UiState.CollaboratorsList(noteId, result.collaborators)
                CollaboratorsActionResult.NoteDeleted -> _uiState.value = UiState.NoteDeleted
                CollaboratorsActionResult.NoteInTrash -> _uiState.value = UiState.NoteInTrash
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object NoteInTrash : UiState()
        object NoteDeleted : UiState()
        object EmptyCollaborators: UiState()
        data class CollaboratorsList(val noteId: String, val collaborators: List<String>) : UiState()
    }

    sealed class Event {
        object AddCollaboratorEvent : Event()
        data class RemoveCollaboratorEvent(val collaborator: String) : Event()
    }
}
