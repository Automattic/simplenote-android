package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.CoroutineTestRule
import com.automattic.simplenote.repositories.CollaboratorsActionResult
import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.Event
import com.automattic.simplenote.viewmodels.CollaboratorsViewModel.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CollaboratorsViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val mockCollaboratorsRepository = mock(CollaboratorsRepository::class.java)
    private val viewModel = CollaboratorsViewModel(mockCollaboratorsRepository)

    private val collaboratorFoo = "foo@email.com"
    private val collaboratorBar = "bar@em.co.de"
    private val collaboratorBaz = "baz@e.c"
    private val collaborators = listOf(
        collaboratorFoo,
        collaboratorBar,
    )
    private val noteId = "key123"

    @Before
    fun setup() = runTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
    }

    @Test
    fun loadCollaboratorsShouldUpdateUiStateWithList() = runTest {
        viewModel.loadCollaborators(noteId)

        val expectedCollaborators = UiState.CollaboratorsList(collaborators)
        assertEquals(expectedCollaborators, viewModel.uiState.value)
    }

    @Test
    fun loadEmptyCollaboratorsShouldUpdateUiStateWithEmpty() = runTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(emptyList()))

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.EmptyCollaborators(allCollaboratorsRemoved = true), viewModel.uiState.value)
    }

    @Test
    fun loadCollaboratorsForNoteInTrashShouldUpdateUiStateNoteInTrash() = runTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.NoteInTrash)

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.NoteInTrash, viewModel.uiState.value)
    }

    @Test
    fun loadCollaboratorsForNoteInTrashShouldUpdateUiStateNoteDeleted() = runTest {
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.NoteDeleted)

        viewModel.loadCollaborators(noteId)

        assertEquals(UiState.NoteDeleted, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorShouldReturnListEmails() = runTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, collaboratorFoo))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(listOf(collaboratorBar)))

        viewModel.removeCollaborator(collaboratorFoo)

        val expectedCollaborators = UiState.CollaboratorsList(listOf(collaboratorBar))
        assertEquals(expectedCollaborators, viewModel.uiState.value)
    }

    @Test
    fun removeLastCollaboratorShouldReturnEmpty() = runTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, collaboratorFoo))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(emptyList()))

        viewModel.removeCollaborator(collaboratorFoo)

        assertEquals(UiState.EmptyCollaborators(allCollaboratorsRemoved = true), viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorForNoteInTrashShouldTriggerEvent() = runTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, collaboratorFoo))
            .thenReturn(CollaboratorsActionResult.NoteInTrash)

        viewModel.removeCollaborator(collaboratorFoo)

        assertEquals(UiState.NoteInTrash, viewModel.uiState.value)
    }

    @Test
    fun removeCollaboratorForNoteDeletedShouldTriggerEvent() = runTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.removeCollaborator(noteId, collaboratorFoo))
            .thenReturn(CollaboratorsActionResult.NoteDeleted)

        viewModel.removeCollaborator(collaboratorFoo)

        assertEquals(UiState.NoteDeleted, viewModel.uiState.value)
    }

    @Test
    fun clickAddCollaboratorShouldTriggerEventAddCollaborator() {
        viewModel.loadCollaborators(noteId)

        viewModel.clickAddCollaborator()

        assertEquals(Event.AddCollaboratorEvent(noteId), viewModel.event.value)
    }

    @Test
    fun longClickAddCollaboratorShouldTriggerLongAddCollaboratorEVent() {
        viewModel.longClickAddCollaborator()

        assertEquals(viewModel.event.value, Event.LongAddCollaboratorEvent)
    }

    @Test
    fun clickRemoveCollaboratorShouldTriggerEventAddCollaborator() {
        val collaborator = collaboratorFoo
        viewModel.clickRemoveCollaborator(collaborator)

        assertEquals(Event.RemoveCollaboratorEvent(collaborator), viewModel.event.value)
    }

    @Test
    fun longClickRemoveCollaboratorShouldTriggerLongRemoveCollaboratorEvent() {
        viewModel.longClickRemoveCollaborator()

        assertEquals(viewModel.event.value, Event.LongRemoveCollaboratorEvent)
    }

    @Test
    fun closeShouldTriggerCloseCollaborators() {
        viewModel.close()

        assertEquals(Event.CloseCollaboratorsEvent, viewModel.event.value)
    }

    @Test
    fun collaboratorAddedAfterStoppedListeningChangesShouldNotUpdateUiState() = runTest {
        // Load collaborators and capture initial state
        viewModel.loadCollaborators(noteId)
        val initialState = viewModel.uiState.value

        // Mock the flow to not emit anything initially
        whenever(mockCollaboratorsRepository.collaboratorsChanged(noteId)).thenReturn(flow { /* no emission */ })

        // Start and then stop listening to changes
        viewModel.startListeningChanges()
        viewModel.stopListeningChanges()

        // Now mock the flow to emit changes and mock getCollaborators to return a different list
        // This simulates collaborators being added after we stopped listening
        whenever(mockCollaboratorsRepository.collaboratorsChanged(noteId)).thenReturn(flow { emit(true) })
        val newList = collaborators + collaboratorBaz
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(newList))

        // Since we stopped listening, the UI state should remain the same as the initial state
        assertEquals(initialState, viewModel.uiState.value)
    }

    @Test
    fun collaboratorAddedShouldUpdateUiState() = runTest {
        viewModel.loadCollaborators(noteId)
        whenever(mockCollaboratorsRepository.collaboratorsChanged(noteId)).thenReturn(flow { emit(true) })
        val expectedList = collaborators + collaboratorBaz
        whenever(mockCollaboratorsRepository.getCollaborators(noteId))
            .thenReturn(CollaboratorsActionResult.CollaboratorsList(expectedList))

        viewModel.startListeningChanges()

        assertEquals(UiState.CollaboratorsList(expectedList), viewModel.uiState.value)
    }

    @Test
    fun closeSearchShouldCleanQuery() = runTest {
        viewModel.loadCollaborators(noteId)
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId) }.doReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
        }
        mockCollaboratorsRepository.stub {
            onBlocking { collaboratorsChanged(noteId) }.doReturn(emptyFlow())
        }
        viewModel.startListeningChanges()
        viewModel.closeSearch()

        assertEquals(UiState.CollaboratorsList(collaborators), viewModel.uiState.value)
    }

    @Test
    fun removeAllCollaboratorsDuringSearchShouldReturnAllRemoved() = runTest {
        viewModel.loadCollaborators(noteId)
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId) }.doReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
        }
        mockCollaboratorsRepository.stub {
            onBlocking { collaboratorsChanged(noteId) }.doReturn(emptyFlow())
        }
        viewModel.startListeningChanges()

        val returnedList = listOf(collaboratorBar)
        val searchQuery = "@"
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId, searchQuery) }.doReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
        }
        viewModel.search(searchQuery)
        mockCollaboratorsRepository.stub {
            onBlocking { removeCollaborator(noteId, collaboratorFoo) }.doReturn(CollaboratorsActionResult.CollaboratorsList(returnedList))
        }
        viewModel.removeCollaborator(collaboratorFoo)
        mockCollaboratorsRepository.stub {
            onBlocking { removeCollaborator(noteId, collaboratorBar) }.doReturn(CollaboratorsActionResult.CollaboratorsList(emptyList()))
        }
        viewModel.removeCollaborator(collaboratorBar)

        assertEquals(UiState.EmptyCollaborators(allCollaboratorsRemoved = true), viewModel.uiState.value)
    }

    @Test
    fun removeOneCollaboratorDuringSearchShouldNotReturnAllRemoved() = runTest {
        viewModel.loadCollaborators(noteId)
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId) }.doReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
        }
        mockCollaboratorsRepository.stub {
            onBlocking { collaboratorsChanged(noteId) }.doReturn(emptyFlow())
        }
        viewModel.startListeningChanges()

        val returnedList = listOf(collaboratorBar)
        val searchQuery = "@"
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId, searchQuery) }.doReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
        }
        viewModel.search(searchQuery)
        mockCollaboratorsRepository.stub {
            onBlocking { removeCollaborator(noteId, collaboratorFoo) }.doReturn(CollaboratorsActionResult.CollaboratorsList(returnedList))
        }
        viewModel.removeCollaborator(collaboratorFoo)

        assertEquals(UiState.CollaboratorsList(returnedList), viewModel.uiState.value)
    }

    @Test
    fun searchShouldFilterCollaborators() = runTest {
        viewModel.loadCollaborators(noteId)
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId) }.doReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
        }
        mockCollaboratorsRepository.stub {
            onBlocking { collaboratorsChanged(noteId) }.doReturn(emptyFlow())
        }
        viewModel.startListeningChanges()

        val collaborator = collaborators[0]
        val filteredList = listOf(collaborator)
        val searchQuery = collaborator.substringBefore("@")
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId, searchQuery) }.doReturn(CollaboratorsActionResult.CollaboratorsList(filteredList))
        }
        viewModel.search(searchQuery)

        assertEquals(UiState.CollaboratorsList(filteredList, true, searchQuery), viewModel.uiState.value)
    }

    @Test
    fun searchShouldShowNoCollaboratorsForUniqueQuery() = runTest {
        viewModel.loadCollaborators(noteId)
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId) }.doReturn(CollaboratorsActionResult.CollaboratorsList(collaborators))
        }
        mockCollaboratorsRepository.stub {
            onBlocking { collaboratorsChanged(noteId) }.doReturn(emptyFlow())
        }
        viewModel.startListeningChanges()

        val searchQuery = "d34db33f"
        mockCollaboratorsRepository.stub {
            onBlocking { getCollaborators(noteId, searchQuery) }.doReturn(CollaboratorsActionResult.CollaboratorsList(emptyList()))
        }
        viewModel.search(searchQuery)

        assertEquals(UiState.EmptyCollaborators(allCollaboratorsRemoved = false, searchUpdate = true), viewModel.uiState.value)
    }
}
