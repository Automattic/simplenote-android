package com.automattic.simplenote.widgets;

import android.support.annotation.NonNull;
import android.text.style.ClickableSpan;
import android.view.View;

public class CheckableSpan extends ClickableSpan {

    private boolean isChecked;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    @Override
    public void onClick(@NonNull View view) {
        setChecked(!isChecked);
        if (view instanceof SimplenoteEditText) {
            try {
                ((SimplenoteEditText)view).toggleCheckbox(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
