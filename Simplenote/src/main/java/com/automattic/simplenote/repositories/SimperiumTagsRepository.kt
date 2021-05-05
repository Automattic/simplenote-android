package com.automattic.simplenote.repositories

import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TagUtils
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectNameInvalid

class SimperiumTagsRepository(private val tagsBucket: Bucket<Tag>) : TagsRepository {
    override fun saveTag(tagName: String): Boolean {
        return try {
            TagUtils.createTagIfMissing(tagsBucket, tagName)
            true
        } catch (bucketObjectNameInvalid: BucketObjectNameInvalid) {
            false
        }
    }

    override fun isTagValid(tagName: String): Boolean {
        return TagUtils.hashTagValid(tagName)
    }

    override fun isTagMissing(tagName: String): Boolean {
        return TagUtils.isTagMissing(tagsBucket, tagName)
    }
}
