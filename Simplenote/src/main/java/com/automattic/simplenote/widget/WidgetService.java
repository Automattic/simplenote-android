package com.automattic.simplenote.widget;

import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_LIST_POSITION;
import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_SIMPERIUM_KEY;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.IntentUtil;
import com.automattic.simplenote.utils.TagsAdapter;
import com.simperium.client.Bucket;

/**
 * Created by richard on 9/6/14.
 */
public class WidgetService extends RemoteViewsService {


    public static final int NO_NOTE = -1;

    private final String TAG = "WidgetService";


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetViewsFactory(getApplicationContext(), intent);
    }

    public class WidgetViewsFactory implements RemoteViewsFactory {

        private final Context mContext;
        private final int mWidgetId;
        private Bucket<Note> mNotesBucket;
        private Bucket<Tag> mTagsBucket;
        private TagsAdapter.TagMenuItem mAllNotesItem;
        private TagsAdapter mTagsAdapter;


        public WidgetViewsFactory(Context ctx, Intent i) {
            mContext = ctx;
            mWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

        }

        /**
         * Initialize the data set.
         */
        @Override
        public void onCreate() {

            Simplenote currentApp = (Simplenote) mContext.getApplicationContext();

            if (mNotesBucket == null) {
                mNotesBucket = currentApp.getNotesBucket();
            }

            if (mTagsBucket == null) {
                mTagsBucket = currentApp.getTagsBucket();
            }

            mTagsAdapter = new TagsAdapter(mContext, mNotesBucket, 0);

            if (mTagsAdapter.getCount() > 0) {

                TagsAdapter.TagMenuItem tmi = mTagsAdapter.getDefaultItem();

            }

        }

        @Override
        public void onDestroy() {

            // M-T

        }

        @Override
        public void onDataSetChanged() {
            // M-T
        }

        private Note getNoteByPosition(int cursorIdx) {

            Bucket.ObjectCursor<Note> mNoteCursor = mNotesBucket.allObjects();

            mNoteCursor.moveToPosition(cursorIdx);
            Note result = mNoteCursor.getObject();
            mNoteCursor.close();

            return result;
        }

        @Override
        public int getCount() {
            // TODO return count from cursor.
            int result = mNotesBucket.count();
            return result;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {

            Note n = getNoteByPosition(position);
            return n.hashCode();
        }

        @Override
        public RemoteViews getLoadingView() {

            // Create a view that will show data for this item.
            RemoteViews result = new RemoteViews(mContext.getPackageName(),
                    R.layout.widget_note_item);

            return result;
        }

        /**
         * Construct the remote view associated with the position of a specified widget item.
         * In this version, the position ignored and the note stored in shared preferences is
         * utilized, but in future versions
         * there may be multiple widget items with different views.
         *
         * @param position the widget item to return a view for (0 ... getCount()-1).
         * @return the widget item's view.
         */
        @Override
        public RemoteViews getViewAt(int position) {

            Note n = getNoteByPosition(position);

            // Create a view that will show data for this item.
            RemoteViews result = new RemoteViews(mContext.getPackageName(),
                    R.layout.widget_note_item);

            result.setTextViewText(R.id.tv_widget_note_item, n.getTitle());

            Bundle extras = new Bundle();
            extras.putInt(EXTRA_LIST_POSITION, position);
            extras.putString(EXTRA_SIMPERIUM_KEY, n.getSimperiumKey());

            Intent fillIntent = new Intent();
            fillIntent.putExtras(extras);
            result.setOnClickFillInIntent(R.id.widget_note_item_layout, fillIntent);


            // XXX sometimes getTitle returns a null value.
            return result;
        }


        @Override
        public boolean hasStableIds() {
            return false;
        }


    }


    // overrides


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


}
