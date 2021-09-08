package com.automattic.simplenote.repositories

import android.text.TextUtils
import androidx.core.util.PatternsCompat.EMAIL_ADDRESS
import javax.inject.Inject

class SimperiumCollaboratorsRepository @Inject constructor() : CollaboratorsRepository {

    /**
     * A valid [collaborator] is just a valid email address.
     *
     * For now we do not make an extra check to see whether there is an account linked to the email or not
     */
    override fun isValidCollaborator(collaborator: String): Boolean {
        return !TextUtils.isEmpty(collaborator) && EMAIL_ADDRESS.matcher(collaborator).matches()
    }
}
