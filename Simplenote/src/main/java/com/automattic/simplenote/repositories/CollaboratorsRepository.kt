package com.automattic.simplenote.repositories

interface CollaboratorsRepository {

    /**
     * Check whether the [collaborator] is a valid under the logic defines for Simplenote clients.
     */
    fun isValidCollaborator(collaborator: String): Boolean

    /**
     * Get a list of collaborators for a given [noteId]
     */
    fun getCollaborators(noteId: String): GetCollaboratorsResult
}

sealed class GetCollaboratorsResult {
    object NoteInTrash : GetCollaboratorsResult()
    object NoteDeleted : GetCollaboratorsResult()
    data class CollaboratorsList(val collaborators: List<String>)
}