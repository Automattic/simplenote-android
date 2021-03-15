package com.automattic.simplenote.authentication;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.R;

public class SimplenoteSignupActivity extends AppCompatActivity {
    public final static String SIGNUP_FRAGMENT_TAG = "signup";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        initContainer();
        initToolbar();
    }

    private void initContainer() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SIGNUP_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new SignupFragment();
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment, SIGNUP_FRAGMENT_TAG)
            .commit();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(com.simperium.R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() ==  android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
