package com.simperium.simplenote;

import java.text.*;
import java.util.*;

import android.database.sqlite.SQLiteDatabase;


public class Note {
	
	protected String title;
	protected String content;
	protected String modificationDatePreview;
	protected String creationDatePreview;
	protected String titlePreview;
	protected String contentPreview;
	protected Calendar creationDate;
	protected Vector<String> tags;
	protected String remoteId;
	protected Calendar modificationDate;
	protected boolean deleted, pinned;
		
    // Opaque reference to the underlying database.
	SQLiteDatabase database;
    // Primary key in the database.
    protected long primaryKey;
	
    // Internal state variables. Hydrated tracks whether attribute data is in the object or the database.
    protected boolean hydrated;
    // Dirty tracks whether there are in-memory changes to data which have no been written to the database.
    protected boolean dirty;	
    
    public Note(long pKey, String s, Date creation, Date modification)
    {
    	Calendar date = Calendar.getInstance();
    	primaryKey = pKey;

   		creationDate = Calendar.getInstance();
   		modificationDate = Calendar.getInstance();

    
   		date.setTime(creation);
		setCreationDate(date);
		date.setTime(modification);
		setModificationDate(date);
    	setContent(s);
    }
    
    protected int insertNewNoteIntoDatabase(SQLiteDatabase database) 
    {
    	int retVal = -1;
    	/* fix this later
        if (insert_statement == nil) {
            static char *sql = "INSERT INTO note (title, content) VALUES('', '')";
            if (sqlite3_prepare_v2(database, sql, -1, &insert_statement, NULL) != SQLITE_OK) {
                NSAssert1(0, @"Error: failed to prepare statement with message '%s'.", sqlite3_errmsg(database));
            }
        }
        int success = sqlite3_step(insert_statement);
        // Because we want to reuse the statement, we "reset" it instead of "finalizing" it.
        sqlite3_reset(insert_statement);
        if (success != SQLITE_ERROR) {
            // SQLite provides a method which retrieves the value of the most recently auto-generated primary key sequence
            // in the database. To access this functionality, the table should have a column declared of type 
            // "INTEGER PRIMARY KEY"
            return sqlite3_last_insert_rowid(database);
        }
    	*/
    	return retVal;
    }
    
 // Finalize (delete) all of the SQLite compiled queries.
    public static void finalizeStatements()
    {
/*        if (insert_statement) sqlite3_finalize(insert_statement);
        if (init_statement) sqlite3_finalize(init_statement);
        if (delete_statement) sqlite3_finalize(delete_statement);
        if (hydrate_statement) sqlite3_finalize(hydrate_statement);
        if (dehydrate_statement) sqlite3_finalize(dehydrate_statement);
*/
    }

