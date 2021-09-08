package com.automattic.simplenote.repositories

interface CollaboratorsRepository {
    /**
     * Check whether the [collaborator] is a valid under the logic defines for Simplenote clients.
     */
    fun isValidCollaborator(collaborator: String): Boolean
}
