package com.automattic.simplenote.authentication;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SimplenoteSignupActivity extends AppCompatActivity {
    public final static String SIGNUP_FRAGMENT_TAG = "signup";

    // Used to differentiate between sign in and sign up in the sign in activity
    public static final String KEY_IS_LOGIN = "KEY_IS_LOGIN";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        final boolean isSignUp = !getIntent().getBooleanExtra(KEY_IS_LOGIN, false);
        initContainer(isSignUp);
        initToolbar(isSignUp);
    }

    private void initContainer(final boolean isSignUp) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SIGNUP_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = createFragment(isSignUp);
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment, SIGNUP_FRAGMENT_TAG)
            .commit();
    }

    private Fragment createFragment(final boolean isSignUp) {
        if (isSignUp) {
            return new SignupFragment();
        } else {
            return new SignInFragment();
        }
    }

    private void initToolbar(final boolean isSignUp) {
        Toolbar toolbar = findViewById(com.simperium.R.id.toolbar);
        if (isSignUp) {
            toolbar.setTitle(R.string.simperium_button_signup);
        } else {
            toolbar.setTitle(R.string.login_screen_title);
        }
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
