package com.automattic.simplenote.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import static com.automattic.simplenote.utils.SearchTokenizer.SPACE;

public class TagsMultiAutoCompleteTextView extends AppCompatMultiAutoCompleteTextView implements OnItemClickListener {
    private OnTagAddedListener mTagAddedListener;
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (count >= 1 && s.charAt(start) == SPACE) {
                notifyTagsChanged();
            }
        }
    };

    public interface OnTagAddedListener {
        void onTagAdded(String tag);
    }

    public TagsMultiAutoCompleteTextView(Context context) {
        super(context);
        init();
    }

    public TagsMultiAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TagsMultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        notifyTagsChanged();
    }

    public void init() {
        setOnItemClickListener(this);
        addTextChangedListener(textWatcher);
    }

    public void notifyTagsChanged() {
        notifyTagsChanged(getText().toString());
    }

    public void notifyTagsChanged(String tag) {
        if (mTagAddedListener != null) {
            mTagAddedListener.onTagAdded(tag);
        }
    }

    public void setOnTagAddedListener(OnTagAddedListener listener) {
        mTagAddedListener = listener;
    }
}
