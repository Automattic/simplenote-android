package com.simperium.simplenote;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/*
 * This class represents the screen for creating an account.
 * Since this is a subclass of Activity, it follows the lifecycle of Activity. 
 * onCreate first gets called, then onResume.
 * 
 */

public class CreateAccountActivity extends Activity implements View.OnClickListener 
{
	protected EditText confirmPasswordView; //a pointer to the textEdit window
	protected EditText createPasswordView; //a pointer to the textEdit window
	protected EditText createEmailView; //a pointer to the textEdit window
	protected String email, password, confirmPassword; // strings to hold the strings in the edit text windows
	protected Button acceptButton, cancelButton; // pointers to buttons, used to see which one was clicked
	
	   /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // have no title
        setContentView(R.layout.create_account_view);
    }
    
    protected void onResume()
    {
    	super.onResume();
        Typeface tf = SimpleNote.getTypeface();
    	TextView tv = (TextView)this.findViewById(R.id.TextView02);
        tv.setTypeface(tf);
   	
    	createPasswordView = 	(EditText)findViewById(R.id.CreateConfirmField);
    	confirmPasswordView = 	(EditText)findViewById(R.id.CreateConfirmField);
    	createEmailView = 		(EditText)findViewById(R.id.CreateEmailField);
    	acceptButton = 			(Button)findViewById(R.id.CreateAccept);
    	cancelButton = 			(Button)findViewById(R.id.CreateCancel);
    	acceptButton.setOnClickListener(this); // the onClick function from this class gets called
    	cancelButton.setOnClickListener(this);
    	
    	createEmailView.setTypeface(tf);
    	createEmailView.requestFocus();
    	createEmailView.setText("");
    	tv = (TextView)findViewById(R.id.CreateEmailLabel);
    	tv.setTypeface(tf);
    	acceptButton.setTypeface(tf);
    	tv = (TextView)findViewById(R.id.CreatePasswordField);
    	tv.setTypeface(tf);
    	cancelButton.setTypeface(tf);
    	tv = (TextView)findViewById(R.id.CreateConfirmLabel);
    	tv.setTypeface(tf);
    	
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(createEmailView, InputMethodManager.SHOW_FORCED);
        //imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        
    }

    protected void onPause()
    {
    	super.onPause();
    }
    
    protected boolean validateFields()
    {
    	boolean validEmail, validPassword, validLength;
   
    	email = createEmailView.getText().toString();
    	password = createPasswordView.getText().toString();
    	confirmPassword = confirmPasswordView.getText().toString();
    	
    	validEmail = email.matches("(\\w+)@(\\w+\\.)(\\w+)(\\.\\w+)*"); //regular expression for email address format
    	validPassword = (password.compareTo(confirmPassword) == 0); // check if the two passwords match
    	validLength = password.length() >= 4; // check if the password length is 4 or more characters
    	
    	
    	// this pops up a dialog explaining why validation has failed.
    	if(!validEmail)
    	{
    		new AlertDialog.Builder(this)
    	      .setMessage("You have entered an invalid email address")
    	      .setPositiveButton("Ok", new DialogInterface.OnClickListener() 
    	      {
				@Override
				public void onClick(DialogInterface arg0, int arg1){}
    	      })
    	      .setCancelable(false)
    	      .show();
    	}
    	else if(!validPassword)
    	{
    		new AlertDialog.Builder(this)
  	      .setMessage("Your passwords do not match")
  	      .setPositiveButton("Ok", new DialogInterface.OnClickListener() 
  	      {
				@Override
				public void onClick(DialogInterface arg0, int arg1)
				{
					createPasswordView.setText("");
					confirmPasswordView.setText("");
				}
  	      })
  	      .setCancelable(false)
  	      .show();
    	}
    	else if(!validLength)
    	{
    		new AlertDialog.Builder(this)
    	      .setMessage("Your password must contain at least 4 characters")
    	      .setPositiveButton("Ok", new DialogInterface.OnClickListener() 
    	      {
  				@Override
  				public void onClick(DialogInterface arg0, int arg1)
  				{
  					createPasswordView.setText("");
  					confirmPasswordView.setText("");
  				}
    	      })
    	      .setCancelable(false)
    	      .show();
    	}
    	
    	// return if the email is valid
    	return (validEmail && validPassword && validLength);
    }

    // the click handler for this activity
    public void onClick(View v) 
	{
    	
    	// user has clicked accept
		if(v == acceptButton)
		{
			if(validateFields())
			{
				Preferences.setEmail(email);
				Preferences.setPassword(password);
				setResult(RESULT_OK);
				finish();
			}
		}
		//user has clicked cancel
		else if(v == cancelButton)
		{
			setResult(RESULT_OK);
			finish();
		}
	}
}
