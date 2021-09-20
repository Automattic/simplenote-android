package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.Event
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.UiState
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
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

    private val notesBucket = Mockito.mock(Bucket::class.java) as Bucket<Note>
    private val collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket, TestCoroutineDispatcher())
    private val viewModel = CollaboratorsViewModel(collaboratorsRepository)

    private val noteId = "key1"
    private val note = Note(noteId).apply {
        content = "Hello World"
        tags = listOf("tag1", "tag2", "test@emil.com", "name@example.co.jp", "name@test", "あいうえお@example.com")
        bucket = notesBucket
    }

    @Before
    fun setup() {
        whenever(notesBucket.get(any())).thenReturn(note)
    }

    @Test
    fun loadCollaboratorsShouldUpdateUiStateWithList() = runBlockingTest {
        viewModel.loadCollaborators(noteId)

        val expectedCollaborators = UiState.CollaboratorsList(listOf("test@emil.com", "name@example.co.jp"))
        assertEquals(expectedCollaborators, viewModel.uiState.value)
    }

    @Test
    fun loadEmptyCollaboratorsShouldUpdateUiStateWithEmpty() = runBlockingTest {
        note.tags = emptyList()

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.EmptyCollaborators, viewModel.uiState.value)
    }

    @Test
    fun loadCollaboratorsForNoteInTrashShouldUpdateUiStateNoteInTrash() = runBlockingTest {
        note.isDeleted = true

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.NoteInTrash, viewModel.uiState.value)
    }

    @Test
    fun loadCollaboratorsForNoteInTrashShouldUpdateUiStateNoteDeleted() = runBlockingTest {
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.NoteDeleted, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorShouldReturnListEmails() = runBlockingTest {
        viewModel.loadCollaborators(noteId)

        viewModel.removeCollaborator("test@emil.com")

        val expectedCollaborators = UiState.CollaboratorsList(listOf("name@example.co.jp"))
        assertEquals(expectedCollaborators, viewModel.uiState.value)
    }

    @Test
    fun removeLastCollaboratorShouldReturnEmpty() = runBlockingTest {
        note.tags = listOf("test@emil.com")
        viewModel.loadCollaborators(noteId)

        viewModel.removeCollaborator("test@emil.com")

        assertEquals(UiState.EmptyCollaborators, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorForNoteInTrashShouldTriggerEvent() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        note.isDeleted = true

        viewModel.removeCollaborator("test@emil.com")

        assertEquals(UiState.NoteInTrash, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorForNoteDeletedShouldTriggerEvent() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())

        viewModel.removeCollaborator("test@emil.com")

        assertEquals(UiState.NoteDeleted, viewModel.uiState.value)
    }

    @Test
    fun clickAddCollaboratorShouldTriggerEventAddCollaborator() {
        viewModel.loadCollaborators(noteId)

        viewModel.clickAddCollaborator()

        assertEquals(Event.AddCollaboratorEvent(noteId), viewModel.event.value)
    }

    @Test
    fun clickRemoveCollaboratorShouldTriggerEventAddCollaborator() {
        val collaborator = "test@emil.com"
        viewModel.clickRemoveCollaborator(collaborator)

        assertEquals(Event.RemoveCollaboratorEvent(collaborator), viewModel.event.value)
    }

    @Test
    fun closeShouldTriggerCloseCollaborators() {
        viewModel.close()

        assertEquals(Event.CloseCollaboratorsEvent, viewModel.event.value)
    }
}
