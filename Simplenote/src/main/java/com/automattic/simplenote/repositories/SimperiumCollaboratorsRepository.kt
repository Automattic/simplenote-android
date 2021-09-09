package com.automattic.simplenote.repositories

import android.text.TextUtils
import androidx.core.util.PatternsCompat.EMAIL_ADDRESS
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.utils.Either
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
    override fun getCollaborators(noteId: String): CollaboratorsActionResult {
       return when(val result = getNote(noteId)) {
           is Either.Left -> result.l
           is Either.Right ->
               CollaboratorsActionResult.CollaboratorsList(filterCollaborators(result.r))
       }
    }

    override fun addCollaborator(noteId: String, collaborator: String): CollaboratorsActionResult {
        return when(val result = getNote(noteId)) {
            is Either.Left -> result.l
            is Either.Right -> {
                val note = result.r
                note.addTag(collaborator)
                CollaboratorsActionResult.CollaboratorsList(filterCollaborators(note))
            }
        }
    }

    override fun removeCollaborator(noteId: String, collaborator: String): CollaboratorsActionResult {
        return when(val result = getNote(noteId)) {
            is Either.Left -> result.l
            is Either.Right -> {
                val note = result.r
                note.removeTag(collaborator)
                CollaboratorsActionResult.CollaboratorsList(filterCollaborators(note))
            }
        }
    }

    private fun getNote(noteId: String): Either<CollaboratorsActionResult, Note> {
        try {
            val note = notesBucket.get(noteId)
            if (note.isDeleted) {
                return Either.Left(CollaboratorsActionResult.NoteInTrash)
            }

            return Either.Right(note)
        } catch (e: BucketObjectMissingException) {
            return Either.Left(CollaboratorsActionResult.NoteDeleted)
        }
    }

    private fun filterCollaborators(note: Note) = note.tags.filter { tag -> isValidCollaborator(tag) }
}
