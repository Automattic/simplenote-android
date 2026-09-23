package com.automattic.simplenote.repositories

import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.utils.Either
import com.automattic.simplenote.utils.StrUtils.isEmail
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@ExperimentalCoroutinesApi
class SimperiumCollaboratorsRepository @Inject constructor(
    private val notesBucket: Bucket<Note>,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : CollaboratorsRepository {

    /**
     * A valid [collaborator] is just a valid email address.
     *
     * For now we do not make an extra check to see whether there is an account linked to the email or not
     */
    override fun isValidCollaborator(collaborator: String): Boolean {
        return isEmail(collaborator)
    }

    /**
     * Get list of collaborators (email addresses as tags) for given [noteId] which is not deleted, not trashed, and
     * contains given (optional) [query].
     *
     * @param noteId [String] Simperium key of note to retrieve collaborators from
     * @param query  [String] (optional) to filter list of collaborators with
     *
     * @return [List]<[String]> of collaborators for [noteId] containing [query]
     */
    override suspend fun getCollaborators(
        noteId: String,
        query: String?
    ) = when (val result = getNote(noteId)) {
        is Either.Left -> result.l
        is Either.Right -> CollaboratorsActionResult.CollaboratorsList(filterCollaborators(result.r, query))
    }

    override suspend fun addCollaborator(
        noteId: String,
        collaborator: String
    ) = when (val result = getNote(noteId)) {
        is Either.Left -> result.l
        is Either.Right -> {
            val note = result.r
            note.addTag(collaborator)
            CollaboratorsActionResult.CollaboratorsList(filterCollaborators(note))
        }
    }

    override suspend fun removeCollaborator(
        noteId: String,
        collaborator: String
    ) = when (val result = getNote(noteId)) {
        is Either.Left -> result.l
        is Either.Right -> {
            val note = result.r
            note.removeTag(collaborator)
            CollaboratorsActionResult.CollaboratorsList(filterCollaborators(note))
        }
    }

    override suspend fun collaboratorsChanged(noteId: String): Flow<Boolean> = callbackFlow {
        val callbackOnSaveObject = Bucket.OnSaveObjectListener<Note> { _, note ->
            if (note.simperiumKey == noteId) {
                trySend(true).isSuccess
            }
        }
        val callbackOnDeleteObject = Bucket.OnDeleteObjectListener<Note> { _, note ->
            if (note.simperiumKey == noteId) {
                trySend(true).isSuccess
            }
        }
        val callbackOnNetworkChange = Bucket.OnNetworkChangeListener<Note> { _, _, updatedNoteId ->
            if (updatedNoteId != null && noteId == updatedNoteId) {
                trySend(true).isSuccess
            }
        }

        notesBucket.addOnSaveObjectListener(callbackOnSaveObject)
        notesBucket.addOnDeleteObjectListener(callbackOnDeleteObject)
        notesBucket.addOnNetworkChangeListener(callbackOnNetworkChange)

        awaitClose {
            notesBucket.removeOnSaveObjectListener(callbackOnSaveObject)
            notesBucket.removeOnDeleteObjectListener(callbackOnDeleteObject)
            notesBucket.removeOnNetworkChangeListener(callbackOnNetworkChange)
        }
    }.flowOn(ioDispatcher)

    private suspend fun getNote(noteId: String) = withContext(ioDispatcher) {
        return@withContext try {
            val note = notesBucket.get(noteId)
            when (note.isDeleted) {
                true -> Either.Left(CollaboratorsActionResult.NoteInTrash)
                false -> Either.Right(note)
            }
        } catch (e: BucketObjectMissingException) {
            Either.Left(CollaboratorsActionResult.NoteDeleted)
        }
    }

    private fun filterCollaborators(note: Note) = note.tags.filter { tag ->
        isValidCollaborator(tag)
    }

    private fun filterCollaborators(note: Note, query: String?) = note.tags.filter { tag ->
        isValidCollaborator(tag) && tag.lowercase(Locale.ROOT).contains(query?.lowercase(Locale.ROOT) ?: "")
    }
}
