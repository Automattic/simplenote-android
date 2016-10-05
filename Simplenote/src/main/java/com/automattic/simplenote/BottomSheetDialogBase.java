package com.automattic.simplenote;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.view.WindowManager;

import com.automattic.simplenote.utils.DisplayUtils;

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
        int dp = (int) getContext().getResources().getDimension(R.dimen.bottom_sheet_dialog_width);
        if (dp > 0) {

            // convert dp to px
            int px = DisplayUtils.dpToPx(getContext(), dp);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(getWindow().getAttributes());
            lp.width = px;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

            getWindow().setAttributes(lp);
        }
    }
}
