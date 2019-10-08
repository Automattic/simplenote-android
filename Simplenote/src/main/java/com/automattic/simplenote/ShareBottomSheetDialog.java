package com.automattic.simplenote;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.IconResizer;
import com.automattic.simplenote.utils.ShareButtonAdapter;

import java.util.ArrayList;
import java.util.List;

public class ShareBottomSheetDialog extends BottomSheetDialogBase {
    private static final String TAG = ShareBottomSheetDialog.class.getSimpleName();
    private static final int SHARE_SHEET_COLUMN_COUNT = 3;

    private Fragment mFragment;
    private Intent mShareIntent;
    private List<ShareButtonAdapter.ShareButtonItem> mShareButtons;
    private RecyclerView mRecyclerView;
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
                    mListener.onSharePublishClicked();
                }
            });

            mUnpublishButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onShareUnpublishClicked();
                }
            });

            mWordPressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onWordPressPostClicked();
                }
            });

            mRecyclerView = getDialog().findViewById(R.id.share_button_recycler_view);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new GridLayoutManager(mFragment.requireActivity(), SHARE_SHEET_COLUMN_COUNT));

            mShareIntent = new Intent(Intent.ACTION_SEND);
            mShareIntent.setType("text/plain");

            mShareButtons = getShareButtons(mFragment.requireActivity(), mShareIntent);
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

            mShareIntent.putExtra(Intent.EXTRA_TEXT, note.getContent());

            final ShareButtonAdapter.ItemListener shareListener = new ShareButtonAdapter.ItemListener() {
                @Override
                public void onItemClick(ShareButtonAdapter.ShareButtonItem item) {
                    mShareIntent.setComponent(new ComponentName(item.getPackageName(), item.getActivityName()));
                    mFragment.requireActivity().startActivity(Intent.createChooser(mShareIntent, mFragment.getString(R.string.share)));
                    dismiss();
                }
            };

            mRecyclerView.setAdapter(new ShareButtonAdapter(mShareButtons, shareListener));
        }
    }

    @NonNull
    private List<ShareButtonAdapter.ShareButtonItem> getShareButtons(Activity activity, Intent intent) {
        List<ShareButtonAdapter.ShareButtonItem> shareButtons = new ArrayList<>();
        final List<ResolveInfo> matches = activity.getPackageManager().queryIntentActivities(intent, 0);
        IconResizer iconResizer = new IconResizer(requireContext());

        for (ResolveInfo match : matches) {
            final Drawable icon = iconResizer.createIconThumbnail(match.loadIcon(activity.getPackageManager()));
            final CharSequence label = match.loadLabel(activity.getPackageManager());
            shareButtons.add(new ShareButtonAdapter.ShareButtonItem(icon, label,
                    match.activityInfo.packageName, match.activityInfo.name));
        }

        return shareButtons;
    }

    public interface ShareSheetListener {
        void onShareCollaborateClicked();
        void onShareDismissed();
        void onSharePublishClicked();
        void onShareUnpublishClicked();
        void onWordPressPostClicked();
    }
}
