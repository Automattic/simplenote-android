package com.automattic.simplenote.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.automattic.simplenote.R;

/**
 * Created by richard on 9/6/14.
 */
public class WidgetService extends RemoteViewsService {

    private final String TAG = "WidgetService";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent){
        return new WidgetViewsFactory(getApplicationContext(), intent);
    }
    public class WidgetViewsFactory implements RemoteViewsFactory{

        private final Context mContext;
        private final int mWidgetId;


        public WidgetViewsFactory(Context ctx, Intent i){
            mContext = ctx;
            mWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        /**
         * Initialize the data set.
         */
        @Override
        public void onCreate(){
            Log.i(TAG, "onCreate");

            // TODO: setup data cursor - 20 seconds before ANR is shown
        }

        @Override
        public int getCount(){
            // TODO return count from cursor.
            Log.i(TAG, "onCreate");
            return 0;
        }

        @Override
        public void onDataSetChanged() {
            Log.i(TAG, "onDataSetChanged");

        }

        @Override
        public int getViewTypeCount() {
            Log.i(TAG, "getViewTypeCount");
            return 0;
        }

        @Override
        public long getItemId(int position) {
            Log.i(TAG, "getItemId");
            return 0;
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "onDestroy");
        }

        @Override
        public RemoteViews getLoadingView() {
            Log.i(TAG, "getLoadingView");
            return null;
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
            Log.i(TAG, "getViewAt");

            // Create a view that will show data for this item.
            RemoteViews result = new RemoteViews(mContext.getPackageName(),
                    R.layout.widget_note_item);
            result.setTextViewText(R.id.tv_widget_note_item, "Item " + Integer.toString(position));


            return result;
        }

        @Override
        public boolean hasStableIds() {
            Log.i(TAG, "hasStableIds");
            return false;
        }
    }
}
