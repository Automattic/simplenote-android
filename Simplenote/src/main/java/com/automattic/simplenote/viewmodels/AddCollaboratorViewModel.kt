package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.repositories.CollaboratorsActionResult
import com.automattic.simplenote.repositories.CollaboratorsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCollaboratorViewModel @Inject constructor(
    private val collaboratorsRepository: CollaboratorsRepository
) : ViewModel() {
    private val _event = SingleLiveEvent<Event>()
    val event: LiveData<Event> = _event

    fun addCollaborator(noteId: String, collaborator: String) {
        viewModelScope.launch {
            when (collaboratorsRepository.isValidCollaborator(collaborator)) {
                true -> when (collaboratorsRepository.addCollaborator(noteId, collaborator)) {
                    is CollaboratorsActionResult.CollaboratorsList -> _event.value = Event.CollaboratorAdded
                    CollaboratorsActionResult.NoteDeleted -> _event.value = Event.NoteDeleted
                    CollaboratorsActionResult.NoteInTrash -> _event.value = Event.NoteInTrash
                }
                false -> _event.value = Event.InvalidCollaborator
            }
        }
    }

    sealed class Event {
        object InvalidCollaborator : Event()
        object CollaboratorAdded : Event()
        object NoteInTrash : Event()
        object NoteDeleted : Event()
    }
}