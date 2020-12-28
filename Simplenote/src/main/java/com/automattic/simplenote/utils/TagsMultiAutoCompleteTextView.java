package com.automattic.simplenote.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;

import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.models.Tag;
import com.simperium.client.Bucket;

import static com.automattic.simplenote.utils.SearchTokenizer.SPACE;

public class TagsMultiAutoCompleteTextView extends AppCompatMultiAutoCompleteTextView implements OnItemClickListener {
    private Bucket<Tag> mBucketTag;
    private OnTagAddedListener mTagAddedListener;
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            } else {
                setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (count >= 1 && s.charAt(start) == SPACE) {
                saveTagOrShowError(s.toString());
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
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            saveTagOrShowError(getText().toString());
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        notifyTagsChanged();
    }

    public void init() {
        setOnItemClickListener(this);
        addTextChangedListener(mTextWatcher);
    }

    public void notifyTagsChanged() {
        String lexical = getText().toString().trim();
        String canonical = TagUtils.getCanonicalFromLexical(mBucketTag, lexical);
        notifyTagsChanged(canonical);
    }

    public void notifyTagsChanged(String tag) {
        if (mTagAddedListener != null) {
            mTagAddedListener.onTagAdded(tag);
        }
    }

    private void saveTagOrShowError(String text) {
        if (TagUtils.hashTagValid(text)) {
            notifyTagsChanged();
        } else {
            removeTextChangedListener(mTextWatcher);
            setText(getText().toString().trim());
            setSelection(getText().length());
            addTextChangedListener(mTextWatcher);
            Context context = getContext();
            DialogUtils.showDialogWithEmail(
                context,
                context.getString(R.string.rename_tag_message_length)
            );
        }
    }

    public void setBucketTag(Bucket<Tag> bucket) {
        mBucketTag = bucket;
    }

    public void setOnTagAddedListener(OnTagAddedListener listener) {
        mTagAddedListener = listener;
    }
}
