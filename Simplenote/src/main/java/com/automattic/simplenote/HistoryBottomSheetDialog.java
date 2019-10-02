package com.automattic.simplenote;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DateTimeUtils;
import com.simperium.client.Bucket;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class HistoryBottomSheetDialog extends BottomSheetDialogBase {
    private static final String TAG = HistoryBottomSheetDialog.class.getSimpleName();

    private ArrayList<Note> mNoteRevisionsList;
    private Fragment mFragment;
    private HistorySheetListener mListener;
    private Note mNote;
    private SeekBar mHistorySeekBar;
    private TextView mHistoryDate;
    private View mErrorText;
    private View mLoadingView;
    private View mProgressBar;
    private View mSliderView;
    private boolean mDidTapButton;
    private final Bucket.RevisionsRequestCallbacks<Note> mRevisionsRequestCallbacks = new
            Bucket.RevisionsRequestCallbacks<Note>() {
                // Note: These callbacks won't be running on the main thread
                @Override
                public void onComplete(Map<Integer, Note> revisionsMap) {
                    if (!mFragment.isAdded() || mNote == null) {
                        return;
                    }

                    // Convert map to an array list, to work better with the 0-index based seekbar
                    mNoteRevisionsList = new ArrayList<>(revisionsMap.values());
                    mFragment.requireActivity().runOnUiThread(new Runnable() {
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
                    if (!mFragment.isAdded() || getDialog() != null && !getDialog().isShowing()) {
                        return;
                    }

                    mFragment.requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                            mErrorText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            };

    public HistoryBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final HistorySheetListener historySheetListener) {
        mFragment = fragment;
        mListener = historySheetListener;
    }

    public boolean isHistoryLoaded() {
        return getDialog() != null && getDialog().isShowing() && mSliderView.getVisibility() == View.VISIBLE;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View history = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.bottom_sheet_history, null, false);
        mHistoryDate = history.findViewById(R.id.history_date);
        mHistorySeekBar = history.findViewById(R.id.seek_bar);
        mProgressBar = history.findViewById(R.id.history_progress_bar);
        mErrorText = history.findViewById(R.id.history_error_text);
        mLoadingView = history.findViewById(R.id.history_loading_view);
        mSliderView = history.findViewById(R.id.history_slider_view);

        mHistorySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mNoteRevisionsList == null || getDialog() != null && !getDialog().isShowing()) {
                    return;
                }

                Calendar noteDate = null;

                if (progress == mNoteRevisionsList.size() && mNote != null) {
                    mListener.onHistoryUpdateNote(mNote.getContent());
                    noteDate = mNote.getModificationDate();
                } else if (progress < mNoteRevisionsList.size() && mNoteRevisionsList.get(progress) != null) {
                    Note revisedNote = mNoteRevisionsList.get(progress);
                    noteDate = revisedNote.getModificationDate();
                    mListener.onHistoryUpdateNote(revisedNote.getContent());
                }

                mHistoryDate.setText(DateTimeUtils.getDateText(mFragment.getActivity(), noteDate));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        View cancelHistoryButton = history.findViewById(R.id.cancel_history_button);
        cancelHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDidTapButton = true;
                mListener.onHistoryCancelClicked();
            }
        });

        View restoreHistoryButton = history.findViewById(R.id.restore_history_button);
        restoreHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDidTapButton = true;
                mListener.onHistoryRestoreClicked();
            }
        });

        if (getDialog() != null) {
            getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mListener.onHistoryDismissed();
                    mNote = null;
                }
            });

            getDialog().setContentView(history);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void show(FragmentManager manager, Note note) {
        if (mFragment.isAdded()) {
            showNow(manager, TAG);
            mNote = note;
            mDidTapButton = false;
            setProgressBar();
        }
    }

    public boolean didTapOnButton() {
        return mDidTapButton;
    }

    public void updateProgressBar() {
        if (getDialog() != null && getDialog().isShowing()) {
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
            mHistoryDate.setText(DateTimeUtils.getDateText(mFragment.getActivity(), mNote.getModificationDate()));
            mLoadingView.setVisibility(View.GONE);
            mSliderView.setVisibility(View.VISIBLE);
        } else {
            mLoadingView.setVisibility(View.VISIBLE);
            mSliderView.setVisibility(View.INVISIBLE);
        }
    }

    public interface HistorySheetListener {
        void onHistoryCancelClicked();
        void onHistoryDismissed();
        void onHistoryRestoreClicked();
        void onHistoryUpdateNote(String content);
    }
}
