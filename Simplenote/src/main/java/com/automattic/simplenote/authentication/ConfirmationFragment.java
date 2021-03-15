package com.automattic.simplenote.authentication;

import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.HtmlCompat;

public class ConfirmationFragment extends Fragment {
    private final static String CONFIRMATION_EMAIL_KEY = "CONFIRMATION_EMAIL_KEY";

    public static ConfirmationFragment newInstance(String email) {
        ConfirmationFragment confirmationFragment = new ConfirmationFragment();
        Bundle bundle = new Bundle();
        bundle.putString(CONFIRMATION_EMAIL_KEY, email);
        confirmationFragment.setArguments(bundle);
        return confirmationFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirmation, container, false);
        initUi(view);
        return view;
    }

    private void initUi(View view) {
        initEmailConfirmation((TextView) view.findViewById(R.id.email_confirmation_text));
        initSupport((TextView) view.findViewById(R.id.support_text));
    }

    private void initEmailConfirmation(TextView emailConfirmation) {
        String boldEmail = "<b>" + requireArguments().getString(CONFIRMATION_EMAIL_KEY) + "</b>";
        Spanned emailConfirmationText = HtmlCompat.fromHtml(String.format(
            getString(R.string.email_confirmation_text),
            boldEmail));
        emailConfirmation.setText(emailConfirmationText);
    }

    private void initSupport(TextView support) {
        String supportEmail = getString(R.string.support_email);
        String link = "<a href='mailto:" + supportEmail + "'>" + supportEmail + "</a>";
        Spanned supportText = HtmlCompat.fromHtml(String.format(
            getString(R.string.support_text),
            link));
        support.setText(supportText);
        support.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
