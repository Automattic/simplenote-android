package com.automattic.simplenote.usecases

import com.automattic.simplenote.models.Note
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.simperium.client.Bucket
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class ValidateTagUseCaseTest {
    private val notesBucket = Mockito.mock(Bucket::class.java) as Bucket<Note>
    private val tagsRepository: TagsRepository = Mockito.mock(TagsRepository::class.java)

    private lateinit var validateTagUseCase: ValidateTagUseCase

    @Before
    fun setup() {
        val collaboratorsRepository = SimperiumCollaboratorsRepository(notesBucket)
        validateTagUseCase = ValidateTagUseCase(tagsRepository, collaboratorsRepository)
    }

    @Test
    fun emptyTagShouldReturnTagEmpty() {
        val result = validateTagUseCase.isTagValid("")
        assertEquals(TagValidationResult.TagEmpty, result)
    }

    @Test
    fun tagWithSpaceShouldReturnTagWithSpaces() {
        val result = validateTagUseCase.isTagValid("tag space")
        assertEquals(TagValidationResult.TagWithSpaces, result)
    }

    @Test
    fun tagWithLongNameShouldReturnTagTooLong() {
        whenever(tagsRepository.isTagValid(any())).thenReturn(false)

        val result = validateTagUseCase.isTagValid("taglong")
        assertEquals(TagValidationResult.TagTooLong, result)
    }

    @Test
    fun tagAlreadyInDatabaseShouldReturnTagExists() {
        whenever(tagsRepository.isTagValid(any())).thenReturn(true)
        whenever(tagsRepository.isTagMissing(any())).thenReturn(false)

        val result = validateTagUseCase.isTagValid("taglong")
        assertEquals(TagValidationResult.TagExists, result)
    }

    @Test
    fun validTagShouldReturnTagValid() {
        whenever(tagsRepository.isTagValid(any())).thenReturn(true)
        whenever(tagsRepository.isTagMissing(any())).thenReturn(true)

        val result = validateTagUseCase.isTagValid("tag1")
        assertEquals(TagValidationResult.TagValid, result)
    }

    @Test
    fun emailShouldReturnTagIsCollaborator() {
        whenever(tagsRepository.isTagValid(any())).thenReturn(true)
        whenever(tagsRepository.isTagMissing(any())).thenReturn(true)

        val result = validateTagUseCase.isTagValid("admin@test.com")
        assertEquals(TagValidationResult.TagIsCollaborator, result)
    }

    @Test
    fun emailAlreadyExistsAsTagShouldReturnTagIsCollaborator() {
        whenever(tagsRepository.isTagValid(any())).thenReturn(true)
        whenever(tagsRepository.isTagMissing(any())).thenReturn(false)

        val result = validateTagUseCase.isTagValid("admin@test.com")
        assertEquals(TagValidationResult.TagIsCollaborator, result)
    }
}
