package com.automattic.simplenote;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.simplenote.utils.BrowserUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.EmptyViewRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.automattic.simplenote.PreferencesFragment.WEB_APP_URL;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_CLASSIC;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_DEFAULT;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_MONO;
import static com.automattic.simplenote.utils.ThemeUtils.STYLE_PUBLICATION;

public class StyleActivity extends ThemedAppCompatActivity {
    private boolean mIsPremium;

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(StyleActivity.this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_style);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        SpannableString title = new SpannableString(getString(R.string.style));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mIsPremium = PrefUtils.isPremium(StyleActivity.this);

        EmptyViewRecyclerView list = findViewById(R.id.list);
        list.setAdapter(new StyleAdapter(Arrays.asList(getResources().getStringArray(R.array.array_style_names))));
        list.setLayoutManager(new LinearLayoutManager(StyleActivity.this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(StyleActivity.this);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void recreate() {
        Intent intent = new Intent(StyleActivity.this, StyleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showDialogLocked() {
        new AlertDialog.Builder(new ContextThemeWrapper(StyleActivity.this, R.style.Dialog))
            .setTitle(R.string.style_dialog_locked_title)
            .setMessage(R.string.style_dialog_locked_message)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(
                R.string.yes,
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
            mSelectedPosition = mIsPremium ? PrefUtils.getStyleIndex(StyleActivity.this) : PrefUtils.getStyleIndexDefault(StyleActivity.this);
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
            holder.mLocked.setVisibility(mIsPremium || position == PrefUtils.getStyleIndexDefault(StyleActivity.this) ? View.GONE : View.VISIBLE);
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
                            recreate();
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
                case STYLE_CLASSIC:
                    return new StyleHolder(LayoutInflater.from(
                        new ContextThemeWrapper(
                            parent.getContext(),
                            R.style.Style_Classic)
                        ).inflate(R.layout.style_list_row_classic, parent, false)
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
                case STYLE_CLASSIC:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.simplenote_blue_50 : R.color.simplenote_blue_20;
                case STYLE_PUBLICATION:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.red_50 : R.color.red_20;
                case STYLE_MONO:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.gray_50 : R.color.gray_20;
                case STYLE_DEFAULT:
                default:
                    return ThemeUtils.isLightTheme(StyleActivity.this) ? R.color.blue_50 : R.color.blue_20;
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
