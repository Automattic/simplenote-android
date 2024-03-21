package com.automattic.simplenote.uipages

import androidx.test.uiautomator.UiObject

const val SEARCH_ICON: String = "com.automattic.simplenote.debug:id/menu_search"

class AllNotesHomePage: BasePage() {
    val searchIcon: UiObject = getUIDeviceInstance().findObject(getUISelectorInstance().resourceId(SEARCH_ICON))
}
