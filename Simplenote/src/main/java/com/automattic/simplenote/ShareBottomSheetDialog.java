package com.automattic.simplenote;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.NetworkUtils;

public class ShareBottomSheetDialog extends BottomSheetDialogBase {
    public static final String TAG = ShareBottomSheetDialog.class.getSimpleName();

    private Fragment mFragment;
    private ShareSheetListener mListener;
    private TextView mPublishButton;
    private TextView mUnpublishButton;
    private TextView mWordPressButton;

    public ShareBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final ShareSheetListener shareSheetListener) {
        mFragment = fragment;
        mListener = shareSheetListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mListener.onShareDismissed();
                }
            });

            getDialog().setContentView(R.layout.bottom_sheet_share);
            TextView mCollaborateButton = getDialog().findViewById(R.id.share_collaborate_button);
            TextView mShareOtherButton = getDialog().findViewById(R.id.share_other_button);
            mPublishButton = getDialog().findViewById(R.id.share_publish_button);
            mUnpublishButton = getDialog().findViewById(R.id.share_unpublish_button);
            mWordPressButton = getDialog().findViewById(R.id.share_wp_post);

            mCollaborateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onShareCollaborateClicked();
                }
            });

            mPublishButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                        Toast.makeText(requireContext(), R.string.error_network_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    mListener.onSharePublishClicked();
                }
            });

            mUnpublishButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                        Toast.makeText(requireContext(), R.string.error_network_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    mListener.onShareUnpublishClicked();
                }
            });

            mWordPressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onWordPressPostClicked();
                }
            });

            mShareOtherButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onShareOtherClicked();
                    dismiss();
                }
            });
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void show(FragmentManager manager, Note note) {
        if (mFragment.isAdded()) {
            showNow(manager, TAG);

            if (note.isPublished()) {
                mPublishButton.setVisibility(View.GONE);
                mUnpublishButton.setVisibility(View.VISIBLE);
            } else {
                mPublishButton.setVisibility(View.VISIBLE);
                mUnpublishButton.setVisibility(View.GONE);
            }
        }
    }

    public interface ShareSheetListener {
        void onShareCollaborateClicked();
        void onShareDismissed();
        void onShareOtherClicked();
        void onSharePublishClicked();
        void onShareUnpublishClicked();
        void onWordPressPostClicked();
    }
}
