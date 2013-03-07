package com.automattic.simplenote;

import com.simperium.client.Simperium;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		findPreference("pref_key_sign_out").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
                Simplenote application = (Simplenote)getApplication();
                application.getSimperium().deAuthorizeUser();
				return true;
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		 if (requestCode == Simperium.SIGNUP_SIGNIN_REQUEST) {
			 if (resultCode == RESULT_CANCELED) {
				 finish();
			 }
		 }
	}
}
