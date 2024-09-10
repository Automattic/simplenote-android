package com.automattic.simplenote.widgets

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

private const val SAMSUNG = "samsung"

class CustomNestedScrollView(context: Context, attrs: AttributeSet? = null) : NestedScrollView(context, attrs) {
    override fun requestChildRectangleOnScreen(child: View, rectangle: Rect?, immediate: Boolean): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer == SAMSUNG && Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            // First, we want to gate this logic to only the affected users. Those being samsung
            // devices running Android 13. Ok, but why? Based on my testing the issue comes from [Rect?] being
            // the incorrect top and bottom values. This only happens when the focus changes. Normally as you
            // type and move the cursor around this is called, which is then used by NestedScrollView to position
            // the EditText in the proper Y position. During my testing, scrollY seems to be accurate all the time.
            rectangle?.top = scrollY
        }
        return super.requestChildRectangleOnScreen(child, rectangle, immediate)
    }
}
