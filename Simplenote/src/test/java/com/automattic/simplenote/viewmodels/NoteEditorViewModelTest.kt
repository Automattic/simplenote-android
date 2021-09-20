package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.usecases.GetTagsUseCase
import com.automattic.simplenote.usecases.ValidateTagUseCase
import com.automattic.simplenote.viewmodels.NoteEditorViewModel.*
import com.simperium.client.Bucket
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class NoteEditorViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val tagsRepository: TagsRepository = mock(TagsRepository::class.java)
    private val mockBucket: Bucket<*> = mock(Bucket::class.java)
    private val collaboratorsRepository = SimperiumCollaboratorsRepository()
    private val getTagsUseCase = GetTagsUseCase(tagsRepository, collaboratorsRepository)
    private val validateTagUseCase = ValidateTagUseCase(tagsRepository, collaboratorsRepository)
    private val viewModel = NoteEditorViewModel(getTagsUseCase, validateTagUseCase)

    private val note = Note("key1").also {
        it.content = "Hello World"
        it.tags = listOf("tag1", "tag2", "name@test.com")
        it.bucket = mockBucket
    }

    @Before
    fun setup() {
        whenever(tagsRepository.isTagValid(any())).thenReturn(true)
        whenever(tagsRepository.isTagMissing(any())).thenReturn(true)
    }

    @Ignore("Patch for code freeze")
    @Test
    fun updateShouldUpdateUiState() {
        viewModel.update(note)

        assertEquals(listOf("tag1", "tag2"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "tag2", "name@test.com"), note.tags)
    }

    @Ignore("Patch for code freeze")
    @Test
    fun addTagShouldUpdateUiState() {
        viewModel.addTag("tag3", note)

        assertEquals(listOf("tag1", "tag2", "tag3"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "tag2", "name@test.com", "tag3"), note.tags)
    }

    @Ignore("Patch for code freeze")
    @Test
    fun addCollaboratorShouldNotUpdateUiState() {
        viewModel.update(note)
        viewModel.addTag("name@email.com", note)

        assertEquals(listOf("tag1", "tag2"), viewModel.uiState.value?.tags)
        assertEquals(NoteEditorEvent.TagAsCollaborator("name@email.com"), viewModel.event.value)
    }

    @Test
    fun addCollaboratorShouldNotUpdateNote() {
        viewModel.update(note)
        viewModel.addTag("name@email.com", note)

        assertEquals(listOf("tag1", "tag2", "name@test.com", "name@email.com"), note.tags)
        assertEquals(NoteEditorEvent.TagAsCollaborator("name@email.com"), viewModel.event.value)
    }

    @Ignore("Patch for code freeze")
    @Test
    fun addInvalidTagShouldNotUpdateUiState() {
        viewModel.update(note)
        viewModel.addTag("test test1", note)

        assertEquals(listOf("tag1", "tag2"), viewModel.uiState.value?.tags)
        assertEquals(NoteEditorEvent.InvalidTag, viewModel.event.value)
    }

    @Test
    fun addInvalidTagShouldNotUpdateNote() {
        viewModel.update(note)
        viewModel.addTag("test test1", note)

        assertEquals(listOf("tag1", "tag2", "name@test.com"), note.tags)
        assertEquals(NoteEditorEvent.InvalidTag, viewModel.event.value)
    }


    @Ignore("Patch for code freeze")
    @Test
    fun removeTagShouldUpdateUiStateAndNote() {
        viewModel.removeTag("tag2", note)

        assertEquals(listOf("tag1"), viewModel.uiState.value?.tags)
        assertEquals(listOf("tag1", "name@test.com"), note.tags)
    }
}
