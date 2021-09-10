package com.automattic.simplenote.usecases

import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.automattic.simplenote.utils.StrUtils.SPACE_STRING
import javax.inject.Inject

class ValidateTagUseCase @Inject constructor(
    private val tagsRepository: TagsRepository,
    private val collaboratorsRepository: CollaboratorsRepository) {

    fun isTagValid(tagName: String) = when {
        tagName.isEmpty() -> TagValidationResult.TagEmpty
        tagName.contains(SPACE_STRING) -> TagValidationResult.TagWithSpaces
        collaboratorsRepository.isValidCollaborator(tagName) -> TagValidationResult.TagIsCollaborator
        !tagsRepository.isTagValid(tagName) -> TagValidationResult.TagTooLong
        !tagsRepository.isTagMissing(tagName) -> TagValidationResult.TagExists
        else -> TagValidationResult.TagValid
    }

    sealed class TagValidationResult {
        object TagValid : TagValidationResult()
        object TagEmpty : TagValidationResult()
        object TagWithSpaces : TagValidationResult()
        object TagTooLong : TagValidationResult()
        object TagIsCollaborator : TagValidationResult()
        object TagExists : TagValidationResult()
    }
}
