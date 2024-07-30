package com.automattic.simplenote.authentication

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Patterns
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.automattic.simplenote.R
import com.automattic.simplenote.utils.AccountNetworkUtils
import com.automattic.simplenote.utils.AccountVerificationEmailHandler
import com.automattic.simplenote.utils.AppLog
import com.automattic.simplenote.utils.HtmlCompat
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputLayout
import com.simperium.Simperium
import com.simperium.SimperiumNotInitializedException
import com.simperium.android.CredentialsActivity
import com.simperium.android.ProgressDialogFragment
import com.simperium.client.AuthException
import com.simperium.client.AuthException.FailureType
import com.simperium.client.AuthProvider
import com.simperium.client.AuthResponseListener
import com.simperium.client.User
import com.simperium.util.Logger
import com.simperium.util.NetworkUtil
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Pattern

open class NewCredentialsActivity : AppCompatActivity() {
    companion object {
        val PATTERN_NEWLINES_RETURNS_TABS: Pattern = Pattern.compile("[\n\r\t]")
        const val PREF_HIDE_EMAIL_FIELD: String = "pref_hide_email_field"
        const val PASSWORD_LENGTH_LOGIN: Int = 4
        const val PASSWORD_LENGTH_MINIMUM: Int = 8
    }
    private var progressDialogFragment: ProgressDialogFragment? = null
    private var button: AppCompatButton? = null
    private var simperium: Simperium? = null
    private var missingEmailMessage: TextView? = null
    private var inputEmail: TextInputLayout? = null
    private var inputPassword: TextInputLayout? = null
    protected var isLogin: Boolean = false
    private var authListener: AuthResponseListener = object : AuthResponseListener {
        override fun onFailure(user: User, error: AuthException) {
            this@NewCredentialsActivity.runOnUiThread {
                when (error.failureType) {
                    FailureType.EXISTING_ACCOUNT -> showDialogErrorExistingAccount()
                    FailureType.COMPROMISED_PASSWORD -> showCompromisedPasswordDialog()
                    FailureType.UNVERIFIED_ACCOUNT -> showUnverifiedAccountDialog()
                    FailureType.TOO_MANY_REQUESTS -> showDialogError(getString(R.string.simperium_too_many_attempts))
                    FailureType.INVALID_ACCOUNT -> showDialogError(
                        getString(
                            if (isLogin) com.simperium.R.string.simperium_dialog_message_login else com.simperium.R.string.simperium_dialog_message_signup
                        )
                    )
                    else -> showDialogError(
                        getString(
                            if (isLogin) com.simperium.R.string.simperium_dialog_message_login else com.simperium.R.string.simperium_dialog_message_signup
                        )
                    )
                }
                Logger.log(error.message, error)
            }
        }

        override fun onSuccess(user: User, userId: String, token: String, provider: AuthProvider) {
            handleResponseSuccess(user, userId, token, provider)
        }
    }

    override fun onBackPressed() {
        this.startActivity(Intent(this, SimplenoteAuthenticationActivity::class.java))
        finish()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.new_activity_credentials)

        try {
            this.simperium = Simperium.getInstance()
        } catch (e: SimperiumNotInitializedException) {
            Logger.log("Can't create CredentialsActivity", e)
        }

