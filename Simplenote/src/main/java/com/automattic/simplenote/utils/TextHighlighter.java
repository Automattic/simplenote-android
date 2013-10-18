package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;

import com.automattic.simplenote.utils.MatchOffsetHighlighter;
import com.automattic.simplenote.utils.SearchSnippetFormatter;

public class TextHighlighter
implements MatchOffsetHighlighter.SpanFactory, SearchSnippetFormatter.SpanFactory {


    int mForegroundColor;
    int mBackgroundColor;

    public TextHighlighter(Context context, int foregroundResId, int backgroundResId) {
        TypedArray colors = context.obtainStyledAttributes(new int[]{ foregroundResId, backgroundResId });
        mForegroundColor = colors.getColor(0, 0xFFFF0000);
        mBackgroundColor = colors.getColor(1, 0xFF00FFFF);
        colors.recycle();
    }

    @Override
    public Object[] buildSpans(){
        return buildSpans(null);
    }

    @Override
    public Object[] buildSpans(String content){
        return new Object[] {
            new ForegroundColorSpan(mForegroundColor),
            new BackgroundColorSpan(mBackgroundColor)
        };
    }


}