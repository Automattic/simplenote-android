package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
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
class AddCollaboratorViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val notesBucket = Mockito.mock(Bucket::class.java) as Bucket<Note>
    private val collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket, TestCoroutineDispatcher())
    private val viewModel = AddCollaboratorViewModel(collaboratorsRepository)

    private val noteId = "key1"
    private val note = Note(noteId).apply {
        content = "Hello World"
        tags = listOf("tag1", "tag2", "name@example.co.jp", "name@test", "あいうえお@example.com")
        bucket = notesBucket
    }

    @Before
    fun setup() {
        whenever(notesBucket.get(any())).thenReturn(note)
    }

    @Test
    fun addValidCollaboratorShouldTriggerAddedEvent() = runBlockingTest {
        viewModel.addCollaborator(noteId, "test@emil.com")

        assertEquals(AddCollaboratorViewModel.Event.CollaboratorAdded, viewModel.event.value)
    }

    @Test
    fun addInvalidCollaboratorShouldTriggerInvalidEvent() = runBlockingTest {
        viewModel.addCollaborator(noteId, "test@emil")

        assertEquals(AddCollaboratorViewModel.Event.InvalidCollaborator, viewModel.event.value)
    }

    @Test
    fun addValidCollaboratorToNoteInTrashShouldTriggerNoteInTrash() = runBlockingTest {
        note.isDeleted = true

        viewModel.addCollaborator(noteId, "test@emil.com")

        assertEquals(AddCollaboratorViewModel.Event.NoteInTrash, viewModel.event.value)
    }

    @Test
    fun addValidCollaboratorToNoteDeletedShouldTriggerNoteDeleted() = runBlockingTest {
        whenever(notesBucket.get(any())).thenThrow(BucketObjectMissingException())

        viewModel.addCollaborator(noteId, "test@emil.com")

        assertEquals(AddCollaboratorViewModel.Event.NoteDeleted, viewModel.event.value)
    }

    @Test
    fun closeShouldTriggerEventClose() {
        viewModel.close()

        assertEquals(AddCollaboratorViewModel.Event.Close, viewModel.event.value)
    }
}