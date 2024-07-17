package com.automattic.simplenote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.automattic.simplenote.authentication.SimplenoteAuthenticationActivity
import com.automattic.simplenote.utils.AuthUtils
import com.automattic.simplenote.utils.IntentUtils
import net.openid.appauth.RedirectUriReceiverActivity
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets

const val TAG = "DeepLinkActivity"

class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        when (uri?.host) {
            AUTHENTICATION_SCHEME -> {
                val intent = Intent(this, RedirectUriReceiverActivity::class.java)
                intent.setData(uri)
                startActivity(intent)
            }
            VERIFIED_WEB_SCHEME -> {
                // New MagicLink
                startMagicLinkConfirmation(uri)
            }
            LOGIN_SCHEME -> {
                if (queryParamContainsData(uri.query, USERNAME_KEY_QUERY) && queryParamContainsData(uri.query, AUTH_CODE_QUERY)) {
                    startMagicLinkConfirmation(uri)
                } else {
                    val intent = IntentUtils.maybeAliasedIntent(applicationContext)
                    val email = AuthUtils.extractEmailFromMagicLink(uri)
                    val app = application as Simplenote
                    if (app.isLoggedIn &&
                        email.lowercase() != app.userEmail.lowercase()
                    ) {
                        intent.putExtra(NotesActivity.KEY_ALREADY_LOGGED_IN, true)
                    } else {
                        AuthUtils.magicLinkLogin(application as Simplenote, uri)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        }
        finish()
    }

    private fun startMagicLinkConfirmation(uri: Uri?) {
        val app = application as Simplenote
        if (app.isLoggedIn) {
            intent.putExtra(NotesActivity.KEY_ALREADY_LOGGED_IN, true)
            val intent = IntentUtils.maybeAliasedIntent(applicationContext)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }
        val base64Username = uri?.getQueryParameter(USERNAME_KEY_QUERY)
        var decodedUsername: String? = null
        try {
            decodedUsername = String(Base64.decode(base64Username, Base64.DEFAULT), StandardCharsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Problem decoding base64 username", e)
        }
        val authCode = uri?.getQueryParameter(AUTH_CODE_QUERY)
        if (!decodedUsername.isNullOrBlank() && !authCode.isNullOrBlank()) {

            val intent = Intent(this, SimplenoteAuthenticationActivity::class.java)
            intent.putExtra(SimplenoteAuthenticationActivity.KEY_IS_MAGIC_LINK, true);
            intent.putExtra(SimplenoteAuthenticationActivity.KEY_MAGIC_LINK_AUTH_KEY, decodedUsername)
            intent.putExtra(SimplenoteAuthenticationActivity.KEY_MAGIC_LINK_AUTH_CODE, authCode)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                getString(R.string.magic_link_complete_login_error_message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun queryParamContainsData(path: String?, otherString: String) : Boolean = path?.contains(otherString, true) == true



    companion object {
        private const val AUTHENTICATION_SCHEME = "auth"
        private const val LOGIN_SCHEME = "login"
        private const val VERIFIED_WEB_SCHEME = "app.simplenote.com"

        private const val USERNAME_KEY_QUERY = "email"
        private const val AUTH_CODE_QUERY = "auth_code"
    }
}
