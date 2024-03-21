package com.automattic.simplenote.uipages

import androidx.test.uiautomator.UiObject

const val SIGNUP_BUTTON: String = "com.automattic.simplenote.debug:id/button_signup"
const val LOGIN_BUTTON: String = "com.automattic.simplenote.debug:id/button_login"
const val LOGIN_EMAIL: String = "com.automattic.simplenote.debug:id/button_email"
const val LOGIN_OTHER: String = "com.automattic.simplenote.debug:id/button_other"

class LaunchPage: BasePage() {

    val signupButton: UiObject = getUIDeviceInstance().findObject(getUISelectorInstance().resourceId(SIGNUP_BUTTON))
    val loginButton: UiObject = getUIDeviceInstance().findObject(getUISelectorInstance().resourceId(LOGIN_BUTTON))
    val loginEmailButton: UiObject = getUIDeviceInstance().findObject(getUISelectorInstance().resourceId(LOGIN_EMAIL))
    val loginOtherButton: UiObject = getUIDeviceInstance().findObject(getUISelectorInstance().resourceId(LOGIN_OTHER))

}
