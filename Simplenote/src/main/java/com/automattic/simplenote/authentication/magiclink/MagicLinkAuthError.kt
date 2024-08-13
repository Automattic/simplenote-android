package com.automattic.simplenote.authentication.magiclink

enum class MagicLinkAuthError(val str: String) {
    INVALID_CODE("invalid-code"),
    REQUEST_NOT_FOUND("request-not-found"),
    UNKNOWN_ERROR("")
}
