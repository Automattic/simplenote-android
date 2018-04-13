package com.automattic.simplenote;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.automattic.simplenote.utils.DisplayUtils;
import com.simperium.android.LoginActivity;

public class SignInActivity extends LoginActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View mainView = findViewById(R.id.main);
        if (!(mainView instanceof ScrollView)) {
            return;
        }

        LinearLayout layout = (LinearLayout)((ScrollView)mainView).getChildAt(0);
        View signInFooter = getLayoutInflater().inflate(R.layout.sign_in_footer, null);

        layout.addView(signInFooter);
    }
}
