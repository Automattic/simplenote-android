package com.automattic.simplenote.uipages

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector

open class BasePage {

    private var uiSelector: UiSelector? = null
    private var uiDevice: UiDevice? = null

    internal fun getUISelectorInstance(): UiSelector {
        if (uiSelector == null) {
            uiSelector = UiSelector()
        }
        return uiSelector as UiSelector
    }

    internal fun getUIDeviceInstance(): UiDevice {
        if (uiDevice == null) {
            uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        }
        return uiDevice as UiDevice
    }
}
