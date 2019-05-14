package com.automattic.simplenote;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.IconResizer;
import com.automattic.simplenote.utils.ShareButtonAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ondrej Ruttkay on 26/03/2016.
 */
public class ShareBottomSheetDialog extends BottomSheetDialogBase {

    private static final int SHARE_SHEET_COLUMN_COUNT = 3;

    private TextView mPublishButton;
    private TextView mUnpublishButton;
    private TextView mWordPressButton;
    private RecyclerView mRecyclerView;

    private Fragment mFragment;
    private Intent mShareIntent;
    private List<ShareButtonAdapter.ShareButtonItem> mShareButtons;

    public ShareBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final ShareSheetListener shareSheetListener) {
        super(fragment.requireActivity());
        mFragment = fragment;
        setContentView(R.layout.bottom_sheet_share);
        TextView mCollaborateButton = findViewById(R.id.share_collaborate_button);
        mPublishButton = findViewById(R.id.share_publish_button);
        mUnpublishButton = findViewById(R.id.share_unpublish_button);
        mWordPressButton = findViewById(R.id.share_wp_post);

        mCollaborateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareSheetListener.onShareCollaborateClicked();
            }
        });

        mPublishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareSheetListener.onSharePublishClicked();
            }
        });

        mUnpublishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareSheetListener.onShareUnpublishClicked();
            }
        });

        mWordPressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareSheetListener.onWordPressPostClicked();
            }
        });

        mRecyclerView = findViewById(R.id.share_button_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new GridLayoutManager(fragment.requireActivity(), SHARE_SHEET_COLUMN_COUNT));

        mShareIntent = new Intent(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");

        mShareButtons = getShareButtons(fragment.requireActivity(), mShareIntent);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                shareSheetListener.onShareDismissed();
            }
        });
    }

    public void show(Note note) {
        if (mFragment.isAdded()) {
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

            show();
        }
    }

    @NonNull
    private List<ShareButtonAdapter.ShareButtonItem> getShareButtons(Activity activity, Intent intent) {
        List<ShareButtonAdapter.ShareButtonItem> shareButtons = new ArrayList<>();
        final List<ResolveInfo> matches = activity.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo match : matches) {
            IconResizer iconResizer = new IconResizer(getContext());
            final Drawable icon = iconResizer.createIconThumbnail(match.loadIcon(activity.getPackageManager()));
            final CharSequence label = match.loadLabel(activity.getPackageManager());
            shareButtons.add(new ShareButtonAdapter.ShareButtonItem(icon, label,
                    match.activityInfo.packageName, match.activityInfo.name));
        }

        return shareButtons;
    }

    public interface ShareSheetListener {
        void onSharePublishClicked();

        void onShareUnpublishClicked();

        void onShareCollaborateClicked();

        void onShareDismissed();

        void onWordPressPostClicked();
    }
}
