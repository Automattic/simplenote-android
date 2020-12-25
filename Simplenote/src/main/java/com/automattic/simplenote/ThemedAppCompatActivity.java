package com.automattic.simplenote;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;

/**
 * Abstract class to apply theme based on {@link PrefUtils#PREF_STYLE_INDEX}
 * to any {@link AppCompatActivity} that extends it.
 */
abstract public class ThemedAppCompatActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getStyle(ThemedAppCompatActivity.this));
    }
}
