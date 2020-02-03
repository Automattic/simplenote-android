package com.automattic.simplenote;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DateTimeUtils;

import java.text.NumberFormat;

public class InfoBottomSheetDialog extends BottomSheetDialogBase {
    private static final String TAG = InfoBottomSheetDialog.class.getSimpleName();

    private Fragment mFragment;
    private InfoSheetListener mListener;
    private TextView mCountCharacters;
    private TextView mCountWords;
    private TextView mDateTimeCreated;
    private TextView mDateTimeModified;

    public InfoBottomSheetDialog(@NonNull Fragment fragment, @NonNull final InfoSheetListener infoSheetListener) {
        mFragment = fragment;
        mListener = infoSheetListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View infoView = inflater.inflate(R.layout.bottom_sheet_info, null, false);
        mCountCharacters = infoView.findViewById(R.id.count_characters);
        mCountWords = infoView.findViewById(R.id.count_words);
        mDateTimeCreated = infoView.findViewById(R.id.date_time_created);
        mDateTimeModified = infoView.findViewById(R.id.date_time_modified);

        if (getDialog() != null) {
            getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mListener.onInfoDismissed();
                }
            });

            getDialog().setContentView(infoView);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void show(FragmentManager manager, Note note) {
        if (mFragment.isAdded()) {
            showNow(manager, TAG);
            mCountCharacters.setText(getCharactersCount(note.getContent()));
            mCountWords.setText(getWordCount(note.getContent()));
            mDateTimeCreated.setText(DateTimeUtils.getDateTextString(requireContext(), note.getCreationDate()));
            mDateTimeModified.setText(DateTimeUtils.getDateTextString(requireContext(), note.getModificationDate()));
        }
    }

    private String getCombinedCount(String content) {
        return String.format("%s\n%s", getWordCount(content), getCharactersCount(content));
    }

    private String getWordCount(String content) {
        int words = (content.trim().length() == 0) ? 0 : content.trim().split("([\\W]+)").length;
        return NumberFormat.getInstance().format(words);
    }

    private String getCharactersCount(String content) {
        return NumberFormat.getInstance().format(content.length());
    }

    public interface InfoSheetListener {
        void onInfoCopyLinkClicked();
        void onInfoDismissed();
        void onInfoMarkdownSwitchChanged(boolean isSwitchedOn);
        void onInfoPinSwitchChanged(boolean isSwitchedOn);
        void onInfoShareLinkClicked();
    }
}
