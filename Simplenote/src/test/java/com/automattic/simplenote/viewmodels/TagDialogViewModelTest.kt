package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.R
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.utils.getLocalRandomStringOfLen
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock


class TagDialogViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: TagDialogViewModel
    private val fakeTagsRepository = mock(TagsRepository::class.java)
    private val tagName = "tag1"
    private lateinit var tag: Tag

    @Before
    fun setup() {
        viewModel = TagDialogViewModel(fakeTagsRepository)
    }

    @Before
    fun setupInitialTag() {
        tag = Tag(tagName)
        tag.name = tagName
        tag.index = 0
        viewModel.start(tag)
    }

    @Test
    fun startShouldSetupUiState() {
        assertNull(viewModel.uiState.value?.errorMsg)
        assertEquals(viewModel.uiState.value!!.tagName, tagName)
        assertEquals(viewModel.uiState.value!!.oldTag, tag)
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
    fun validateValidTag() {
        val tagName = "tag2"

        `when`(fakeTagsRepository.isTagValid(tagName)).thenReturn(true)
        `when`(fakeTagsRepository.isTagMissing(tagName)).thenReturn(true)

        viewModel.updateUiState(tagName)

        assertNull(viewModel.uiState.value?.errorMsg)
    }

    @Test
    fun editTagWithSameName() {
        viewModel.updateUiState(tagName)
        viewModel.renameTagIfValid()

        assertTrue(viewModel.event.value is FinishEvent)
    }

    @Test
    fun editTagWithNewName() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.isTagMissing(newTagName)).thenReturn(true)
        `when`(fakeTagsRepository.renameTag(newTagName, tag)).thenReturn(true)
        viewModel.renameTagIfValid()

        assertEquals(viewModel.uiState.value!!.tagName, newTagName)
        assertTrue(viewModel.event.value is FinishEvent)
    }

    @Test
    fun editTagWithConflict() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.isTagMissing(newTagName)).thenReturn(false)
        `when`(fakeTagsRepository.getCanonicalTagName(newTagName)).thenReturn(newTagName)
        viewModel.renameTagIfValid()

        assertEquals(viewModel.uiState.value!!.tagName, newTagName)
        assertTrue(viewModel.event.value is ConflictEvent)
        assertEquals((viewModel.event.value as ConflictEvent).canonicalTagName, newTagName)
        assertEquals((viewModel.event.value as ConflictEvent).oldTagName, tagName)
    }

    @Test
    fun editTagWithError() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.isTagMissing(newTagName)).thenReturn(true)
        `when`(fakeTagsRepository.renameTag(newTagName, tag)).thenReturn(false)
        viewModel.renameTagIfValid()

        assertEquals(viewModel.uiState.value!!.tagName, newTagName)
        assertTrue(viewModel.event.value is ShowErrorEvent)
    }
}
