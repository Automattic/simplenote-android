package com.automattic.simplenote;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.TagsAdapter;
import com.simperium.client.Bucket;

/**
 * Created by richard on 4/2/15.
 */
public class WidgetSearch extends Activity {

    protected Bucket<Note> mNotesBucket;

    private Bucket.ObjectCursor<Note> mObjectCursor;
    private TagsAdapter mTagsAdapter;

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_widget_search);

        Simplenote currentApp = (Simplenote) getApplication();
        mNotesBucket = currentApp.getNotesBucket();
        mObjectCursor = mNotesBucket.allObjects();

        ((AutoCompleteTextView)findViewById(R.id.actv_search)).setAdapter(
                new NoteAdapter(this, mObjectCursor));

    }

    @Override
    protected void onResume(){
        super.onResume();
        mNotesBucket.start();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mNotesBucket = null;
    }


    private class NoteAdapter extends CursorAdapter{
        private LayoutInflater mInflater;

        public NoteAdapter(Context ctx, Bucket.ObjectCursor<Note> c){
            super(ctx, c, 0);
            mInflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            Bucket.ObjectCursor<Note> c = (Bucket.ObjectCursor<Note>)cursor;
            Note n = c.getObject();
            ((TextView)view).setText(n.getTitle());

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            Note n = ((Bucket.ObjectCursor<Note>)cursor).getObject();
            TextView result = (TextView)mInflater.inflate(R.layout.widget_search_textview, parent);
            result.setText(n.getTitle());

            return result;
        }
    }




}

