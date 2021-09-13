package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CollaboratorsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mockBucket: Bucket<*> = Mockito.mock(Bucket::class.java)
    private val notesBucket = Mockito.mock(Bucket::class.java) as Bucket<Note>
    private val collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket, TestCoroutineDispatcher())
    private val viewModel = CollaboratorsViewModel(collaboratorsRepository)

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
    fun loadCollaboratorsShouldReturnListEmails() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        val expectedCollaborators = listOf("test@emil.com", "name@example.co.jp")

        assertEquals(expectedCollaborators, viewModel.uiState.value?.collaborators)
    }

    @Test
    fun loadCollaboratorsForNoteInTrashShouldTriggerEvent() = runBlockingTest {
        note.isDeleted = true

        assertNull(viewModel.uiState.value)
        assertEquals(CollaboratorsViewModel.CollaboratorsEvent.NoteInTrash, viewModel.event.value)
    }

    @Test
    fun loadCollaboratorsForNoteDeletedShouldTriggerEvent() = runBlockingTest {
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())

        assertNull(viewModel.uiState.value)
        assertEquals(CollaboratorsViewModel.CollaboratorsEvent.NoteDeleted, viewModel.event.value)
    }

    @Test
    fun removeCollaboratorsShouldReturnListEmails() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        viewModel.removeCollaborator("test@emil.com")
        val expectedCollaborators = listOf("name@example.co.jp")

        assertEquals(expectedCollaborators, viewModel.uiState.value?.collaborators)
    }

    @Test
    fun removeCollaboratorsForNoteInTrashShouldTriggerEvent() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        note.isDeleted = true
        viewModel.removeCollaborator("test@emil.com")

        assertNull(viewModel.uiState.value)
        assertEquals(CollaboratorsViewModel.CollaboratorsEvent.NoteInTrash, viewModel.event.value)
    }

    @Test
    fun removeCollaboratorsForNoteDeletedShouldTriggerEvent() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())
        viewModel.removeCollaborator("test@emil.com")

        assertNull(viewModel.uiState.value)
        assertEquals(CollaboratorsViewModel.CollaboratorsEvent.NoteDeleted, viewModel.event.value)
    }
}