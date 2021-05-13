package com.automattic.simplenote.repositories

import com.automattic.simplenote.models.Tag

interface TagsRepository {
    fun saveTag(tagName: String): Boolean
    fun isTagValid(tagName: String): Boolean
    fun isTagMissing(tagName: String): Boolean
    fun isTagConflict(tagName: String, oldTagName: String): Boolean
    fun getCanonicalTagName(tagName: String): String
    fun renameTag(tagName: String, oldTag: Tag): Boolean
}
