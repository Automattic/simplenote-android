package com.automattic.simplenote.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.automattic.simplenote.NoteListFragment.NotesCursorAdapter;
import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.TagsAdapter;
import com.simperium.client.Bucket;

/**
 * Created by richard on 8/30/14.
 */
public class SimpleNoteWidgetProvider extends AppWidgetProvider{

    public static final String PREF_WIDGET_NOTE = "PREF_WIDGET_NOTE";

    private static final String TAG = "WidgetProvider";
    protected Bucket<Note> mNotesBucket;
    protected Bucket<Tag> mTagsBucket;
    private TagsAdapter.TagMenuItem mAllNotesItem;
    private TagsAdapter mTagsAdapter;
    private NotesCursorAdapter mNotesAdapter;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.i(TAG, "onReceive: intent " + intent.getAction().toString());
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate");

        Simplenote currentApp = (Simplenote) context.getApplicationContext();

        if (mNotesBucket == null) {
            mNotesBucket = currentApp.getNotesBucket();
        }

        if (mTagsBucket == null) {
            mTagsBucket = currentApp.getTagsBucket();
        }

        if (mNotesAdapter == null){
            mNotesAdapter = new NotesCursorAdapter(currentApp, null, 0);
        }


        mTagsAdapter = new TagsAdapter(context, mNotesBucket);

        Log.i(TAG, "Found " + mTagsAdapter.getCount() + " tags items.");
        if (mTagsAdapter.getCount() > 0){
            TagsAdapter.TagMenuItem tmi = mTagsAdapter.getDefaultItem();
            Log.i(TAG, "Default tag item is : '" + tmi.name + "'");

        }

        Log.i(TAG, "Found " + mNotesBucket.count() + " notes");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String curNote= pref.getString(PREF_WIDGET_NOTE, null);
        if (curNote != null){
            // load a note
        } else {
            selectFirstNote();
        }

    }

    /**
     * Selects first row in the list if available
     */
    public void selectFirstNote() {
        if (mNotesAdapter.getCount() > 0) {
            Note selectedNote = mNotesAdapter.getItem(0);
            mCallbacks.onNoteSelected(selectedNote.getSimperiumKey(), false, null);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.i(TAG, "onDeleted");

    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "onEnabled");

    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.i(TAG, "onDisabled");

    }
}
