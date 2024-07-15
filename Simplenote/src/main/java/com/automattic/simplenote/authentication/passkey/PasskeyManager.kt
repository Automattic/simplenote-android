package com.automattic.simplenote.authentication.passkey

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
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

    fun getCredential(context: Context, username: String, jsonChallenge: String, viewmodel: PasskeyViewModel) {
        val getCredRequest = GetCredentialRequest(
            listOf(
                GetPublicKeyCredentialOption(requestJson = jsonChallenge)
            )
        )
        val credentialManager = CredentialManager.create(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = credentialManager.getCredential(context, getCredRequest)
                withContext(Dispatchers.Main) {
                    viewmodel.verifyAuthChallenge(username, result)
                }
            } catch (e: GetCredentialException) {
                viewmodel.handleGetCredentialException(e)
            }
        }
    }
}
