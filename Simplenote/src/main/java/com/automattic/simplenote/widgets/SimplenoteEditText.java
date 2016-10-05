package com.automattic.simplenote.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class SimplenoteEditText extends EditText {

    private List<OnSelectionChangedListener> listeners;

    public SimplenoteEditText(Context context) {
        super(context);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public SimplenoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public SimplenoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public void addOnSelectionChangedListener(OnSelectionChangedListener o) {
        listeners.add(o);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (listeners != null) {
            for (OnSelectionChangedListener l : listeners)
                l.onSelectionChanged(selStart, selEnd);
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selStart, int selEnd);
    }
}
