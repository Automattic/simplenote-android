package com.automattic.simplenote.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.R;
import com.automattic.simplenote.authentication.magiclink.MagicLinkConfirmationFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SimplenoteSignupActivity extends AppCompatActivity implements SignUpCallback {
    public final static String SIGNUP_FRAGMENT_TAG = "signup";

    // Used to differentiate between sign in and sign up in the sign in activity
    public static final String KEY_IS_LOGIN = "KEY_IS_LOGIN";

    Toolbar mToolbar;

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
            .add(R.id.fragment_container, fragment, SIGNUP_FRAGMENT_TAG)
                .addToBackStack(null)
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
        mToolbar = findViewById(com.simperium.R.id.toolbar);
        if (isSignUp) {
            mToolbar.setTitle(R.string.simperium_button_signup);
        } else {
            mToolbar.setTitle(R.string.login_screen_title);
        }
        setSupportActionBar(mToolbar);

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

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SIGNUP_FRAGMENT_TAG);
        if (fragment instanceof MagicLinkConfirmationFragment) {
            // Old logic doesn't expect a backstack of fragments. This is to fit magic links only.
            super.onBackPressed();
            return;
        }
        // This is weird. But see SimplenoteCredentialsActivity for why this is necessary.
        startActivity(new Intent(this, SimplenoteAuthenticationActivity.class));
        finish();
    }

    @Override
    public void setTitle(String title) {
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        }
    }
}
