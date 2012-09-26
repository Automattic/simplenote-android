package com.simperium.simplenote;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/* The activity for setting the login name / password for an existing account
 */

public class LoginActivity extends Activity implements View.OnClickListener 
{
	protected String email, password; 
	protected EditText emailView; // pointer to the email text window
	protected EditText passwordView; // pointer to the password text window
	protected Button acceptButton, cancelButton; // pointers to buttons

	   /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // no title
        setContentView(R.layout.login_view);
    }


    protected void onResume()
    {
    	// set the pointers
    	super.onResume();
    	Typeface tf = SimpleNote.getTypeface();
    	emailView = (EditText)this.findViewById(R.id.LoginEmailField);
    	passwordView = (EditText)this.findViewById(R.id.LoginPasswordField);
    	acceptButton = 			(Button)findViewById(R.id.LoginAccept);
    	cancelButton = 			(Button)findViewById(R.id.LoginCancel);
    	acceptButton.setOnClickListener(this);
    	cancelButton.setOnClickListener(this);
    	emailView.setTypeface(tf);
    	passwordView.setTypeface(tf);
    	acceptButton.setTypeface(tf);
    	cancelButton.setTypeface(tf);
    	((TextView)findViewById(R.id.LoginActivityTitle)).setTypeface(tf);
    	
        email = Preferences.getEmail();
        emailView.setText(email);
        emailView.requestFocus();
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.showSoftInput(emailView, InputMethodManager.SHOW_IMPLICIT);

        emailView.selectAll(); // select all so that when you start typing, it replaces text
        passwordView.setText(password = Preferences.getPassword());
    }

    protected void onPause()
    {
    	super.onPause();
    }
    
    protected boolean validateFields()
    {
    	boolean validEmail, validLength;
   
    	email = emailView.getText().toString();
    	password = passwordView.getText().toString();
    	
    	// regular expression to test for email address
    	validEmail = email.matches("(\\w+)@(\\w+\\.)(\\w+)(\\.\\w+)*");
    	validLength = password.length() >= 4; // password has at least 4 characters
    	
    	if(!validEmail)
    	{
    		// show alert dialog
    		new AlertDialog.Builder(this)
    	      .setMessage(getString(R.string.InvalidEmailAddress))
    	      .setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() 
    	      {
				@Override
				public void onClick(DialogInterface arg0, int arg1){}
    	      })
    	      .setCancelable(false)
    	      .show();
    	}
    	else if(!validLength)
    	{
    		new AlertDialog.Builder(this)
    	      .setMessage(getString(R.string.PasswordLength))
    	      .setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() 
    	      {
  				@Override
  				public void onClick(DialogInterface arg0, int arg1)
  				{
  					passwordView.setText("");
  				}
    	      })
    	      .setCancelable(false)
    	      .show();
    	}
    	return (validEmail && validLength);
    }
    
    public void onClick(View v) 
	{
		if(v == acceptButton)
		{
			if(validateFields())
			{
				// store new email and password in preferences
				Preferences.setEmail(email);
				Preferences.setPassword(password);
				setResult(RESULT_OK);
				finish();
			}	
		}
		else if(v == cancelButton)
		{
			setResult(RESULT_CANCELED);
			finish();
		}
	}
}
