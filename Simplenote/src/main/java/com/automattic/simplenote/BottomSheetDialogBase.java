package com.automattic.simplenote;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.DateTimeUtils;

import java.text.NumberFormat;

/**
 * Created by Ondrej Ruttkay on 27/03/2016.
 */
public class BottomSheetDialogBase extends BottomSheetDialog {

    public BottomSheetDialogBase(@NonNull Context context) {
        super(context);
    }

    @Override
    public void show() {
        super.show();

        // limit the width of the bottom sheet on wide screens
        // non-zero width defined only for sw600dp
        float dp = getContext().getResources().getDimension(R.dimen.bottom_sheet_dialog_width);
        if (dp > 0) {

            // convert dp to px
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            int px = (int) ((dp * displayMetrics.density) + 0.5);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(getWindow().getAttributes());
            lp.width = px;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

            getWindow().setAttributes(lp);
        }
    }
}
