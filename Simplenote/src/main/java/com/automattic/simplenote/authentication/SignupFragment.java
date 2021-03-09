package com.automattic.simplenote.authentication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.BrowserUtils;

public class SignupFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);
        initUi(view);
        return view;
    }

    private void initUi(View view) {
        initFooter((TextView) view.findViewById(com.simperium.R.id.text_footer));
    }

    private void initFooter(TextView footer) {
        String colorLink = Integer.toHexString(ContextCompat.getColor(requireActivity(), com.simperium.R.color.text_link) & 0xffffff);
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
