package com.automattic.simplenote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;

/**
 * Abstract class to apply theme based on {@link PrefUtils#PREF_STYLE_INDEX}
 * to any {@link AppCompatActivity} that extends it.
 */
abstract public class ThemedAppCompatActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Boolean mThemeChanged = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getStyle(this));
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mThemeChanged) {
            recreate();
            mThemeChanged = false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefUtils.PREF_THEME) || key.equals(PrefUtils.PREF_STYLE_INDEX)) {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                recreate();
            } else {
                mThemeChanged = true;
            }
        }
    }

    @Override
    public void recreate() {
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }
}
