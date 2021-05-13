package com.automattic.simplenote.repositories

import android.util.Log
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TagUtils
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectNameInvalid

class SimperiumTagsRepository(
        private val tagsBucket: Bucket<Tag>,
        private val notesBucket: Bucket<Note>
) : TagsRepository {
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

    override fun isTagConflict(tagName: String, oldTagName: String): Boolean {
        val isRenamingToLexical = TagUtils.hashTag(tagName).equals(TagUtils.hashTag(oldTagName))
        return !isRenamingToLexical && !isTagMissing(tagName)
    }

    override fun getCanonicalTagName(tagName: String): String {
        return TagUtils.getCanonicalFromLexical(tagsBucket, tagName)
    }

    override fun renameTag(tagName: String, oldTag: Tag): Boolean {
        return try {
            val index = if (oldTag.hasIndex()) oldTag.index else tagsBucket.count()
            oldTag.renameTo(oldTag.name, tagName, index, notesBucket)
            true
        } catch (e: BucketObjectNameInvalid) {
            Log.e(Simplenote.TAG, "Unable to rename tag", e)
            false
        }
    }
}
