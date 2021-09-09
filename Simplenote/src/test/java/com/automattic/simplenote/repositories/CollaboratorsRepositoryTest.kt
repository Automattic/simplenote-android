package com.automattic.simplenote.repositories

import com.automattic.simplenote.models.Note
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class CollaboratorsRepositoryTest {
    private val notesBucket = mock(Bucket::class.java) as Bucket<Note>

    private lateinit var collaboratorsRepository: CollaboratorsRepository

    private val noteId = "key1"
    private val note = Note(noteId)

    @Before
    fun setup() {
        note.content = "Hello World"
        note.tags = listOf("tag1", "tag2", "test@emil.com", "name@example.co.jp", "name@test", "あいうえお@example.com")

        whenever(notesBucket.get(any())).thenReturn(note)
        collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket)
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
    fun getCollaboratorsShouldReturnJustEmails() {
        val expected = GetCollaboratorsResult.CollaboratorsList(listOf("test@emil.com", "name@example.co.jp"))
        val result = collaboratorsRepository.getCollaborators(noteId)

        assertEquals(expected, result)
    }

    @Test
    fun getCollaboratorsWhenNoteInTrashShouldReturnError() {
        note.isDeleted = true


        whenever(notesBucket.get(any())).thenReturn(note)
        val expected = GetCollaboratorsResult.NoteInTrash
        val result = collaboratorsRepository.getCollaborators(noteId)

        assertEquals(expected, result)
    }

    @Test
    fun getCollaboratorsWhenNoteIsDeletedShouldReturnError() {
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())
        val expected = GetCollaboratorsResult.NoteDeleted
        val result = collaboratorsRepository.getCollaborators(noteId)

        assertEquals(expected, result)
    }
}
