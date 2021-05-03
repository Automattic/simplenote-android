package com.automattic.simplenote.viewmodels

import androidx.lifecycle.MutableLiveData
import com.automattic.simplenote.R
import com.automattic.simplenote.models.Tag
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

    private val _tagError = MutableLiveData<Int?>()
    val tagError = _tagError

    private val _isResultOK = MutableLiveData<Boolean>()
    val isResultOK = _isResultOK

    fun validateTag(tagName: String) {
        if (tagName.isEmpty()) {
            _tagError.value = R.string.tag_error_empty
            return
        }

        if (tagName.contains(" ")) {
            _tagError.value = R.string.tag_error_spaces
            return
        }

        if (!TagUtils.hashTagValid(tagName)) {
            _tagError.value = R.string.tag_error_length
            return
        }

        if (!TagUtils.isTagMissing(tagsBucket, tagName)) {
            _tagError.value = R.string.tag_error_exists
            return
        }

        _tagError.value = null
    }

    fun saveTag(tagName: String) {
        try {
            _showKeyboard.value = false

            TagUtils.createTagIfMissing(tagsBucket, tagName)
            _isResultOK.value = true
        } catch (bucketObjectNameInvalid: BucketObjectNameInvalid) {
            _isResultOK.value = false
        }
    }

    fun showKeyboard() {
        _showKeyboard.value = true
    }
}
