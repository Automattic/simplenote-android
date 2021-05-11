package com.automattic.simplenote.viewmodels

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.SimperiumTagsRepository
import com.simperium.client.Bucket

class ViewModelFactory constructor(
        private val tagsBucket: Bucket<Tag>,
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
                AddTagViewModel(SimperiumTagsRepository(tagsBucket))
            else ->
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T
}
