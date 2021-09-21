package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.repositories.CollaboratorsActionResult
import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.Event
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CollaboratorsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mockCollaboratorsRepository = mock(CollaboratorsRepository::class.java)
    private val viewModel = CollaboratorsViewModel(mockCollaboratorsRepository)

    private val noteId = "key1"


    @Before
    fun setup() = runBlockingTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(listOf("test@emil.com", "name@example.co.jp")))
    }

    @Test
    fun loadCollaboratorsShouldUpdateUiStateWithList() = runBlockingTest {
        viewModel.loadCollaborators(noteId)

        val expectedCollaborators = UiState.CollaboratorsList(listOf("test@emil.com", "name@example.co.jp"))
        assertEquals(expectedCollaborators, viewModel.uiState.value)
    }

    @Test
    fun loadEmptyCollaboratorsShouldUpdateUiStateWithEmpty() = runBlockingTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(emptyList()))

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.EmptyCollaborators, viewModel.uiState.value)
    }

    @Test
    fun loadCollaboratorsForNoteInTrashShouldUpdateUiStateNoteInTrash() = runBlockingTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.NoteInTrash)

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.NoteInTrash, viewModel.uiState.value)
    }

    @Test
    fun loadCollaboratorsForNoteInTrashShouldUpdateUiStateNoteDeleted() = runBlockingTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.NoteDeleted)

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.NoteDeleted, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorShouldReturnListEmails() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, "test@emil.com"))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(listOf("name@example.co.jp")))

        viewModel.removeCollaborator("test@emil.com")

        val expectedCollaborators = UiState.CollaboratorsList(listOf("name@example.co.jp"))
        assertEquals(expectedCollaborators, viewModel.uiState.value)
    }

    @Test
    fun removeLastCollaboratorShouldReturnEmpty() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, "test@emil.com"))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(emptyList()))

        viewModel.removeCollaborator("test@emil.com")

        assertEquals(UiState.EmptyCollaborators, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorForNoteInTrashShouldTriggerEvent() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, "test@emil.com"))
            .thenReturn(CollaboratorsActionResult.NoteInTrash)

        viewModel.removeCollaborator("test@emil.com")

        assertEquals(UiState.NoteInTrash, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorForNoteDeletedShouldTriggerEvent() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, "test@emil.com"))
            .thenReturn(CollaboratorsActionResult.NoteDeleted)

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

    @Test
    fun collaboratorAddedAfterStoppedListeningChangesShouldNotUpdateUiState() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        viewModel.startListeningChanges()
        viewModel.stopListeningChanges()

        whenever(mockCollaboratorsRepository.collaboratorsChanged(noteId)).thenReturn(flow { emit(true) })
        val expectedList = listOf("test@emil.com", "name@example.co.jp")
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(expectedList))

        assertEquals(UiState.CollaboratorsList(expectedList), viewModel.uiState.value)
    }

    @Test
    fun collaboratorAddedShouldUpdateUiState() = runBlockingTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.collaboratorsChanged(noteId)).thenReturn(flow { emit(true) })
        val expectedList = listOf("test@emil.com", "name@example.co.jp", "test2@email.com")
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(expectedList))

        viewModel.startListeningChanges()

        assertEquals(UiState.CollaboratorsList(expectedList), viewModel.uiState.value)
    }
}
