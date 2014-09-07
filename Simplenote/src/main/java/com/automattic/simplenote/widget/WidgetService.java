package com.automattic.simplenote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.CursorIndexOutOfBoundsException;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.automattic.simplenote.R;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.TagsAdapter;
import com.simperium.client.Bucket;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Created by richard on 9/6/14.
 */
public class WidgetService extends RemoteViewsService {


    public static final String PREF_WIDGET_NOTE = "PREF_WIDGET_NOTE";
    public static final int NO_NOTE = -1;

    private final String TAG = "WidgetService";



    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent){
        Log.i(TAG, "onGetViewFactory");
        return new WidgetViewsFactory(getApplicationContext(), intent);
    }
    public class WidgetViewsFactory implements RemoteViewsFactory{

        private final Context mContext;
        private final int mWidgetId;
        private Bucket<Note> mNotesBucket;
        private Bucket<Tag> mTagsBucket;
        private TagsAdapter.TagMenuItem mAllNotesItem;
        private TagsAdapter mTagsAdapter;
        private Bucket.ObjectCursor<Note> mNoteCursor;



        public WidgetViewsFactory(Context ctx, Intent i){
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

            if (mNotesBucket.count() == 0) {
                // do nothing.
                Log.i(TAG, "No notes available.");
                return;
            }

            if (mTagsBucket == null) {
                mTagsBucket = currentApp.getTagsBucket();
            }

            mTagsAdapter = new TagsAdapter(mContext, mNotesBucket);

            // TODO will allObjects ever return null?
            mNoteCursor = mNotesBucket.allObjects();

            if (mNoteCursor == null || mNoteCursor.getCount() == 0) {
                Log.i(TAG, "no notes exist");
                return;
            }

            Log.i(TAG, "Found " + mTagsAdapter.getCount() + " tags items.");
            if (mTagsAdapter.getCount() > 0){

                TagsAdapter.TagMenuItem tmi = mTagsAdapter.getDefaultItem();
                Log.i(TAG, "Default tag item is : '" + tmi.name + "'");

            }

            Log.i(TAG, "Found " + mNotesBucket.count() + " notes");

        }

        private void setCursorPositionToSaved(){
            // see if there's a note saved
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int currentNote = prefs.getInt(PREF_WIDGET_NOTE, NO_NOTE);

            if (currentNote == NO_NOTE || currentNote < 0) {

                currentNote = 0;

                // override whatever value is in shared preferences because it wasn't valid.
                saveNoteIndex(prefs, currentNote);

            } else if (currentNote > mNoteCursor.getCount() - 1) {

                currentNote = mNoteCursor.getCount() - 1;

                // override whatever value is in shared preferences because it wasn't valid.
                saveNoteIndex(prefs, currentNote);

            }

            mNoteCursor.move(currentNote);

            Log.i(TAG, "current note overridden to : " + currentNote);
        }

        private void findNoteByKey(SharedPreferences prefs, String key){
            boolean found = false;
            while (!mNoteCursor.isAfterLast()){
                if (mNoteCursor.getObject().getSimperiumKey().equals(key)){
                    found = true;
                    break;
                }
                mNoteCursor.moveToNext();
            }

            if (!found){

                saveNoteIndex(prefs, 0);
                Log.i(TAG, key + " not found. set to first: "
                        + mNoteCursor.getObject().getSimperiumKey());
            } else {
                Log.i(TAG, "Note set to " + key);
            }


        }

        @Override
        public int getCount(){
            // TODO return count from cursor.
            Log.i(TAG, "WidgetViewsFactory.getCount");
            return mNoteCursor == null ? 0 : mNoteCursor.getCount();
        }

        @Override
        public void onDataSetChanged() {
            Log.i(TAG, "WidgetViewsFactory.onDataSetChanged");

        }

        @Override
        public int getViewTypeCount() {
            Log.i(TAG, "WidgetViewsFactory.getViewTypeCount");
            return 1;
        }

        @Override
        public long getItemId(int position) {
            Log.i(TAG, "WidgetViewsFactory.getItemId: returning " + mNoteCursor.getObject().hashCode());
            mNoteCursor.getObject().hashCode();
            return 0;
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
         * @param position the widget item to return a view for (0 ... getCount()-1).
         * @return the widget item's view.
         */
        @Override
        public RemoteViews getViewAt(int position) {

            setCursorPositionToSaved();

            // Create a view that will show data for this item.
            RemoteViews result = new RemoteViews(mContext.getPackageName(),
                    R.layout.widget_note_item);

            result.setTextViewText(R.id.tv_widget_note_item, mNoteCursor.getObject().getTitle());

            Log.i(TAG, "WidgetViewsFactory.getViewAt " + position + " note: "
                    + mNoteCursor.getObject().getTitle());
            return result;
        }


        @Override
        public boolean hasStableIds() {
            Log.i(TAG, "WidgetViewsFactory.hasStableIds");
            return false;
        }

        /**
         * Used to set the value of the current noted index to a specified number.
         * SimpleNoteWidgetProvider also sets the note index value (increment/decrement), but this
         * function is used by the service to ensure that the index value is always valid.
         * @param prefs the shared preferences to save to.
         */
        private void saveNoteIndex(SharedPreferences prefs, int idx){

            mNoteCursor.moveToFirst();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_WIDGET_NOTE, 0);
            editor.commit();

            Log.i(TAG, "Fetched first note: " + mNoteCursor.getObject().getSimperiumKey());
        }
    }


    // overrides


    public WidgetService() {
        super();
        Log.i(TAG, "<<init>>");
    }

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
    }

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

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart(Intent, int)");
        super.onStart(intent, startId);
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.i(TAG, "onTrimMemory");
        super.onTrimMemory(level);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind");
        super.onRebind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        Log.i(TAG, "dump");
        super.dump(fd, writer, args);
    }



}
