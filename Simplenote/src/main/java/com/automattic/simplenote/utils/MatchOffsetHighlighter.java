package com.automattic.simplenote.utils;

import android.text.Spanned;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.os.Handler;
import android.widget.TextView;

import java.util.Scanner;
import java.io.UnsupportedEncodingException;

public class MatchOffsetHighlighter {

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
                android.util.Log.d("Simplenote", "Setting span");
                content.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

    }

    public static void highlightMatchesOnTextView(final TextView textView, final String matches,
        final int columnIndex, final SpanFactory factory) {

        final Spannable content = textView.getEditableText();
        final Handler handler = textView.getHandler();
        final OnMatchListener listener = new DefaultMatcher();

        Thread highlighter = new Thread(new Runnable() {

            @Override
            public void run(){
                highlightMatches(content, matches, columnIndex, factory, new OnMatchListener() {

                    @Override
                    public void onMatch(final SpanFactory factory, final Spannable content,
                        final int start, final int end) {

                        if (textView == null){
                            Thread.currentThread().interrupt();
                            return;
                        }

                        if (handler == null) {
                            Thread.currentThread().interrupt();
                            return;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run(){
                                if (textView != null)
                                    listener.onMatch(factory, content, start, end);
                            }
                        });

                    }
                });
                android.util.Log.d("Simplenote", "Done highlighting");
            }

        });

        highlighter.start();
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

        while (scanner.hasNext()) {

            if (Thread.interrupted()) return;

            android.util.Log.d("Simplenote", "adding highlight");

            int column = scanner.nextInt();
            int token = scanner.nextInt();
            int start = scanner.nextInt();
            int length = scanner.nextInt();

            if (column != columnIndex) continue;

            int span_start = start + getByteOffset(content, 0, start);
            int span_end = span_start + length + getByteOffset(content, start, start + length);

            listener.onMatch(factory, content, span_start, span_end);

        }

    }

    static public final String CHARSET = "UTF-8";

    protected static int getByteOffset(CharSequence text, int start, int end) {
        String substring = text.toString().substring(start, end);
        try {
            return substring.length() - substring.getBytes(CHARSET).length;
        } catch (UnsupportedEncodingException e) {
            return 0;
        }
    }

}