package com.automattic.simplenote.viewmodels

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.SimperiumTagsRepository
import com.simperium.client.Bucket

class ViewModelFactory constructor(
        private val tagsBucket: Bucket<Tag>,
        private val notesBucket: Bucket<Note>,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
    ) = with(modelClass) {
        when {
            isAssignableFrom(AddTagViewModel::class.java) ->
                AddTagViewModel(SimperiumTagsRepository(tagsBucket, notesBucket))
            isAssignableFrom(TagDialogViewModel::class.java) ->
                TagDialogViewModel(SimperiumTagsRepository(tagsBucket, notesBucket))
            isAssignableFrom(TagsViewModel::class.java) ->
                TagsViewModel(SimperiumTagsRepository(tagsBucket, notesBucket))
            else ->
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T
}
