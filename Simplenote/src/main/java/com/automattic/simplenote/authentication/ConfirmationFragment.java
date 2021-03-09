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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.HtmlCompat;

public class ConfirmationFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirmation, container, false);
        initUi(view);
        return view;
    }

    private void initUi(View view) {
        initEmailConfirmation((TextView) view.findViewById(R.id.email_confirmation_text), "email");
        initSupport((TextView) view.findViewById(R.id.support_text));
    }

    private void initEmailConfirmation(TextView emailConfirmation, String email) {
        String boldEmail = "<b>" + email + "</b>";
        Spanned emailConfirmationText = HtmlCompat.fromHtml(String.format(
            getString(R.string.email_confirmation_text),
            boldEmail));
        emailConfirmation.setText(emailConfirmationText);
        emailConfirmation.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_80));
    }

    private void initSupport(TextView support) {
        String supportEmail = getString(R.string.support_email);
        String link = "<a href='mailto:" + supportEmail + "'>" + supportEmail + "</a>";
        Spanned supportText = HtmlCompat.fromHtml(String.format(
            getString(R.string.support_text),
            link));
        support.setText(supportText);
        support.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_50));
        support.setLinkTextColor(ContextCompat.getColor(requireContext(), R.color.gray_50));
        support.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
