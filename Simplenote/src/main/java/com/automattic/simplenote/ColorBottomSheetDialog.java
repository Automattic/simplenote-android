package com.automattic.simplenote;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;


import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Reminder;

import org.dmfs.android.colorpicker.ColorPickerDialogFragment;
import org.dmfs.android.colorpicker.ColorPickerDialogFragment.ColorDialogResultListener;
import org.dmfs.android.colorpicker.palettes.AbstractPalette;
import org.dmfs.android.colorpicker.palettes.ArrayPalette;


/**
 * Created by asmadek on 28/09/2016.
 */
public class ColorBottomSheetDialog extends BottomSheetDialogBase implements View.OnClickListener {

    public static final int UPDATE_COLOR_REQUEST_CODE = 101;
    public static final String TIMESTAMP_BUNDLE_KEY = "color";

    private final static int[] MATERIAL_COLORS_PRIMARY = { 0xffe91e63, 0xfff44336, 0xffff5722, 0xffff9800, 0xffffc107, 0xffffeb3b, 0xffcddc39, 0xff8bc34a,
            0xff4caf50, 0xff009688, 0xff00bcd4, 0xff03a9f4, 0xff2196f3, 0xff3f51b5, 0xff673ab7, 0xff9c27b0 };

    private Button mSetColor;
    private Button mResetColor;

    private Fragment mFragment;
    private Note mNote;

    public ColorBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final ColorSheetListener colorSheetListener) {
        super(fragment.getActivity());

        mFragment = fragment;

        View colorView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.bottom_sheet_color, null, false);
        mSetColor = (Button) colorView.findViewById(R.id.set_color);
        mResetColor = (Button) colorView.findViewById(R.id.reset_color);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                colorSheetListener.onColorDismissed();
            }
        });

        setContentView(colorView);
    }

    public void show(Note note) {
        mNote = note;
        if (mFragment.isAdded()) {
            mSetColor.setOnClickListener(this);
            mResetColor.setOnClickListener(this);
            refreshColor();
            show();
        }
    }

    private void refreshColor() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.set_color:
                ColorPickerDialogFragment dialog = new ColorPickerDialogFragment();

                ArrayList<AbstractPalette> palettes = new ArrayList<AbstractPalette>();

                palettes.add(new ArrayPalette("material_primary", "Material Colors", MATERIAL_COLORS_PRIMARY, 4));

                dialog.setPalettes(palettes.toArray(new AbstractPalette[palettes.size()]));
                dialog.show(mFragment.getFragmentManager(), "");

                break;
            case R.id.reset_color:

                break;
        }
    }

    public void updateColor(int color) {
        mNote.setColor(color);
        refreshColor();
    }

    public interface ColorSheetListener {
        void onColorOn();

        void onColorOff();

        public void onColorUpdated(int color);

        void onColorDismissed();
    }
}