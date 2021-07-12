package com.automattic.simplenote.viewmodels

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.TagsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub


@ExperimentalCoroutinesApi
class TagsViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: TagsViewModel
    private val fakeTagsRepository = mock(TagsRepository::class.java)
    private val tagItems = listOf(
        TagItem(Tag("tag1"), 0),
        TagItem(Tag("tag2"), 2),
        TagItem(Tag("tag3"), 5),
        TagItem(Tag("tag4"), 10),
        TagItem(Tag("tag5"), 0),
    )

    @Before
    fun setup() {
        viewModel = TagsViewModel(fakeTagsRepository)
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
    fun whenTagsChangedWithANewTagUiStateShouldUpdate() = runBlockingTest {
        val variableTagItems = tagItems.toMutableList()

        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(tagItems)
        }

        val tagsFlow = MutableSharedFlow<Boolean>()
        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(tagsFlow)
        }

        viewModel.start()
        viewModel.startListeningTagChanges()

        assertEquals(viewModel.uiState.value?.tagItems, tagItems)
        assertEquals(viewModel.uiState.value?.searchUpdate, false)
        assertNull(viewModel.uiState.value?.searchQuery)

        variableTagItems.add(TagItem(Tag("tag6"), 3))
        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(variableTagItems)
        }
        tagsFlow.emit(true)

        assertEquals(viewModel.uiState.value?.tagItems, variableTagItems)
        assertEquals(viewModel.uiState.value?.searchUpdate, false)
        assertNull(viewModel.uiState.value?.searchQuery)
    }

    @Test
    fun whenPauseIsCalledAllChangedToTagsAreNotListened() = runBlockingTest {
        val variableTagItems = tagItems.toMutableList()

        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(tagItems)
        }

        val tagsFlow = MutableSharedFlow<Boolean>()
        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(tagsFlow)
        }

        viewModel.start()
        viewModel.startListeningTagChanges()

        assertEquals(viewModel.uiState.value?.tagItems, tagItems)
        assertEquals(viewModel.uiState.value?.searchUpdate, false)
        assertNull(viewModel.uiState.value?.searchQuery)

        viewModel.stopListeningTagChanges()

        variableTagItems.add(TagItem(Tag("tag6"), 3))
        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(variableTagItems)
        }
        tagsFlow.emit(true)

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

    @Test
    fun searchShouldFilterTags() = runBlockingTest {
        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(tagItems)
        }
        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(emptyFlow())
        }
        viewModel.start()

        val filteredList = listOf(tagItems[1])
        val searchQuery = "tag2"
        fakeTagsRepository.stub {
            onBlocking { searchTags(any()) }.doReturn(filteredList)
        }
        viewModel.search(searchQuery)

        assertEquals(viewModel.uiState.value?.tagItems, filteredList)
        assertEquals(viewModel.uiState.value?.searchUpdate, true)
        assertEquals(viewModel.uiState.value?.searchQuery, "tag2")
    }

    @Test
    fun afterAddingATagUpdateOnResultShouldUpdateUiState() = runBlockingTest {
        val mutableTagItems = tagItems.toMutableList()

        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(mutableTagItems)
        }
        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(emptyFlow())
        }
        viewModel.start()

        // Add a new tag
        mutableTagItems.add(TagItem(Tag("tag10"), 2))

        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(mutableTagItems)
        }

        viewModel.updateOnResult()

        assertEquals(viewModel.uiState.value?.tagItems, mutableTagItems)
        assertEquals(viewModel.uiState.value?.searchUpdate, false)
        assertNull(viewModel.uiState.value?.searchQuery)
    }

    @Test
    fun clickEditTagShouldTriggerEventEditTagEvent() {
        viewModel.clickEditTag(tagItems[0])

        assertEquals(viewModel.event.value, TagsEvent.EditTagEvent(tagItems[0]))
    }

    @Test
    fun clickDeleteTagOfTagWithNotesShouldTriggerDeleteTagEvent() {
        viewModel.clickDeleteTag(tagItems[1])

        assertEquals(viewModel.event.value, TagsEvent.DeleteTagEvent(tagItems[1]))
    }

    @Test
    fun clickDeleteTagOfTagWithoutNotesShouldDeleteTagDirectly() = runBlockingTest {
        fakeTagsRepository.stub {
            onBlocking { deleteTag(any()) }.doReturn(Unit)
        }

        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(flow{ emit(true) })
        }

        val updatedTags = tagItems.filter { tagItem -> tagItem.tag.name != "tag1" }
        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(updatedTags)
        }

        viewModel.clickDeleteTag(tagItems[0])

        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(updatedTags)
        }
        viewModel.updateOnResult()

        assertEquals(viewModel.uiState.value?.tagItems, updatedTags)
        assertEquals(viewModel.uiState.value?.searchUpdate, false)
        assertNull(viewModel.uiState.value?.searchQuery)
    }

    @Test
    fun longClickDeleteTagShouldTriggerLongDeleteTagEvent() {
        val view = mock<View>()
        viewModel.longClickDeleteTag(view)

        assertEquals(viewModel.event.value, TagsEvent.LongDeleteTagEvent(view))
    }

    @Test
    fun deleteTagUpdatesListOfTags() {
        fakeTagsRepository.stub {
            onBlocking { deleteTag(any()) }.doReturn(Unit)
        }

        fakeTagsRepository.stub {
            onBlocking { tagsChanged() }.doReturn(flow{ emit(true) })
        }

        val updatedTags = tagItems.filter { tagItem -> tagItem.tag.name != "tag3" }
        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(updatedTags)
        }

        viewModel.deleteTag(tagItems[2])

        fakeTagsRepository.stub {
            onBlocking { allTags() }.doReturn(updatedTags)
        }
        viewModel.updateOnResult()

        assertEquals(viewModel.uiState.value?.tagItems, updatedTags)
        assertEquals(viewModel.uiState.value?.searchUpdate, false)
        assertNull(viewModel.uiState.value?.searchQuery)
    }
}