 // Creates the object with primary key and title is brought into memory.
    protected Note initWithPrimaryKey(int pk, SQLiteDatabase db) 
    { 
    	/*
    	   if (self = [super init]) {
    	        primaryKey = pk;
    	        database = db;
    	        // Compile the query for retrieving book data. See insertNewBookIntoDatabase: for more detail.
    	        if (init_statement == nil) {
    	            // Note the '?' at the end of the query. This is a parameter which can be replaced by a bound variable.
    	            // This is a great way to optimize because frequently used queries can be compiled once, then with each
    	            // use new variable values can be bound to placeholders.
    	            const char *sql = "SELECT title, content, creationDate, modificationDate, remoteId, deleted FROM note WHERE pk=?";
    	            if (sqlite3_prepare_v2(database, sql, -1, &init_statement, NULL) != SQLITE_OK) {
    	                NSAssert1(0, @"Error: failed to prepare statement with message '%s'.", sqlite3_errmsg(database));
    	            }
    	        }
    	        // For this query, we bind the primary key to the first (and only) placeholder in the statement.
    	        // Note that the parameters are numbered from 1, not from 0.
    	        sqlite3_bind_int(init_statement, 1, primaryKey);
    	        if (sqlite3_step(init_statement) == SQLITE_ROW) {
    	            self.title = [NSString stringWithUTF8String:(char *)sqlite3_column_text(init_statement, 0)];
    				self.content = [NSString stringWithUTF8String:(char *)sqlite3_column_text(init_statement, 1)];
    				self.creationDate = [NSDate dateWithTimeIntervalSince1970:sqlite3_column_double(init_statement, 2)];
    				self.modificationDate = [NSDate dateWithTimeIntervalSince1970:sqlite3_column_double(init_statement, 3)];
    				char *remoteText = (char *)sqlite3_column_text(init_statement, 4);
    				self.remoteId = remoteText == NULL ? @"" : [NSString stringWithUTF8String:remoteText];
    				self.deleted =  sqlite3_column_int(init_statement, 5);
    	        } else {
    	            self.title = @"No title";
    				self.content = @"";
    				self.remoteId = @"";
    				self.creationDate = [NSDate date];
    				self.modificationDate = [NSDate date];
    				self.deleted = NO;
    	        }
    	        // Reset the statement for future reuse.
    	        sqlite3_reset(init_statement);
    	        dirty = NO;
    	    }*/
    	    return this;
    }
 // Brings the rest of the object data into memory. If already in memory, no action is taken (harmless no-op).
 	protected void hydrate() 
 	{
 	    // Check if action is necessary.
 	    if (hydrated) return;
  		/*
	    // Compile the hydration statement, if needed.
 	    if (hydrate_statement == nil) {
 	        const char *sql = "SELECT creationDate FROM note WHERE pk=?";
 	        if (sqlite3_prepare_v2(database, sql, -1, &hydrate_statement, NULL) != SQLITE_OK) {
 	            NSAssert1(0, @"Error: failed to prepare statement with message '%s'.", sqlite3_errmsg(database));
 	        }
 	    }
 	    // Bind the primary key variable.
 	    sqlite3_bind_int(hydrate_statement, 1, primaryKey);
 	    // Execute the query.
 	    int success =sqlite3_step(hydrate_statement);
 	    if (success == SQLITE_ROW) {
 	        //char *str = (char *)sqlite3_column_text(hydrate_statement, 0);
 	        //self.content = (str) ? [NSString stringWithUTF8String:str] : @"";
 	        //self.creationDate = [NSDate dateWithTimeIntervalSince1970:sqlite3_column_double(hydrate_statement, 0)];
 	    } else {
 	        // The query did not return 
 	        //self.content = @"";
 			//self.creationDate = [NSDate date];
 	    }
 	    // Reset the query for the next use.
 	    sqlite3_reset(hydrate_statement);
 	    // Update object state with respect to hydration.
 	    hydrated = YES;
 	    */
 	}
 // Flushes all but the primary key and title out to the database.
	protected void dehydrate()
	{
		/*
	    if (dirty) {
	        // Write any changes to the database.
	        // First, if needed, compile the dehydrate query.
	        if (dehydrate_statement == nil) {
	            const char *sql = "UPDATE note SET title=?, content=?, creationDate=?, modificationDate=?, remoteId=?, deleted=? WHERE pk=?";
	            if (sqlite3_prepare_v2(database, sql, -1, &dehydrate_statement, NULL) != SQLITE_OK) {
	                NSAssert1(0, @"Error: failed to prepare statement with message '%s'.", sqlite3_errmsg(database));
	            }
	        }
	        // Bind the query variables.
	        sqlite3_bind_text(dehydrate_statement, 1, [title UTF8String], -1, SQLITE_TRANSIENT);
	        sqlite3_bind_text(dehydrate_statement, 2, [content UTF8String], -1, SQLITE_TRANSIENT);
	        sqlite3_bind_double(dehydrate_statement, 3, [creationDate timeIntervalSince1970]);
			sqlite3_bind_double(dehydrate_statement, 4, [modificationDate timeIntervalSince1970]);
			sqlite3_bind_text(dehydrate_statement, 5, [remoteId UTF8String], -1, SQLITE_TRANSIENT);
			sqlite3_bind_int(dehydrate_statement, 6, deleted);
	        sqlite3_bind_int(dehydrate_statement, 7, primaryKey);
	        // Execute the query.
	        int success = sqlite3_step(dehydrate_statement);
	        // Reset the query for the next use.
	        sqlite3_reset(dehydrate_statement);
	        // Handle errors.
	        if (success != SQLITE_DONE) {
	            NSAssert1(0, @"Error: failed to dehydrate with message '%s'.", sqlite3_errmsg(database));
	        }
	        // Update the object state with respect to unwritten changes.
	        dirty = NO;
	    }
	    // Update the object state with respect to hydration.
	    hydrated = NO;*/
	}
	
	protected void setDeleted(boolean b)
	{
		if(deleted != b)
		{
			deleted = b;
			dirty = true;
		}
	}
 // Remove the book complete from the database. In memory deletion to follow...
	protected void deleteFromDatabase()
	{
		/*fix this later
	    if (delete_statement == nil) {
	        const char *sql = "DELETE FROM note WHERE pk=?";
	        if (sqlite3_prepare_v2(database, sql, -1, &delete_statement, NULL) != SQLITE_OK) {
	            NSAssert1(0, @"Error: failed to prepare statement with message '%s'.", sqlite3_errmsg(database));
	        }
	    }
	    // Bind the primary key variable.
	    sqlite3_bind_int(delete_statement, 1, primaryKey);
	    // Execute the query.
	    int success = sqlite3_step(delete_statement);
	    // Reset the statement for future use.
	    sqlite3_reset(delete_statement);
	    // Handle errors.
	    if (success != SQLITE_DONE) {
	        NSAssert1(0, @"Error: failed to delete from database with message '%s'.", sqlite3_errmsg(database));
	    }
		*/
	}

