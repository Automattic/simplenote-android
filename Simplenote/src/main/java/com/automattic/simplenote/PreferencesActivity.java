package com.automattic.simplenote;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;

import com.automattic.simplenote.billing.SubscriptionBottomSheetDialog;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.viewmodels.IapViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.passcodelock.PasscodePreferenceFragment;
import org.wordpress.passcodelock.PasscodePreferenceFragmentCompat;

import static com.automattic.simplenote.PreferencesFragment.WEB_APP_URL;
import static com.automattic.simplenote.models.Preferences.PREFERENCES_OBJECT_KEY;
import static com.automattic.simplenote.utils.DisplayUtils.disableScreenshotsIfLocked;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferencesActivity extends ThemedAppCompatActivity {
    private PasscodePreferenceFragmentCompat mPasscodePreferenceFragment;
    private PreferencesFragment mPreferencesFragment;
    private Bucket<Preferences> mPreferencesBucket;

    private IapViewModel mViewModel;

    private View mIapBanner;
    private View mIapThankYouBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mIapBanner = findViewById(R.id.iap_banner);
        mIapThankYouBanner = findViewById(R.id.iap_thank_you_banner);

        Simplenote currentApp = (Simplenote) getApplication();
        mPreferencesBucket = currentApp.getPreferencesBucket();

        try {
            if (mPreferencesBucket.get(PREFERENCES_OBJECT_KEY).getCurrentSubscriptionPlatform() == null) {
                mIapThankYouBanner.setVisibility(View.GONE);

                mViewModel = new ViewModelProvider(this).get(IapViewModel.class);

                findViewById(R.id.iap_banner).setOnClickListener(view -> mViewModel.onIapBannerClicked());

                mViewModel.getPlansBottomSheetVisibility().observe(this, isVisible -> {
                    BottomSheetDialogFragment fragment = (BottomSheetDialogFragment) getSupportFragmentManager().findFragmentByTag(SubscriptionBottomSheetDialog.getTAG());
                    if (isVisible) {
                        if (fragment == null) {
                            fragment = new SubscriptionBottomSheetDialog();
                        }
                        if (!(fragment.getDialog() != null && fragment.getDialog().isShowing())) {
                            fragment.show(getSupportFragmentManager(), SubscriptionBottomSheetDialog.getTAG());
                        }
                    } else {
                        if (fragment != null && fragment.isVisible()) {
                            fragment.dismiss();
                        }
                    }
                });

                mViewModel.getSnackbarMessage().observe(this, message -> {
                    Snackbar.make(findViewById(R.id.main_parent_view), message.getMessageResId(), Snackbar.LENGTH_SHORT).show();
                });

                mViewModel.getIapBannerVisibility().observe(this, isVisible -> {
                    if (isVisible) {
                        mIapBanner.setVisibility(View.GONE);
                        mIapBanner.setVisibility(View.VISIBLE);
                    } else {
                        mIapBanner.setVisibility(View.GONE);
                        try {
                            if (mPreferencesBucket.get(PREFERENCES_OBJECT_KEY)
                                    .getCurrentSubscriptionPlatform() != null) {
                                mIapThankYouBanner.setVisibility(View.VISIBLE);
                            }
                        } catch (BucketObjectMissingException e) {
                            mIapThankYouBanner.setVisibility(View.GONE);
                        }
                    }
                });
            } else {
                mIapBanner.setVisibility(View.GONE);
                mIapThankYouBanner.setVisibility(View.VISIBLE);
            }
        } catch (BucketObjectMissingException e) {
            mIapBanner.setVisibility(View.GONE);
            mIapThankYouBanner.setVisibility(View.GONE);
        }

        String preferencesTag = "tag_preferences";
        String passcodeTag = "tag_passcode";
        if (savedInstanceState == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);
            mPasscodePreferenceFragment = new PasscodePreferenceFragmentCompat();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            mPreferencesFragment = new PreferencesFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.preferences_container, mPreferencesFragment, preferencesTag)
                    .add(R.id.preferences_container, mPasscodePreferenceFragment, passcodeTag)
                    .commit();
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            mPreferencesFragment = (PreferencesFragment) fragmentManager.findFragmentByTag(preferencesTag);
            mPasscodePreferenceFragment = (PasscodePreferenceFragmentCompat) fragmentManager.findFragmentByTag(passcodeTag);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Preference togglePref =
                mPreferencesFragment.findPreference(getString(R.string.pref_key_passcode_toggle));
        Preference changePref =
                mPreferencesFragment.findPreference(getString(R.string.pref_key_change_passcode));

        if (togglePref != null && changePref != null) {
            mPasscodePreferenceFragment.setPreferences(togglePref, changePref);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        disableScreenshotsIfLocked(this);
    }

    public void openBrowserForMembership(View view) {
        try {
            BrowserUtils.launchBrowserOrShowError(PreferencesActivity.this, WEB_APP_URL);
        } catch (Exception e) {
            Toast.makeText(PreferencesActivity.this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
        }
    }
}
