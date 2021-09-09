package com.automattic.simplenote.repositories

interface CollaboratorsRepository {

    /**
     * Check whether the [collaborator] is a valid under the logic defines for Simplenote clients.
     */
    fun isValidCollaborator(collaborator: String): Boolean

    /**
     * Get a list of collaborators for a given [noteId]
     */
    fun getCollaborators(noteId: String): CollaboratorsActionResult

    fun addCollaborator(noteId: String, collaborator: String): CollaboratorsActionResult
}

sealed class CollaboratorsActionResult {
    object NoteInTrash : CollaboratorsActionResult()
    object NoteDeleted : CollaboratorsActionResult()
    data class CollaboratorsList(val collaborators: List<String>): CollaboratorsActionResult()
}