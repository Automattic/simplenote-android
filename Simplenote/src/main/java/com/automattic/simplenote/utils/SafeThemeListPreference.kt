package com.automattic.simplenote.utils

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference

/**
 * Custom ListPreference that handles invalid index values to prevent ArrayIndexOutOfBoundsException.
 * This preference safely handles cases where the stored value doesn't match any of the available entries.
 */
class SafeThemeListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun getSummary(): CharSequence? {
        val value = getValue()
        val entries = getEntries()
        
        if (entries == null || entries.isEmpty()) {
            return super.getSummary()
        }
        
        val index = findIndexOfValue(value)
        
        // If index is invalid (out of bounds or -1), fallback to default value
        return if (index >= 0 && index < entries.size) {
            entries[index]
        } else {
            // Get the default value (2 = theme_system) and return its entry
            val defaultIndex = findIndexOfValue("2")
            if (defaultIndex >= 0 && defaultIndex < entries.size) {
                entries[defaultIndex]
            } else {
                // Ultimate fallback - return the first entry
                entries[0]
            }
        }
    }
}