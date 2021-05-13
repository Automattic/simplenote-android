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
        assertEquals(viewModel.uiState.value?.tagName, tagName)
        assertEquals(viewModel.uiState.value?.oldTag, tag)
    }

    @Test
    fun closeShouldTriggerEventClose() {
        viewModel.close()

        assertEquals(viewModel.event.value, TagDialogEvent.CloseEvent)
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
        val hewTagName = "tag2"

        `when`(fakeTagsRepository.isTagValid(hewTagName)).thenReturn(true)
        `when`(fakeTagsRepository.isTagConflict(hewTagName, tagName)).thenReturn(false)

        viewModel.updateUiState(hewTagName)

        assertNull(viewModel.uiState.value?.errorMsg)
    }

    @Test
    fun editTagWithSameName() {
        viewModel.updateUiState(tagName)
        viewModel.renameTagIfValid()

        assertTrue(viewModel.event.value is TagDialogEvent.FinishEvent)
    }

    @Test
    fun editTagWithNewName() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.isTagConflict(newTagName, tagName)).thenReturn(false)
        `when`(fakeTagsRepository.renameTag(newTagName, tag)).thenReturn(true)
        viewModel.renameTagIfValid()

        assertEquals(viewModel.uiState.value?.tagName, newTagName)
        assertTrue(viewModel.event.value is TagDialogEvent.FinishEvent)
    }

    @Test
    fun editTagWithConflict() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.isTagConflict(newTagName, tagName)).thenReturn(true)
        `when`(fakeTagsRepository.getCanonicalTagName(newTagName)).thenReturn(newTagName)
        viewModel.renameTagIfValid()

        assertEquals(viewModel.uiState.value?.tagName, newTagName)
        assertTrue(viewModel.event.value is TagDialogEvent.ConflictEvent)
        assertEquals((viewModel.event.value as TagDialogEvent.ConflictEvent).canonicalTagName, newTagName)
        assertEquals((viewModel.event.value as TagDialogEvent.ConflictEvent).oldTagName, tagName)
    }

    @Test
    fun editTagWithError() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.isTagConflict(newTagName, tagName)).thenReturn(false)
        `when`(fakeTagsRepository.renameTag(newTagName, tag)).thenReturn(false)
        viewModel.renameTagIfValid()

        assertEquals(viewModel.uiState.value?.tagName, newTagName)
        assertTrue(viewModel.event.value is TagDialogEvent.ShowErrorEvent)
    }

    @Test
    fun renameTagValid() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.renameTag(newTagName, tag)).thenReturn(true)
        viewModel.renameTag()

        assertEquals(viewModel.uiState.value?.tagName, newTagName)
        assertTrue(viewModel.event.value is TagDialogEvent.FinishEvent)
    }

    @Test
    fun renameTagError() {
        val newTagName = "tag2"

        viewModel.updateUiState(newTagName)
        `when`(fakeTagsRepository.renameTag(newTagName, tag)).thenReturn(false)
        viewModel.renameTag()

        assertEquals(viewModel.uiState.value?.tagName, newTagName)
        assertTrue(viewModel.event.value is TagDialogEvent.ShowErrorEvent)
    }
}
