package com.automattic.simplenote.uipages

import androidx.test.uiautomator.UiObject

const val LOGINPAGE_LOGINBUTTON: String = "com.automattic.simplenote.debug:id/button"
const val FORGOT_PASSWORD_LINK: String = "com.automattic.simplenote.debug:id/text_footer"
const val PASSWORD_HIDE_IMAGE: String = "com.automattic.simplenote.debug:id/text_input_end_icon"

class LoginPage: BasePage() {
    val loginPageLoginButton: UiObject = getUIDeviceInstance().findObject(getUISelectorInstance().resourceId(LOGINPAGE_LOGINBUTTON))

}
