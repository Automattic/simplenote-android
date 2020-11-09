package com.automattic.simplenote;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import androidx.annotation.LayoutRes;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.ChecklistUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.simperium.client.Bucket;
import com.simperium.client.Query;

import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_LIST_WIDGET_NOTE_TAPPED;
import static com.automattic.simplenote.utils.WidgetUtils.KEY_LIST_WIDGET_CLICK;

public class NoteListWidgetFactory implements RemoteViewsFactory {
    public static final String EXTRA_IS_LIGHT = "is_light";

    private Bucket.ObjectCursor<Note> mCursor;
    private Context mContext;
    private boolean mIsLight;

    public NoteListWidgetFactory(Context context, Intent intent) {
        mContext = context;
        mIsLight = intent.getExtras() == null || intent.getExtras().getBoolean(EXTRA_IS_LIGHT, true);
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        @LayoutRes int layout = PrefUtils.getLayoutWidgetListItem(mContext, mIsLight);
        RemoteViews views = new RemoteViews(mContext.getPackageName(), layout);

        if (mCursor.moveToPosition(position)) {
            Note note = mCursor.getObject();

            views.setTextViewText(R.id.note_title, note.getTitle());
            SpannableStringBuilder contentSpan = new SpannableStringBuilder(note.getContentPreview());
            contentSpan = (SpannableStringBuilder) ChecklistUtils.addChecklistUnicodeSpansForRegex(
                contentSpan,
                ChecklistUtils.CHECKLIST_REGEX
            );
            views.setTextViewText(R.id.note_content, contentSpan);
            views.setViewVisibility(R.id.note_pinned, note.isPinned() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.note_published, note.isPublished() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.note_status, note.isPinned() || note.isPublished() ? View.VISIBLE : View.GONE);

            boolean isCondensed = PrefUtils.getBoolPref(mContext, PrefUtils.PREF_CONDENSED_LIST, false);
            views.setViewVisibility(R.id.note_content, isCondensed ? View.GONE : View.VISIBLE);

            // Create intent to navigate to note editor on note list item click
            Intent intent = new Intent(mContext, NoteEditorActivity.class);
            intent.putExtra(KEY_LIST_WIDGET_CLICK, NOTE_LIST_WIDGET_NOTE_TAPPED);
            intent.putExtra(NoteEditorFragment.ARG_IS_FROM_WIDGET, true);
            intent.putExtra(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
            intent.putExtra(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());
            intent.putExtra(NoteEditorFragment.ARG_PREVIEW_ENABLED, note.isPreviewEnabled());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            views.setOnClickFillInIntent(R.id.widget_item, intent);
        }

        return views;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        Bucket<Note> notesBucket = ((Simplenote) mContext.getApplicationContext()).getNotesBucket();
        Query<Note> query = Note.all(notesBucket);
        query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        PrefUtils.sortNoteQuery(query, mContext, true);
        mCursor = query.execute();
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }
}
