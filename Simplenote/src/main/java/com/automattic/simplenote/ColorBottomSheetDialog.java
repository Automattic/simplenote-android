package com.automattic.simplenote;

import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
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

    private final static int[] MATERIAL_COLORS_PRIMARY = {0xffe91e63, 0xfff44336, 0xffff5722, 0xffff9800, 0xffffc107, 0xffffeb3b, 0xffcddc39, 0xff8bc34a,
            0xff4caf50, 0xff009688, 0xff00bcd4, 0xff03a9f4, 0xff2196f3, 0xff3f51b5, 0xff673ab7, 0xff9c27b0};

    private Button mSetColor;
    private Button mResetColor;

    private Fragment mFragment;
    private Note mNote;
    private ColorPickerDialogFragment dialog;

    private TextView mTextView;
    private View mColorBox;
    private int index = 0;


    public ColorBottomSheetDialog(@NonNull final Fragment fragment, @NonNull final ColorSheetListener colorSheetListener) {
        super(fragment.getActivity());

        mFragment = fragment;

        View colorView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.bottom_sheet_color, null, false);
        mResetColor = (Button) colorView.findViewById(R.id.reset_color);

        mTextView = (TextView) colorView.findViewById(R.id.textView);
        mColorBox = (View) colorView.findViewById(R.id.colorbox);

        Context cx = mFragment.getContext();
        RelativeLayout rLayout = (RelativeLayout) colorView.findViewById(R.id.colorbox);
//       RelativeLayout.LayoutParams lprams = new RelativeLayout.LayoutParams(64, 64);
//        lprams.rightMargin = 5;
//        lprams.topMargin = 5;
//        lprams.leftMargin = 5;
//        lprams.addRule(RelativeLayout.BELOW);
//        lprams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        View[] tv1 = new View[MATERIAL_COLORS_PRIMARY.length];
//
//        for (int i = 0; i < MATERIAL_COLORS_PRIMARY.length; i++) {
//
//            tv1[i] = new View(cx);
//            tv1[i].setId(i + 56789999);
//            RelativeLayout.LayoutParams lprams = new RelativeLayout.LayoutParams(64, 64);
//            lprams.rightMargin = 5;
//            lprams.topMargin = 5;
//            lprams.leftMargin = 5;
//            if (i == 0) lprams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
//            if (i > 0)
//                lprams.addRule(RelativeLayout.RIGHT_OF, tv1[i - 1].getId());
//
//            tv1[i].setLayoutParams(lprams);
//            tv1[i].setBackgroundColor(MATERIAL_COLORS_PRIMARY[i]);
//
//            rLayout.addView(tv1[i]);
        int marg = (int)convertDpToPixel(4.0, cx);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                tv1[index] = new View(cx);
                tv1[index].setId(index + 56789999);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams((int)convertDpToPixel(36.0, cx), (int)convertDpToPixel(36.0, cx));
                rlp.setMargins(marg, marg, marg, marg);
                if (j == 0) {
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                            RelativeLayout.TRUE);
                } else {
                    rlp.addRule(RelativeLayout.RIGHT_OF, tv1[index - 1].getId());
                }
                if (i == 0) {
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                            RelativeLayout.TRUE);
                } else {
                    rlp.addRule(RelativeLayout.BELOW, tv1[index - 8].getId());
                }
                tv1[index].setLayoutParams(rlp);
                tv1[index].setBackgroundColor(MATERIAL_COLORS_PRIMARY[index]);
                rLayout.addView(tv1[index]);
                index++;
            }
        }


        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
//                colorSheetListener.onColorDismissed();
            }
        });



        setContentView(colorView);
    }

    public void show(Note note) {
        mNote = note;
        if (mFragment.isAdded()) {
            mResetColor.setOnClickListener(this);
            refreshColor();
            show();
        }
    }

    private void refreshColor() {
        int color = mNote.getColor();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reset_color:
                mNote.setColor(Color.WHITE);
                mNote.save();
                refreshColor();

                break;
        }
    }

    public void updateColor(int color) {
        mNote.setColor(color);
        refreshColor();
    }

    public static double convertDpToPixel(double dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        double px = dp * ((double)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public interface ColorSheetListener {
        public void onColorUpdate(int color);

        void onColorDismissed();
    }

}