package com.automattic.simplenote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class ScopedViewModel(private val defaultDispatcher: CoroutineDispatcher) : ViewModel() {
    fun launch(
            context: CoroutineContext = defaultDispatcher,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(context, start, block)
    }
}
