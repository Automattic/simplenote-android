package com.automattic.simplenote.authentication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.NetworkUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.simperium.android.ProgressDialogFragment;

public class SignupFragment extends Fragment {
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

    }

    private void showDialogError(String message) {
        hideDialogProgress();
        new AlertDialog.Builder(requireActivity())
            .setTitle(R.string.simperium_dialog_title_error)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
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
