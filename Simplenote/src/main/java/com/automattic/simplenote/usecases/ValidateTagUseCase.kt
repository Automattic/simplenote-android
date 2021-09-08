package com.automattic.simplenote.usecases

import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import javax.inject.Inject


sealed class TagValidationResult {
    object TagValid : TagValidationResult()
    object TagEmpty : TagValidationResult()
    object TagWithSpaces : TagValidationResult()
    object TagTooLong : TagValidationResult()
    object TagIsCollaborator : TagValidationResult()
    object TagExists : TagValidationResult()

}

class ValidateTagUseCase @Inject constructor(
    private val tagsRepository: TagsRepository,
    private val collaboratorsRepository: CollaboratorsRepository) {

    fun isTagValid(tagName: String): TagValidationResult {
        if (tagName.isEmpty()) {
            return TagValidationResult.TagEmpty
        }

        if (tagName.contains(" ")) {
            return TagValidationResult.TagWithSpaces
        }

        if (!tagsRepository.isTagValid(tagName)) {
            return TagValidationResult.TagTooLong
        }

        if (!tagsRepository.isTagMissing(tagName)) {
            return TagValidationResult.TagExists
        }

        if (collaboratorsRepository.isValidCollaborator(tagName)) {
            return TagValidationResult.TagIsCollaborator
        }


        return TagValidationResult.TagValid
    }
}