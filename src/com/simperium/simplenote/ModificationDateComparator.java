package com.simperium.simplenote;

import java.util.Comparator;

/* This class implements the compare method to be used to sort an array  
 * in by modification date 
 */

public class ModificationDateComparator implements Comparator<Note>
{
	public int compare(Note first, Note second) 
	{
		return first.modificationDate.compareTo(second.modificationDate);
	}

}
