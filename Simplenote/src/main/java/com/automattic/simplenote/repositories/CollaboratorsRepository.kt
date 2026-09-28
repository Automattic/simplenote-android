package com.automattic.simplenote.repositories

import kotlinx.coroutines.flow.Flow

interface CollaboratorsRepository {

    /**
     * Check whether the [collaborator] is a valid under the logic defines for Simplenote clients.
     */
    fun isValidCollaborator(collaborator: String): Boolean

    /**
     * Get list of collaborators for given [noteId] and {optional} [query].
     */
    suspend fun getCollaborators(noteId: String, query: String? = null): CollaboratorsActionResult

    suspend fun addCollaborator(noteId: String, collaborator: String): CollaboratorsActionResult

    suspend fun removeCollaborator(noteId: String, collaborator: String): CollaboratorsActionResult

    /**
     * Return true in case the note has been updated locally or on the network.
     */
    suspend fun collaboratorsChanged(noteId: String): Flow<Boolean>
}

sealed class CollaboratorsActionResult {
    object NoteInTrash : CollaboratorsActionResult()
    object NoteDeleted : CollaboratorsActionResult()
    data class CollaboratorsList(val collaborators: List<String>): CollaboratorsActionResult()
}
