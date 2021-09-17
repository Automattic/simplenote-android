package com.automattic.simplenote.usecases

import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.repositories.TagsRepository
import javax.inject.Inject

/**
 * Since the collaborators are saved as tags, those need to be filtered before returning the tags.
 */
class GetTagsUseCase @Inject constructor(
    private val tagsRepository: TagsRepository,
    private val collaboratorsRepository: CollaboratorsRepository
) {

    suspend fun allTags(): List<TagItem> {
        return tagsRepository.allTags()
            .filter { tagItem -> !collaboratorsRepository.isValidCollaborator(tagItem.tag.name) }
    }

    fun getTags(note: Note): List<String> {
        //return note.tags.filter { tag -> !collaboratorsRepository.isValidCollaborator(tag) }
        // Small patch until we launch collaborator UI
        return note.tags
    }
}
