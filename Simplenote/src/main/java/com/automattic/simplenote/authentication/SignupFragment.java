package com.automattic.simplenote.authentication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.NetworkUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.simperium.android.ProgressDialogFragment;
import com.simperium.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupFragment extends Fragment {
    private static final int TIMEOUT_SECS = 30;
    private static final String HTTP_SCHEME = "https";
    private static final String HTTP_HOST = "app.simplenote.com";
    private static final String SIMPLENOTE_SIGNUP_PATH = "account/request-signup";
    private static final String ACCEPT_LANGUAGE = "Accept-Language";

    private static final MediaType JSON_MEDIA_TYPE =
        MediaType.parse("application/json; charset=utf-8");

    private ProgressDialogFragment progressDialogFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);
        initUi(view);
        return view;
    }

    private void initUi(View view) {
        initFooter((TextView) view.findViewById(com.simperium.R.id.text_footer));
        initSignupButton(view);
    }

    @SuppressWarnings("ConstantConditions")
    private void initSignupButton(View view) {
        EditText emailEditText = ((TextInputLayout) view.findViewById(R.id.input_email)).getEditText();
        final Button signupButton = view.findViewById(R.id.button);
        setButtonState(signupButton, emailEditText.getText());
        listenToEmailChanges(emailEditText, signupButton);
        listenToSignupClick(signupButton, emailEditText);
    }

    private void listenToEmailChanges(EditText emailEditText, final Button signupButton) {
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                setButtonState(signupButton, s);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void listenToSignupClick(Button signupButton, final EditText emailEditText) {
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    showProgressDialog();
                    signupUser(emailEditText.getText().toString());
                } else {
                    showDialogError(getString(R.string.simperium_dialog_message_network));
                }
            }
        });
    }

    private void showProgressDialog() {
        progressDialogFragment =
            ProgressDialogFragment.newInstance(getString(R.string.simperium_dialog_progress_signing_up));
        progressDialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Simperium);
        progressDialogFragment.show(requireFragmentManager(), ProgressDialogFragment.TAG);
    }

    private void hideDialogProgress() {
        if (progressDialogFragment != null && !progressDialogFragment.isHidden()) {
            progressDialogFragment.dismiss();
            progressDialogFragment = null;
        }
    }

    private void signupUser(String email) {
        new OkHttpClient()
            .newBuilder()
            .readTimeout(TIMEOUT_SECS, TimeUnit.SECONDS)
            .build()
            .newCall(buildCall(email))
            .enqueue(buildCallback(email));
    }

    private Request buildCall(String email) {
        return new Request.Builder()
            .url(buildUrl())
            .post(buildJsonBody(email))
            .header(ACCEPT_LANGUAGE, getLanguage())
            .build();
    }

    private RequestBody buildJsonBody(String email) {
        JSONObject json = new JSONObject();
        try {
            json.put("username", email);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot construct json with supplied email: " + email);
        }
        return RequestBody.create(JSON_MEDIA_TYPE, json.toString());
    }

    private HttpUrl buildUrl() {
        return new HttpUrl.Builder()
            .scheme(HTTP_SCHEME)
            .host(HTTP_HOST)
            .addPathSegments(SIMPLENOTE_SIGNUP_PATH)
            .build();
    }

    private String getLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return LocaleList.getDefault().toLanguageTags();
        } else {
            return Locale.getDefault().getLanguage();
        }
    }

    private Callback buildCallback(final String email) {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull final IOException error) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialogError(getString(R.string.dialog_message_signup_error));
                            AppLog.add(AppLog.Type.ACCOUNT, "Sign up failure: " + error.getMessage());
                            Logger.log(error.getMessage(), error);
                        }
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideDialogProgress();
                            DisplayUtils.hideKeyboard(getView());
                            showConfirmationScreen(email);
                        }
                    });
                }
            }
        };
    }

    private void showDialogError(String message) {
        hideDialogProgress();
        new AlertDialog.Builder(requireActivity())
            .setTitle(R.string.simperium_dialog_title_error)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void showConfirmationScreen(String email) {
        ConfirmationFragment confirmationFragment = ConfirmationFragment.newInstance(email);
        requireFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, confirmationFragment, SimplenoteSignupActivity.SIGNUP_FRAGMENT_TAG)
            .commit();
    }

    private void setButtonState(Button signupButton, CharSequence email) {
        signupButton.setEnabled(isValidEmail(email));
    }

    private boolean isValidEmail(CharSequence text) {
        return Patterns.EMAIL_ADDRESS.matcher(text).matches();
    }

    private void initFooter(TextView footer) {
        String colorLink = Integer.toHexString(
            ContextCompat.getColor(requireActivity(), com.simperium.R.color.text_link) & 0xffffff);
        footer.setText(
            Html.fromHtml(
                String.format(
                    getResources().getString(com.simperium.R.string.simperium_footer_signup),
                    "<span style=\"color:#",
                    colorLink,
                    "\">",
                    "</span>"
                )
            )
        );
        footer.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = getString(com.simperium.R.string.simperium_footer_signup_url);
                    if (BrowserUtils.isBrowserInstalled(requireContext())) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } else {
                        BrowserUtils.showDialogErrorBrowser(requireContext(), url);
                    }
                }
            }
        );
    }
}
