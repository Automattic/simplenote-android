package com.automattic.simplenote

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry

open class BaseUITest {
    protected fun getResourceString(id: Int): String? {
        val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        return targetContext.resources.getString(id)
    }
}
