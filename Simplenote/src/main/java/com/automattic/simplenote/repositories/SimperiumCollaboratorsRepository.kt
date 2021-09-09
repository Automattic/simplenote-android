package com.automattic.simplenote.repositories

import android.text.TextUtils
import androidx.core.util.PatternsCompat.EMAIL_ADDRESS
import com.automattic.simplenote.models.Note
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import javax.inject.Inject

class SimperiumCollaboratorsRepository @Inject constructor(
    private val notesBucket: Bucket<Note>
) : CollaboratorsRepository {

    /**
     * A valid [collaborator] is just a valid email address.
     *
     * For now we do not make an extra check to see whether there is an account linked to the email or not
     */
    override fun isValidCollaborator(collaborator: String): Boolean {
        return !TextUtils.isEmpty(collaborator) && EMAIL_ADDRESS.matcher(collaborator).matches()
    }

    /**
     * Return a list of collaborators (email addresses as tags) if the note for the given simperiumKey ([noteId]) is
     * not in the trash and has not been deleted
     */
    override fun getCollaborators(noteId: String): GetCollaboratorsResult {
        return try {
            val note = notesBucket.get(noteId)
            if (note.isDeleted) {
                return GetCollaboratorsResult.NoteInTrash
            }

            return GetCollaboratorsResult.CollaboratorsList(note.tags.filter { tag -> isValidCollaborator(tag) })
        } catch (e: BucketObjectMissingException) {
            return GetCollaboratorsResult.NoteDeleted
        }
    }
}
