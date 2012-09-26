package com.simperium.simplenote;

import java.util.*;
import android.app.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

/*
 * This class is the screen to show what premium features are available
 */

public class PremiumFeatures extends Activity implements OnItemClickListener
{
	protected ArrayList<String> features;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
    	Typeface tf = SimpleNote.getTypeface();
    	ListView lv;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
     	setContentView(R.layout.premium_features);
       
     	Button b = (Button)findViewById(R.id.PremiumFeaturesBackButton);
     	b.setTypeface(tf);
		b.setOnClickListener(new View.OnClickListener()
		{
		      public void onClick(View view) 
		      {
		    	  setResult(PremiumFeatures.RESULT_OK);
		    	  finish();
		      }
		});

		((TextView)findViewById(R.id.PremiumFeaturesTitle)).setTypeface(tf);
        // add the features
		features = new ArrayList<String>();
		features.add("Disable Ads");
		features.add("Automatic Backup");
		features.add("Create By Email");
		features.add("RSS Feed");
		features.add("Unlimited API Usage");
		features.add("Insider Access");
		features.add("Premium Support");

		lv = (ListView)this.findViewById(R.id.PremiumFeaturesList);
		// show them
        lv.setAdapter(new PremiumFeaturesAdapter(this, tf));
        lv.setOnItemClickListener(this);
    }
    
    protected void onResume()
    {
    	super.onResume();	
    }

    protected void onPause()
    {
    	super.onPause();
    }

    public int getNumFeatures()
    {
    	return features.size();
    }
    
    public Object getItem(int pos)
    {
    	return features.get(pos);
    }
    
    // when the user selects a premium feature, show a pop-up message
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) 
	{
		String title = "Premium Features";
		String message = "";
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);

		switch(position)
		{
		case 0:
			message = "After you purchase Simplenote Premium, you can disable the ads on your device.";
			break;
		case 1:
			message = "For peace of mind, Simplenote Premium will regularly backup all of your notes. You'll rest easy knowing that older versions of your notes are accessible in the web app if you ever need them.";
			break;
		case 2:
			message = "Wouldn't it be nice if you could turn an email into a note? With Simplenote Premium, you'll get a private email address for creating new notes by email";
			break;
		case 3:
			message = "RSS is a publishing format you can use to read and share your notes in a variety of ways. When you subscribe to Simplenote Premium, you'll get your very own RSS feed";
			break;
		case 4:
			message = "When you use Simplenote Extras, such as desktop applications and scripts, we need to reserve the right to limit your usage to about 2000 API requests per day.\n\nWith Simplenote Premium, your daily usage is unlimited (as long as you don't abuse or misuse the system)";
			break;
		case 5:

			message = "With your purchase of Simplenote Premium, you'll get early access to new services for the duration of your subscription.";
			break;
		case 6:
			message = "Our regular support is usually quite fast, but with your Simplenote Premium subscription, we'll give you a high priority support address";
			break;
		}
		builder.setMessage(message);
		builder.setPositiveButton("Ok", null);
		Dialog diag = builder.create();

		diag.show();
	}
}
