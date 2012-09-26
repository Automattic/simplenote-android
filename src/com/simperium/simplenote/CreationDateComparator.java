package com.simperium.simplenote;

import java.util.Comparator;

/* This class implements the compare method to be used to sort an array  
 * in by creation date 
 */

public class CreationDateComparator implements Comparator<Note>
{
	public int compare(Note first, Note second) 
	{
		if(first == null)
			return 1;
		else if (first.creationDate == null)
		{
			return 1;
		}
		return first.creationDate.compareTo(second.creationDate);
	}

}
