package com.automattic.simplenote;

import android.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;

/**
 * Created by Dan Roundhill on 6/26/13. (In Greece!)
 */
public class TagsListActivity extends ListActivity implements AdapterView.OnItemClickListener, ActionMode.Callback, AbsListView.MultiChoiceModeListener, AdapterView.OnItemLongClickListener {

    private ActionMode mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.edit_tags));

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setMultiChoiceModeListener(this);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setBackgroundColor(getResources().getColor(R.color.white));

        listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
        listView.setDividerHeight(1);
        Simplenote application = (Simplenote) getApplication();
        Bucket<Tag> tagBucket = application.getTagsBucket();
        ObjectCursor<Tag> cursor = Tag.all(tagBucket).execute();
        TagsAdapter tagsAdapter = new TagsAdapter(getBaseContext(), cursor, 0);
        setListAdapter(tagsAdapter);

    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int row, long l) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        LinearLayout alertView = (LinearLayout)getLayoutInflater().inflate(R.layout.edit_tag, null);

        final EditText tagNameEditText = (EditText)alertView.findViewById(R.id.tag_name_edit);
        tagNameEditText.setText(((TextView)view.findViewById(R.id.tag_name)).getText());
        alert.setView(alertView);
        alert.setTitle(R.string.edit_tag);
        alert.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = tagNameEditText.getText().toString().trim();

            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();

    }

    // TODO: Finish bulk editing
    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.bulk_edit_tags, menu);
        mActionMode = actionMode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_delete:
                actionMode.finish(); // Action picked, so close the CAB
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mActionMode = null;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
        final int checkedCount = getListView().getCheckedItemCount();
        switch (checkedCount) {
            case 0:
                actionMode.setSubtitle(null);
                break;
            case 1:
                actionMode.setSubtitle("One item selected");
                break;
            default:
                actionMode.setSubtitle("" + checkedCount + " items selected");
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setItemChecked(position, true);
        if (mActionMode == null)
            startActionMode(this);
        //isCABDestroyed = false;
        return false; // so this action does not consume the event!!!
    }

    private class TagsAdapter extends CursorAdapter {

        private Cursor mCursor;

        public TagsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mCursor = c;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            mCursor.moveToPosition(position);

            if (convertView == null) {
                convertView = (LinearLayout)getLayoutInflater().inflate(R.layout.tags_list_row, null);
            }

            convertView.setTag(mCursor.getString(1));

            TextView tagTitle = (TextView)convertView.findViewById(R.id.tag_name);
            tagTitle.setText(mCursor.getString(2));

            ImageButton deleteButton = (ImageButton)convertView.findViewById(R.id.tag_trash);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getBaseContext(), "YUP" + position, Toast.LENGTH_LONG).show();
                }
            });

            return convertView;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

        }
    }
}
