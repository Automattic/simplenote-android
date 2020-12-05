package com.automattic.simplenote;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.DialogUtils;
import com.automattic.simplenote.utils.TagUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;

public class TagDialogFragment extends AppCompatDialogFragment implements TextWatcher, OnShowListener {
    public static String DIALOG_TAG = "dialog_tag";

    private AlertDialog mDialogEditTag;
    private Bucket<Note> mBucketNote;
    private Bucket<Tag> mBucketTag;
    private Button mButtonPositive;
    private String mTagOld;
    private Tag mTag;
    private TextInputEditText mEditTextTag;
    private TextInputLayout mEditTextLayout;
    private TextView mMessage;
    private View.OnClickListener mClickListenerNegativeConflict;
    private View.OnClickListener mClickListenerNegativeRename;
    private View.OnClickListener mClickListenerPositiveConflict;
    private View.OnClickListener mClickListenerPositiveRename;

    public TagDialogFragment(Tag tag, Bucket<Note> bucketNote, Bucket<Tag> bucketTag) {
        mTag = tag;
        mBucketNote = bucketNote;
        mBucketTag = bucketTag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(requireContext(), R.style.Dialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.rename_tag);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.save, null);
        View view = LayoutInflater.from(context).inflate(R.layout.edit_tag, null);
        mEditTextLayout = view.findViewById(R.id.input_tag_name);
        mEditTextTag = (TextInputEditText) mEditTextLayout.getEditText();
        mMessage = view.findViewById(R.id.message);

        if (mEditTextTag != null) {
            mEditTextTag.addTextChangedListener(this);
        }

        builder.setView(view);
        mDialogEditTag = builder.create();
        mDialogEditTag.setOnShowListener(this);

        mClickListenerNegativeRename = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        };

        mClickListenerPositiveRename = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tagNew = mEditTextTag.getText() != null ? mEditTextTag.getText().toString().trim() : "";

                if (tagNew.equals(mTagOld)) {
                    dismiss();
                }

                int index = mTag.hasIndex() ? mTag.getIndex() : mBucketTag.count();
                boolean isRenamingToLexicalTag = TagUtils.hashTag(tagNew).equals(TagUtils.hashTag(mTagOld));
                boolean hasCanonicalTag = TagUtils.hasCanonicalOfLexical(mBucketTag, tagNew);

                if (hasCanonicalTag && !isRenamingToLexicalTag) {
                    String tagCanonical = TagUtils.getCanonicalFromLexical(mBucketTag, tagNew);
                    showDialogErrorConflict(tagCanonical, mTagOld);
                    return;
                }

                tryToRenameTag(tagNew, index);
            }
        };

        mClickListenerNegativeConflict = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogRenameTag();
            }
        };

        mClickListenerPositiveConflict = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tagNew = mEditTextTag.getText() != null ? mEditTextTag.getText().toString().trim() : "";
                int index = mTag.hasIndex() ? mTag.getIndex() : mBucketTag.count();
                tryToRenameTag(tagNew, index);
                dismiss();
            }
        };

        return mDialogEditTag;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mButtonPositive = mDialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE);
        mEditTextTag.setText(mTag.getName());
        mTagOld = mEditTextTag.getText() != null ? mEditTextTag.getText().toString() : "";
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (isTagNameValid()) {
            mButtonPositive.setEnabled(true);
            mEditTextLayout.setError(null);
        } else if (isTagNameInvalidEmpty()) {
            mButtonPositive.setEnabled(false);
            mEditTextLayout.setError(getString(R.string.rename_tag_error_empty));
        } else if (isTagNameInvalidLength()) {
            mButtonPositive.setEnabled(false);
            mEditTextLayout.setError(getString(R.string.rename_tag_error_length));
        } else if (isTagNameInvalidSpaces()) {
            mButtonPositive.setEnabled(false);
            mEditTextLayout.setError(getString(R.string.rename_tag_error_spaces));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    private boolean isTagNameValid() {
        return !isTagNameInvalidSpaces() && !isTagNameInvalidLength() && !isTagNameInvalidEmpty();
    }

    private boolean isTagNameInvalidEmpty() {
        return mEditTextTag.getText() != null && mEditTextTag.getText().toString().isEmpty();
    }

    private boolean isTagNameInvalidLength() {
        return mEditTextTag.getText() != null && !TagUtils.hashTagValid(mEditTextTag.getText().toString());
    }

    private boolean isTagNameInvalidSpaces() {
        return mEditTextTag.getText() != null && mEditTextTag.getText().toString().contains(" ");
    }

    private void showDialogErrorConflict(String canonical, String tagOld, final String tagNew, final int index) {
        new AlertDialog.Builder(new ContextThemeWrapper(requireContext(), R.style.Dialog))
            .setTitle(R.string.dialog_tag_conflict_title)
            .setMessage(getString(R.string.dialog_tag_conflict_message, canonical, tagOld, canonical))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                R.string.dialog_tag_conflict_button_positive,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tryToRenameTag(tagNew, index);
                    }
                }
            )
            .show();
    }

    private void tryToRenameTag(String tagNew, int index) {
        try {
            mTag.renameTo(mTagOld, tagNew, index, mBucketNote);
            AnalyticsTracker.track(
                AnalyticsTracker.Stat.TAG_EDITOR_ACCESSED,
                AnalyticsTracker.CATEGORY_TAG,
                "tag_alert_edit_box"
            );
        } catch (BucketObjectNameInvalid e) {
            Log.e(Simplenote.TAG, "Unable to rename tag", e);
            Context context = requireContext();
            DialogUtils.showDialogWithEmail(
                context,
                context.getString(R.string.error),
                context.getString(R.string.rename_tag_message)
            );
        }
    }
}
