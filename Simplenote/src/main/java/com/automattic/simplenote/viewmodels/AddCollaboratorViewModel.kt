package com.automattic.simplenote.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.authentication.SessionManager
import com.automattic.simplenote.authentication.UserSession
import com.automattic.simplenote.repositories.CollaboratorsActionResult
import com.automattic.simplenote.repositories.CollaboratorsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCollaboratorViewModel @Inject constructor(
    private val collaboratorsRepository: CollaboratorsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _event = SingleLiveEvent<Event>()
    val event: LiveData<Event> = _event

    fun addCollaborator(noteId: String, collaborator: String) {
        when (isCurrentUser(collaborator)) {
            true -> _event.value = Event.CollaboratorCurrentUser
            false -> viewModelScope.launch {
                when (collaboratorsRepository.isValidCollaborator(collaborator)) {
                    true -> when (collaboratorsRepository.addCollaborator(noteId, collaborator)) {
                        is CollaboratorsActionResult.CollaboratorsList -> {
                            _event.value = Event.CollaboratorAdded

                            AnalyticsTracker.track(
                                AnalyticsTracker.Stat.COLLABORATOR_ADDED,
                                AnalyticsTracker.CATEGORY_NOTE,
                                "collaborator_added_to_note",
                                mapOf("source" to "collaborators")
                            )
                        }
                        CollaboratorsActionResult.NoteDeleted -> _event.value = Event.NoteDeleted
                        CollaboratorsActionResult.NoteInTrash -> _event.value = Event.NoteInTrash
                    }
                    false -> _event.value = Event.InvalidCollaborator
                }
            }
        }
    }

    private fun isCurrentUser(collaborator: String): Boolean {
        return when (val currentUser = sessionManager.getCurrentUser()) {
            is UserSession.AuthorizedUser -> currentUser.user.email == collaborator
            is UserSession.UnauthorizedUser -> false // This should not happen.
        }
    }

    fun close() {
        _event.value = Event.Close
    }

    sealed class Event {
        object InvalidCollaborator : Event()
        object CollaboratorCurrentUser : Event()
        object CollaboratorAdded : Event()
        object Close : Event()
        object NoteInTrash : Event()
        object NoteDeleted : Event()
    }
}
