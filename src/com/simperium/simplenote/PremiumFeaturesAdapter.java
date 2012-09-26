package com.simperium.simplenote;


import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

public class PremiumFeaturesAdapter extends BaseAdapter 
{
	PremiumFeatures context;
	Typeface tf;
	
	public PremiumFeaturesAdapter(PremiumFeatures pf, Typeface _tf)
	{
		context = pf;
		tf = _tf;
	}
	
	@Override
	public int getCount()
	{
		return context.getNumFeatures();
	}

	@Override
	public Object getItem(int position)
	{
		return context.getItem(position);
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
		TextView tv;

		premiumRow = li.inflate(R.layout.premium_row, null);
		tv = (TextView)premiumRow.findViewById(R.id.FeatureName);
		tv.setTypeface(tf);
		tv.setText((String)getItem(position));
	
		return premiumRow;
	}
}
