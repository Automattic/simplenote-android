package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.R
import com.automattic.simplenote.repositories.FakeTagsRepository
import com.automattic.simplenote.utils.getLocalRandomStringOfLen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class AddActivityViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: AddTagViewModel
    private val fakeTagsRepository = FakeTagsRepository()

    @Before
    fun setup() {
        fakeTagsRepository.clear()
        fakeTagsRepository.failAtSave = false

        viewModel = AddTagViewModel(fakeTagsRepository)
    }

    @Test
    fun validateEmptyTag() {
        viewModel.updateUiState("")

        //  assertEquals(viewModel.tagError.value, R.string.tag_error_empty)
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
        fakeTagsRepository.saveTag(tagName)

        viewModel.updateUiState(tagName)

        assertEquals(viewModel.uiState.value?.errorMsg, R.string.tag_error_exists)
    }

    @Test
    fun validateValidTag() {
        viewModel.updateUiState("tag1")

        assertEquals(viewModel.uiState.value?.errorMsg, -1)
    }

    @Test
    fun saveTagCorrectly() {
        viewModel.updateUiState("tag1")
        viewModel.saveTag()

        assertEquals(viewModel.event.value, AddTagViewModel.Event.FINISH)
    }

    @Test
    fun saveTagWithError() {
        fakeTagsRepository.failAtSave = true
        viewModel.updateUiState("tag1")
        viewModel.saveTag()

        assertEquals(viewModel.event.value, AddTagViewModel.Event.SHOW_ERROR)
    }
}
