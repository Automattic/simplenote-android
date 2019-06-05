package com.automattic.simplenote;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.automattic.simplenote.utils.DrawableUtils;

/**
 * Created by Ondrej Ruttkay on 24/03/2016.
 */
public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        setTitle("");

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && toolbar != null) {
            toolbar.setElevation(0);
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(DrawableUtils.tintDrawableWithResource(this,
                    R.drawable.ic_action_remove_24dp, android.R.color.white));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
