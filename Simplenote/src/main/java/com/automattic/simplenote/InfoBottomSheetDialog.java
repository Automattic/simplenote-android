package com.automattic.simplenote;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DateTimeUtils;
import com.automattic.simplenote.utils.PrefUtils;

import java.text.NumberFormat;

/**
 * Created by onko on 27/03/2016.
 */
public class InfoBottomSheetDialog extends BottomSheetDialogBase {

    private TextView mInfoModifiedDate;
    private TextView mInfoWords;
    private TextView mInfoLinkUrl;
    private TextView mInfoLinkTitle;
    private SwitchCompat mInfoPinSwitch;
    private SwitchCompat mInfoMarkdownSwitch;
    private ImageButton mCopyButton;
    private ImageButton mShareButton;
    private Fragment mFragment;

    public InfoBottomSheetDialog(@NonNull Fragment fragment, @NonNull final InfoSheetListener infoSheetListener) {
        super(fragment.getActivity());

        mFragment = fragment;

        View infoView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.bottom_sheet_info, null, false);
        mInfoModifiedDate = infoView.findViewById(R.id.info_modified_date_text);
        mInfoWords = infoView.findViewById(R.id.info_words_text);
        mInfoLinkUrl = infoView.findViewById(R.id.info_public_link_url);
        mInfoLinkTitle = infoView.findViewById(R.id.info_public_link_title);

        mInfoPinSwitch = infoView.findViewById(R.id.info_pin_switch);
        mInfoPinSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                infoSheetListener.onInfoPinSwitchChanged(isChecked);
            }
        });

        mInfoMarkdownSwitch = infoView.findViewById(R.id.info_markdown_switch);
        mInfoMarkdownSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mFragment.isAdded()) return;

                infoSheetListener.onInfoMarkdownSwitchChanged(isChecked);

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
                infoSheetListener.onInfoCopyLinkClicked();
            }
        });

        mShareButton = infoView.findViewById(R.id.info_share_button);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoSheetListener.onInfoShareLinkClicked();
            }
        });

        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                infoSheetListener.onInfoDismissed();
            }
        });

        setContentView(infoView);
    }

    public void show(Note note) {

        if (mFragment.isAdded()) {
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

            show();
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
        void onInfoPinSwitchChanged(boolean isSwitchedOn);

        void onInfoMarkdownSwitchChanged(boolean isSwitchedOn);

        void onInfoCopyLinkClicked();

        void onInfoShareLinkClicked();

        void onInfoDismissed();
    }
}
