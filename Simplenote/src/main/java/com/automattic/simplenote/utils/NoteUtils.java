package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.mobeta.android.dslv.DragSortItemView;
import com.mobeta.android.dslv.DragSortListView;

import java.util.Calendar;

/**
 * Created by Ondrej Ruttkay on 28/03/2016.
 */
public class NoteUtils {

    public static void setNotePin(Note note, boolean isPinned) {
        if (note != null && isPinned != note.isPinned()) {
            note.setPinned(isPinned);
            note.setModificationDate(Calendar.getInstance());
            note.save();

            AnalyticsTracker.track(
                    isPinned ? AnalyticsTracker.Stat.EDITOR_NOTE_PINNED :
                            AnalyticsTracker.Stat.EDITOR_NOTE_UNPINNED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "pin_button"
            );
        }
    }

    public static void deleteNote(Note note, Activity activity) {
        if (note != null) {
            note.setDeleted(!note.isDeleted());
            note.setModificationDate(Calendar.getInstance());
            note.save();
            Intent resultIntent = new Intent();
            if (note.isDeleted()) {
                resultIntent.putExtra(Simplenote.DELETED_NOTE_ID, note.getSimperiumKey());
            }
            activity.setResult(Activity.RESULT_OK, resultIntent);

            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.EDITOR_NOTE_DELETED,
                    AnalyticsTracker.CATEGORY_NOTE,
                    "trash_menu_item"
            );
        }
    }

    public static void templateNote(Note note, Activity activity) {
        if (note != null) {
            note.setTemplate(!note.isTemplate());
            note.setModificationDate(Calendar.getInstance());
            note.save();
            Intent resultIntent = new Intent();
            if (note.isDeleted()) {
                resultIntent.putExtra(Simplenote.TEMPLATE_NOTE_ID, note.getSimperiumKey());
            }
            activity.setResult(Activity.RESULT_OK, resultIntent);
        }
    }

    public static void setListViewHeight(DragSortListView listView) {
            ListAdapter listAdapter = listView.getAdapter();
            if (listAdapter == null)
                return;

            int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(),
                    View.MeasureSpec.UNSPECIFIED);
            int totalHeight = 0;
            View view = null;
            int count = listAdapter.getCount();

            for (int i = 0; i < count; i++) {
                view = listAdapter.getView(i, null, listView);
                if (i == 0) {
                    view.setLayoutParams(new RelativeLayout.LayoutParams(desiredWidth, RelativeLayout.LayoutParams.WRAP_CONTENT));
                }

                view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                totalHeight += view.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight
                    + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
            listView.setLayoutParams(params);
        listView.requestLayout();
        listView.getParent().requestLayout();
    }


}
