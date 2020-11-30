package com.automattic.simplenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ShareCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.AppLog.Type;
import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.CrashUtils;
import com.automattic.simplenote.utils.HtmlCompat;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.WidgetUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.User;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.passcodelock.AppLockManager;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.automattic.simplenote.models.Preferences.PREFERENCES_OBJECT_KEY;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends PreferenceFragmentCompat implements User.StatusChangeListener, Simperium.OnUserCreatedListener {
    public static final String WEB_APP_URL = "https://app.simplenote.com";

    private static final int REQUEST_EXPORT_DATA = 9001;
    private static final int REQUEST_EXPORT_UNSYNCED = 9002;

    private Bucket<Preferences> mPreferencesBucket;
    private SwitchPreferenceCompat mAnalyticsSwitch;

    public PreferencesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Preference authenticatePreference = findPreference("pref_key_authenticate");
        Simplenote currentApp = (Simplenote) getActivity().getApplication();
        Simperium simperium = currentApp.getSimperium();
        simperium.setUserStatusChangeListener(this);
        simperium.setOnUserCreatedListener(this);
        mPreferencesBucket = currentApp.getPreferencesBucket();

        authenticatePreference.setSummary(currentApp.getSimperium().getUser().getEmail());
        if (simperium.needsAuthorization()) {
            authenticatePreference.setTitle(R.string.log_in);
        } else {
            authenticatePreference.setTitle(R.string.log_out);
        }

        authenticatePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isAdded()) {
                    return false;
                }

                Simplenote currentApp = (Simplenote) getActivity().getApplication();
                if (currentApp.getSimperium().needsAuthorization()) {
                    Intent loginIntent = new Intent(getActivity(), SimplenoteAuthenticationActivity.class);
                    startActivityForResult(loginIntent, Simperium.SIGNUP_SIGNIN_REQUEST);
                } else {
                    new LogOutTask(PreferencesFragment.this).execute();
                }
                return true;
            }
        });

        findPreference("pref_key_help").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    BrowserUtils.launchBrowserOrShowError(requireContext(), "https://simplenote.com/help");
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        findPreference("pref_key_website").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    BrowserUtils.launchBrowserOrShowError(requireContext(), "http://simplenote.com");
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        findPreference("pref_key_about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            }
        });

        findPreference("pref_key_export").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.export_file));
                startActivityForResult(intent, REQUEST_EXPORT_DATA);
                return true;
            }
        });

        final ListPreference themePreference = findPreference(PrefUtils.PREF_THEME);
        themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateTheme(requireActivity(), Integer.parseInt(newValue.toString()));
                return true;
            }

            private void updateTheme(Activity activity, int index) {
                CharSequence[] entries = themePreference.getEntries();
                themePreference.setSummary(entries[index]);

                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.SETTINGS_THEME_UPDATED,
                        AnalyticsTracker.CATEGORY_USER,
                        "theme_preference"
                );

                // recreate the activity so new theme is applied
                activity.recreate();
            }
        });

        final Preference stylePreference = findPreference("pref_key_style");
        stylePreference.setSummary(
            PrefUtils.isPremium(requireContext()) ?
                PrefUtils.getStyleNameFromIndexSelected(requireContext()) :
                PrefUtils.getStyleNameDefault(requireContext())
        );
        stylePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(requireContext(), StyleActivity.class));
                return true;
            }
        });

        final Preference membershipPreference = findPreference("pref_key_membership");
        membershipPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((PreferencesActivity) requireActivity()).openBrowserForMembership(getView());
                return true;
            }
        });

        if (PrefUtils.isPremium(requireContext())) {
            membershipPreference.setLayoutResource(R.layout.preference_default);
            membershipPreference.setSummary(R.string.membership_premium);
        } else {
            membershipPreference.setLayoutResource(R.layout.preference_button);
            membershipPreference.setSummary(R.string.membership_free);
        }

        final ListPreference sortPreference = findPreference(PrefUtils.PREF_SORT_ORDER);
        sortPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int index = Integer.parseInt(newValue.toString());
                CharSequence[] entries = sortPreference.getEntries();
                sortPreference.setSummary(entries[index]);

                return true;
            }
        });

        SwitchPreferenceCompat switchPreference = findPreference("pref_key_condensed_note_list");
        switchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (((SwitchPreferenceCompat) preference).isChecked()) {
                    AnalyticsTracker.track(
                            AnalyticsTracker.Stat.SETTINGS_LIST_CONDENSED_ENABLED,
                            AnalyticsTracker.CATEGORY_USER,
                            "condensed_list_preference"
                    );
                }

                return true;
            }
        });

        // Add the hyperlink to the analytics summary
        Preference analyticsSummaryPreference = findPreference("pref_key_analytics_enabled_summary");
        String formattedSummary = String.format(
                getString(R.string.share_analytics_summary),
                "<a href=\"https://automattic.com/cookies\">",
                "</a>"
        );
        analyticsSummaryPreference.setSummary(HtmlCompat.fromHtml(formattedSummary));

        mAnalyticsSwitch = findPreference("pref_key_analytics_switch");
        mAnalyticsSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    boolean isChecked = (boolean)newValue;
                    Preferences prefs = mPreferencesBucket.get(PREFERENCES_OBJECT_KEY);
                    prefs.setAnalyticsEnabled(isChecked);
                    prefs.save();
                } catch (BucketObjectMissingException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

        updateAnalyticsSwitchState();

        findPreference("pref_key_logs").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(
                    ShareCompat.IntentBuilder.from(requireActivity())
                        .setText(AppLog.get())
                        .setType("text/plain")
                        .createChooserIntent()
                );
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK || resultData == null) {
            return;
        }

        if (resultData.getData() == null) {
            Toast.makeText(requireContext(), getString(R.string.export_message_failure), Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case REQUEST_EXPORT_DATA:
                exportData(resultData.getData(), false);
                break;
            case REQUEST_EXPORT_UNSYNCED:
                exportData(resultData.getData(), true);
                break;
        }
    }

    private boolean hasUnsyncedNotes() {
        Simplenote application = (Simplenote) getActivity().getApplication();
        Bucket<Note> notesBucket = application.getNotesBucket();
        Bucket.ObjectCursor<Note> notesCursor = notesBucket.allObjects();
        while (notesCursor.moveToNext()) {
            Note note = notesCursor.getObject();
            if (note.isNew() || note.isModified()) {
                return true;
            }
        }

        return false;
    }

    private void logOut() {
        Simplenote application = (Simplenote) getActivity().getApplication();
        application.getSimperium().deauthorizeUser();

        application.getNotesBucket().reset();
        application.getTagsBucket().reset();
        application.getPreferencesBucket().reset();

        application.getNotesBucket().stop();
        AppLog.add(Type.SYNC, "Stopped note bucket (PreferencesFragment)");
        application.getTagsBucket().stop();
        AppLog.add(Type.SYNC, "Stopped tag bucket (PreferencesFragment)");
        application.getPreferencesBucket().stop();
        AppLog.add(Type.SYNC, "Stopped preference bucket (PreferencesFragment)");

        AnalyticsTracker.track(
                AnalyticsTracker.Stat.USER_SIGNED_OUT,
                AnalyticsTracker.CATEGORY_USER,
                "preferences_sign_out_button"
        );

        // Resets analytics user back to 'anon' type
        AnalyticsTracker.refreshMetadata(null);
        CrashUtils.clearCurrentUser();

        // Remove wp.com token
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.remove(PrefUtils.PREF_WP_TOKEN);

        // Remove WordPress sites
        editor.remove(PrefUtils.PREF_WORDPRESS_SITES);
        editor.apply();

        // Remove Passcode Lock password
        AppLockManager.getInstance().getAppLock().setPassword("");

        WidgetUtils.updateNoteWidgets(requireActivity().getApplicationContext());

        getActivity().finish();
    }

    @Override
    public void onUserStatusChange(User.Status status) {
        if (isAdded() && status == User.Status.AUTHORIZED) {
            // User signed in
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Preference authenticatePreference = findPreference("pref_key_authenticate");
                    authenticatePreference.setTitle(R.string.log_out);
                }
            });

            Simplenote app = (Simplenote) getActivity().getApplication();
            AnalyticsTracker.refreshMetadata(app.getSimperium().getUser().getEmail());
            CrashUtils.setCurrentUser(app.getSimperium().getUser());

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.USER_SIGNED_IN,
                    AnalyticsTracker.CATEGORY_USER,
                    "signed_in_from_preferences_activity"
            );
        }
    }

    @Override
    public void onUserCreated(User user) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.USER_ACCOUNT_CREATED,
                AnalyticsTracker.CATEGORY_USER,
                "account_created_from_preferences_activity"
        );
    }

    private void exportData(Uri uri, boolean isUnsyncedNotes) {
        Simplenote currentApp = (Simplenote) requireActivity().getApplication();
        Bucket<Note> noteBucket = currentApp.getNotesBucket();
        JSONObject account = new JSONObject();
        Bucket.ObjectCursor<Note> cursor = noteBucket.allObjects();

        try {
            JSONArray activeNotes = new JSONArray();
            JSONArray trashedNotes = new JSONArray();
            Comparator<String> comparator = new Comparator<String>() {
                @Override
                public int compare(String text1, String text2) {
                    return text1.compareToIgnoreCase(text2);
                }
            };

            while (cursor.moveToNext()) {
                Note note = cursor.getObject();

                if (isUnsyncedNotes && !note.isNew() && !note.isModified()) {
                    continue;
                }

                JSONObject noteJson = new JSONObject();

                noteJson.put("id", note.getSimperiumKey());
                noteJson.put("content", note.getContent());
                noteJson.put("creationDate", note.getCreationDateString());
                noteJson.put("lastModified", note.getModificationDateString());

                if (note.isPinned()) {
                    noteJson.put("pinned", note.isPinned());
                }

                if (note.isMarkdownEnabled()) {
                    noteJson.put("markdown", note.isMarkdownEnabled());
                }

                if (note.getTags().size() > 0) {
                    List<String> tags = note.getTags();
                    Collections.sort(tags, comparator);
                    noteJson.put("tags", new JSONArray(tags));
                }

                if (!note.getPublishedUrl().isEmpty()) {
                    noteJson.put("publicURL", note.getPublishedUrl());
                }

                if (note.isDeleted()) {
                    trashedNotes.put(noteJson);
                } else {
                    activeNotes.put(noteJson);
                }
            }

            account.put("activeNotes", activeNotes);
            account.put("trashedNotes", trashedNotes);

            ParcelFileDescriptor parcelFileDescriptor = requireContext().getContentResolver().openFileDescriptor(uri, "w");

            if (parcelFileDescriptor != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                fileOutputStream.write(account.toString(2).replace("\\/","/").getBytes());
                parcelFileDescriptor.close();
                Toast.makeText(requireContext(), getString(R.string.export_message_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.export_message_failure), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.export_message_failure), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAnalyticsSwitchState() {
        try {
            Preferences prefs = mPreferencesBucket.get(PREFERENCES_OBJECT_KEY);
            mAnalyticsSwitch.setChecked(prefs.getAnalyticsEnabled());
        } catch (BucketObjectMissingException e) {
            // The preferences object doesn't exist for this user yet, create it
            try {
                Preferences prefs = mPreferencesBucket.newObject(PREFERENCES_OBJECT_KEY);
                prefs.setAnalyticsEnabled(true);
                prefs.save();
            } catch (BucketObjectNameInvalid bucketObjectNameInvalid) {
                bucketObjectNameInvalid.printStackTrace();
            }
        }
    }

    private static class LogOutTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<PreferencesFragment> mPreferencesFragmentReference;

        LogOutTask(PreferencesFragment fragment) {
            mPreferencesFragmentReference = new WeakReference<>(fragment);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PreferencesFragment fragment = mPreferencesFragmentReference.get();
            return fragment == null || fragment.hasUnsyncedNotes();
        }

        @Override
        protected void onPostExecute(Boolean hasUnsyncedNotes) {
            final PreferencesFragment fragment = mPreferencesFragmentReference.get();

            if (fragment == null) {
                return;
            }

            // Safety first! Check if any notes are unsynced and warn the user if so.
            if (hasUnsyncedNotes) {
                new AlertDialog.Builder(new ContextThemeWrapper(fragment.requireContext(), R.style.Dialog))
                    .setTitle(R.string.unsynced_notes)
                    .setMessage(R.string.unsynced_notes_message)
                    .setPositiveButton(R.string.log_out_anyway,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fragment.logOut();
                            }
                        }
                    )
                    .setNeutralButton(R.string.export_unsynced_notes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("application/json");
                                intent.putExtra(Intent.EXTRA_TITLE, fragment.getString(R.string.export_file));
                                fragment.startActivityForResult(intent, REQUEST_EXPORT_UNSYNCED);
                            }
                        }
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            } else {
                fragment.logOut();
            }
        }
    }
}
