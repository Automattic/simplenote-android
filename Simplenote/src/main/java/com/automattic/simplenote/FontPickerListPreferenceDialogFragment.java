package com.automattic.simplenote;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreferenceDialogFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.automattic.simplenote.widgets.FontPickerListPreference;
import com.automattic.simplenote.widgets.TypefaceCache;

/**
 * Custom dialog fragment used in preferences to show font selections
 */

public class FontPickerListPreferenceDialogFragment extends ListPreferenceDialogFragmentCompat {

    private CharSequence[] mEntries;
    private FontPickerListPreference mPreference;

    public static FontPickerListPreferenceDialogFragment newInstance(String key) {
        final FontPickerListPreferenceDialogFragment fragment = new FontPickerListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        mEntries = getResources().getStringArray(R.array.array_font_names);

        mPreference = (FontPickerListPreference)getPreference();

        CustomListPreferenceAdapter customListPreferenceAdapter = new CustomListPreferenceAdapter(getContext());
        builder.setAdapter(customListPreferenceAdapter, null);

        super.onPrepareDialogBuilder(builder);
    }

    private class CustomListPreferenceAdapter extends BaseAdapter
    {
        private Context mContext;

        private CustomListPreferenceAdapter(Context context)
        {
            mContext = context;
        }

        public int getCount()
        {
            return mEntries.length;
        }

        public Object getItem(int position)
        {
            return position;
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent)
        {
            if (mContext == null) {
                return null;
            }

            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(mContext).inflate(R.layout.preference_font_item, parent, false);
            }

            TextView fontNameTextView = row.findViewById(R.id.font_name);
            fontNameTextView.setText(mEntries[position]);

            TextView fontPreviewTextView = row.findViewById(R.id.font_preview);

            if (position == 1) {
                fontNameTextView.setTypeface(TypefaceCache.getTypeface(mContext, TypefaceCache.TYPEFACE_NAME_MONOSPACE));
                fontPreviewTextView.setTypeface(TypefaceCache.getTypeface(mContext, TypefaceCache.TYPEFACE_NAME_MONOSPACE));
                fontPreviewTextView.setText(R.string.font_summary_monospace);
            } else {
                fontNameTextView.setTypeface(Typeface.DEFAULT);
                fontPreviewTextView.setTypeface(Typeface.DEFAULT);
                fontPreviewTextView.setText(R.string.font_summary_default);
            }

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleClickAtPosition(position);
                }
            });

            RadioButton radioButton = row.findViewById(R.id.font_radio_button);
            radioButton.setChecked(mPreference.getPreference().equals(String.valueOf(position)));
            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleClickAtPosition(position);
                }
            });

            return row;
        }
    }

    private void handleClickAtPosition(int position) {
        mPreference.savePreference(String.valueOf(position), mEntries[position].toString());
        getDialog().dismiss();
    }
}
