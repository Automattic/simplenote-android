package com.automattic.simplenote;

import static com.automattic.simplenote.models.Account.KEY_EMAIL_VERIFICATION;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.FullScreenDialogFragment.FullScreenDialogContent;
import com.automattic.simplenote.FullScreenDialogFragment.FullScreenDialogController;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Account;
import com.automattic.simplenote.utils.AccountNetworkUtils;
import com.automattic.simplenote.utils.AccountVerificationEmailHandler;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.NetworkUtils;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

/**
 * A {@link FullScreenDialogFragment} for reviewing an account and verifying an email address.  When
 * an account has not been confirmed through a verification email link, the review account interface
 * is shown.  If a verification email has been sent, the verify email interface is shown.
 */
public class ReviewAccountVerifyEmailFragment extends Fragment implements FullScreenDialogContent {
    public static final String EXTRA_SENT_EMAIL = "EXTRA_SENT_EMAIL";

    private static final String URL_SETTINGS_REDIRECT = "https://app.simplenote.com/settings/";
    private static final String URL_VERIFY_EMAIL = "https://app.simplenote.com/account/verify-email/";
    private static final int TIMEOUT_SECONDS = 30;

    private AppCompatButton mButtonPrimary;
    private AppCompatButton mButtonSecondary;
    private Bucket<Account> mBucketAccount;
    private FullScreenDialogController mDialogController;
    private ImageView mImageIcon;
    private String mEmail;
    private TextView mTextSubtitle;
    private TextView mTextTitle;
    private boolean mHasSentEmail;

    @Override
    public boolean onConfirmClicked(FullScreenDialogController controller) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.error_network_required, Toast.LENGTH_LONG).show();
            return false;
        }

        if (mHasSentEmail) {
            Toast.makeText(requireContext(), R.string.toast_email_sent, Toast.LENGTH_SHORT).show();
        } else {
            showVerifyEmail();
        }

        sendVerificationEmail();
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBucketAccount = ((Simplenote) requireActivity().getApplication()).getAccountBucket();

        View layout = inflater.inflate(R.layout.fragment_review_account_verify_email, container, false);
        mHasSentEmail = getArguments() != null && getArguments().getBoolean(EXTRA_SENT_EMAIL);
        mEmail = ((Simplenote) requireActivity().getApplication()).getSimperium().getUser().getEmail();

        mImageIcon = layout.findViewById(R.id.image);
        mImageIcon.setImageResource(mHasSentEmail ? R.drawable.ic_mail_24dp : R.drawable.ic_warning_24dp);
        mImageIcon.setContentDescription(getString(mHasSentEmail ? R.string.description_mail : R.string.description_warning));

        @StringRes int title = mHasSentEmail ? R.string.fullscreen_verify_email_title : R.string.fullscreen_review_account_title;
        mTextTitle = layout.findViewById(R.id.text_title);
        mTextTitle.setText(title);

        @StringRes int subtitle = mHasSentEmail ? R.string.fullscreen_verify_email_subtitle : R.string.fullscreen_review_account_subtitle;
        mTextSubtitle = layout.findViewById(R.id.text_subtitle);
        mTextSubtitle.setText(Html.fromHtml(String.format(getResources().getString(subtitle), mEmail)));

        mButtonPrimary = layout.findViewById(R.id.button_primary);
        mButtonPrimary.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnalyticsTracker.track(
                        AnalyticsTracker.Stat.VERIFICATION_CONFIRM_BUTTON_TAPPED,
                        AnalyticsTracker.CATEGORY_USER,
                        "verification_confirm"
                    );
                    onConfirmClicked(mDialogController);
                }
            }
        );
        mButtonPrimary.setVisibility(mHasSentEmail ? View.GONE : View.VISIBLE);

        mButtonSecondary = layout.findViewById(R.id.button_secondary);
        mButtonSecondary.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mHasSentEmail) {
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.VERIFICATION_RESEND_EMAIL_BUTTON_TAPPED,
                            AnalyticsTracker.CATEGORY_USER,
                            "verification_resend_email"
                        );
                        onConfirmClicked(mDialogController);
                    } else {
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.VERIFICATION_CHANGE_EMAIL_BUTTON_TAPPED,
                            AnalyticsTracker.CATEGORY_USER,
                            "verification_change_email"
                        );
                        BrowserUtils.launchBrowserOrShowError(requireContext(), URL_SETTINGS_REDIRECT);
                    }
                }
            }
        );
        mButtonSecondary.setText(mHasSentEmail ? R.string.fullscreen_verify_email_button_secondary : R.string.fullscreen_review_account_button_secondary);

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public boolean onDismissClicked(FullScreenDialogController controller) {
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.VERIFICATION_DISMISSED,
            AnalyticsTracker.CATEGORY_USER,
            "verification_dismissed"
        );
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        new Handler(Looper.getMainLooper()).post(
            new Runnable() {
                @Override
                public void run() {
                    dismissIfVerified();
                }
            }
        );
    }

    @Override
    public void onViewCreated(FullScreenDialogController controller) {
        mDialogController = controller;
    }

    private void dismissIfVerified() {
        if (isDetached() || isRemoving()) {
            return;
        }

        try {
            Account account = mBucketAccount.get(KEY_EMAIL_VERIFICATION);

            if (account.hasVerifiedEmail(mEmail)) {
                mDialogController.dismiss();
            }
        } catch (BucketObjectMissingException bucketObjectMissingException) {
            // Do nothing if account cannot be retrieved.
        }
    }

    public static Bundle newBundle(boolean hasSentEmail) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_SENT_EMAIL, hasSentEmail);
        return bundle;
    }

    private void sendVerificationEmail() {
        AccountNetworkUtils.makeSendVerificationEmailRequest(mEmail, new AccountVerificationEmailHandler() {
            @Override
            public void onSuccess(@NonNull String url) {
                AppLog.add(AppLog.Type.AUTH, "Email sent (200 - " + url + ")");
            }

            @Override
            public void onFailure(@NonNull Exception e, @NonNull String url) {
                AppLog.add(AppLog.Type.AUTH, "Verification email error (" + e.getMessage() + " - " + url + ")");
            }
        });

        mHasSentEmail = true;
    }

    private void showVerifyEmail() {
        mImageIcon.setImageResource(R.drawable.ic_mail_24dp);
        mTextTitle.setText(R.string.fullscreen_verify_email_title);
        mTextSubtitle.setText(Html.fromHtml(String.format(getResources().getString(R.string.fullscreen_verify_email_subtitle), mEmail)));
        mButtonPrimary.setVisibility(View.GONE);
        mButtonSecondary.setText(R.string.fullscreen_verify_email_button_secondary);
    }
}
