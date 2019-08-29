package com.automattic.simplenote;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DateTimeUtils;
import com.simperium.client.Bucket;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class HistoryBottomSheetDialog extends BottomSheetDialogBase {

    private boolean mDidTapButton;
    private ArrayList<Note> mNoteRevisionsList;

    private View mProgressBar;
    private View mErrorText;
    private View mLoadingView;
    private View mSliderView;
    private TextView mHistoryDate;
    private SeekBar mHistorySeekBar;

    private Fragment mFragment;
    private Note note;
    private final Bucket.RevisionsRequestCallbacks<Note> mRevisionsRequestCallbacks = new
            Bucket.RevisionsRequestCallbacks<Note>() {
                // Note: These callbacks won't be running on the main thread
                @Override
                public void onComplete(Map<Integer, Note> revisionsMap) {
                    if (!mFragment.isAdded() || note == null) return;

                    // Convert map to an array list, to work better with the 0-index based seekbar
                    mNoteRevisionsList = new ArrayList<>(revisionsMap.values());
                    mFragment.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateProgressBar();
                        }
                    });
                }

                @Override
                public void onRevision(String key, int version, JSONObject object) {
                }

                @Override
                public void onError(Throwable exception) {
                    if (!mFragment.isAdded() || !isShowing()) return;

                    mFragment.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                            mErrorText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            };

    public HistoryBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final HistorySheetListener historySheetListener) {
        super(fragment.getActivity());

        mFragment = fragment;

        View mHistoryView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.bottom_sheet_history, null, false);
        mHistoryDate = mHistoryView.findViewById(R.id.history_date);
        mHistorySeekBar = mHistoryView.findViewById(R.id.seek_bar);
        mProgressBar = mHistoryView.findViewById(R.id.history_progress_bar);
        mErrorText = mHistoryView.findViewById(R.id.history_error_text);
        mLoadingView = mHistoryView.findViewById(R.id.history_loading_view);
        mSliderView = mHistoryView.findViewById(R.id.history_slider_view);

        mHistorySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mNoteRevisionsList == null || !isShowing()) {
                    return;
                }

                Calendar noteDate = null;
                if (progress == mNoteRevisionsList.size() && note != null) {
                    historySheetListener.onHistoryUpdateNote(note.getContent());
                    noteDate = note.getModificationDate();
                } else if (progress < mNoteRevisionsList.size() && mNoteRevisionsList.get(progress) != null) {
                    Note revisedNote = mNoteRevisionsList.get(progress);
                    noteDate = revisedNote.getModificationDate();
                    historySheetListener.onHistoryUpdateNote(revisedNote.getContent());
                }

                mHistoryDate.setText(DateTimeUtils.getDateText(mFragment.getActivity(), noteDate));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // noop
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // noop
            }
        });

        View cancelHistoryButton = mHistoryView.findViewById(R.id.cancel_history_button);
        cancelHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDidTapButton = true;
                historySheetListener.onHistoryCancelClicked();
            }
        });

        View restoreHistoryButton = mHistoryView.findViewById(R.id.restore_history_button);
        restoreHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDidTapButton = true;
                historySheetListener.onHistoryRestoreClicked();
            }
        });

        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                historySheetListener.onHistoryDismissed();
                note = null;
            }
        });

        setContentView(mHistoryView);
    }

    public boolean isHistoryLoaded() {
        return isShowing() && mSliderView.getVisibility() == View.VISIBLE;
    }

    public void show(Note note) {

        if (mFragment.isAdded()) {
            this.note = note;
            this.mDidTapButton = false;

            setProgressBar();
            show();
        }
    }

    public boolean didTapOnButton() {
        return mDidTapButton;
    }

    public void updateProgressBar() {
        if (isShowing()) {
            setProgressBar();
        }
    }

    public Bucket.RevisionsRequestCallbacks<Note> getRevisionsRequestCallbacks() {
        return mRevisionsRequestCallbacks;
    }

    private void setProgressBar() {
        int totalRevs = mNoteRevisionsList == null ? 0 : mNoteRevisionsList.size();
        if (totalRevs > 0) {
            mHistorySeekBar.setMax(totalRevs);
            mHistorySeekBar.setProgress(totalRevs);

            mHistoryDate.setText(DateTimeUtils.getDateText(mFragment.getActivity(), note.getModificationDate()));

            mLoadingView.setVisibility(View.GONE);
            mSliderView.setVisibility(View.VISIBLE);
        } else {
            mLoadingView.setVisibility(View.VISIBLE);
            mSliderView.setVisibility(View.INVISIBLE);
        }
    }

    public interface HistorySheetListener {
        void onHistoryCancelClicked();

        void onHistoryRestoreClicked();

        void onHistoryDismissed();

        void onHistoryUpdateNote(String content);
    }
}
