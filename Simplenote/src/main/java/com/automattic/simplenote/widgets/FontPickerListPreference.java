package com.automattic.simplenote.widgets;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Dialog picker that allows for a custom font. Used for the Font Name selection in Preferences.
 */
@SuppressWarnings("unused")
public class FontPickerListPreference extends ListPreference {

    public FontPickerListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FontPickerListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FontPickerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FontPickerListPreference(Context context) {
        super(context);
    }

    public void savePreference(String value, String summary) {
        persistString(value);
        setSummary(summary);
    }

    public String getPreference() {
        return getPersistedString("0");
    }
}
