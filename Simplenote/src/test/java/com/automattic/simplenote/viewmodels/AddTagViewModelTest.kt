package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.R
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.usecases.ValidateTagUseCase
import com.automattic.simplenote.utils.getLocalRandomStringOfLen
import com.simperium.client.Bucket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@ExperimentalCoroutinesApi
class AddTagViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private val fakeTagsRepository = mock(TagsRepository::class.java)
    private val notesBucket = mock(Bucket::class.java) as Bucket<Note>
    private val collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket, TestCoroutineDispatcher())
    private val validateTagUseCase = ValidateTagUseCase(fakeTagsRepository, collaboratorsRepository)
    private val viewModel = AddTagViewModel(fakeTagsRepository, validateTagUseCase)

    @Test
    fun startShouldSetupUiState() {
        viewModel.start()

        assertEquals(viewModel.event.value, AddTagViewModel.Event.START)
        assertNull(viewModel.uiState.value?.errorMsg)
        assertEquals(viewModel.uiState.value!!.tagName, "")
    }

    @Test
    fun validateEmptyTag() {
        viewModel.updateUiState("")

        assertEquals(viewModel.uiState.value?.errorMsg, R.string.tag_error_empty)
    }

    @Test
    fun validateSpaceTag() {
        viewModel.updateUiState("tag 5")

        assertEquals(viewModel.uiState.value?.errorMsg, R.string.tag_error_spaces)
    }

    @Test
    fun validateTooLongTag() {
        val randomLongTag = getLocalRandomStringOfLen(279)

        viewModel.updateUiState(randomLongTag)

        assertEquals(viewModel.uiState.value?.errorMsg, R.string.tag_error_length)
    }

    @Test
    fun validateTagExists() {
        val tagName = "tag1"
        `when`(fakeTagsRepository.isTagValid(tagName)).thenReturn(true)
        `when`(fakeTagsRepository.isTagMissing(tagName)).thenReturn(false)

        viewModel.updateUiState(tagName)

        assertEquals(viewModel.uiState.value?.errorMsg, R.string.tag_error_exists)
    }

    @Test
    fun validateTagIsCollaborator() {
        val tagName = "tag1@email.com"
        `when`(fakeTagsRepository.isTagValid(tagName)).thenReturn(true)
        `when`(fakeTagsRepository.isTagMissing(tagName)).thenReturn(true)

        viewModel.updateUiState(tagName)

        assertEquals(viewModel.uiState.value?.errorMsg, R.string.tag_error_collaborator)
    }

    @Test
    fun validateValidTag() {
        val tagName = "tag1"
        `when`(fakeTagsRepository.isTagValid(tagName)).thenReturn(true)
        `when`(fakeTagsRepository.isTagMissing(tagName)).thenReturn(true)

        viewModel.updateUiState(tagName)

        assertNull(viewModel.uiState.value?.errorMsg)
    }

    @Test
    fun saveTagCorrectly() {
        val tagName = "tag1"
        viewModel.updateUiState(tagName)
        `when`(fakeTagsRepository.saveTag(tagName)).thenReturn(true)

        viewModel.saveTag()

        assertEquals(viewModel.event.value, AddTagViewModel.Event.FINISH)
    }

    @Test
    fun saveTagWithError() {
        viewModel.updateUiState("tag1")

        viewModel.saveTag()

        assertEquals(viewModel.event.value, AddTagViewModel.Event.SHOW_ERROR)
    }
}
