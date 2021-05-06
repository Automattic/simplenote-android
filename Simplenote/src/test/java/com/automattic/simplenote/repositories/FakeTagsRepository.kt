package com.automattic.simplenote.repositories

import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TagUtils

class FakeTagsRepository : TagsRepository {
    val tags = mutableListOf<Tag>()
    var failAtSave = false

    override fun saveTag(tagName: String): Boolean {
        if (failAtSave) {
            return false
        }

        val hash = TagUtils.hashTag(tagName)
        val tag = Tag(hash)
        tag.name = tagName
        tag.index = tags.size
        tags.add(tag)

        return true
    }

    override fun isTagValid(tagName: String): Boolean {
        return TagUtils.hashTagValid(tagName)
    }

    override fun isTagMissing(tagName: String): Boolean {
        return !tags.any { tag -> tag.name == tagName }
    }

    fun clear() {
        tags.clear()
    }
}
