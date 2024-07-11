package com.automattic.simplenote.authentication.passkey

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.viewmodels.PasskeyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TAG = "PasskeyManager"

// Helper class to extract the credential manager work. Also, caller is in Java, and we need kotlin.
object PasskeyManager {
    fun createCredential(context: Context, jsonChallenge: String, viewmodel: PasskeyViewModel) {
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            requestJson = jsonChallenge,
            preferImmediatelyAvailableCredentials = true
        )
        val credentialManager = CredentialManager.create(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = credentialManager.createCredential(context, createPublicKeyCredentialRequest)
                withContext(Dispatchers.Main) {
                    val simpleNote = context.applicationContext as Simplenote
                    viewmodel.handleCreateCredentialResponse(simpleNote.userEmail, result)
                }
            } catch (e: CreateCredentialException) {
                viewmodel.handleCreateCredentialException(e)
            }
        }
    }
}
