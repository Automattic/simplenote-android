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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.ShareButtonAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jesus Gumiel on 24/08/2016.
 */
public class ReminderBottomSheetDialog extends BottomSheetDialogBase {

	private static final int SHARE_SHEET_COLUMN_COUNT = 3;

	//private TextView mPublishButton;
	//private TextView mUnpublishButton;
	//private RecyclerView mRecyclerView;

	private Fragment mFragment;
	private Intent mShareIntent;
	private List<ShareButtonAdapter.ShareButtonItem> mShareButtons;

	public ReminderBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final ReminderSheetListener reminderSheetListener) {
		super(fragment.getActivity());

		mFragment = fragment;

		View reminderView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.bottom_sheet_reminder, null, false);
	/*	mPublishButton = (TextView) reminderView.findViewById(R.id.share_publish_button);
		mUnpublishButton = (TextView) reminderView.findViewById(R.id.share_unpublish_button);

		mRecyclerView = (RecyclerView) reminderView.findViewById(R.id.share_button_recycler_view);
		mRecyclerView.setHasFixedSize(true);
		mRecyclerView.setLayoutManager(new GridLayoutManager(fragment.getActivity(), SHARE_SHEET_COLUMN_COUNT));

		mShareIntent = new Intent(Intent.ACTION_SEND);
		mShareIntent.setType("text/plain");

		mShareButtons = getShareButtons(fragment.getActivity(), mShareIntent);
*/
		setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				reminderSheetListener.onReminderDismissed();
			}
		});

		setContentView(reminderView);
	}

	public void show(Note note) {

		if (mFragment.isAdded()) {

		/*	mShareIntent.putExtra(Intent.EXTRA_TEXT, note.getContent());

			final ShareButtonAdapter.ItemListener shareListener = new ShareButtonAdapter.ItemListener() {
				@Override
				public void onItemClick(ShareButtonAdapter.ShareButtonItem item) {
					mShareIntent.setComponent(new ComponentName(item.getPackageName(), item.getActivityName()));
					mFragment.getActivity().startActivity(Intent.createChooser(mShareIntent, mFragment.getString(R.string.share)));
					dismiss();
				}
			};
*/
			//mRecyclerView.setAdapter(new ShareButtonAdapter(mShareButtons, shareListener));

			show();
		}
	}

	@NonNull
	private List<ShareButtonAdapter.ShareButtonItem> getShareButtons(Activity activity, Intent intent) {

		List<ShareButtonAdapter.ShareButtonItem> shareButtons = new ArrayList<>();
		final List<ResolveInfo> matches = activity.getPackageManager().queryIntentActivities(intent, 0);
		for (ResolveInfo match : matches) {
			final Drawable icon = match.loadIcon(activity.getPackageManager());
			final CharSequence label = match.loadLabel(activity.getPackageManager());
			shareButtons.add(new ShareButtonAdapter.ShareButtonItem(icon, label,
					match.activityInfo.packageName, match.activityInfo.name));
		}

		return shareButtons;
	}

	public interface ReminderSheetListener {
		void onReminderOn();

		void onReminderOff();

		void onReminderDismissed();
	}
}
