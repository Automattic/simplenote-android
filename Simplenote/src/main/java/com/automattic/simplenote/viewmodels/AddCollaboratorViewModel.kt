package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.automattic.simplenote.repositories.CollaboratorsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddCollaboratorViewModel @Inject constructor(
    private val collaboratorsRepository: CollaboratorsRepository
) : ViewModel() {
    private val _event = SingleLiveEvent<Event>()
    val event: LiveData<Event> = _event

    fun addCollaborator(noteId: String, collaborator: String) {

    }

    sealed class Event {
        object InvalidCollaborator : Event()
        object CollaboratorAdded : Event()
        object NoteInTrash : Event()
        object NoteDeleted : Event()
    }
}