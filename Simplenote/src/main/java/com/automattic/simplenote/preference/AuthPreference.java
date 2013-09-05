package com.automattic.simplenote.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.automattic.simplenote.R;

public class AuthPreference extends Preference implements View.OnClickListener{

    CharSequence mButtonLabel;

    public AuthPreference(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    public AuthPreference(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public AuthPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view){
        super.onBindView(view);
        Button button = (Button) view.findViewById(R.id.auth_button);
        if (button != null) {
            button.setText(getButtonLabel());
            button.setOnClickListener(this);
        }
    }

    public void setButtonLabel(int resourceId){
        setButtonLabel(getContext().getString(resourceId));
    }

    public void setButtonLabel(CharSequence label){
        mButtonLabel = label;
        notifyChanged();
    }

    public CharSequence getButtonLabel(){
        return mButtonLabel;
    }

    public void onClick(View v){
        Preference.OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null) {
            listener.onPreferenceClick(this);
        }
    }
}

