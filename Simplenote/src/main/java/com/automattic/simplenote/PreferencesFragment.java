package com.automattic.simplenote;

import static android.app.Activity.RESULT_OK;
import static com.automattic.simplenote.models.Preferences.PREFERENCES_OBJECT_KEY;
import static com.automattic.simplenote.utils.PrefUtils.ALPHABETICAL_ASCENDING;
import static com.automattic.simplenote.utils.PrefUtils.ALPHABETICAL_ASCENDING_LABEL;
import static com.automattic.simplenote.utils.PrefUtils.ALPHABETICAL_DESCENDING;
import static com.automattic.simplenote.utils.PrefUtils.ALPHABETICAL_DESCENDING_LABEL;
import static com.automattic.simplenote.utils.PrefUtils.DATE_CREATED_ASCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_CREATED_ASCENDING_LABEL;
import static com.automattic.simplenote.utils.PrefUtils.DATE_CREATED_DESCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_CREATED_DESCENDING_LABEL;
import static com.automattic.simplenote.utils.PrefUtils.DATE_MODIFIED_ASCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_MODIFIED_ASCENDING_LABEL;
import static com.automattic.simplenote.utils.PrefUtils.DATE_MODIFIED_DESCENDING;
import static com.automattic.simplenote.utils.PrefUtils.DATE_MODIFIED_DESCENDING_LABEL;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.authentication.SimplenoteAuthenticationActivity;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Preferences;
import com.automattic.simplenote.utils.AccountNetworkUtils;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.AppLog.Type;
import com.automattic.simplenote.utils.AuthUtils;
import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.CrashUtils;
import com.automattic.simplenote.utils.DeleteAccountRequestHandler;
import com.automattic.simplenote.utils.DialogUtils;
import com.automattic.simplenote.utils.HtmlCompat;
import com.automattic.simplenote.utils.NetworkUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.SimplenoteProgressDialogFragment;
import com.simperium.Simperium;
import com.simperium.android.ProgressDialogFragment;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends PreferenceFragmentCompat implements User.StatusChangeListener, Simperium.OnUserCreatedListener {
    public static final String WEB_APP_URL = "https://app.simplenote.com";

    private static final int REQUEST_EXPORT_DATA = 9001;
    private static final int REQUEST_EXPORT_UNSYNCED = 9002;
    private static final int REQUEST_IMPORT_DATA = 9003;

    private Bucket<Preferences> mPreferencesBucket;
    private SwitchPreferenceCompat mAnalyticsSwitch;
    private SimplenoteProgressDialogFragment mProgressDialogFragment;

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

        Preference deleteAppPreference = findPreference("pref_key_delete_account");
        deleteAppPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.USER_ACCOUNT_DELETE_REQUESTED,
                        AnalyticsTracker.CATEGORY_USER,
                        "preferences_delete_account_button"
                );

                showDeleteAccountDialog();
                return true;
            }
        });

        findPreference("pref_key_help").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    BrowserUtils.launchBrowserOrShowError(requireContext(), "https://simplenote.com/help");
                } catch (Exception e) {
                    toast(R.string.no_browser_available, Toast.LENGTH_LONG);
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
                    toast(R.string.no_browser_available, Toast.LENGTH_LONG);
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

        findPreference("pref_key_import").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.SETTINGS_IMPORT_NOTES,
                        AnalyticsTracker.CATEGORY_NOTE,
                        "preferences_import_data_button"
                );

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*", "application/json"});
                startActivityForResult(intent, REQUEST_IMPORT_DATA);
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

                if (!sortPreference.getValue().equals(newValue)) {
                    switch (index) {
                        case ALPHABETICAL_ASCENDING:
                            trackSortOrder(ALPHABETICAL_ASCENDING_LABEL);
                            break;
                        case ALPHABETICAL_DESCENDING:
                            trackSortOrder(ALPHABETICAL_DESCENDING_LABEL);
                            break;
                        case DATE_CREATED_ASCENDING:
                            trackSortOrder(DATE_CREATED_ASCENDING_LABEL);
                            break;
                        case DATE_CREATED_DESCENDING:
                            trackSortOrder(DATE_CREATED_DESCENDING_LABEL);
                            break;
                        case DATE_MODIFIED_ASCENDING:
                            trackSortOrder(DATE_MODIFIED_ASCENDING_LABEL);
                            break;
                        case DATE_MODIFIED_DESCENDING:
                            trackSortOrder(DATE_MODIFIED_DESCENDING_LABEL);
                            break;
                    }
                }

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

    private void showProgressDialogDeleteAccount() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mProgressDialogFragment = SimplenoteProgressDialogFragment.newInstance(getString(R.string.requesting_message));
        mProgressDialogFragment.show(activity.getSupportFragmentManager(), ProgressDialogFragment.TAG);
    }

    private void closeProgressDialogDeleteAccount() {
        if (mProgressDialogFragment != null && !mProgressDialogFragment.isHidden()) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    private void showDeleteAccountDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        final DeleteAccountRequestHandler deleteAccountHandler = new DeleteAccountRequestHandlerImpl(this);

        Simplenote currentApp = (Simplenote) requireActivity().getApplication();
        Simperium simperium = currentApp.getSimperium();
        String userEmail = simperium.getUser().getEmail();
        final AlertDialog dialogDeleteAccount = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.Dialog))
                .setTitle(R.string.delete_account)
                .setMessage(getString(R.string.delete_account_email_message, userEmail))
                .setPositiveButton(R.string.delete_account, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FragmentActivity activity = getActivity();
                            if (activity == null) {
                                return;
                            }

                            showProgressDialogDeleteAccount();

                            // makeDeleteAccountRequest can throw an exception when it cannot build
                            // the JSON object. In those cases, we show the error dialog since
                            // it can be related to memory constraints or something else that is
                            // just a transient fault
                            try {
                                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                                    AppLog.add(Type.ACCOUNT, "Making request to delete account");
                                    String userToken = simperium.getUser().getAccessToken();
                                    AccountNetworkUtils.makeDeleteAccountRequest(
                                            userEmail,
                                            userToken,
                                            deleteAccountHandler);
                                } else {
                                    AppLog.add(Type.ACCOUNT, "No connectivity to make request to delete account");
                                    closeProgressDialogDeleteAccount();
                                    showDialogDeleteAccountNoConnectivity();
                                }

                            } catch (IllegalArgumentException exception) {
                                AppLog.add(Type.ACCOUNT, "Error trying to make request " +
                                        "to delete account. Error: " + exception.getMessage());
                                closeProgressDialogDeleteAccount();
                                showDialogDeleteAccountError();
                            }
                        }
                    }
                )
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialogDeleteAccount.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                FragmentActivity activity = getActivity();
                if (activity == null) {
                    return;
                }

                int colorRed = ContextCompat.getColor(activity, R.color.text_button_red);
                dialogDeleteAccount
                        .getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(colorRed);
            }
        });
        dialogDeleteAccount.show();
    }

    private void showDialogDeleteAccountNoConnectivity() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        AlertDialog dialogDeleteAccountConfirmation = new AlertDialog.Builder(
                new ContextThemeWrapper(activity, R.style.Dialog))
                .setTitle(R.string.error)
                .setMessage(R.string.simperium_dialog_message_network)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }
                )
                .create();

        dialogDeleteAccountConfirmation.show();
    }

    private void showDialogDeleteAccountError() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        DialogUtils.showDialogWithEmail(
                context,
                getString(R.string.error_ocurred_message)
        );
    }

    private void showDeleteAccountConfirmationDialog() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        Simplenote currentApp = (Simplenote) activity.getApplication();
        Simperium simperium = currentApp.getSimperium();
        String userEmail = simperium.getUser().getEmail();

        AlertDialog dialogDeleteAccountConfirmation = new AlertDialog.Builder(
                new ContextThemeWrapper(activity, R.style.Dialog))
                .setTitle(R.string.request_received)
                .setMessage(getString(R.string.account_deletion_message, userEmail))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }
                )
                .create();

        dialogDeleteAccountConfirmation.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK || resultData == null) {
            return;
        }

        if (resultData.getData() == null) {
            toast(R.string.export_message_failure);
            return;
        }

        switch (requestCode) {
            case REQUEST_EXPORT_DATA:
                exportData(resultData.getData(), false);
                break;
            case REQUEST_EXPORT_UNSYNCED:
                exportData(resultData.getData(), true);
                break;
            case REQUEST_IMPORT_DATA:
                importData(resultData.getData());
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
        AppLog.add(Type.ACTION, "Tapped logout button (PreferencesFragment)");
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.USER_SIGNED_OUT,
                AnalyticsTracker.CATEGORY_USER,
                "preferences_sign_out_button"
        );

        AuthUtils.logOut((Simplenote) requireActivity().getApplication());

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
                toast(R.string.export_message_success);
            } else {
                toast(R.string.export_message_failure);
            }
        } catch (Exception e) {
            toast(R.string.export_message_failure);
        }
    }

    private void importData(Uri uri) {
        try {
            AppLog.add(Type.IMPORT, "Importing notes from " + uri + ".");

            FragmentActivity activity = getActivity();
            if (activity == null) {
                AppLog.add(Type.IMPORT, "Could not import notes since activity is null");
                return;
            }

            Importer.fromUri(activity, uri);
            toast(R.string.import_message_success);

            AppLog.add(Type.IMPORT, "Notes imported correctly!");
        } catch (Importer.ImportException e) {
            switch (e.getReason()) {
                case FileError:
                    AppLog.add(Type.IMPORT, "File error while importing note. Exception: " + e.getMessage());

                    toast(R.string.import_error_file);
                    break;
                case ParseError:
                    AppLog.add(Type.IMPORT, "Parse error while importing note. Exception: " + e.getMessage());

                    toast(R.string.import_error_parse);
                    break;
                case UnknownExportType:
                    AppLog.add(Type.IMPORT, "Unknown error while importing note. Exception: " + e.getMessage());

                    toast(R.string.import_unknown);
                    break;
            }
        }
    }

    private void trackSortOrder(String label) {
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.SETTINGS_SEARCH_SORT_MODE,
            AnalyticsTracker.CATEGORY_SETTING,
            label
        );
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
  
    private void toast(int stringId) {
        toast(stringId, Toast.LENGTH_SHORT);
    }

    private void toast(int stringId, int length) {
        Toast.makeText(requireContext(), getString(stringId), length).show();
    }

    static class DeleteAccountRequestHandlerImpl implements DeleteAccountRequestHandler {
        final WeakReference<PreferencesFragment> preferencesFragment;

        DeleteAccountRequestHandlerImpl(PreferencesFragment fragment) {
            this.preferencesFragment = new WeakReference<>(fragment);
        }

        @Override
        public void onSuccess() {
            final PreferencesFragment fragment = preferencesFragment.get();
            if (fragment == null) {
                return;
            }

            FragmentActivity activity = fragment.getActivity();
            if (activity == null) {
                return;
            }

            AppLog.add(Type.ACCOUNT, "Request to delete account was successful");
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.USER_ACCOUNT_DELETE_REQUESTED,
                    AnalyticsTracker.CATEGORY_USER,
                    "delete_account_request_success"
            );

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragment.closeProgressDialogDeleteAccount();
                    fragment.showDeleteAccountConfirmationDialog();
                }
            });
        }

        @Override
        public void onFailure() {
            final PreferencesFragment fragment = preferencesFragment.get();
            if (fragment == null) {
                return;
            }

            FragmentActivity activity = fragment.getActivity();
            if (activity == null) {
                return;
            }

            AppLog.add(Type.ACCOUNT, "Failure while calling server to delete account");
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.USER_ACCOUNT_DELETE_REQUESTED,
                    AnalyticsTracker.CATEGORY_USER,
                    "delete_account_request_failure"
            );

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragment.closeProgressDialogDeleteAccount();
                    fragment.showDialogDeleteAccountError();
                }
            });
        }
    }
}
