package com.automattic.simplenote;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.preference.PreferenceManager;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.StrUtils;
import com.automattic.simplenote.utils.WordPressUtils;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WordPressDialogFragment extends AppCompatDialogFragment {
    public final static String DIALOG_TAG = "wordpress_dialog";
    private final static String API_FIELD_URL = "URL";
    private final static String API_FIELD_NAME = "name";
    private final static String API_FIELD_SITES = "sites";

    private View mConnectSection, mPostingSection, mFieldsSection, mSuccessSection;
    private ListView mListView;
    private CheckBox mDraftCheckbox;
    private TextView mPostSuccessTextView;
    private Button mPostButton, mCancelButton;
    private String mPostUrl;

    private JSONArray mSitesArray = new JSONArray();
    private Note mNote;
    private String mAuthState;

    private enum DialogStatus {
        CONNECT,
        FIELDS,
        POSTING,
        SUCCESS
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.dialog_wordpress_post, null);
        mConnectSection = view.findViewById(R.id.wp_dialog_section_connect);
        mPostingSection = view.findViewById(R.id.wp_dialog_section_posting);
        mFieldsSection = view.findViewById(R.id.wp_dialog_section_fields);
        mSuccessSection = view.findViewById(R.id.wp_dialog_section_success);

        mListView = view.findViewById(R.id.wp_dialog_list_view);
        mDraftCheckbox = view.findViewById(R.id.wp_dialog_draft_checkbox);

        Button copyUrlButton = view.findViewById(R.id.wp_dialog_copy_url);
        Button shareUrlButton = view.findViewById(R.id.wp_dialog_share);
        mPostSuccessTextView = view.findViewById(R.id.wp_dialog_success_summary);

        copyUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() == null) {
                    return;
                }

                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Simplenote", mPostUrl);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), R.string.link_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });

        shareUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() == null) {
                    return;
                }

                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, mPostUrl);

                startActivity(Intent.createChooser(share, getString(R.string.wordpress_post_link)));
            }
        });

        if (getActivity() != null) {
            return new AlertDialog.Builder(new ContextThemeWrapper(requireContext(), R.style.Dialog))
                    .setView(view)
                    .setTitle(R.string.post_to_wordpress)
                    .setPositiveButton(R.string.send_post, null)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        } else {
            return super.onCreateDialog(savedInstanceState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get the alert dialog buttons, so we can set their visibility and titles
        final AlertDialog dialog = (AlertDialog)getDialog();
        if (dialog != null)
        {
            mPostButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
            mPostButton.setOnClickListener(onPostClickListener);
            mCancelButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);
        }

        // Don't reset if we've resumed in a state besides connect
        if (mConnectSection.getVisibility() != View.VISIBLE) {
            return;
        }

        if (!WordPressUtils.hasWPToken(getActivity())) {
            // No WordPress token found, show connect UI
            setDialogStatus(DialogStatus.CONNECT);
        } else {
            setDialogStatus(DialogStatus.FIELDS);
        }

        loadSites(); // Load locally stored sites
        fetchSitesFromAPI(); // Fetch any new sites from the API
    }

    private void loadSites() {
        if (getActivity() == null || !WordPressUtils.hasWPToken(getActivity())) {
            return;
        }

        if (loadSitesFromPreferences()) {
            SitesAdapter sitesAdapter = new SitesAdapter(getActivity());
            mListView.setAdapter(sitesAdapter);
            return;
        }

        fetchSitesFromAPI();
    }

    private class SitesAdapter extends ArrayAdapter<String> {
        private SitesAdapter(@NonNull Context context) {
            super(context, 0);
        }

        @Override
        public int getCount() {
            return mSitesArray.length();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final SiteViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_single_choice, parent, false);
                holder = new SiteViewHolder();
                holder.titleTextView = convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (SiteViewHolder) convertView.getTag();
            }

            try {
                JSONObject site = mSitesArray.getJSONObject(position);
                Spanned rowText = Html.fromHtml(String.format(
                        Locale.ENGLISH,
                        "%s<br/><small><span style=\"color:#" +
                        Integer.toHexString(DrawableUtils.getColor(requireContext(), R.attr.notePreviewColor) & 0xffffff) +
                        "\">%s</span></small>",
                        site.getString(API_FIELD_NAME),
                        site.getString(API_FIELD_URL)
                ));
                holder.titleTextView.setText(rowText);
            } catch (JSONException e) {
                holder.titleTextView.setText(R.string.untitled_site);
            }

            if (position == 0 && mListView.getCheckedItemPosition() < 0) {
                mListView.setItemChecked(0, true);
            }

            return convertView;
        }

        private class SiteViewHolder {
            AppCompatCheckedTextView titleTextView;
        }
    }

    private void saveSitesToPreferences() {
        if (getActivity() == null) {
            return;
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putString(PrefUtils.PREF_WORDPRESS_SITES, mSitesArray.toString());
        editor.apply();
    }

    private boolean loadSitesFromPreferences() {
        if (getActivity() == null) {
            return false;
        }

        String savedSites = PrefUtils.getStringPref(getActivity(), PrefUtils.PREF_WORDPRESS_SITES);
        if (!TextUtils.isEmpty(savedSites)) {
            try {
                mSitesArray = new JSONArray(savedSites);
                return true;
            } catch (JSONException e) {
                return false;
            }
        }

        return false;
    }

    public void setNote(Note note) {
        mNote = note;
    }

    private void setDialogStatus(DialogStatus status) {
        mConnectSection.setVisibility(status == DialogStatus.CONNECT ? View.VISIBLE : View.GONE);
        mFieldsSection.setVisibility(status == DialogStatus.FIELDS ? View.VISIBLE : View.GONE);
        mPostingSection.setVisibility(status == DialogStatus.POSTING ? View.VISIBLE : View.GONE);
        mSuccessSection.setVisibility(status == DialogStatus.SUCCESS ? View.VISIBLE : View.GONE);
        mCancelButton.setVisibility(status == DialogStatus.FIELDS || status == DialogStatus.CONNECT ? View.VISIBLE : View.GONE);
        mPostButton.setVisibility(status == DialogStatus.POSTING ? View.GONE : View.VISIBLE);

        if (status == DialogStatus.SUCCESS) {
            mPostButton.setText(R.string.done);
        } else if (status == DialogStatus.CONNECT) {
            mPostButton.setText(R.string.connect_with_wordpress);
        } else {
            mPostButton.setText(R.string.send_post);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != WordPressUtils.OAUTH_ACTIVITY_CODE || data == null || getActivity() == null) {
            return;
        }

        AuthorizationResponse authResponse = AuthorizationResponse.fromIntent(data);
        AuthorizationException authException = AuthorizationException.fromIntent(data);
        if (authException != null) {
            // Error encountered
            Uri dataUri = data.getData();

            if (dataUri == null) {
                return;
            }

            if (StrUtils.isSameStr(dataUri.getQueryParameter("code"), "1")) {
                Toast.makeText(getActivity(), getString(R.string.wpcom_sign_in_error_unverified), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.wpcom_sign_in_error_generic), Toast.LENGTH_SHORT).show();
            }
        } else if (authResponse != null) {
            // Save token and finish activity
            Simplenote app = (Simplenote)getActivity().getApplication();
            boolean authSuccess = WordPressUtils.processAuthResponse(app, authResponse, mAuthState, false);
            if (!authSuccess) {
                Toast.makeText(getActivity(), getString(R.string.wpcom_sign_in_error_generic), Toast.LENGTH_SHORT).show();
            } else {
                AnalyticsTracker.track(
                        AnalyticsTracker.Stat.WPCC_LOGIN_SUCCEEDED,
                        AnalyticsTracker.CATEGORY_USER,
                        "wpcc_login_succeeded_post_fragment"
                );

                loadSites();
                setDialogStatus(DialogStatus.FIELDS);
            }
        }
    }

    private final View.OnClickListener onPostClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mNote == null || !isAdded()) {
                return;
            }

            // Perform different actions depending on the state of the dialog
            if (mSuccessSection.getVisibility() == View.VISIBLE) {
                dismiss();
                return;
            } else if (mConnectSection.getVisibility() == View.VISIBLE) {
                if (getActivity() == null) {
                    return;
                }

                AuthorizationRequest.Builder authBuilder = WordPressUtils.getWordPressAuthorizationRequestBuilder();
                // Set a unique state value
                mAuthState = "app-" + UUID.randomUUID();
                authBuilder.setState(mAuthState);

                AuthorizationRequest request = authBuilder.build();
                AuthorizationService authService = new AuthorizationService(getActivity());
                Intent authIntent = authService.getAuthorizationRequestIntent(request);
                startActivityForResult(authIntent, WordPressUtils.OAUTH_ACTIVITY_CODE);
                return;
            }

            int selectedListPosition = mListView.getCheckedItemPosition();
            if (selectedListPosition < 0) {
                Toast.makeText(getContext(), R.string.select_site, Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject site;
            try {
                site = mSitesArray.getJSONObject(selectedListPosition);
            } catch (JSONException e) {
                Toast.makeText(getContext(), R.string.could_not_access_site_data, Toast.LENGTH_SHORT).show();
                return;
            }

            String noteContent = mNote.getContent();
            if (TextUtils.isEmpty(noteContent)) {
                Toast.makeText(getContext(), R.string.empty_note_post, Toast.LENGTH_SHORT).show();
                return;
            }

            String postStatus = mDraftCheckbox.isChecked() ? "draft" : "publish";

            String title = "";
            String content = mNote.getContent();
            if (!mNote.getTitle().equals(mNote.getContent())) {
                title = mNote.getTitle();
                content = content.substring(title.length());

                // Get rid of the #'s in front of markdown note titles
                if (mNote.isMarkdownEnabled()) {
                    title = title.replaceFirst("^(#{1,6}[\\s]?)", "");
                }
            }

            setDialogStatus(DialogStatus.POSTING);
            WordPressUtils.publishPost(getContext(), site.optString(API_FIELD_URL, ""), title, content, postStatus, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() == null) {
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "A network error was encountered. Please try again.", Toast.LENGTH_SHORT).show();
                            setDialogStatus(DialogStatus.FIELDS);
                        }
                    });

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull final Response response) {
                    if (getActivity() == null) {
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (response.body() == null) {
                                    return;
                                }

                                if (response.code() == 200) {
                                    String responseString = response.body().string();
                                    JSONObject postResult = new JSONObject(responseString);

                                    mPostUrl = postResult.getString(API_FIELD_URL);
                                    mPostSuccessTextView.setText(getString(R.string.success, mPostUrl));
                                    setDialogStatus(DialogStatus.SUCCESS);

                                    AnalyticsTracker.track(
                                            AnalyticsTracker.Stat.NOTE_SHARED_TO_WORDPRESS,
                                            AnalyticsTracker.CATEGORY_NOTE,
                                            "wordpress_note_share_success"
                                    );
                                } else if (response.code() == 403) {
                                    Toast.makeText(getContext(), R.string.reconnect_to_wordpress, Toast.LENGTH_SHORT).show();
                                    setDialogStatus(DialogStatus.CONNECT);
                                } else {
                                    Toast.makeText(getContext(), R.string.network_error_message, Toast.LENGTH_SHORT).show();
                                    setDialogStatus(DialogStatus.FIELDS);
                                }
                            } catch (IOException | JSONException e) {
                                Toast.makeText(getContext(), R.string.network_error_message, Toast.LENGTH_SHORT).show();
                                setDialogStatus(DialogStatus.FIELDS);
                            }
                        }
                    });
                }
            });
        }
    };

    private void fetchSitesFromAPI() {
        WordPressUtils.getSites(getActivity(), new Callback() {
            @Override
            public void onFailure(@NonNull final Call call, @NonNull IOException e) {
                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSitesArray.length() == 0) {
                            // Reset to connect state if we reached an error
                            setDialogStatus(DialogStatus.CONNECT);
                        }
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                if (getActivity() == null) {
                    return;
                }

                if (response.code() == 200 && response.body() != null) {
                    String resultString = response.body().string();
                    try {
                        JSONArray sitesArray = new JSONObject(resultString).getJSONArray(API_FIELD_SITES);
                        final JSONArray newSitesArray = new JSONArray();
                        for (int i = 0; i < sitesArray.length(); i++) {
                            JSONObject site = sitesArray.getJSONObject(i);
                            JSONObject parsedSite = new JSONObject();
                            parsedSite.put(API_FIELD_NAME, site.getString(API_FIELD_NAME));

                            URI uri;
                            try {
                                uri = new URI(site.getString(API_FIELD_URL));
                            } catch (URISyntaxException e) {
                                // Reset to connect state if we reach an error
                                setDialogStatus(DialogStatus.CONNECT);
                                return;
                            }

                            parsedSite.put(API_FIELD_URL, uri.getHost());
                            newSitesArray.put(i, parsedSite);
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (newSitesArray.length() > 0) {
                                    mSitesArray = newSitesArray;
                                    saveSitesToPreferences();
                                }
                                SitesAdapter sitesAdapter = new SitesAdapter(getActivity());
                                mListView.setAdapter(sitesAdapter);
                            }
                        });

                    } catch (JSONException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Reset to connect state if we reached an error
                                if (mSitesArray.length() == 0) {
                                    setDialogStatus(DialogStatus.CONNECT);
                                }
                            }
                        });

                    }
                } else if (response.code() == 400 || mSitesArray.length() == 0) {
                    if (!isAdded()) {
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Remove WordPress sites
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                            editor.remove(PrefUtils.PREF_WORDPRESS_SITES);
                            editor.apply();
                            // Reset to connect state if we reached an error
                            setDialogStatus(DialogStatus.CONNECT);
                        }
                    });
                }
            }
        });
    }
}
