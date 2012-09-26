package com.simperium.simplenote;

import java.util.Comparator;

/* This class implements the compare method to be used to sort an array  
 * in alphabetical order 
 */

public class AlphanumericComparator implements Comparator<Note> 
{
	public int compare(Note first, Note second) 
	{
		int answer = 0;
		if(first == null) 
			answer = 1;
		else
		{
			answer = first.getTitle().compareTo(second.getTitle());
			if(answer == 0)
			{
				answer = first.getContent().compareTo(second.getContent());
			}
		}
		return answer;
	}

}
