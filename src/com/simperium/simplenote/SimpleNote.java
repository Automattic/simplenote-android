package com.simperium.simplenote;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

/* This is the main class which starts the application. It shows the notes, 
 * or the user can set preferences or add more notes. 
 */


public class SimpleNote extends Activity implements OnClickListener, OnItemClickListener, OnFocusChangeListener, TextWatcher {

	protected NotesDbAdapter mDbHelper;
	protected ImageButton createButton, optionsButton;
	protected static final int ACTIVITY_CREATE = 0;
	protected static final int ACTIVITY_EDIT = 1;
	protected static final int ACTIVITY_OPTIONS = 2;
	protected Vector<Note> notesVector, tempVector;
	protected String email, password, searchString;
	public static String PREFS_NAME = "LoginPrefs";
	protected WebSync wSync;
	protected BaseAdapter notesAdapter;
   	protected EditText search;	
   	protected static SimpleDateFormat formatter;
   	protected Button clearSearch;
   	protected static Typeface tFace;
   	
   	public static Typeface getTypeface()
   	{
   		return tFace;
   	}
   	
   	public static String getDateFormat()
   	{
   		return "yyyy.MM.dd hh:mm:ss a zzz";
   	}
   	
   	public static String format(Calendar c)
   	{
   		return formatter.format(c.getTime());
   	}
   	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	TextView title;
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        notesVector = new Vector<Note>();
        tempVector = new Vector<Note>();
        wSync = new WebSync(this);
    	formatter = new SimpleDateFormat(getDateFormat());

    	tFace = Typeface.createFromAsset(getAssets(), "FreeSans.ttf");
    	title = (TextView)this.findViewById(R.id.Title);
    	title.setTypeface(tFace);
        Preferences.InitInstance(this);
        optionsButton = (ImageButton)this.findViewById(R.id.options);
        createButton = (ImageButton)this.findViewById(R.id.create);
        optionsButton.setOnClickListener(this);
		createButton.setOnClickListener(this);
		clearSearch = (Button)findViewById(R.id.ClearSearch);
		clearSearch.setOnClickListener(this);
		search = (EditText)findViewById(R.id.SearchBar);
		search.setTypeface(tFace);
		search.addTextChangedListener(this);
		search.setOnFocusChangeListener(this);

		
		// create the database helper
		mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
		clearSearch.setVisibility(View.GONE);
      
        // start fixing sync here!!!
        email = Preferences.getEmail();
        password = Preferences.getPassword();
       	wSync.start();
    }
    // event handler for buttons
    public void onClick(View v)
    {
    	// edit the options
    	if(v == optionsButton)
    	{
    		startActivityForResult(new Intent(this, EditOptions.class), ACTIVITY_OPTIONS);
    	}
    	// create a new note
    	else if(v == createButton)
    	{
    		startActivityForResult(new Intent(this, EditNote.class), ACTIVITY_CREATE);
    	}
    	else if(v == clearSearch)
    	{
    		search.setText("");
    	}
    }
    
    protected void editNote(int id)
    {
    	Intent i = new Intent(this, EditNote.class);
    	i.putExtra(NotesDbAdapter.KEY_ROWID, id);
    	startActivityForResult(i, ACTIVITY_EDIT);
	}

    private void fillData() 
    {
        Cursor notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);
        notesVector.clear();
        /*      Check if our result was valid. */ 
        int rowIdColumn = notesCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_ROWID);
        int notesColumn = notesCursor.getColumnIndex(NotesDbAdapter.KEY_NOTE);
        int creationColumn = notesCursor.getColumnIndex(NotesDbAdapter.KEY_CREATION_DATE);
        int modificationColumn = notesCursor.getColumnIndex(NotesDbAdapter.KEY_MODIFICATION_DATE);
        String firstNote, creation, modification;
        Date creationDate, modificationDate;
        Note newNote;
        searchString = "";
        creationDate = modificationDate = null;
        
        int i = 0, rowId; 
        if (notesCursor != null) 
        { 
             /* Check if at least one Result was returned. */ 
             if (notesCursor.moveToFirst()) 
             { 
                  /* Loop through all Results */ 
                  for(i = 1;!notesCursor.isAfterLast(); i++)
                  {
                       /* Retrieve the values of the Entry 
                        * the Cursor is pointing to. */ 
                	  firstNote = notesCursor.getString(notesColumn);
                	  if(firstNote.length() > 0)
                	  {
                		  rowId = rowIdColumn<0?0:notesCursor.getInt(rowIdColumn);
                		  try 
                		  {
                			  creation = notesCursor.getString(creationColumn);
                			  modification = notesCursor.getString(modificationColumn);
                			  creationDate = creationColumn < 0? null : formatter.parse(creation);
                			  modificationDate = modificationColumn < 0? null : formatter.parse(modification);
                		  } catch (ParseException e) 
                		  {
							if(creationDate == null)
								creationDate = new Date();
							if(modificationDate == null)
								modificationDate = new Date();
                		  }
                 		  newNote = new Note(rowId, firstNote , creationDate, modificationDate);
                		  notesVector.add(newNote);
                	  }
                 	  notesCursor.moveToNext();
                  }
             } 
             this.sortNotes(Preferences.getSortOrder());
        }

        ListView v = (ListView)this.findViewById(R.id.NotesList);
        if(v != null)
        {
        //	Typeface tf = Typeface.createFromAsset(this.getAssets(), "FreeSans.ttf");
        	
        	v.setAdapter(notesAdapter = new NotesListAdapter(this, getTypeface(), tempVector));
        	v.setOnItemClickListener(this);
        	v.requestFocus();
        }
    }    
    
    // sort the notes according to sort order
    public void sortNotes(int sortOrder)
    {
    	Comparator<Note> temp = null;
    	String content;
    	tempVector.clear(); // remove notes to display
    	switch(sortOrder)
    	{
    	case 0:
    		temp = new ModificationDateComparator();
    		break;
    	case 1:
    		temp = new CreationDateComparator();
    		break;
    	case 2:
    		temp = new AlphanumericComparator();
    		break;    	
    	}
    	
      	Collections.sort(notesVector, temp);
      	for( Note n : notesVector)
    	{
      		content = n.getContent();
    		//check if it matches the search string
      		if(n.getTitle().toLowerCase().contains(searchString.toLowerCase()))
      			tempVector.add(n);
      		else if((content != null) && content.toLowerCase().contains(searchString.toLowerCase()))
    		{
    			tempVector.add(n);
    		}
    	}
   }
    
    protected void onResume()
    {
    	super.onResume();	
    	fillData();
    }

    protected void onPause()
    {
    	super.onPause();
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    //event handler for clicking on a list item
	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id)
	{
		Intent i = new Intent(this, EditNote.class);
		long noteId = notesVector.get(position).getPrimaryKey();
		i.putExtra(NotesDbAdapter.KEY_ROWID, noteId);
		startActivityForResult(i, ACTIVITY_EDIT);
	}

	// method for listening for typing in search bar
	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub
		
	}

	// method for listening for typing in search bar
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	// method for listening for typing in search bar
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) 
	{
		String content;
		tempVector.clear();
		for(Note n : notesVector)
		{
			content = n.getContent();
			if((n.getTitle().contains(s)) || ((content != null ) && content.contains(s)))
				tempVector.add(n);
		}
		notesAdapter.notifyDataSetChanged();
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) 
	{
		if(hasFocus)
		{
			search.setText("");
			clearSearch.setVisibility(View.VISIBLE);
		}
		else
			clearSearch.setVisibility(View.GONE);
	}
}