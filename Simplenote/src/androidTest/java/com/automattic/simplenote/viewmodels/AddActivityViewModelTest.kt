package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.R
import com.automattic.simplenote.SimplenoteTest
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.SimperiumTagsRepository
import com.automattic.simplenote.utils.TagUtils
import com.automattic.simplenote.utils.TestBucket
import com.automattic.simplenote.utils.getRandomStringOfLen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class AddActivityViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    protected lateinit var tagsBucket: TestBucket<Tag>
    protected lateinit var viewModel: AddTagViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext() as SimplenoteTest
        // Make sure to use TestBucket buckets in UI tests
        application.useTestBucket = true

        tagsBucket = application.tagsBucket as TestBucket<Tag>
        tagsBucket.clear()
        tagsBucket.newObjectShouldFail = false

        viewModel = AddTagViewModel(SimperiumTagsRepository(tagsBucket))
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
        val randomLongTag = getRandomStringOfLen(279)
        viewModel.updateUiState(randomLongTag)

        assertEquals(viewModel.uiState.value?.errorMsg, R.string.tag_error_length)
    }

    @Test
    fun validateTagExists() {
        val tagName = "tag1"
        TagUtils.createTagIfMissing(tagsBucket, tagName)

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
        tagsBucket.newObjectShouldFail = true
        viewModel.updateUiState("tag1")
        viewModel.saveTag()

        assertEquals(viewModel.event.value, AddTagViewModel.Event.SHOW_ERROR)
    }
}
