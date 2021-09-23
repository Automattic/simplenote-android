package com.automattic.simplenote.repositories

import android.util.Log
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.models.TagItem
import com.automattic.simplenote.utils.AppLog
import com.automattic.simplenote.utils.TagUtils
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectNameInvalid
import com.simperium.client.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

@ExperimentalCoroutinesApi
class SimperiumTagsRepository @Inject constructor(
    private val tagsBucket: Bucket<Tag>,
    private val notesBucket: Bucket<Note>,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
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

    override suspend fun deleteTag(tag: Tag) = withContext(ioDispatcher) {
        deleteTagFromNotes(tag)
        tag.delete()
    }

    private fun deleteTagFromNotes(tag: Tag) {
        val cursor = tag.findNotes(notesBucket, tag.name)

        while (cursor.moveToNext()) {
            val note = cursor.getObject()
            note.removeTag(tag.name)
        }

        cursor.close()
    }

    override suspend fun tagsChanged(): Flow<Boolean> = callbackFlow {
        val callbackOnSaveObject = Bucket.OnSaveObjectListener<Tag> { _, _ -> offer(true) }
        val callbackOnDeleteObject = Bucket.OnDeleteObjectListener<Tag> { _, _ -> offer(true) }
        val callbackOnNetworkChange = Bucket.OnNetworkChangeListener<Tag> { _, _, _ -> offer(true) }

        tagsBucket.addOnSaveObjectListener(callbackOnSaveObject)
        tagsBucket.addOnDeleteObjectListener(callbackOnDeleteObject)
        tagsBucket.addOnNetworkChangeListener(callbackOnNetworkChange)
        AppLog.add(AppLog.Type.SYNC, "Added tag bucket listener (TagsActivity)")

        awaitClose {
            tagsBucket.removeOnSaveObjectListener(callbackOnSaveObject)
            tagsBucket.removeOnDeleteObjectListener(callbackOnDeleteObject)
            tagsBucket.removeOnNetworkChangeListener(callbackOnNetworkChange)
            AppLog.add(AppLog.Type.SYNC, "Removed tag bucket listener (TagsActivity)")
        }
    }.flowOn(ioDispatcher)

    override suspend fun allTags(): List<TagItem> = withContext(ioDispatcher) {
        val tagQuery = Tag.all(tagsBucket).reorder().orderByKey().include(Tag.NOTE_COUNT_INDEX_NAME)
        val cursor = tagQuery.execute()

        return@withContext cursorToTagItems(cursor)
    }

    override suspend fun searchTags(query: String): List<TagItem> = withContext(ioDispatcher) {
        val tags = Tag.all(tagsBucket)
                .where(Tag.NAME_PROPERTY, Query.ComparisonType.LIKE, "%$query%")
                .orderByKey().include(Tag.NOTE_COUNT_INDEX_NAME)
                .reorder()
        val cursor = tags.execute()

        return@withContext cursorToTagItems(cursor)
    }

    private fun cursorToTagItems(cursor: Bucket.ObjectCursor<Tag>): List<TagItem> {
        return (1 .. cursor.count).map {
            cursor.moveToNext()
            val tag = cursor.`object`
            val noteCount: Int = notesBucket
                    .query()
                    .where(Note.TAGS_PROPERTY, Query.ComparisonType.EQUAL_TO, tag.name)
                    .count()
            TagItem(tag, noteCount)
        }
    }
}
