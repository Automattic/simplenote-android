package com.automattic.simplenote;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.automattic.simplenote.databinding.ActivityTagAddBinding;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.HtmlCompat;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.viewmodels.AddTagViewModel;
import com.automattic.simplenote.viewmodels.ViewModelFactory;
import com.automattic.simplenote.widgets.MorphCircleToRectangle;
import com.automattic.simplenote.widgets.MorphSetup;
import com.simperium.client.Bucket;

import java.util.Objects;

public class AddTagActivity extends AppCompatActivity implements TextWatcher {
    private AddTagViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        ActivityTagAddBinding binding = ActivityTagAddBinding.inflate(getLayoutInflater());

        Bucket<Tag> mBucketTag = ((Simplenote) getApplication()).getTagsBucket();
        ViewModelFactory viewModelFactory = new ViewModelFactory(mBucketTag, this, null);
        ViewModelProvider viewModelProvider = new ViewModelProvider(this, viewModelFactory);
        viewModel = viewModelProvider.get(AddTagViewModel.class);

        setObservers(binding);
        setupLayout(binding);
        setupViews(binding);

        viewModel.start();

        setContentView(binding.getRoot());
    }

    private void setupViews(ActivityTagAddBinding binding) {
        binding.title.setText(getString(R.string.add_tag));
        binding.tagInput.addTextChangedListener(this);

        binding.buttonNegative.setOnClickListener(
                v -> viewModel.close()
        );

        binding.buttonPositive.setOnClickListener(
                v -> viewModel.saveTag()
        );
    }

    private void setupLayout(ActivityTagAddBinding binding) {
        int widthScreen = getResources().getDisplayMetrics().widthPixels;
        int widthMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
        int widthMaximum = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 312, getResources().getDisplayMetrics());
        LinearLayout layout = binding.layout;
        layout.getLayoutParams().width = Math.min(widthMaximum, widthScreen - widthMargin);
        layout.requestLayout();

        ((View) layout.getParent()).setOnClickListener(
                view -> viewModel.close()
        );
        layout.setOnClickListener(null);

        MorphSetup.setSharedElementTransitions(this, layout, getResources().getDimensionPixelSize(R.dimen.corner_radius_dialog));
    }

    private void setObservers(ActivityTagAddBinding binding) {
        // Observe changes in the UI state
        viewModel.getUiState().observe(this, uiState -> {
            // Validate if the current state has an error
            if (uiState.getErrorMsg() != null) {
                binding.tagLayout.setError(getString(uiState.getErrorMsg()));
                binding.buttonPositive.setEnabled(false);
            } else {
                // If there is not an error, enable save button
                binding.tagLayout.setError(null);
                binding.buttonPositive.setEnabled(true);
            }

            // Check if the keyboard should be closed
            if (!uiState.isKeyboardShowing()) {
                DisplayUtils.hideKeyboard(binding.tagInput);
            }
        });

        viewModel.getEvent().observe(this, event -> {
            switch (event) {
                case START:
                    binding.buttonPositive.setEnabled(false);
                    new Handler().postDelayed(
                            () -> {
                                binding.tagInput.requestFocus();
                                DisplayUtils.showKeyboard(binding.tagInput);
                            },
                            MorphCircleToRectangle.DURATION
                    );
                    break;
                case CLOSE:
                    finishAfterTransition();
                    break;
                case FINISH:
                    setResult(RESULT_OK);
                    finishAfterTransition();
                    break;
                case SHOW_ERROR:
                    showDialogError();
                    break;
            }
        });
    }

    @Override
    public void afterTextChanged(Editable s) {
        String tag = s.toString();
        viewModel.updateUiState(tag);
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
