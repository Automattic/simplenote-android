package com.automattic.simplenote;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DateTimeUtils;
import com.automattic.simplenote.utils.PrefUtils;

import java.text.NumberFormat;

public class InfoBottomSheetDialog extends BottomSheetDialogBase {
    private static final String TAG = InfoBottomSheetDialog.class.getSimpleName();

    private Fragment mFragment;
    private ImageButton mCopyButton;
    private ImageButton mShareButton;
    private InfoSheetListener mListener;
    private TextView mInfoLinkTitle;
    private TextView mInfoLinkUrl;
    private TextView mInfoModifiedDate;
    private TextView mInfoWords;
    private SwitchCompat mInfoMarkdownSwitch;
    private SwitchCompat mInfoPinSwitch;

    public InfoBottomSheetDialog(@NonNull Fragment fragment, @NonNull final InfoSheetListener infoSheetListener) {
        mFragment = fragment;
        mListener = infoSheetListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View infoView = inflater.inflate(R.layout.bottom_sheet_info, null, false);
        mInfoModifiedDate = infoView.findViewById(R.id.info_modified_date_text);
        mInfoWords = infoView.findViewById(R.id.info_words_text);
        mInfoLinkUrl = infoView.findViewById(R.id.info_public_link_url);
        mInfoLinkTitle = infoView.findViewById(R.id.info_public_link_title);

        mInfoPinSwitch = infoView.findViewById(R.id.info_pin_switch);
        mInfoPinSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mListener.onInfoPinSwitchChanged(isChecked);
            }
        });

        mInfoMarkdownSwitch = infoView.findViewById(R.id.info_markdown_switch);
        mInfoMarkdownSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mFragment.isAdded()){
                    return;
                }

                mListener.onInfoMarkdownSwitchChanged(isChecked);

                // Set preference so that next new note will have markdown enabled
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mFragment.getActivity());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PrefUtils.PREF_MARKDOWN_ENABLED, isChecked);
                editor.apply();
            }
        });

        mCopyButton = infoView.findViewById(R.id.info_copy_link_button);
        mCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onInfoCopyLinkClicked();
            }
        });
        mCopyButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v.isHapticFeedbackEnabled()) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(getContext(), requireContext().getString(R.string.copy), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        mShareButton = infoView.findViewById(R.id.info_share_button);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onInfoShareLinkClicked();
            }
        });
        mShareButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v.isHapticFeedbackEnabled()) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }

                Toast.makeText(getContext(), requireContext().getString(R.string.share), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

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

            String date = DateTimeUtils.getDateText(mFragment.getActivity(), note.getModificationDate());
            mInfoModifiedDate.setText(String.format(mFragment.getString(R.string.modified_time), date));
            mInfoPinSwitch.setChecked(note.isPinned());
            mInfoMarkdownSwitch.setChecked(note.isMarkdownEnabled());
            mInfoWords.setText(getCombinedCount(note.getContent()));

            if (note.isPublished()) {
                mInfoLinkTitle.setText(mFragment.getString(R.string.public_link));
                mInfoLinkUrl.setText(note.getPublishedUrl());
                mInfoLinkUrl.setVisibility(View.VISIBLE);
                mCopyButton.setVisibility(View.VISIBLE);
                mShareButton.setVisibility(View.GONE);
            } else {
                mInfoLinkTitle.setText(mFragment.getString(R.string.note_not_published));
                mInfoLinkUrl.setVisibility(View.GONE);
                mCopyButton.setVisibility(View.GONE);
                mShareButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getCombinedCount(String content) {
        return String.format("%s\n%s", getWordCount(content), getCharactersCount(content));
    }

    private String getWordCount(String content) {
        int numWords = (content.trim().length() == 0) ? 0 : content.trim().split("([\\W]+)").length;
        String wordCountString = mFragment.getResources().getQuantityString(R.plurals.word_count, numWords);
        String formattedWordCount = NumberFormat.getInstance().format(numWords);
        return String.format("%s %s", formattedWordCount, wordCountString);
    }

    private String getCharactersCount(String content) {
        int numChars = content.length();
        String charCount = NumberFormat.getInstance().format(numChars);
        String charCountString = mFragment.getResources().getQuantityString(R.plurals.char_count, numChars);
        return String.format("%s %s", charCount, charCountString);
    }

    public interface InfoSheetListener {
        void onInfoCopyLinkClicked();
        void onInfoDismissed();
        void onInfoMarkdownSwitchChanged(boolean isSwitchedOn);
        void onInfoPinSwitchChanged(boolean isSwitchedOn);
        void onInfoShareLinkClicked();
    }
}
