package com.automattic.simplenote;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.DialogUtils;
import com.automattic.simplenote.viewmodels.CloseEvent;
import com.automattic.simplenote.viewmodels.ConflictEvent;
import com.automattic.simplenote.viewmodels.FinishEvent;
import com.automattic.simplenote.viewmodels.ShowErrorEvent;
import com.automattic.simplenote.viewmodels.TagDialogViewModel;
import com.automattic.simplenote.viewmodels.ViewModelFactory;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.simperium.client.Bucket;

public class TagDialogFragment extends AppCompatDialogFragment implements TextWatcher, OnShowListener {
    public static String DIALOG_TAG = "dialog_tag";

    private AlertDialog mDialogEditTag;
    private Bucket<Note> mBucketNote;
    private Bucket<Tag> mBucketTag;
    private Button mButtonNegative;
    private Button mButtonNeutral;
    private Button mButtonPositive;
    private Tag mTag;
    private TextInputEditText mEditTextTag;
    private TextInputLayout mEditTextLayout;
    private TextView mMessage;
    private View.OnClickListener mClickListenerNegativeRename;
    private View.OnClickListener mClickListenerNeutralConflict;
    private View.OnClickListener mClickListenerPositiveConflict;
    private View.OnClickListener mClickListenerPositiveRename;

    private TagDialogViewModel viewModel;

    public TagDialogFragment(Tag tag, Bucket<Note> bucketNote, Bucket<Tag> bucketTag) {
        mTag = tag;
        mBucketNote = bucketNote;
        mBucketTag = bucketTag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewModelFactory viewModelFactory = new ViewModelFactory(mBucketTag, mBucketNote, this, null);
        ViewModelProvider viewModelProvider = new ViewModelProvider(this, viewModelFactory);
        viewModel = viewModelProvider.get(TagDialogViewModel.class);

        viewModel.start(mTag);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        viewModel.close();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setupDialog();

        return mDialogEditTag;
    }

    private void setupDialog() {
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

        mClickListenerNegativeRename = v -> viewModel.close();
        mClickListenerPositiveRename = v -> viewModel.renameTagIfValid();
        mClickListenerNeutralConflict = v -> showDialogRenameTag();

        mClickListenerPositiveConflict = v -> viewModel.renameTag();
    }


    private void setObservers() {
        viewModel.getUiState().observe(this, uiState -> {
            // Validate if the current state has an error
            if (uiState.getErrorMsg() != null) {
                mEditTextLayout.setError(getString(uiState.getErrorMsg()));
                mButtonPositive.setEnabled(false);
            } else {
                // If there is not an error, enable save button
                mEditTextLayout.setError(null);
                mButtonPositive.setEnabled(true);
            }
        });

        viewModel.getEvent().observe(this, event -> {
            if (event instanceof CloseEvent || event instanceof FinishEvent) {
                dismiss();
            } else if (event instanceof ShowErrorEvent) {
                Context context = requireContext();
                DialogUtils.showDialogWithEmail(
                        context,
                        context.getString(R.string.rename_tag_message)
                );
            } else if (event instanceof ConflictEvent) {
                ConflictEvent conflictEvent = (ConflictEvent) event;
                showDialogErrorConflict(conflictEvent.getCanonicalTagName(), conflictEvent.getOldTagName());
            }
        });
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mButtonNegative = mDialogEditTag.getButton(DialogInterface.BUTTON_NEGATIVE);
        mButtonNeutral = mDialogEditTag.getButton(DialogInterface.BUTTON_NEUTRAL);
        mButtonPositive = mDialogEditTag.getButton(DialogInterface.BUTTON_POSITIVE);

        // Set observers when views are available
        setObservers();

        showDialogRenameTag();
        mEditTextTag.setText(mTag.getName());
    }

    @Override
    public void afterTextChanged(Editable s) {
        String tagName = s.toString();
        viewModel.updateUiState(tagName);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    private void showDialogErrorConflict(String canonical, String tagOld) {
        mDialogEditTag.setTitle(R.string.dialog_tag_conflict_title);

        mMessage.setText(getString(R.string.dialog_tag_conflict_message, canonical, tagOld, canonical));
        mButtonNeutral.setText(R.string.back);
        mButtonPositive.setText(R.string.dialog_tag_conflict_button_positive);

        mMessage.setVisibility(View.VISIBLE);
        mEditTextLayout.setVisibility(View.GONE);
        mButtonNegative.setVisibility(View.GONE);
        mButtonNeutral.setVisibility(View.VISIBLE);

        mButtonNeutral.setOnClickListener(mClickListenerNeutralConflict);
        mButtonPositive.setOnClickListener(mClickListenerPositiveConflict);
    }

    private void showDialogRenameTag() {
        mDialogEditTag.setTitle(R.string.rename_tag);

        mButtonNegative.setText(R.string.cancel);
        mButtonPositive.setText(R.string.save);

        mMessage.setVisibility(View.GONE);
        mEditTextLayout.setVisibility(View.VISIBLE);
        mButtonNegative.setVisibility(View.VISIBLE);
        mButtonNeutral.setVisibility(View.GONE);

        mButtonNegative.setOnClickListener(mClickListenerNegativeRename);
        mButtonPositive.setOnClickListener(mClickListenerPositiveRename);
    }
}
