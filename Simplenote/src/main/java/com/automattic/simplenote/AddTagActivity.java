package com.automattic.simplenote;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.HtmlCompat;
import com.automattic.simplenote.utils.TagUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.viewmodels.AddTagViewModel;
import com.automattic.simplenote.viewmodels.ViewModelFactory;
import com.automattic.simplenote.widgets.MorphCircleToRectangle;
import com.automattic.simplenote.widgets.MorphSetup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectNameInvalid;

import java.util.Objects;

public class AddTagActivity extends AppCompatActivity implements TextWatcher {
    private Bucket<Tag> mBucketTag;
    private Button mButtonNegative;
    private Button mButtonPositive;
    private TextInputEditText mTagInput;
    private TextInputLayout mTagLayout;
    private AddTagViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_add);

        mBucketTag = ((Simplenote) getApplication()).getTagsBucket();
        ViewModelFactory viewModelFactory = new ViewModelFactory(mBucketTag, this, null);
        ViewModelProvider viewModelProvider = new ViewModelProvider(this, viewModelFactory);
        viewModel = viewModelProvider.get(AddTagViewModel.class);

        setObservers();
        setLayout();

        TextView title = findViewById(R.id.title);
        title.setText(getString(R.string.add_tag));

        mTagLayout = findViewById(R.id.tag_layout);
        mTagInput = findViewById(R.id.tag_input);

        if (mTagInput != null) {
            mTagInput.addTextChangedListener(this);

            new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        viewModel.showKeyboard();
                    }
                },
                MorphCircleToRectangle.DURATION
            );
        }

        mButtonNegative = findViewById(R.id.button_negative);
        mButtonNegative.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAfterTransition();
                }
            }
        );

        mButtonPositive = findViewById(R.id.button_positive);
        mButtonPositive.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String tag = mTagInput.getText() != null ? mTagInput.getText().toString() : "";
                    boolean tagSaved = viewModel.saveTag(tag);
                    if (tagSaved) {
                        setResult(RESULT_OK);
                        finishAfterTransition();
                    } else {
                        showDialogError();
                    }
                }
            }
        );
    }

    private void setLayout() {
        int widthScreen = getResources().getDisplayMetrics().widthPixels;
        int widthMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
        int widthMaximum = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 312, getResources().getDisplayMetrics());
        LinearLayout layout = findViewById(R.id.layout);
        layout.getLayoutParams().width = Math.min(widthMaximum, widthScreen - widthMargin);
        layout.requestLayout();

        ((View) layout.getParent()).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finishAfterTransition();
                }
            }
        );
        layout.setOnClickListener(null);

        MorphSetup.setSharedElementTransitions(this, layout, getResources().getDimensionPixelSize(R.dimen.corner_radius_dialog));
    }

    private void setObservers() {
        // Setup an observer to enable or disable the Save button
        viewModel.getEnableSaveButton().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enable) {
                mButtonPositive.setEnabled(enable);
            }
        });

        // Setup observer to show an error in case the tag name is not valid
        viewModel.getTagError().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer error) {
                String errorMessage = error == null ? null : getString(error);
                mTagLayout.setError(errorMessage);
            }
        });

        // Setup observer to show or hide the keyboard
        viewModel.getShowKeyboard().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean showKeyboard) {
                if (showKeyboard) {
                    mTagInput.requestFocus();
                    DisplayUtils.showKeyboard(mTagInput);
                } else {
                    DisplayUtils.hideKeyboard(mTagInput);
                }
            }
        });
    }

    @Override
    public void afterTextChanged(Editable s) {
        String tag = s.toString();

        viewModel.validateTag(tag);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    private void showDialogError() {
        final AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(AddTagActivity.this, R.style.Dialog))
            .setTitle(R.string.error)
            .setMessage(HtmlCompat.fromHtml(String.format(
                getString(R.string.dialog_tag_error_message),
                getString(R.string.dialog_tag_error_message_email),
                "<span style=\"color:#",
                Integer.toHexString(ThemeUtils.getColorFromAttribute(AddTagActivity.this, R.attr.colorAccent) & 0xffffff),
                "\">",
                "</span>"
            )))
            .setPositiveButton(android.R.string.ok, null)
            .show();
        ((TextView) Objects.requireNonNull(dialog.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
