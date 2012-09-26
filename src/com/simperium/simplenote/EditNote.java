package com.simperium.simplenote;


import java.util.*;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import java.text.*;

import android.view.*;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/* This activity represents the screen for editing a note.
 */

public class EditNote extends Activity  implements OnClickListener
{
    private EditText mBodyText; // a pointer to the edit window
    private Long mRowId; // the row id of the note being edited
    private NotesDbAdapter mDbHelper; // a pointer to the database
    private ImageButton deleteButton; // a pointer to the delete note button
    private Button acceptButton; // a pointer to the accept button
    protected ImageButton newButton;
    protected TextView dateStamp, editTitle; // pointers to the time stamp and title
    protected Note noteBeingEdited; // the note being edited
    protected SimpleDateFormat formatter; // for formatting the date
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	Typeface tf = SimpleNote.getTypeface();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the automatic title
        setContentView(R.layout.note_edit);
        mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        
        formatter = new SimpleDateFormat(SimpleNote.getDateFormat());
        mBodyText = (EditText) findViewById(R.id.body);
        mRowId = savedInstanceState != null ? savedInstanceState.getLong(NotesDbAdapter.KEY_ROWID) 
                							: null;
		if (mRowId == null) 
		{
			Bundle extras = getIntent().getExtras();            
			mRowId = extras != null ? extras.getLong(NotesDbAdapter.KEY_ROWID) : null;
		
		}

		deleteButton = (ImageButton)this.findViewById(R.id.EditNoteDelete);
        deleteButton.setOnClickListener(this);
        
        newButton = (ImageButton)findViewById(R.id.EditNoteNew);
        newButton.setOnClickListener(this);
        
        acceptButton = (Button)this.findViewById(R.id.EditNoteAccept);
        acceptButton.setOnClickListener(this);
        acceptButton.setTypeface(tf);
        
        dateStamp = (TextView)findViewById(R.id.TimeStamp);
        dateStamp.setTypeface(tf);
        editTitle = (TextView)findViewById(R.id.EditTitle);
        editTitle.setTypeface(tf);
   }
    
    // event handler for clicking on the delete or accept buttons
    public void onClick(View v)
    {
    	boolean finished = false;
    	if(v == deleteButton)
    	{
    		finished = true;
    		if(mRowId != null)
    			mDbHelper.deleteNote(mRowId);
     	}
    	else if(v == acceptButton)
    	{
    		finished = true;
    		saveState();
    	}
    	else if(v == newButton)
    	{
    		saveState();
    		mBodyText.setText("");
    		dateStamp.setText(R.string.Name);
    		editTitle.setText(R.string.Name);
    		mRowId = null;
    	}
    	if(finished)
    	{
        	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        	imm.hideSoftInputFromWindow(mBodyText.getWindowToken(), 0);
       	    setResult(RESULT_OK);
       	    finish();
    	}
    }

    private void populateFields() 
    {
    	String content, creation, modification;
    	Date creationDate, modificationDate;
    	creationDate = modificationDate = null;
    	
        if (mRowId != null) 
        {
        	Cursor note = mDbHelper.fetchNote(mRowId);
            startManagingCursor(note);
            content = note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_NOTE));
            creation = note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_CREATION_DATE));
            modification = note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_MODIFICATION_DATE));

            
            try 
            {
            	creationDate = formatter.parse(creation);
            	modificationDate = formatter.parse(modification);
            } catch (ParseException e) 
            {
            	if(creationDate == null)
            		creationDate = new Date();
            	if(modificationDate == null)
            		modificationDate = new Date();
            }
                        
            noteBeingEdited = new Note(mRowId, content, creationDate, modificationDate);
            content = noteBeingEdited.getContent();
            mBodyText.setText(noteBeingEdited.getTitle() +"\n"+ (content==null?"":content));
            if(dateStamp != null)
            	dateStamp.setText(noteBeingEdited.modificationDateString(false));
            if(editTitle != null)
            	editTitle.setText(noteBeingEdited.getTitle());
        }
        else
        {
        	dateStamp.setText(Note.dateString(Calendar.getInstance(), false));
        }
       
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(NotesDbAdapter.KEY_ROWID, mRowId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }
    
    // saves the note in the database
    private void saveState() 
    {
        String body = mBodyText.getText().toString();
        String date;
        if(body.length() > 0)
        {
        	
        	// if creating a new note
        	if (mRowId == null)
        	{
        		date = formatter.format(new Date());
        		long id = mDbHelper.createNote( body, date, date);
        		if (id > 0) 
        		{
        			mRowId = id;
        		}
        	} 
        	else //updating a note
        	{
        		noteBeingEdited.setModificationDate(Calendar.getInstance());
        		mDbHelper.updateNote(mRowId, body, formatter.format(noteBeingEdited.getCreationDate().getTime()), formatter.format(noteBeingEdited.getModificationDate().getTime()));
        	}
        }
        else
        { // if the note has no text, delete it
        	if (mRowId != null)
        		mDbHelper.deleteNote(mRowId);
        }
    }
}
