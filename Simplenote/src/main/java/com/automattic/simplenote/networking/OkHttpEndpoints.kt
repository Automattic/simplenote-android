package com.automattic.simplenote.networking

object OkHttpEndpoints {
    const val PASSKEY_REQUEST_CHALLENGE = "api2/login"
    const val PASSKEY_ADD_CREDENTIAL = "webauthn/register/verify"
    const val PASSKEY_PREPARE_AUTH_CHALLENGE = "webauthn/authenticate/start"
    const val PASSKEY_DISCOVERABLE_CHALLENGE = "webauthn/authenticate/discoverable/start"
    const val PASSKEY_VERIFY_LOGIN_CREDENTIAL = "webauthn/authenticate/verify"
}
