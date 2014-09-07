package com.automattic.simplenote.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
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
        Log.i(TAG, "onGetViewFactory");
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

            Log.i(TAG, "WidgetViewsFactory.<<init>> for id " + mWidgetId);
        }

        /**
         * Initialize the data set.
         */
        @Override
        public void onCreate() {

            Log.i(TAG, "WidgetViewsFactory.onCreate");
            Simplenote currentApp = (Simplenote) mContext.getApplicationContext();

            if (mNotesBucket == null) {
                mNotesBucket = currentApp.getNotesBucket();
            }

            if (mTagsBucket == null) {
                mTagsBucket = currentApp.getTagsBucket();
            }

            mTagsAdapter = new TagsAdapter(mContext, mNotesBucket);

            Log.i(TAG, "Found " + mTagsAdapter.getCount() + " tags items.");
            if (mTagsAdapter.getCount() > 0) {

                TagsAdapter.TagMenuItem tmi = mTagsAdapter.getDefaultItem();
                Log.i(TAG, "Default tag item is : '" + tmi.name + "'");

            }

            Log.i(TAG, "Found " + mNotesBucket.count() + " notes");

        }

        private Note getNoteByPosition(int cursorIdx) {

            Bucket.ObjectCursor<Note> mNoteCursor = mNotesBucket.allObjects();


            Log.i(TAG, "fetching note from position " + cursorIdx);
            mNoteCursor.moveToPosition(cursorIdx);
            Note result = mNoteCursor.getObject();
            mNoteCursor.close();

            return result;
        }

        @Override
        public int getCount() {
            // TODO return count from cursor.
            int result = mNotesBucket.count();
            Log.i(TAG, "WidgetViewsFactory.getCount: " + result);
            return result;
        }

        @Override
        public void onDataSetChanged() {
            Log.i(TAG, "WidgetViewsFactory.onDataSetChanged");
        }

        @Override
        public int getViewTypeCount() {
            Log.i(TAG, "WidgetViewsFactory.getViewTypeCount: 1");
            return 1;
        }

        @Override
        public long getItemId(int position) {

            Note n = getNoteByPosition(position);
            Log.i(TAG, "WidgetViewsFactory.getItemId: found note " + n.getTitle());
            return n.hashCode();
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "WidgetViewsFactory.onDestroy");
        }

        @Override
        public RemoteViews getLoadingView() {
            Log.i(TAG, "WidgetViewsFactory.getLoadingView");

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

            Log.i(TAG, "WidgetViewsFactory.getViewAt " + position + " note: "
                    + n.getTitle());
            return result;
        }


        @Override
        public boolean hasStableIds() {
            Log.i(TAG, "WidgetViewsFactory.hasStableIds");
            return false;
        }


    }


    // overrides


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: " + intent.toString());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
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

            Log.i(TAG, "WidgetViewsFactory.<<init>> for id " + mWidgetId);
        }

        /**
         * Initialize the data set.
         */
        @Override
        public void onCreate() {

            Log.i(TAG, "WidgetViewsFactory.onCreate");
            Simplenote currentApp = (Simplenote) mContext.getApplicationContext();

            if (mNotesBucket == null) {
                mNotesBucket = currentApp.getNotesBucket();
            }

            if (mTagsBucket == null) {
                mTagsBucket = currentApp.getTagsBucket();
            }

            mTagsAdapter = new TagsAdapter(mContext, mNotesBucket);

            Log.i(TAG, "Found " + mTagsAdapter.getCount() + " tags items.");
            if (mTagsAdapter.getCount() > 0) {

                TagsAdapter.TagMenuItem tmi = mTagsAdapter.getDefaultItem();
                Log.i(TAG, "Default tag item is : '" + tmi.name + "'");

            }

            Log.i(TAG, "Found " + mNotesBucket.count() + " notes");

        }

        private Note getNoteByPosition(int cursorIdx) {

            Bucket.ObjectCursor<Note> mNoteCursor = mNotesBucket.allObjects();


            Log.i(TAG, "fetching note from position " + cursorIdx);
            mNoteCursor.moveToPosition(cursorIdx);
            Note result = mNoteCursor.getObject();
            mNoteCursor.close();

            return result;
        }

        @Override
        public int getCount() {
            // TODO return count from cursor.
            int result = mNotesBucket.count();
            Log.i(TAG, "WidgetViewsFactory.getCount: " + result);
            return result;
        }

        @Override
        public void onDataSetChanged() {
            Log.i(TAG, "WidgetViewsFactory.onDataSetChanged");
        }

        @Override
        public int getViewTypeCount() {
            Log.i(TAG, "WidgetViewsFactory.getViewTypeCount: 1");
            return 1;
        }

        @Override
        public long getItemId(int position) {

            Note n = getNoteByPosition(position);
            Log.i(TAG, "WidgetViewsFactory.getItemId: found note " + n.getTitle());
            return n.hashCode();
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "WidgetViewsFactory.onDestroy");
        }

        @Override
        public RemoteViews getLoadingView() {
            Log.i(TAG, "WidgetViewsFactory.getLoadingView");

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

            Log.i(TAG, "WidgetViewsFactory.getViewAt " + position + " note: "
                    + n.getTitle());
            return result;
        }


        @Override
        public boolean hasStableIds() {
            Log.i(TAG, "WidgetViewsFactory.hasStableIds");
            return false;
        }


    }    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }


}
