package com.automattic.simplenote;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Abstract class to apply default app theme to all activities that extend from it.
 * Applies `@style/Theme.Simplestyle` theme. Override `themeId` to apply a different theme.
 */
abstract public class ThemedAppCompatActivity extends AppCompatActivity {
    
    // Theme for activity. Override in activity to apply a different theme
    protected @StyleRes int themeId = R.style.Theme_Simplestyle;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTheme(themeId);
    }
}
