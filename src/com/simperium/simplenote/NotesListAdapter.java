package com.simperium.simplenote;

import java.util.*;
import android.app.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

/*
 * this class helps format lists, in this case it is the main list of notes.
 * it sets the maximum number of lines visible, date, etc.
 */

public class NotesListAdapter extends BaseAdapter 
{
	Activity context;
	Vector<Note> notes;
	Typeface tf;
	
	public NotesListAdapter(Activity pf, Typeface _tf, Vector<Note> array)
	{
		context = pf;
		notes = array;
		tf = _tf;
	}
	
	@Override
	public int getCount()
	{
		//number of notes
		return notes.size();
	}

	@Override
	public Object getItem(int position)
	{
		return notes.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View premiumRow;
		LayoutInflater li = context.getLayoutInflater();
		premiumRow = li.inflate(R.layout.notes_row, null);
		//set the content preview
		TextView tv;
		
		tv = (TextView)premiumRow.findViewById(R.id.NotesRowTitle);
		tv.setTypeface(tf);
		tv.setText(notes.get(position).getTitle());
		
		tv = (TextView)premiumRow.findViewById(R.id.NotesRowText);
		tv.setMinLines(Preferences.getNumLines());
		tv.setMaxLines(Preferences.getNumLines());
		tv.setText(notes.get(position).contentPreview);

		// set the date
		if(Preferences.isShowDates())
		{
			tv = (TextView)premiumRow.findViewById(R.id.NotesRowDate);
			tv.setTypeface(tf);
			tv.setText(notes.get(position).getCreationDatePreview());
		}
		// voila!
		return premiumRow;
	}
}
