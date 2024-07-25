package com.automattic.simplenote.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.automattic.simplenote.R;
import com.automattic.simplenote.authentication.magiclink.MagicLinkConfirmationFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SimplenoteSignupActivity extends AppCompatActivity {
    public final static String SIGNUP_FRAGMENT_TAG = "signup";

    // Used to differentiate between sign in and sign up in the sign in activity
    public static final String KEY_IS_LOGIN = "KEY_IS_LOGIN";

    Toolbar mToolbar;

    FragmentManager.OnBackStackChangedListener mBackstackListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            final Fragment fragment = getSupportFragmentManager().findFragmentByTag(SIGNUP_FRAGMENT_TAG);
            if (fragment instanceof SignInFragment) {
                mToolbar.setTitle(R.string.login_screen_title);
            } else if (fragment instanceof SignupFragment) {
                mToolbar.setTitle(R.string.simperium_button_signup);
            } else if (fragment instanceof MagicLinkConfirmationFragment) {
                mToolbar.setTitle(R.string.magic_link_enter_code_title);
            }
        }
    };

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
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, SIGNUP_FRAGMENT_TAG)
                    .commit();
        }
    }

    private Fragment createFragment(final boolean isSignUp) {
        if (isSignUp) {
            return new SignupFragment();
        } else {
            return new SignInFragment();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSupportFragmentManager().removeOnBackStackChangedListener(mBackstackListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().addOnBackStackChangedListener(mBackstackListener);
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
}
