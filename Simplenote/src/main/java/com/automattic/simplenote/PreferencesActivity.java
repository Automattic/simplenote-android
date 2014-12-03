package com.automattic.simplenote;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.PreferencesFragment;

public class PreferencesActivity extends ActionBarActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        // if a new theme was picked, activity is recreated with theme changed intent
        // set result to notify the calling activity once this activity is complete
        if (ThemeUtils.themeWasChanged(getIntent()))
            setResult(RESULT_OK, getIntent());

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            PreferencesFragment preferencesFragment = new PreferencesFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.preferences_container, preferencesFragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
