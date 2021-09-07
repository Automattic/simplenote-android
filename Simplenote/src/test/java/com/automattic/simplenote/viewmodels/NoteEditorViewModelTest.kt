package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.usecases.GetTagsUseCase
import com.simperium.client.Bucket
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class NoteEditorViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val tagsRepository: TagsRepository = mock(TagsRepository::class.java)
    private val mockBucket: Bucket<*> = mock(Bucket::class.java)

    private lateinit var viewModel: NoteEditorViewModel
    private lateinit var note: Note

    @Before
    fun setup() {
        val getTagsUseCase = GetTagsUseCase(tagsRepository, SimperiumCollaboratorsRepository())
        viewModel = NoteEditorViewModel(getTagsUseCase)

        note = Note("key1")
        note.content = "Hello World"
        note.tags = listOf("tag1", "tag2", "name@test.com")
        note.bucket = mockBucket
    }

    @Test
    fun updateShouldUpdateUiState() {
        viewModel.update(note)

        assertEquals(listOf("tag1", "tag2"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "tag2", "name@test.com"), note.tags)
    }

    @Test
    fun addTagShouldUpdateUiState() {
        viewModel.addTag("tag3", note)

        assertEquals(listOf("tag1", "tag2", "tag3"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "tag2", "name@test.com", "tag3"), note.tags)
    }

    @Test
    fun addCollaboratorShouldNotUpdateUiStateAndNote() {
        viewModel.addTag("name@email.com", note)

        assertEquals(listOf("tag1", "tag2"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "tag2", "name@test.com"), note.tags)
    }

    @Test
    fun addInvalidTagShouldNotUpdateUiStateAndNote() {
        viewModel.addTag("test test1", note)

        assertEquals(listOf("tag1", "tag2"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "tag2", "name@test.com"), note.tags)
    }

    @Test
    fun removeTagShouldUpdateUiStateAndNote() {
        viewModel.removeTag("tag2", note)

        assertEquals(listOf("tag1"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "name@test.com"), note.tags)
    }
}