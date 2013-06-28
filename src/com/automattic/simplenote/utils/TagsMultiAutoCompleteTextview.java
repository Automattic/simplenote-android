package com.automattic.simplenote.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.automattic.simplenote.R;

public class TagsMultiAutoCompleteTextView extends MultiAutoCompleteTextView implements OnItemClickListener {

    private final String TAG = "TagsMultiAutoCompleteTextView";

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

    /* set listeners for item click and text change */
    public void init(Context context) {
        setOnItemClickListener(this);
        addTextChangedListener(textWatcher);
    }

    /*TextWatcher, If user types any tag name and presses space then following code will regenerate chips */
    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            // If we reach an image span, let's remove it as well as the tag text behind it.
            if (before == 1) {
                SpannableStringBuilder ssb = new SpannableStringBuilder(s);
                ImageSpan[] imageSpans = ssb.getSpans(start, start, ImageSpan.class);
                if (imageSpans.length > 0) {
                    ImageSpan tagImageSpan = imageSpans[0];
                    int tagStart = ssb.getSpanStart(tagImageSpan);
                    int tagEnd = ssb.getSpanEnd(tagImageSpan);
                    ssb.removeSpan(tagImageSpan);
                    ssb.replace(tagStart, tagEnd, "");
                    setText(ssb);
                    setSelection(ssb.length());
                    return;
                }
            }


            if (count >= 1) {
                if (s.charAt(start) == ' ')
                    setChips(); // generate chips
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /*This function has whole logic for chips generate*/
    public void setChips() {
        if (getText().toString().contains(" ")) // check space in string
        {
            SpannableStringBuilder ssb = new SpannableStringBuilder(getText());
            // split string with space
            String tags[] = getText().toString().trim().split(" ");
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
            // set chips span
            if (!getText().toString().trim().equals(""))
                setText(ssb);
            // move cursor to last
            setSelection(getText().length());
        }


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setChips(); // call generate chips when user select any item from auto complete
    }

}
