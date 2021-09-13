package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.CollaboratorsRepository
import javax.inject.Inject

class CollaboratorsViewModel @Inject constructor(
    private val collaboratorsRepository: CollaboratorsRepository
) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _event = SingleLiveEvent<CollaboratorsEvent>()
    val event: LiveData<CollaboratorsEvent> = _event

    fun loadCollaborators(noteId: String) {

    }

    fun removeCollaborator(collaborator: String) {

    }

    data class UiState(val noteId: String, val collaborators: List<String>)

    sealed class CollaboratorsEvent {
        object NoteInTrash : CollaboratorsEvent()
        object NoteDeleted : CollaboratorsEvent()
        data class RemoveCollaborator(val collaborator: String) : CollaboratorsEvent()
    }
}
