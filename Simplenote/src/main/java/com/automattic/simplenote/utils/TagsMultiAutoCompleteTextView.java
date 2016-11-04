package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.automattic.simplenote.NoteListFragment;
import com.automattic.simplenote.R;
import java.util.LinkedList;

public class TagsMultiAutoCompleteTextView extends AppCompatMultiAutoCompleteTextView implements OnItemClickListener {

    private boolean mShouldMoveNewTagText;
    private OnTagAddedListener mTagsChangedListener;

    /* Constructor */
    public TagsMultiAutoCompleteTextView(Context context) {
        super(context);
        init(context);
    }

    /* Constructor */
    public TagsMultiAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /* Constructor */
    public TagsMultiAutoCompleteTextView(Context context, AttributeSet attrs,
                                         int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setOnTagAddedListener(OnTagAddedListener listener) {
        mTagsChangedListener = listener;
    }

    /* set listeners for item click and text change */
    public void init(Context context) {
        setOnItemClickListener(this);
        addTextChangedListener(textWatcher);
    }
    /*This function has whole logic for chips generate*/
    public void drawChips(LinkedList<String> tagList, NoteListFragment aNoteListFragment) {
        // LinkedList<Integer> xs = new LinkedList<Integer>();
        StringBuilder txt = new StringBuilder();
        String appendix = " | ×";
        for (String tag : tagList) {
            txt.append(tag);
            txt.append(appendix);
            txt.append(" ");
        }

        String text = txt.toString();
        int cursorLocation = getSelectionStart();
        // split string with space
        SimpleStringSplitter tags = new SimpleStringSplitter(' ');
        tags.setString(text.toString());
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        int x = 0;
        // Loop will generate ImageSpan for every tag separated by spaces
        for (String tag : tags) {
            // Inflate tags_textview layout
            LayoutInflater lf = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            TextView textView = (TextView) lf.inflate(R.layout.tags_textview, null);
            textView.setText(tag); // set text

            // Capture bitmap of generated textview
            int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            textView.measure(spec, spec);
            textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
            Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            canvas.translate(-textView.getScrollX(), -textView.getScrollY());
            textView.draw(canvas);
            textView.setDrawingCacheEnabled(true);
            Bitmap cacheBmp = textView.getDrawingCache();
            Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
            textView.destroyDrawingCache();  // destory drawable
            // Create bitmap drawable for imagespan
            BitmapDrawable bmpDrawable = new BitmapDrawable(getContext().getResources(), viewBmp);
            bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
            // Create and set imagespan
            ssb.setSpan(new ImageSpan(bmpDrawable), x, x + tag.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ClickableSpan clickSpan = new IndexedClickableSpan(tagList.indexOf(tag.substring(0,tag.length()-appendix.length())), aNoteListFragment);
            ssb.setSpan(clickSpan, x, x + tag.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            //ssb.setSpan(new ForegroundColorSpan(Color.RED), x + tag.length()-3, x + tag.length(),  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            x = x + tag.length() + 1;
            //xs.add(x);
        }
        if (ssb.length() > 0) ssb.append(' ');
        // set chips span
        setText(ssb);

        setMovementMethod(LinkMovementMethod.getInstance());
        setSelection(getText().length());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        notifyTagsChanged();
    }


    public class IndexedClickableSpan extends ClickableSpan {
        int _index;
        NoteListFragment mNoteListFragment;

        public IndexedClickableSpan(int index, NoteListFragment aNoteListFragment) {
            this._index = index;
            mNoteListFragment = aNoteListFragment;
        }

        @Override
        public void onClick(View v) {
            mNoteListFragment.removeTag(_index);
        }
    }


    /*TextWatcher, If user types any tag name and presses space then following code will regenerate chips */
    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (mShouldMoveNewTagText && before > 0)
                mShouldMoveNewTagText = false;

            if (mShouldMoveNewTagText) {
                mShouldMoveNewTagText = false;
                // Get the entered text, and move it to the end of the editor
                SpannableStringBuilder ssb = new SpannableStringBuilder(s);
                CharSequence addedText = ssb.subSequence(start, start + count);
                ssb.replace(start, start + count, "");
                ssb.append(addedText);
                // Don't want text watcher to fire when we set the text, so we'll remove it temporarily.
                removeTextChangedListener(this);
                setText(ssb);
                addTextChangedListener(this);
                setSelection(getText().length());
                return;
            }

            // If we reach an image span, let's remove it as well as the tag text behind it
            if (before == 1) {
                SpannableStringBuilder ssb = new SpannableStringBuilder(s);
                ImageSpan[] imageSpans = ssb.getSpans(start, start, ImageSpan.class);
                if (imageSpans.length > 0) {
                    ImageSpan tagImageSpan = imageSpans[0];
                    int tagStart = ssb.getSpanStart(tagImageSpan);
                    int tagEnd = ssb.getSpanEnd(tagImageSpan);
                    ssb.removeSpan(tagImageSpan);
                    ssb.replace(tagStart, tagEnd, "");
                    notifyTagsChanged(ssb.toString());
                    return;
                }
            }

            if (count >= 1 && s.charAt(start) == ' ') notifyTagsChanged();

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            SpannableStringBuilder ssb = new SpannableStringBuilder(s);
            ImageSpan[] imageSpans = ssb.getSpans(start, start, ImageSpan.class);

            // only allow tags to be added at the end of the text
            if (imageSpans.length > 0 && after > 0) {
                mShouldMoveNewTagText = true;
            }

        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public void notifyTagsChanged() {
        notifyTagsChanged(getText().toString());
    }

    public void notifyTagsChanged(String tagString) {
        if (mTagsChangedListener != null) {
            mTagsChangedListener.onTagsChanged(tagString);
        }
    }

    /*This function has whole logic for chips generate*/
    public void setChips(CharSequence text) {
        // split string with space
        SimpleStringSplitter tags = new SimpleStringSplitter(' ');
        tags.setString(text.toString());
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        int x = 0;
        // Loop will generate ImageSpan for every tag separated by spaces
        for (String tag : tags) {
            // Inflate tags_textview layout
            LayoutInflater lf = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            TextView textView = (TextView) lf.inflate(R.layout.tags_textview, null);
            textView.setText(tag); // set text

            // Capture bitmap of generated textview
            int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            textView.measure(spec, spec);
            textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
            Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            canvas.translate(-textView.getScrollX(), -textView.getScrollY());
            textView.draw(canvas);
            textView.setDrawingCacheEnabled(true);
            Bitmap cacheBmp = textView.getDrawingCache();
            Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
            textView.destroyDrawingCache();  // destory drawable
            // Create bitmap drawable for imagespan
            BitmapDrawable bmpDrawable = new BitmapDrawable(getContext().getResources(), viewBmp);
            bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());
            // Create and set imagespan
            ssb.setSpan(new ImageSpan(bmpDrawable), x, x + tag.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            x = x + tag.length() + 1;
        }
        if (ssb.length() > 0) ssb.append(' ');
        // set chips span
        setText(ssb);
        setSelection(getText().length());

    }



    public interface OnTagAddedListener {
        void onTagsChanged(String tagString);
    }

}
