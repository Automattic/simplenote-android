package com.automattic.simplenote;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Reference;
import com.automattic.simplenote.utils.DateTimeUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.NoteUtils;
import com.automattic.simplenote.utils.SimplenoteLinkify;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InfoBottomSheetDialog extends BottomSheetDialogBase {
    public static final String TAG = InfoBottomSheetDialog.class.getSimpleName();

    private Fragment mFragment;
    private LinearLayout mDateTimeSyncedLayout;
    private LinearLayout mReferencesLayout;
    private RecyclerView mReferences;
    private TextView mCountCharacters;
    private TextView mCountWords;
    private TextView mDateTimeCreated;
    private TextView mDateTimeModified;
    private TextView mDateTimeSynced;

    private LastSyncTimeCache.SyncTimeListener mSyncTimeListener = null;
    private Bucket.OnNetworkChangeListener<Note> mNetworkChangeListener = null;

    public InfoBottomSheetDialog(@NonNull Fragment fragment) {
        mFragment = fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View infoView = inflater.inflate(R.layout.bottom_sheet_info, null, false);
        mCountCharacters = infoView.findViewById(R.id.count_characters);
        mCountWords = infoView.findViewById(R.id.count_words);
        mDateTimeCreated = infoView.findViewById(R.id.date_time_created);
        mDateTimeModified = infoView.findViewById(R.id.date_time_modified);
        mDateTimeSynced = infoView.findViewById(R.id.date_time_synced);
        mDateTimeSyncedLayout = infoView.findViewById(R.id.date_time_synced_layout);
        mReferencesLayout = infoView.findViewById(R.id.references_layout);
        mReferences = infoView.findViewById(R.id.references);

        if (getDialog() != null) {
            // Set peek height to half height of screen.
            getDialog().setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                    FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

                    if (bottomSheet != null) {
                        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                        behavior.setPeekHeight(DisplayUtils.getDisplayPixelSize(requireContext()).y / 2);
                        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        behavior.setSkipCollapsed(true);
                    }
                }
            });

            getDialog().setContentView(infoView);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeListeners((Simplenote) requireActivity().getApplication());
    }

    public void show(FragmentManager manager, Note note) {
        if (mFragment.isAdded()) {
            showNow(manager, TAG);
            updateNoteData(note);
            Simplenote app = (Simplenote) requireActivity().getApplication();
            Calendar sync = app.getLastSyncTimeCache().getLastSyncTime(note.getSimperiumKey());

            if (sync != null) {
                mDateTimeSynced.setText(DateTimeUtils.getDateTextString(requireContext(), sync));
                mDateTimeSyncedLayout.setVisibility(View.VISIBLE);
            } else {
                mDateTimeSyncedLayout.setVisibility(View.GONE);
            }

            getReferences(note);
            addListeners(app, note.getSimperiumKey());
        }
    }

    private void updateNoteData(Note note) {
        mCountCharacters.setText(NoteUtils.getCharactersCount(note.getContent()));
        mCountWords.setText(NoteUtils.getWordCount(note.getContent()));
        mDateTimeCreated.setText(DateTimeUtils.getDateTextString(requireContext(), note.getCreationDate()));
        mDateTimeModified.setText(DateTimeUtils.getDateTextString(requireContext(), note.getModificationDate()));
    }

    private void addListeners(Simplenote app, final String noteId) {
        removeListeners(app);

        mNetworkChangeListener = new Bucket.OnNetworkChangeListener<Note>() {
            @Override
            public void onNetworkChange(Bucket<Note> bucket, Bucket.ChangeType type, String key) {
                if (!noteId.equals(key)) {
                    return;
                }

                try {
                    updateNoteData(bucket.getObject(noteId));
                } catch (BucketObjectMissingException e) {
                    // no harm no foul
                }
            }
        };

        mSyncTimeListener = new LastSyncTimeCache.SyncTimeListener() {
            @Override
            public void onUpdate(String entityId, Calendar lastSyncTime, boolean isSynced) {
                if (!noteId.equals(entityId)) {
                    return;
                }

                mDateTimeSyncedLayout.setVisibility(View.VISIBLE);
                mDateTimeSynced.setText(DateTimeUtils.getDateTextString(requireContext(), lastSyncTime));
            }
        };

        app.getNotesBucket().addOnNetworkChangeListener(mNetworkChangeListener);
        app.getLastSyncTimeCache().addListener(mSyncTimeListener);
    }

    private void removeListeners(Simplenote app) {
        app.getNotesBucket().removeOnNetworkChangeListener(mNetworkChangeListener);
        app.getLastSyncTimeCache().removeListener(mSyncTimeListener);
    }

    private void getReferences(Note note) {
        Simplenote application = (Simplenote) mFragment.requireActivity().getApplicationContext();
        Bucket<Note> bucket = application.getNotesBucket();
        List<Reference> references = Note.getReferences(bucket, note.getSimperiumKey());

        if (references.size() > 0) {
            mReferencesLayout.setVisibility(View.VISIBLE);
            ReferenceAdapter adapter = new ReferenceAdapter(references);
            mReferences.setAdapter(adapter);
            mReferences.setLayoutManager(new LinearLayoutManager(requireContext()));
        } else {
            mReferencesLayout.setVisibility(View.GONE);
        }
    }

    private class ReferenceAdapter extends RecyclerView.Adapter<ReferenceAdapter.ViewHolder> {
        private final List<Reference> mReferences;

        private ReferenceAdapter(List<Reference> references) {
            mReferences = new ArrayList<>(references);
        }

        @Override
        public int getItemCount() {
            return mReferences.size();
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final Reference reference = mReferences.get(position);
            holder.mTitle.setText(reference.getTitle());
            holder.mSubtitle.setText(
                getResources().getQuantityString(
                    R.plurals.references_count,
                    reference.getCount(),
                    reference.getCount(),
                    DateTimeUtils.getDateTextNumeric(reference.getDate())
                )
            );
            holder.mView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.INTERNOTE_LINK_TAPPED,
                            AnalyticsTracker.CATEGORY_LINK,
                            "internote_link_tapped_info"
                        );
                        SimplenoteLinkify.openNote(mFragment.requireActivity(), reference.getKey());
                    }
                }
            );
        }

        @NonNull
        @Override
        public ReferenceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.reference_list_row, parent, false));
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mSubtitle;
            private TextView mTitle;
            private View mView;

            private ViewHolder(View itemView) {
                super(itemView);
                mView = itemView;
                mTitle = itemView.findViewById(R.id.reference_title);
                mSubtitle = itemView.findViewById(R.id.reference_subtitle);
            }
        }
    }
}
