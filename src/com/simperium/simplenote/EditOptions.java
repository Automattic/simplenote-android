package com.simperium.simplenote;

import java.util.ArrayList;

import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.content.*;
import android.graphics.Typeface;

/* This activity is the screen for viewing and setting the options. 
 */

public class EditOptions extends Activity implements OnItemClickListener, CompoundButton.OnCheckedChangeListener
{
	protected static final int ACTIVITY_CREATE_ACCOUNT = 0; // enum for activities to be launched from here
	protected static final int ACTIVITY_LOGIN_ACCOUNT = 1; // enum for activities to be launched from here
	protected static final int ACTIVITY_PREMIUM = 2; // enum for activities to be launched from here
	protected ArrayList<String> items; // the strings to be listed in options
	protected BaseAdapter adapter; // need a pointer to the data so that it can re-display when changed
	protected CheckBox lockOrientationBox, webSyncingBox, detectLinksBox, showDatesBox; // checkboxes
	protected ListView optionsList;
	//not now	protected int orientation;
//not now	protected OrientationEventListener mListener;
	
	
	   /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	Typeface tf;
    	super.onCreate(savedInstanceState);
     	requestWindowFeature(Window.FEATURE_NO_TITLE);
     	setContentView(R.layout.options_view);
     	String email = Preferences.getEmail();
    	items = new ArrayList<String>();
    	
    	if(email==null) // if there is no email address stored in preferences, get message instead
    		email = this.getString(R.string.LoginAccount);
    	
    	items.add("Create Account");
    	items.add(email);
    	items.add("Premium Features");
    	items.add("Sort Order");
    	items.add("Preview Lines");
//not now    	items.add("Lock Orientation");
       	items.add("Web Syncing");
       	items.add("Show Dates");
//not now     	items.add("Detect Links");
//not now     	items.add("Show Ads");
       	
       	Button b = (Button)this.findViewById(R.id.OptionsViewBackButton);
       	b.setTypeface(tf = SimpleNote.getTypeface());
       	b.setOnClickListener(new View.OnClickListener()
				{
			      public void onClick(View view) 
			      {
			    	  setResult(EditOptions.RESULT_OK);
			    	  finish();
			      }
				});
       	optionsList = (ListView)this.findViewById(R.id.OptionsList);
       	
       	// set the data
        optionsList.setAdapter(adapter = new OptionsListAdapter(this, tf, items));
        // set click listener
        ((TextView)findViewById(R.id.OptionsViewTitle)).setTypeface(tf);
     	optionsList.setOnItemClickListener(this);
    }
    
    //called when a sub-activity finishes. Needed to update options selected
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        if (requestCode == ACTIVITY_LOGIN_ACCOUNT) 
        {
            if (resultCode == RESULT_OK)
            {
               items.set(1, Preferences.getEmail());
               adapter.notifyDataSetChanged();
            }
        }
    }
    
    // event handler for when someone selects a list item
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) 
	{
		LayoutInflater	li;
		ArrayList<String> items;
		View dialogView;
		AlertDialog.Builder builder;
		ArrayAdapter<String> array_adapter;
		
		switch(position)
		{
		case 0: //Create account
			startActivityForResult(new Intent(this, CreateAccountActivity.class), ACTIVITY_CREATE_ACCOUNT);
			break;
		case 1://Email
			startActivityForResult(new Intent(this, LoginActivity.class), ACTIVITY_LOGIN_ACCOUNT);
			break;
		case 2://Premium Features
	 	       startActivityForResult(new Intent(this, PremiumFeatures.class), ACTIVITY_PREMIUM);
	 	      
			break;
		case 3://Sort Order, create pop-up menu
			items = new ArrayList<String>();
			items.add("Date Modified");
			items.add("Date Created");
			items.add("Alphabetic");
			
			li = this.getLayoutInflater();
			dialogView = (View)li.inflate(R.layout.num_rows_dialog, null);
			
			final Spinner spin = (Spinner)dialogView.findViewById(R.id.NumRowsSpinner);
			array_adapter = new ArrayAdapter<String>(this, R.layout.preview_lines_row, items);
			array_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spin.setAdapter(array_adapter);
			spin.setSelection(Preferences.getSortOrder());
			
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.Sort_Order_Dialog_Title)
			.setView(dialogView)
	  	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() 
	  	      {
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						int value = spin.getSelectedItemPosition();
						Preferences.setSortOrder(value);
						adapter.notifyDataSetChanged();
					}
	  	      })
	  	      .setCancelable(false)
	  	      .show();
			break;
		case 4://Preview lines, show pop-up window
			items = new ArrayList<String>();
			items.add("0");
			items.add("1");
			items.add("2");
			items.add("3");
			items.add("4");
			
			li = this.getLayoutInflater();
			dialogView = (View)li.inflate(R.layout.num_rows_dialog, null);
			
			final Spinner spinn = (Spinner)dialogView.findViewById(R.id.NumRowsSpinner);
			spinn.setAdapter(new ArrayAdapter<String>(this, R.layout.preview_lines_row, items));
			spinn.setSelection(Preferences.getNumLines());
			
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.Num_Rows_Dialog_Title)
			.setView(dialogView)
	  	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() 
	  	      {
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						int value = spinn.getSelectedItemPosition();
						Preferences.setNumLines(value);
						adapter.notifyDataSetChanged();
					}
	  	      })
	  	      .setCancelable(false)
	  	      .show();
			break;
		case 5://Web syncing
			break;
		case 6://show dates. Do nothing because it's handled by checkbox changed
			break;
/*		case 7://Detect links
			break;
		case 5: //Lock orientation
			break;
//		case 9://show ads
//			break;
*/
			}
	}

	public void setCheckBox(CheckBox cb, int position)
	{
		switch(position)
		{
/*		case Preferences.LockOrientation:
			lockOrientationBox = cb;
			break;
*/		case Preferences.WebSyncing:
			webSyncingBox = cb;
			break;
		case Preferences.ShowDates:
			showDatesBox = cb;
			break;
/*		case Preferences.DetectLinks:
			detectLinksBox = cb;
			break;
*/		}
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
	{
		if(buttonView == lockOrientationBox)
			Preferences.setLockOrientation(isChecked);
		else if(buttonView == webSyncingBox)
			Preferences.setWebSync(isChecked);
		else if(buttonView == showDatesBox)
			Preferences.setShowDates(isChecked);
		else if(buttonView == detectLinksBox)
			Preferences.setDetectLinks(isChecked);
	}
}
