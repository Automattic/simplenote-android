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
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
class GetTagsUseCaseTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private val tagsRepository: TagsRepository = mock(TagsRepository::class.java)
    private val mockBucket: Bucket<*> = mock(Bucket::class.java)
    private val collaboratorsRepository: CollaboratorsRepository = SimperiumCollaboratorsRepository()
    private val getTagsUseCase: GetTagsUseCase = GetTagsUseCase(tagsRepository, collaboratorsRepository)
    private val tagItems = listOf(
        TagItem(Tag("tag1"), 0),
        TagItem(Tag("tag2"), 2),
        TagItem(Tag("tag3"), 5),
        TagItem(Tag("name@example.co.jp"), 2),
        TagItem(Tag("name1@test.com"), 0),
        TagItem(Tag("name@test"), 1),
        TagItem(Tag("あいうえお@example.com"), 1),
    )

    @Before
    fun setup() {
        // Set mock bucket to avoid NPE
        tagItems.forEach { it.tag.bucket = mockBucket }
    }

    @Test
    fun allTagsShouldFilterCollaborators() = runBlockingTest {
        tagsRepository.stub { onBlocking { allTags() }.doReturn(tagItems) }
        val tagItemsExpected = tagItems.toMutableList()
        tagItemsExpected.removeAt(3) // TagItem(Tag("name@example.co.jp"), 2)
        tagItemsExpected.removeAt(3) // TagItem(Tag("name1@test.com"), 0)

        val tagItemsResult = getTagsUseCase.allTags()

        assertEquals(tagItemsExpected, tagItemsResult)
    }

    @Ignore("Patch for code freeze")
    @Test
    fun tagsForNoteShouldFilterCollaborators() {
        val note = Note("key1")
        note.content = "Hello World"
        note.tags = listOf("tag1", "tag2", "name@example.co.jp", "name@test")

        val expected = listOf("tag1", "tag2", "name@test")
        val result = getTagsUseCase.getTags(note)

        assertEquals(expected, result)
    }
}
