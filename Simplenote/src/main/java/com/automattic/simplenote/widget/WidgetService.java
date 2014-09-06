package com.automattic.simplenote.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.automattic.simplenote.R;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Created by richard on 9/6/14.
 */
public class WidgetService extends RemoteViewsService {

    private final String TAG = "WidgetService";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent){
        Log.i(TAG, "onGetViewFactory");
        return new WidgetViewsFactory(getApplicationContext(), intent);
    }
    public class WidgetViewsFactory implements RemoteViewsFactory{

        private final Context mContext;
        private final int mWidgetId;


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
        public void onCreate(){
            Log.i(TAG, "WidgetViewsFactory.onCreate");

            // TODO: setup data cursor - 20 seconds before ANR is shown
        }

        @Override
        public int getCount(){
            // TODO return count from cursor.
            Log.i(TAG, "WidgetViewsFactory.getCount");
            return 1;
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
            Log.i(TAG, "WidgetViewsFactory.getItemId");
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
         * In this version, the position should always be the same number, but in future versions
         * there may be multiple widget items with different views.
         * @param position the widget item to return a view for (0 ... getCount()-1).
         * @return the widget item's view.
         */
        @Override
        public RemoteViews getViewAt(int position) {
            Log.i(TAG, "WidgetViewsFactory.getViewAt " + position);

            // Create a view that will show data for this item.
            RemoteViews result = new RemoteViews(mContext.getPackageName(),
                    R.layout.widget_note_item);

            return result;
        }

        @Override
        public boolean hasStableIds() {
            Log.i(TAG, "WidgetViewsFactory.hasStableIds");
            return false;
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
