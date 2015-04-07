package com.automattic.simplenote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.TagsAdapter;
import com.automattic.simplenote.widget.SearchNoteAutocompleteTextView;
import com.simperium.client.Bucket;

import static com.automattic.simplenote.utils.PrefUtils.PREF_ACTIVITY_COMMAND;
import static com.automattic.simplenote.widget.commands.WidgetConstants.EXTRA_SIMPERIUM_KEY;

/**
 * Created by richard on 4/2/15.
 */
public class WidgetSearch extends Activity {

    // create a custom view that overrides AutoCompleteTextView.

    protected Bucket<Note> mNotesBucket;

    private SearchNoteAutocompleteTextView mAutocompleteTextView;

    private Bucket.ObjectCursor<Note> mObjectCursor;
    private TagsAdapter mTagsAdapter;

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_widget_search);

        mAutocompleteTextView = (SearchNoteAutocompleteTextView)findViewById(R.id.actv_search);

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


    public void onGo(View v){

        // get the selected item, and launch the activity.

        Note note = mAutocompleteTextView.getSelectedItem();
        if (note == null){
            return;
        }

        Intent i = new Intent(this, com.automattic.simplenote.NotesActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(EXTRA_SIMPERIUM_KEY, note.getSimperiumKey());

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                this).edit();
        editor.putString(PREF_ACTIVITY_COMMAND, ActivityCommand.EDIT_NOTE.name());
        editor.commit();

        this.startActivity(i);




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
            TextView result = (TextView)mInflater.inflate(R.layout.widget_search_textview, parent,
                    false);
            result.setText(n.getTitle());

            return result;
        }
    }




}

