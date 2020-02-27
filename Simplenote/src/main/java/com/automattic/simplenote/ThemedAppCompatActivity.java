package com.automattic.simplenote;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Abstract class to apply {@link R.style#Theme_Simplestyle} theme to activities extending from it.
 * Override {@link ThemedAppCompatActivity#mThemeId} in extended activity to apply another theme.
 */
abstract public class ThemedAppCompatActivity extends AppCompatActivity {
    protected @StyleRes int mThemeId = R.style.Theme_Simplestyle;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(mThemeId);
    }
}
