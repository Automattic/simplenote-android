package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.automattic.simplenote.billing.IabHelper;
import com.automattic.simplenote.billing.IabResult;
import com.automattic.simplenote.billing.Inventory;
import com.automattic.simplenote.billing.Purchase;
import com.simperium.client.User;

public class BillingActivity extends SherlockFragmentActivity {
	
	/***
	 * in-app purchasing - added by nbradbury 01-Apr-2013 - still a work-in-progress (currently uses a test SKU)
	 * 
	 * references:
	 * 	http://developer.android.com/google/play/billing/billing_best_practices.html
	 * 	http://developer.android.com/google/play/billing/billing_testing.html
	 * 	http://developer.android.com/google/play/billing/billing_subscriptions.html
	 * 	https://support.google.com/googleplay/android-developer/answer/140504
	 */
	

	// TODO: instead of hard-coding public key, construct from pieces or XOR with some other string to hide the actual key
	private static final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqlZaK0DdSAVsZi7EipQDQ4/0JpGZfbMA5abzSCo7PqbcnccbYXogCPfjnoHBSmFfyoMiquv7dS1frHCFAOJjdPkTRXEWmeZ1y3kOjXcVbld033AFLsguYkwI8zVfj4vtmyIO2RS8RuelWaYepeb+9oLtJ6qj6lxBrOWWoltEF+gf+4DGWKbGSs+O2Mbtp3JW6CMC1LvIxYX0C+yL4q3aOTnh2nXTaUEr2EgNosf9ksM/ZqEAd4c7LJ9cxkz+4yo+LkPwsNnSwegdRpwAXGlXdTMDWPFXA1bC/l1LYIl9yTLUMhuoWf2KvmYNsPS7kQ2TTdFLUsxPap5JW1PPGXcyHwIDAQAB";
    
	// (arbitrary) request code for the purchase flow
	private static final int RC_REQUEST = 10001; 
	
	private boolean mUserIsPremium = false;
	
	// billing helper object
	private IabHelper mHelper;

	private static final String getSku() {
		// TODO: replace this with actual SKU once testing is completed
		return "android.test.purchased";
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// enable showing progress in title (used by showProgress/hideProgress)
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_billing);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// locate controls
		Button btnBuy = (Button) findViewById(R.id.button_buy);
		TextView txtLink = (TextView) findViewById(R.id.text_premium_link);
		
		// start purchase flow when "buy" button tapped
		btnBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startPurchase();
			}
		});
		
		// open browser to premium info page when link tapped
		txtLink.setMovementMethod(LinkMovementMethod.getInstance());
		CharSequence htmlLink = Html.fromHtml("<a href='" + getString(R.string.premium_link_url)+ "'>" + getString(R.string.premium_link_title) + "</a>");
		txtLink.setText(htmlLink);
		
		initBilling();
	}
	
	@Override
	public void onDestroy() {
	   super.onDestroy();
	   
	   // unbind from billing service
	   if (mHelper!=null) {
		   mHelper.dispose();
		   mHelper = null;
	   }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default :
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Simplenote.TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // pass on the activity result to the helper for handling
        if (mHelper==null || !mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(Simplenote.TAG, "onActivityResult handled by IABUtil.");
        }
    }
	
	private void showToast(String message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}
	
	private void showProgress() {
		setProgressBarIndeterminateVisibility(true);
	}
	private void hideProgress() {
		setProgressBarIndeterminateVisibility(false);
	}
	
	private void setUserIsPremium(boolean value) {
		mUserIsPremium = value;
		// TODO: update UI to show that user is a premium subscriber
	}
	
	private void initBilling() {
		showProgress();
		
		// Create the helper, passing it our context and the public key to verify signatures with
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // TODO: disable debug logging in production version
        mHelper.enableDebugLogging(true);
        
        // start asynchronous setup - listener will be called once setup completes.
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    showToast(getString(R.string.billing_err_setup_failed, result));
                    hideProgress();
                    return;
                }

                // IAB is fully set up, now get an inventory of stuff this user has purchased
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
	}
	
	// listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
        	hideProgress();
        	
            if (result.isFailure()) {
                showToast(getString(R.string.billing_err_query_inventory_failed, result));
                return;
            }

            // does the user have a subscription to the app?
            Purchase subscriptionPurchase = inventory.getPurchase(getSku());
            //mHelper.consumeAsync(subscriptionPurchase, null); // <-- use this to remove the purchase - useful for testing so purchase can be made more than once
            if (verifyDeveloperPayload(subscriptionPurchase))
            	setUserIsPremium(true);
        }
    };
    
    // callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
            	Log.w(Simplenote.TAG, "purchase failed - " + result);
            	switch (result.getResponse()) {
            	case IabHelper.IABHELPER_USER_CANCELLED :
            		// no error toast when cancelled
            		break;
            	default :
            		showToast(getString(R.string.billing_err_purchasing, result));
            		break;
            	}
                return;
            }
            
            // make sure payload is valid (should rarely, if ever, fail unless app/device is hacked)
            if (!verifyDeveloperPayload(purchase)) {
            	Log.w(Simplenote.TAG, "purchase payload verification failed");
            	showToast(getString(R.string.billing_err_payload_verification_failed));
                return;
            }

            Log.i(Simplenote.TAG, "purchase successful");
            showToast(getString(R.string.billing_toast_purchase_success));
            setUserIsPremium(true);
        }
    };

    // the "payload" is a string unique to this customer, used to associate the customer with a specific purchase
    // TODO: for now user's email address is the payload - this way they can login from other devices and still have their subscription purchase honored
    // may need to change this if customers login to multiple accounts in the same app
    private String getDeveloperPayload() {
    	// note that user will never be null here, since user has to be logged in before they
    	// can get to this activity
    	User user = ((Simplenote) getApplication()).getSimperium().getUser();
    	return user.getEmail();
    }
    
    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase purchase) {
    	if (purchase==null)
    		return false;
    	
        String payload = purchase.getDeveloperPayload();
        return payload.equals(getDeveloperPayload());
        
        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         * 
         * WARNING: Locally generating a random string when starting a purchase and 
         * verifying it here might seem like a good approach, but this will fail in the 
         * case where the user purchases an item on one device and then uses your app on 
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         * 
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         * 
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on 
         *    one device work on other devices owned by the user).
         * 
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
    }
    
    private void startPurchase() {
    	// make sure device supports subscription purchase (most do)
    	if (!mHelper.subscriptionsSupported()) {
            showToast(getString(R.string.billing_err_subscription_not_supported));
            return;
        }
    	
    	mHelper.launchPurchaseFlow(this,
    							   getSku(),
    							   IabHelper.ITEM_TYPE_INAPP, // TODO: should be ITEM_TYPE_SUBS when using actual subscription SKU (using ITEM_TYPE_INAPP for now since subscriptions can't be tested)
    							   RC_REQUEST,
    							   mPurchaseFinishedListener,
    							   getDeveloperPayload()); 
    }

}
