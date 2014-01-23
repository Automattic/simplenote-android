package com.automattic.simplenote.utils;

import android.os.Handler;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class MatchOffsetHighlighter implements Runnable {

    private static List<Object> mMatchedSpans = Collections.synchronizedList(new ArrayList<Object>());

    public interface SpanFactory {
        public Object[] buildSpans();
    }

    public interface OnMatchListener {
        public void onMatch(SpanFactory factory, Spannable text, int start, int end);
    }

    private static class DefaultMatcher implements OnMatchListener {

        @Override
        public void onMatch(SpanFactory factory, Spannable content, int start, int end){

            Object[] spans = factory.buildSpans();

            for (Object span : spans) {
                content.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mMatchedSpans.add(span);
            }
        }

    }

    private SpanFactory mFactory;
    private Thread mThread;
    private TextView mTextView;
    private String mMatches;
    private int mIndex;
    private Spannable mText;
    private boolean mStopped = false;

    protected static OnMatchListener sListener = new DefaultMatcher();

    private OnMatchListener mListener = new OnMatchListener() {

        @Override
        public void onMatch(final SpanFactory factory, final Spannable text, final int start, final int end) {

            if (mTextView == null) return;

            Handler handler = mTextView.getHandler();
            if (handler == null) return;

            handler.post(new Runnable() {

                @Override
                public void run(){
                    if (mStopped) return;
                    sListener.onMatch(factory, text, start, end);
                }

            });
        }

    };

    public MatchOffsetHighlighter(SpanFactory factory, TextView textView) {
        mFactory = factory;
        mTextView = textView;
    }

    @Override
    public void run(){
        highlightMatches(mText, mMatches, mIndex, mFactory, mListener);
    }

    public void start(){
        // if there are no matches, we don't have to do anything
        if (TextUtils.isEmpty(mMatches)) return;

        mThread = new Thread(this);
        mStopped = false;
        mThread.start();
    }

    public void stop(){
        mStopped = true;
        if (mThread != null) mThread.interrupt();
    }

    public void highlightMatches(String matches, int columnIndex) {
        synchronized(this){
            stop();
            mText = mTextView.getEditableText();
            mMatches = matches;
            mIndex = columnIndex;
            start();
        }
    }

    public static void highlightMatches(Spannable content, String matches, int columnIndex,
    SpanFactory factory) {

        highlightMatches(content, matches, columnIndex, factory, new DefaultMatcher());
    }

    public static void highlightMatches(Spannable content, String matches, int columnIndex,
        SpanFactory factory, OnMatchListener listener) {

        if (TextUtils.isEmpty(matches)) return;

        Scanner scanner = new Scanner(matches);
        int count = 0;

        int total_offset = 0;

        // TODO: keep track of offsets and last index so we don't have to recalculate the entire byte length for every match which is pretty memory intensive
        while (scanner.hasNext()) {

            if (Thread.interrupted()) return;

            int column = scanner.nextInt();
            int token = scanner.nextInt();
            int start = scanner.nextInt();
            int length = scanner.nextInt();

            if (column != columnIndex) continue;

            int span_start = start + getByteOffset(content, 0, start);
            int span_end = span_start + length + getByteOffset(content, start, start + length);

            if (Thread.interrupted()) return;

            listener.onMatch(factory, content, span_start, span_end);

        }
    }

    public synchronized void removeMatches() {
        stop();
        if (mText != null && mMatchedSpans != null) {
            for (Object span : mMatchedSpans) {
                mText.removeSpan(span);
            }
            mMatchedSpans.clear();
        }
    }

    static public final String CHARSET = "UTF-8";

    // TODO: get ride of memory pressure by preventing the toString()
    protected static int getByteOffset(CharSequence text, int start, int end) {
        String source = text.toString();
        String substring = "";
        int length = source.length();

        // starting index cannot be negative
        if (start < 0) {
            start = 0;
        }

        if (start > length - 1) {
            // if start is past the end of string
            return 0;
        } else if (end > length - 1) {
            // end is past the end of the string, so cap at string's end
            substring = source.substring(start, length -1);
        } else {
            // start and end are both valid indices
            substring = source.substring(start, end);
        }
        try {
            return substring.length() - substring.getBytes(CHARSET).length;
        } catch (UnsupportedEncodingException e) {
            return 0;
        }
    }

}