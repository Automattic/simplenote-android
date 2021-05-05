package com.automattic.simplenote.repositories

interface TagsRepository {
    fun saveTag(tagName: String): Boolean
    fun isTagValid(tagName: String): Boolean
    fun isTagMissing(tagName: String): Boolean
}
