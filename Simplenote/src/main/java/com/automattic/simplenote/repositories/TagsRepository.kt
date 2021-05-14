package com.automattic.simplenote.repositories

import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import kotlinx.coroutines.flow.Flow

interface TagsRepository {
    fun saveTag(tagName: String): Boolean
    fun isTagValid(tagName: String): Boolean
    fun isTagMissing(tagName: String): Boolean
    fun isTagConflict(tagName: String, oldTagName: String): Boolean
    fun getCanonicalTagName(tagName: String): String
    fun renameTag(tagName: String, oldTag: Tag): Boolean
    suspend fun allTags(): List<TagItem>
    suspend fun searchTags(query: String): List<TagItem>
    suspend fun deleteTag(tag: Tag)
    suspend fun tagsChanged(): Flow<Boolean>
}
