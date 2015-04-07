package com.automattic.simplenote.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.AutoCompleteTextView;

import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;

/**
 * Created by richard on 4/7/15.
 */
public class SearchNoteAutocompleteTextView extends AutoCompleteTextView {

    private Note mSelectedItem;

    public SearchNoteAutocompleteTextView(Context context) {
        super(context);
    }

    public SearchNoteAutocompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchNoteAutocompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected CharSequence convertSelectionToString(Object selectedItem) {
        Bucket.ObjectCursor<Note> cursor = (Bucket.ObjectCursor<Note>)selectedItem;
        mSelectedItem = cursor.getObject();
        return mSelectedItem.getTitle();
    }

    public Note getSelectedItem(){

        return mSelectedItem;

    }

}
