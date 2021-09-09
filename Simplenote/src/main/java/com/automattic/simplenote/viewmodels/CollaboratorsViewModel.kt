package com.automattic.simplenote.viewmodels

import androidx.lifecycle.ViewModel
import com.automattic.simplenote.repositories.CollaboratorsRepository
import javax.inject.Inject

class CollaboratorsViewModel @Inject constructor(
    private val collaboratorsRepository: CollaboratorsRepository
) : ViewModel() {
}