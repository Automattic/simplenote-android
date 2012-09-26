package com.simperium.simplenote;

import java.util.*;

import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

public class OptionsListAdapter extends BaseAdapter 
{
	EditOptions context;
	ArrayList<String> items; 
	Typeface tf;
	
	public OptionsListAdapter(EditOptions act, Typeface _tf, ArrayList<String> itms)
	{
		context = act;
		items = itms;
		tf = _tf;
	}
	
	@Override
	public int getCount()
	{
		int count = items.size();
		return count;
	}

	@Override
	public Object getItem(int position)
	{
		return items.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View thisRow = null;
		LayoutInflater li = context.getLayoutInflater();
		TextView tv;
		String temp = null;
		
		if(position < 5)
		{
			thisRow = li.inflate(R.layout.options_two_row, null);
			tv = (TextView)thisRow.findViewById(R.id.EditOptionTwoText);
			tv.setTypeface(tf);
			tv.setText(items.get(position));
			// set the sort order
			if(position == 3)
			{
		    	switch(Preferences.getSortOrder())
		    	{
		    	case 0:
		    		temp = context.getString(R.string.ModificationDateLabel);
		    		break;
		    	case 1:
		    		temp = context.getString(R.string.CreationDateLabel);
		    		break;
		    	case 2:
		    		temp = context.getString(R.string.AlphaNumericLabel);
		    		break;    	
		    	}
			}
			// show the number of preview lines
			else if(position == 4)
			{
				temp = String.valueOf(Preferences.getNumLines());
			}
			
			// set the string to be item on the right, the string can be date, or number of preview lines
			if(temp != null)
			{
				tv = (TextView)thisRow.findViewById(R.id.EditOptionTwoOptional);
				tv.setTypeface(tf);
				tv.setText(temp);
			}
		}
		else
		{
			// otherwise, set checkboxes accordingly.
			
			thisRow = li.inflate(R.layout.edit_options_checkbox_row, null);
			tv = (TextView)thisRow.findViewById(R.id.EditOptionsCheckBoxRowText);
			tv.setTypeface(tf);
			tv.setText(items.get(position));
			CheckBox cb = (CheckBox)thisRow.findViewById(R.id.EditOptionsRowCheckBox);
			cb.setChecked(Preferences.isOptionChecked(position));
			context.setCheckBox(cb, position);
			cb.setOnCheckedChangeListener(context);
		}
		return thisRow;
	}

}
