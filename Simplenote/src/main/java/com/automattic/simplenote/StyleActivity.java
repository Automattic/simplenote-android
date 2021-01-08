package com.automattic.simplenote;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.EmptyViewRecyclerView;

import java.util.ArrayList;
import java.util.List;

import static com.automattic.simplenote.PreferencesFragment.WEB_APP_URL;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_ARRAY;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_BLACK;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_CLASSIC;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_DEFAULT;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_MATRIX;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_MONO;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_PUBLICATION;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_SEPIA;

public class StyleActivity extends ThemedAppCompatActivity {
    private static final String EXTRA_SCROLL = "EXTRA_SCROLL";

    private LinearLayoutManager mLayoutManager;
    private boolean mIsPremium;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_style);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        SpannableString title = new SpannableString(getString(R.string.style));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mIsPremium = PrefUtils.isPremium(StyleActivity.this);
        List<String> styles = new ArrayList<>(STYLE_ARRAY.length);

        for (int i = 0; i < STYLE_ARRAY.length; i++) {
            styles.add(PrefUtils.getStyleNameFromIndex(StyleActivity.this, i));
        }

        EmptyViewRecyclerView list = findViewById(R.id.list);
        list.setAdapter(new StyleAdapter(styles));
        mLayoutManager = new LinearLayoutManager(StyleActivity.this);
        list.setLayoutManager(mLayoutManager);

        if (getIntent().hasExtra(EXTRA_SCROLL)) {
            mLayoutManager.onRestoreInstanceState(getIntent().getParcelableExtra(EXTRA_SCROLL));
        }
    }

    /**
     *  Overrides recreate to allow restoring the scroll position
     */
    @Override
    public void recreate() {
        Intent intent = new Intent(StyleActivity.this, StyleActivity.class);
        intent.putExtra(EXTRA_SCROLL, mLayoutManager.onSaveInstanceState());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showDialogLocked() {
        new AlertDialog.Builder(new ContextThemeWrapper(StyleActivity.this, R.style.Dialog))
            .setTitle(R.string.style_dialog_locked_title)
            .setMessage(R.string.style_dialog_locked_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                R.string.style_dialog_locked_button_positive,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            BrowserUtils.launchBrowserOrShowError(StyleActivity.this, WEB_APP_URL);
                        } catch (Exception e) {
                            Toast.makeText(StyleActivity.this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            )
            .show();
    }

    private class StyleAdapter extends RecyclerView.Adapter<StyleAdapter.StyleHolder> {
        private ArrayList<String> mStyles;
        private int mSelectedPosition;

        public StyleAdapter(List<String> styles) {
            this.mStyles = new ArrayList<>(styles);
            mSelectedPosition = mIsPremium ? PrefUtils.getStyleIndexSelected(StyleActivity.this) : STYLE_DEFAULT;
        }

        @Override
        public int getItemCount() {
            return mStyles.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public void onBindViewHolder(@NonNull final StyleHolder holder, final int position) {
            String style = mStyles.get(position);
            holder.mLocked.setVisibility(mIsPremium || position == STYLE_DEFAULT ? View.GONE : View.VISIBLE);
            holder.mTitle.setText(style);
            holder.mContent.setText(Html.fromHtml(String.format(
                getResources().getString(R.string.style_preview),
                "<u><span style=\"color:#",
                Integer.toHexString(
                    ContextCompat.getColor(
                        holder.mView.getContext(),
                        getLinkColorFromStyle(position)
                    ) & 0xffffff
                ),
                "\">",
                "</span></u>"
            )));
            holder.mView.setActivated(mSelectedPosition == position);
            holder.mView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsPremium) {
                            notifyItemChanged(mSelectedPosition);
                            mSelectedPosition = position;
                            notifyItemChanged(mSelectedPosition);
                            PrefUtils.setStyleIndex(StyleActivity.this, position);
                        } else if (holder.mLocked.getVisibility() == View.VISIBLE) {
                            showDialogLocked();
                        }
                    }
                }
            );
        }

        @NonNull
        @Override
        public StyleHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            switch (type) {
                case STYLE_BLACK:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Black)
                        ).inflate(R.layout.style_list_row_black, parent, false)
                    );
                case STYLE_CLASSIC:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Classic)
                        ).inflate(R.layout.style_list_row_default, parent, false)
                    );
                case STYLE_MATRIX:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Matrix)
                        ).inflate(R.layout.style_list_row_matrix, parent, false)
                    );
                case STYLE_MONO:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Mono)
                        ).inflate(R.layout.style_list_row_mono, parent, false)
                    );
                case STYLE_PUBLICATION:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Publication)
                        ).inflate(R.layout.style_list_row_publication, parent, false)
                    );
                case STYLE_SEPIA:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Sepia)
                        ).inflate(R.layout.style_list_row_sepia, parent, false)
                    );
                case STYLE_DEFAULT:
                default:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Default)
                        ).inflate(R.layout.style_list_row_default, parent, false)
                    );
            }
        }

        private @ColorRes int getLinkColorFromStyle(int position) {
            switch (position) {
                case STYLE_BLACK:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? android.R.color.black : android.R.color.white;
                case STYLE_CLASSIC:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.blue_50 : R.color.blue_20;
                case STYLE_MATRIX:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.green_50 : R.color.green_20;
                case STYLE_MONO:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.gray_50 : R.color.gray_20;
                case STYLE_PUBLICATION:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.red_50 : R.color.red_20;
                case STYLE_SEPIA:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.orange_50 : R.color.orange_20;
                case STYLE_DEFAULT:
                default:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.simplenote_blue_50 : R.color.simplenote_blue_20;
            }
        }

        class StyleHolder extends RecyclerView.ViewHolder {
            private ImageView mLocked;
            private TextView mContent;
            private TextView mTitle;
            private View mView;

            StyleHolder(View view) {
                super(view);
                mView = view;
                mLocked = view.findViewById(R.id.preview_locked);
                mTitle = view.findViewById(R.id.preview_title);
                mContent = view.findViewById(R.id.preview_content);
            }
        }
    }
}