        if (this.intent.extras != null && this.intent.hasExtra("EXTRA_IS_LOGIN")) {
            this.isLogin = this.intent.getBooleanExtra("EXTRA_IS_LOGIN", false)
        }

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.setTitle(if (this.isLogin) R.string.simperium_button_login else R.string.simperium_button_signup)
        this.setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        missingEmailMessage = findViewById(R.id.login_with_password_email_message)
        inputEmail = findViewById<View>(R.id.input_email) as TextInputLayout
        if (inputEmail?.editText != null) {
            if (this.intent.extras != null && this.intent.hasExtra("android.intent.extra.EMAIL")) {
                inputEmail?.editText?.setText(this.intent.getStringExtra("android.intent.extra.EMAIL"))
            }

            inputEmail?.editText?.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    setButtonState()
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }
            })
            inputEmail?.editText?.onFocusChangeListener =
                OnFocusChangeListener { view, hasFocus ->
                    inputEmail?.let {
                        if (!hasFocus &&
                            !isValidEmail(getEditTextString(it))) {
                            inputEmail?.setError(getString(R.string.simperium_error_email))
                        } else {
                            inputEmail?.setError("")
                        }
                    }
                }
        }

        intent?.let {
            val email: String? = intent.getStringExtra("android.intent.extra.EMAIL")
            val hideEmailField = it.getBooleanExtra(PREF_HIDE_EMAIL_FIELD, false)
            if (missingEmailMessage != null && hideEmailField && isValidEmail(email ?: "")) {
                missingEmailMessage?.visibility = View.VISIBLE
                inputEmail?.visibility = View.GONE

                val colorLink: String =
                    Integer.toHexString(MaterialColors.getColor(missingEmailMessage!!, R.attr.onMainBackgroundColor) and 16777215)
                val boldEmail = "<b><font color=\"#${colorLink}\">${email}<font/></b>"
                missingEmailMessage?.text = HtmlCompat.fromHtml(
                    String.format(
                        getString(R.string.login_with_password_message),
                        boldEmail
                    )
                )
            }
        }

        this.inputPassword = findViewById<View>(R.id.input_password) as TextInputLayout
        if (inputPassword!!.editText != null) {
            if (this.intent.extras != null && this.intent.hasExtra("EXTRA_PASSWORD")) {
                inputPassword?.editText?.setText(this.intent.getStringExtra("EXTRA_PASSWORD"))
            }

            inputPassword?.editText?.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    setButtonState()
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }
            })
            inputPassword?.editText?.onFocusChangeListener =
                OnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) {
                        inputPassword?.setError("")
                    } else if (!isValidPasswordLength(isLogin)) {
                        inputPassword?.setError(getString(R.string.simperium_error_password))
                    }
                }
        }

        this.button = findViewById<View>(R.id.button) as AppCompatButton
        button?.setText(
            if (this.isLogin) R.string.simperium_button_login else R.string.simperium_button_signup
        )
        button?.setOnClickListener {
            if (NetworkUtil.isNetworkAvailable(this)) {
                if (isLogin) {
                    startLogin()
                } else {
                    startSignup()
                }
            } else {
                showDialogError(getString(R.string.simperium_dialog_message_network))
            }
        }
        val colorLink = Integer.toHexString(ContextCompat.getColor(this, R.color.text_link) and 16777215)
        val footer = findViewById<View>(R.id.text_footer) as TextView
        footer.text = Html.fromHtml(
            String.format(
                if (this.isLogin) this.resources.getString(R.string.simperium_footer_login) else this.resources.getString(
                    R.string.simperium_footer_signup
                ), "<span style=\"color:#", colorLink, "\">", "</span>"
            )
        )
        footer.setOnClickListener {
            inputEmail?.let {
                val url: String =
                    if (isLogin) getString(
                        R.string.simperium_footer_login_url,
                        arrayOf<Any>(getEditTextString(it))
                    ) else getString(R.string.simperium_footer_signup_url)
                if (isBrowserInstalled()) {
                    startActivity(
                        Intent(
                            "android.intent.action.VIEW",
                            Uri.parse(url)
                        )
                    )
                } else {
                    showDialogErrorBrowser(url)
                }
            }
        }
        this.setButtonState()
        if (savedInstanceState != null) {
            this.setEditTextString(inputEmail, savedInstanceState.getString("STATE_EMAIL", ""))
            this.setEditTextString(inputPassword, savedInstanceState.getString("STATE_PASSWORD", ""))
        }

        if (this.intent.extras != null && this.intent.hasExtra("EXTRA_AUTOMATE_LOGIN") && this.intent.getBooleanExtra(
                "EXTRA_AUTOMATE_LOGIN",
                false
            )
        ) {
            Handler().postDelayed({ startLogin() }, 600L)
        }
    }

    protected fun handleResponseSuccess(user: User, userId: String?, token: String?, provider: AuthProvider) {
        this.runOnUiThread {
            hideDialogProgress()
            val inputMethodManager =
                getSystemService("input_method") as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(button?.windowToken, 0)
            if (isValidPassword(user.email, user.password) && isValidPasswordLength(false)) {
                user.status = User.Status.AUTHORIZED
                user.accessToken = token
                user.userId = userId
                provider.saveUser(user)
                setResult(-1)
                finish()
            } else {
                user.status = User.Status.NOT_AUTHORIZED
                user.accessToken = ""
                user.userId = ""
                provider.saveUser(user)
                showDialogErrorLoginReset()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            this.onBackPressed()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("STATE_EMAIL", this.getEditTextString(inputEmail!!))
        outState.putString("STATE_PASSWORD", this.getEditTextString(inputPassword!!))
    }

    private fun clearPassword() {
        if (inputPassword!!.editText != null) {
            inputPassword!!.editText!!.text.clear()
        }
    }

    private fun copyToClipboard(url: String) {
        val context: Context = ContextThemeWrapper(this, this.theme)

        try {
            val clipboard = this.getSystemService("clipboard") as ClipboardManager
            val clip = ClipData.newPlainText(this.getString(R.string.app_name), url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, R.string.simperium_error_browser_copy_success, Toast.LENGTH_SHORT).show()
        } catch (var5: Exception) {
            Toast.makeText(context, R.string.simperium_error_browser_copy_failure, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getEditTextString(inputLayout: TextInputLayout?): String {
        if (inputLayout == null) return ""
        return if (inputLayout.editText != null) inputLayout.editText!!.text.toString() else ""
    }

    protected fun hideDialogProgress() {
        if (this.progressDialogFragment != null && !progressDialogFragment!!.isHidden) {
            progressDialogFragment!!.dismiss()
            this.progressDialogFragment = null
        }
    }

    private fun isBrowserInstalled(): Boolean {
        val intent = Intent("android.intent.action.VIEW", Uri.parse(this.getString(R.string.simperium_url)))
        return intent.resolveActivity(this.packageManager) != null
    }

    private fun isValidEmail(text: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(text).matches()
    }

    private fun isValidPassword(email: String, password: String): Boolean {
        return this.isValidPasswordLength(this.isLogin) && !PATTERN_NEWLINES_RETURNS_TABS.matcher(password)
            .find() && !email.contentEquals(password)
    }

    private fun isValidPasswordLength(isLogin: Boolean): Boolean {
        return inputPassword!!.editText != null &&
                (if (isLogin) getEditTextString(inputPassword).length >= PASSWORD_LENGTH_LOGIN else getEditTextString(
                    inputPassword
                ).length >= PASSWORD_LENGTH_MINIMUM)
    }

    private fun isValidPasswordLogin(): Boolean {
        return this.isValidPasswordLength(this.isLogin)
    }

    private fun setButtonState() {
        button!!.isEnabled =
            (inputEmail!!.editText != null) && (inputPassword!!.editText != null) && this.isValidEmail(
                this.getEditTextString(
                    inputEmail!!
                )
            ) && this.isValidPasswordLength(this.isLogin)
    }

    private fun setEditTextString(inputLayout: TextInputLayout?, text: String) {
        if (inputLayout?.editText != null) {
            inputLayout.editText?.setText(text)
        }
    }

    protected fun showDialogError(message: String?) {
        this.hideDialogProgress()
        val context: Context = ContextThemeWrapper(this, this.theme)
        AlertDialog.Builder(context).setTitle(R.string.simperium_dialog_title_error).setMessage(message)
            .setPositiveButton(R.string.simperium_okay, null as DialogInterface.OnClickListener?).show()
    }

    protected fun showDialogErrorExistingAccount() {
        this.hideDialogProgress()
        val context: Context = ContextThemeWrapper(this, this.theme)
        AlertDialog.Builder(context).setTitle(R.string.simperium_dialog_title_error)
            .setMessage(R.string.simperium_dialog_message_signup_existing)
            .setNegativeButton(R.string.cancel, null as DialogInterface.OnClickListener?)
            .setPositiveButton(R.string.simperium_button_login
            ) { _, _ ->
                val intent = Intent(this, CredentialsActivity::class.java)
                intent.putExtra("EXTRA_IS_LOGIN", true)
                intent.putExtra(
                    "android.intent.extra.EMAIL",
                    getEditTextString(inputEmail)
                )
                intent.putExtra(
                    "EXTRA_PASSWORD",
                    getEditTextString(inputPassword)
                )
                intent.putExtra("EXTRA_AUTOMATE_LOGIN", true)
                startActivity(intent)
                finish()
            }.show()
    }

    private fun showDialogErrorLoginReset() {
        this.hideDialogProgress()
        val context: Context = ContextThemeWrapper(this, this.theme)
        AlertDialog.Builder(context).setTitle(R.string.simperium_dialog_title_error).setMessage(
            this.getString(R.string.simperium_dialog_message_login_reset, PASSWORD_LENGTH_MINIMUM)
        ).setNegativeButton(R.string.cancel, null as DialogInterface.OnClickListener?)
            .setPositiveButton(R.string.simperium_button_login_reset
            ) { _, _ ->
                try {
                    val url: String =
                        getString(
                            R.string.simperium_dialog_button_reset_url, arrayOf<Any>(
                                URLEncoder.encode(
                                    getEditTextString(inputEmail),
                                    "UTF-8"
                                )
                            )
                        )
                    if (isBrowserInstalled()) {
                        startActivity(Intent("android.intent.action.VIEW", Uri.parse(url)))
                        clearPassword()
                    } else {
                        showDialogErrorBrowser(url)
                    }
                } catch (e: UnsupportedEncodingException) {
                    throw RuntimeException("Unable to parse URL", e)
                }
            }.show()
    }

    fun showUnverifiedAccountDialog() {
        hideDialogProgress()

        val context: Context = ContextThemeWrapper(this, theme)
        AlertDialog.Builder(context)
            .setTitle(com.simperium.R.string.simperium_account_verification)
            .setMessage(com.simperium.R.string.simperium_account_verification_message)
            .setNegativeButton(com.simperium.R.string.simperium_okay, null)
            .setPositiveButton(
                "Resend Verification Email"
            ) { _, _ -> sendVerificationEmail() }
            .show()
    }

    private fun showDialogErrorBrowser(url: String) {
        val context: Context = ContextThemeWrapper(this, this.theme)
        AlertDialog.Builder(context).setTitle(R.string.simperium_dialog_title_error_browser)
            .setMessage(R.string.simperium_error_browser).setNeutralButton(R.string.simperium_dialog_button_copy_url
            ) { _, _ -> copyToClipboard(url) }
            .setPositiveButton(R.string.simperium_okay, null as DialogInterface.OnClickListener?).show()
    }

    protected fun showCompromisedPasswordDialog() {
        this.hideDialogProgress()
        val context: Context = ContextThemeWrapper(this, this.theme)
        AlertDialog.Builder(context).setTitle(R.string.simperium_compromised_password)
            .setMessage(R.string.simperium_compromised_password_message)
            .setNegativeButton(R.string.simperium_not_now, null as DialogInterface.OnClickListener?)
            .setPositiveButton(R.string.simperium_change_password
            ) { _, _ ->
                try {
                    val url: String =
                        getString(
                            R.string.simperium_dialog_button_reset_url, arrayOf<Any>(
                                URLEncoder.encode(
                                    getEditTextString(inputEmail),
                                    "UTF-8"
                                )
                            )
                        )
                    if (isBrowserInstalled()) {
                        startActivity(Intent("android.intent.action.VIEW", Uri.parse(url)))
                        clearPassword()
                    } else {
                        showDialogErrorBrowser(url)
                    }
                } catch (e: UnsupportedEncodingException) {
                    throw RuntimeException("Unable to parse URL", e)
                }
            }.show()
    }

    private fun startLogin() {
        val email = this.getEditTextString(inputEmail!!)
        val password = this.getEditTextString(inputPassword!!)
        if (this.isValidPasswordLogin()) {
            this.progressDialogFragment =
                ProgressDialogFragment.newInstance(this.getString(R.string.simperium_dialog_progress_logging_in))
            progressDialogFragment?.show(this.supportFragmentManager, ProgressDialogFragment.TAG)
            simperium?.authorizeUser(email, password, this.authListener)
        } else {
            this.showDialogError(this.getString(R.string.simperium_dialog_message_password_login, PASSWORD_LENGTH_LOGIN))
        }
    }

    private fun startSignup() {
        val email = this.getEditTextString(inputEmail)
        val password = this.getEditTextString(inputPassword)
        if (this.isValidPassword(email, password)) {
            this.progressDialogFragment = ProgressDialogFragment.newInstance(
                this.getString(R.string.simperium_dialog_progress_signing_up)
            )
            progressDialogFragment?.show(this.supportFragmentManager, ProgressDialogFragment.TAG)
            simperium?.createUser(email, password, this.authListener)
        } else {
            this.showDialogError(this.getString(R.string.simperium_dialog_message_password, PASSWORD_LENGTH_MINIMUM))
        }
    }

    protected fun getEmail(): String = getEditTextString(inputEmail)

    private fun sendVerificationEmail() {
        AccountNetworkUtils.makeSendVerificationEmailRequest(getEmail(), object : AccountVerificationEmailHandler {
            override fun onSuccess(url: String) {
                AppLog.add(AppLog.Type.AUTH, "Email sent (200 - $url)")
            }

            override fun onFailure(e: java.lang.Exception, url: String) {
                AppLog.add(AppLog.Type.AUTH, "Verification email error (" + e.message + " - " + url + ")")
            }
        })
    }
}
