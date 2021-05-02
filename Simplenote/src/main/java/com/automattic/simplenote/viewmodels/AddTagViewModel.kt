package com.automattic.simplenote.viewmodels

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.automattic.simplenote.R
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.DisplayUtils
import com.automattic.simplenote.utils.TagUtils
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectNameInvalid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AddTagViewModel(
        private val tagsBucket: Bucket<Tag>,
        private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ScopedViewModel(defaultDispatcher) {

    private val _showKeyboard = MutableLiveData<Boolean>()
    val showKeyboard = _showKeyboard

    private val _enableSaveButton = MutableLiveData(false)
    val enableSaveButton = _enableSaveButton

    private val _tagError = MutableLiveData<Int?>()
    val tagError = _tagError

    fun validateTag(tagName: String) {
        if (tagName.isEmpty()) {
            _enableSaveButton.value = false
            _tagError.value = R.string.tag_error_empty
            return
        }

        if (tagName.contains(" ")) {
            _enableSaveButton.value = false
            _tagError.value = R.string.tag_error_spaces
            return
        }

        if (!TagUtils.hashTagValid(tagName)) {
            _enableSaveButton.value = false
            _tagError.value = R.string.tag_error_length
            return
        }

        if (!TagUtils.isTagMissing(tagsBucket, tagName)) {
            _enableSaveButton.value = false
            _tagError.value = R.string.tag_error_exists
            return
        }

        _enableSaveButton.value = true
        _tagError.value = null
    }

    fun saveTag(tagName: String): Boolean {
        try {
            _showKeyboard.value = false

            TagUtils.createTagIfMissing(tagsBucket, tagName)
            return true
        } catch (bucketObjectNameInvalid: BucketObjectNameInvalid) {
            return false
        }
    }

    fun showKeyboard() {
        _showKeyboard.value = true
    }
}
