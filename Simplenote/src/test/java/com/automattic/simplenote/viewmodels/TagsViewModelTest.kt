package com.automattic.simplenote.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.TagsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.mockito.Mockito.mock


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
}
