package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.automattic.simplenote.R;

public class PreferencesActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		findPreference("pref_key_sign_out").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				
                Simplenote application = (Simplenote)getApplication();
                application.getSimperium().deAuthorizeUser();
                application.getNotesBucket().reset();
                application.getTagsBucket().reset();
				return true;
			}
		});
		
		findPreference("pref_key_billing").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(PreferencesActivity.this, BillingActivity.class);
                PreferencesActivity.this.startActivityForResult(intent, Simplenote.INTENT_BILLING);
				return true;
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Simplenote currentApp = (Simplenote) getApplication();
		if( currentApp.getSimperium().getUser() == null || currentApp.getSimperium().getUser().needsAuthentication() ){
			finish();
		}
	}
	
}
