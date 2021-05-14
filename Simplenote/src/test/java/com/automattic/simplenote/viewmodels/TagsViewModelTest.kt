package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.TagsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub


@ExperimentalCoroutinesApi
class TagsViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: TagsViewModel
    private val fakeTagsRepository = mock(TagsRepository::class.java)
    private val tagItems = mutableListOf<TagItem>()

    @Before
    fun setup() {
        viewModel = TagsViewModel(fakeTagsRepository)
    }

    @Before
    fun setupTags() {
        tagItems.add(TagItem(Tag("tag1"), 0))
        tagItems.add(TagItem(Tag("tag2"), 2))
        tagItems.add(TagItem(Tag("tag3"), 5))
        tagItems.add(TagItem(Tag("tag4"), 10))
        tagItems.add(TagItem(Tag("tag5"), 0))
    }

    @Test
    fun startShouldSetupUiState() = runBlockingTest {
        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(tagItems)
        }
        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(emptyFlow())
        }
        viewModel.start()

        assertEquals(viewModel.uiState.value?.tagItems, tagItems)
        assertEquals(viewModel.uiState.value?.searchUpdate, false)
        assertNull(viewModel.uiState.value?.searchQuery)
    }

    @Test
    fun clickAddTagShouldTriggerAddTagEvent() {
        viewModel.clickAddTag()

        assertEquals(viewModel.event.value, TagsEvent.AddTagEvent)
    }

    @Test
    fun lonClickAddTagShouldTriggerLongAddTagEvent() {
        viewModel.longClickAddTag()

        assertEquals(viewModel.event.value, TagsEvent.LongAddTagEvent)
    }

    @Test
    fun closeShouldTriggerFinishEvent() {
        viewModel.close()

        assertEquals(viewModel.event.value, TagsEvent.FinishEvent)
    }

    @Test
    fun closeSearchShouldCleanQuery() = runBlockingTest {
        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(tagItems)
        }
        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(emptyFlow())
        }
        viewModel.start()
        viewModel.closeSearch()

        assertEquals(viewModel.uiState.value?.tagItems, tagItems)
        assertEquals(viewModel.uiState.value?.searchUpdate, true)
        assertNull(viewModel.uiState.value?.searchQuery)
    }
}