	public long getPrimaryKey()
	{
		return primaryKey;
	}
	
	public String getTitle()
	{
		return titlePreview;
	}

	public int compareModificationDate(Note note)
	{
		return modificationDate.compareTo(note.getModificationDate());
	}

	public int compareCreationDate(Note note)
	{
		return creationDate.compareTo(note.getCreationDate());
	}

	public int compareAlpha(Note note)
	{
		return content.compareToIgnoreCase(note.getContent());
	}
	
	protected <T> boolean setIfNotEqual(T member, T newValue) // returns true if dirty
	{
	    if ( (member == newValue ) ||
		    	 ((member != null )&& (member.equals(newValue))))
		    return false;
	    else
	    {
	    	member = newValue;
	    	return dirty = true;
	    }
	}
	
	public void setTitle(String aString) 
	{
		setIfNotEqual(title, aString);
	}
	
	public String getContent()
	{
	    return contentPreview;
	}


	public void setContent(String aString)
	{
		String thisLine;
		String [] lines = aString.split("\n");
		setIfNotEqual(content, aString);
		boolean foundTitle, foundContent;
		foundTitle = foundContent = false;
		
		for(int i = 0; i < lines.length; i++)
		{
			thisLine = lines[i];
			if(!foundContent)
			{
				if(thisLine != null)
				{
					if(!foundTitle)
					{
						setTitle(titlePreview = thisLine);
						foundTitle = true;
					}
					else
					{
						contentPreview = thisLine;
						foundContent = true;
					}
				}
			}
			else
				contentPreview += thisLine;
		}
	}
	
	public String getRemoteId()
	{
	    return remoteId;
	}
	
	public void setRemoteId(String aString)
	{
		setIfNotEqual(remoteId, aString);
	}
	
	public String creationDateString(boolean brief) 
	{
		return dateString(creationDate ,brief);
	}
	
	public void setCreationDate(Calendar d)
	{

	    if (setIfNotEqual(creationDate, d))
	    {
			creationDatePreview = dateString(creationDate = d, true);
	    }
	}
	
	public static String dateString(Calendar c, boolean shortFormat)
	{
		int year, month, day;

		String time, date, retVal;
		time = date = "";
		
		Calendar diff = Calendar.getInstance();
		diff.setTimeInMillis(diff.getTimeInMillis() - c.getTimeInMillis());		
				
		year = diff.get(Calendar.YEAR);
		month = diff.get(Calendar.MONTH);
		day = diff.get(Calendar.DAY_OF_MONTH) ;
		
		diff.setTimeInMillis(0); // starting time
		time = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());
		if(	(year == diff.get(Calendar.YEAR)) &&(month == diff.get(Calendar.MONTH))&&(day == diff.get(Calendar.DAY_OF_MONTH)))
		{
			date = "Today";
			if(shortFormat)
				retVal = time;
			else
				retVal = date +", " + time;
		}
		else if((year == diff.get(Calendar.YEAR)) && (month == diff.get(Calendar.MONTH)) && (day == 1))
		{
				date = "Yesterday";
				if(shortFormat)
					retVal = date;
				else
					retVal = date + ", " + time;
		}
		else
		{
			date = new SimpleDateFormat("MMM dd").format(c.getTime());
			retVal = date + ", " + time;
		}

		return retVal;
	}
	
	public String getCreationDatePreview()
	{
		return creationDatePreview;
	}
	
	public Calendar getCreationDate()
	{
		return creationDate;
	}
	
	public String modificationDateString(boolean brief)
	{
		return dateString(modificationDate ,brief);
	}
	
	public void setModificationDate(Calendar d)
	{
	    if (setIfNotEqual(modificationDate, d))
	    {
	    	modificationDatePreview = dateString(modificationDate = d, true);
	    }
	}
	
	public Calendar getModificationDate()
	{
	    return modificationDate;
	}
	
	public String toString()
	{
		String s = isPinned()?"!":"";
		
		for(String tag:tags)
		{
			s += "#" + tag + " "; //space in between tags
		}
		
		if(!tags.isEmpty()) // an extra space after the last tag.
			s += " ";
		return s + content;
	}
	
	public boolean isPinned() 
	{
		return pinned;
	}	
	
	public void setPinEnabled(boolean bPin)
	{
		pinned = bPin;
	}
	public void addPinMarker(){
		setPinEnabled(true);
	}
	
	public void removePinMarker()
	{
		setPinEnabled(false);
	}
	
	
	public void setTags(Vector<String> tagList)
	{
		tags = tagList;
	}

}
