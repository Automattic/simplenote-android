package com.automattic.simplenote.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.simperium.client.Bucket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
class GetTagsUseCaseTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private val notesBucket = mock(Bucket::class.java) as Bucket<Note>
    private val tagsRepository: TagsRepository = mock(TagsRepository::class.java)
    private val collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket, TestCoroutineDispatcher())
    private val getTagsUseCase: GetTagsUseCase = GetTagsUseCase(tagsRepository, collaboratorsRepository)
    private val tagItems = listOf(
        TagItem(Tag("tag1"), 0),
        TagItem(Tag("tag2"), 2),
        TagItem(Tag("tag3"), 5),
        TagItem(Tag("name@example.co.jp"), 2),
        TagItem(Tag("tag@test.com"), 0),
        TagItem(Tag("name@test"), 1),
        TagItem(Tag("あいうえお@example.com"), 1),
    )

    @Before
    fun setup() {
        // Set mock bucket to avoid NPE
        tagItems.forEach { it.tag.bucket = notesBucket }
    }

    @Test
    fun allTagsShouldFilterCollaborators() = runBlockingTest {
        tagsRepository.stub { onBlocking { allTags() }.doReturn(tagItems) }


        val tagItemsResult = getTagsUseCase.allTags()

        val tagItemsExpected = tagItems.toMutableList()
        tagItemsExpected.removeAt(3) // TagItem(Tag("name@example.co.jp"), 2)
        tagItemsExpected.removeAt(3) // TagItem(Tag("tag@test.com"), 0)
        assertEquals(tagItemsExpected, tagItemsResult)
    }

    @Test
    fun searchTagsShouldFilterCollaborators() = runBlockingTest {
        val searchTagItems = listOf(
            TagItem(Tag("tag1"), 0),
            TagItem(Tag("tag2"), 2),
            TagItem(Tag("tag3"), 5),
            TagItem(Tag("tag@test.com"), 0),
        )
        tagsRepository.stub { onBlocking { searchTags(any()) }.doReturn(searchTagItems) }

        val tagItemsResult = getTagsUseCase.searchTags("tag")

        val tagItemsExpected = searchTagItems.toMutableList()
        tagItemsExpected.removeAt(3) // TagItem(Tag("tag@test.com"), 0)
        assertEquals(tagItemsExpected, tagItemsResult)
    }

    @Test
    fun tagsForNoteShouldFilterCollaborators() {
        val note = Note("key1")
        note.content = "Hello World"
        note.tags = listOf("tag1", "tag2", "name@example.co.jp", "name@test")

        val result = getTagsUseCase.getTags(note)

        val expected = listOf("tag1", "tag2", "name@test")
        assertEquals(expected, result)
    }
}
