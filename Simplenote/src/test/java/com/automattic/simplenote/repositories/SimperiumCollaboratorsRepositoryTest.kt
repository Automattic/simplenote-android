package com.automattic.simplenote.repositories

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Note
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SimperiumCollaboratorsRepositoryTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mockBucket: Bucket<*> = mock(Bucket::class.java)
    private val notesBucket = mock(Bucket::class.java) as Bucket<Note>

    private val collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket, TestCoroutineDispatcher())

    private val noteId = "key1"
    private val note = Note(noteId).apply {
        content = "Hello World"
        tags = listOf("tag1", "tag2", "test@emil.com", "name@example.co.jp", "name@test", "あいうえお@example.com")
        bucket = mockBucket
    }

    @Before
    fun setup() {
        whenever(notesBucket.get(any())).thenReturn(note)
    }

    @Test
    fun validEmailsShouldBeValidCollaborator() {
        assertTrue(collaboratorsRepository.isValidCollaborator("name1@test.com"))
        // Email with + sign
        assertTrue(collaboratorsRepository.isValidCollaborator("name12.lastname+503@test.com"))
        // IP address
        assertTrue(collaboratorsRepository.isValidCollaborator("name12_lastname+503@192.168.43.54"))
        assertTrue(collaboratorsRepository.isValidCollaborator("name12-lastname+503@192.168.43.54"))
        // Email top level domain
        assertTrue(collaboratorsRepository.isValidCollaborator("name@example.co.jp"))

    }

    @Test
    fun invalidEmailsShouldBeInvalidCollaborator() {
        // Email without complete domain
        assertFalse(collaboratorsRepository.isValidCollaborator("name@test"))
        assertFalse(collaboratorsRepository.isValidCollaborator("d"))
        // Valid URL but not email
        assertFalse(collaboratorsRepository.isValidCollaborator("nametest.com"))
        // With subdomain
        assertFalse(collaboratorsRepository.isValidCollaborator("sub.test@test"))
        // With double @
        assertFalse(collaboratorsRepository.isValidCollaborator("test@@test.com"))
        assertFalse(collaboratorsRepository.isValidCollaborator("あいうえお@example.com"))
        assertFalse(collaboratorsRepository.isValidCollaborator("just”not”right@example.com"))
    }

    @Test
    fun getCollaboratorsShouldReturnJustEmails() = runBlockingTest {
        val expected = CollaboratorsActionResult.CollaboratorsList(listOf("test@emil.com", "name@example.co.jp"))
        val result = collaboratorsRepository.getCollaborators(noteId)

        assertEquals(expected, result)
    }

    @Test
    fun getCollaboratorsWhenNoteInTrashShouldReturnError() = runBlockingTest {
        note.isDeleted = true

        val result = collaboratorsRepository.getCollaborators(noteId)

        val expected = CollaboratorsActionResult.NoteInTrash
        assertEquals(expected, result)
    }

    @Test
    fun getCollaboratorsWhenNoteIsDeletedShouldReturnError() = runBlockingTest {
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())

        val result = collaboratorsRepository.getCollaborators(noteId)

        val expected = CollaboratorsActionResult.NoteDeleted
        assertEquals(expected, result)
    }

    @Test
    fun addCollaboratorShouldAddATagToNote() = runBlockingTest {
        val collaborator = "test1@email.com"

        val result = collaboratorsRepository.addCollaborator(noteId, collaborator)

        val newCollaborators = listOf("test@emil.com", "name@example.co.jp", collaborator)
        val expected = CollaboratorsActionResult.CollaboratorsList(newCollaborators)
        assertEquals(expected, result)
    }

    @Test
    fun addCollaboratorWhenNoteInTrashShouldReturnError() = runBlockingTest {
        note.isDeleted = true
        val collaborator = "test1@email.com"

        val result = collaboratorsRepository.addCollaborator(noteId, collaborator)

        val expected = CollaboratorsActionResult.NoteInTrash
        assertEquals(expected, result)
    }

    @Test
    fun addCollaboratorWhenNoteIsDeletedShouldReturnError() = runBlockingTest {
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())
        val collaborator = "test1@email.com"

        val result = collaboratorsRepository.addCollaborator(noteId, collaborator)

        val expected = CollaboratorsActionResult.NoteDeleted
        assertEquals(expected, result)
    }

    @Test
    fun removeCollaboratorShouldAddATagToNote() = runBlockingTest {
        val collaborator = "name@example.co.jp"

        val result = collaboratorsRepository.removeCollaborator(noteId, collaborator)

        val newCollaborators = listOf("test@emil.com")
        val expected = CollaboratorsActionResult.CollaboratorsList(newCollaborators)
        assertEquals(expected, result)
    }

    @Test
    fun removeCollaboratorWhenNoteInTrashShouldReturnError() = runBlockingTest {
        note.isDeleted = true
        val collaborator = "test1@email.com"

        val result = collaboratorsRepository.removeCollaborator(noteId, collaborator)

        val expected = CollaboratorsActionResult.NoteInTrash
        assertEquals(expected, result)
    }

    @Test
    fun removeCollaboratorWhenNoteIsDeletedShouldReturnError() = runBlockingTest {
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())
        val collaborator = "test1@email.com"

        val result = collaboratorsRepository.removeCollaborator(noteId, collaborator)

        val expected = CollaboratorsActionResult.NoteDeleted
        assertEquals(expected, result)
    }
}
