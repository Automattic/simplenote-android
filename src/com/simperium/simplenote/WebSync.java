package com.simperium.simplenote;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.*;
import org.apache.http.protocol.HTTP;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;

public class WebSync extends Thread
{

	/*eric

	NSMutableData *receivedData;*/
	protected int syncState;
	protected int syncPhase;
	protected Note [] localNotes;
	protected boolean finished;
	protected String authToken;
	Vector<Note> webNotes;
	Context context;
/*	NSEnumerator webEnum;
	NSEnumerator localEnum;	
	NSMutableArray *currentWebNote;
*/	protected Note currentNote;

	protected java.text.DateFormat dateFormatter;
	Vector<Note> notesToDelete;
	// Notifications
	protected static String syncCompleted = "syncCompleted";				// sync completed, no errors
	protected static String syncAccountInfoMissing = "syncAccountInfoMissing";	// sync could not start, missing account info
	protected static String syncLoginFailed = "syncLoginFailed";										// sync login failed, bad login info
	protected static String syncLoginFailedNeedsVerification = "syncLoginFailedNeedsVerification";	// sync login failed, valid login but not verified yet
	protected static String syncAccountCreated = "syncAccountCreated";		// account created, no errors
	protected static String syncAccountCreateFailed = "syncAccountCreateFailed";	// account create failed, user exists or bad email
	protected static String syncCommunicationError = "syncCommunicationError";	// failed, network problem
	protected static String syncInProgressError = "syncInProgressError";		// sync or account create failed, currently busy
	protected static String syncLoginSucceeded = "syncLoginSucceeded";		
	protected static String syncStarted = "syncStarted";

	protected Locale myLocale = null;

	protected static String baseurl = "https://simple-note.appspot.com";
	protected static String loginurl = "/login";
	protected static String createurl = "/create";
	protected static String indexurl = "/api/index";
	protected static String deleteurl = "/api/delete";
	protected static String storeurl = "/api/store";
	protected String email;
	protected String password;
	protected Calendar loginDate;
	
	enum ResponseCodes {
		RESPONSE_OK,
		RESPONSE_LOGIN_FAILED,
		RESPONSE_RETRIEVE_FAILED
	}
	
	public WebSync(Context c)
	{
		super("WebSyncThread");
		
		myLocale = Locale.getDefault();
		finished = false;
		loginDate = null;
		dateFormatter = DateFormat.getDateFormat(context = c);
	}
	
	protected class SyncInProgressException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;
	}
	protected class SyncAccountInfoMissingException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;
	}
	
	public void run()
	{
		
		long sleepTime = 1000; // 1 second
		
		while (!finished)
		{
			if(Preferences.isWebSync())
			{
				if(loginDate == null)
				{
					if(	login() == ResponseCodes.RESPONSE_OK)
					{
						loginDate = Calendar.getInstance();
					}
				}	
				else if(webNotes == null)
				{
					getNotesFromServer();
					sleepTime = 1000*10;
				}
				else if((Calendar.getInstance().getTimeInMillis() - loginDate.getTimeInMillis()) > (1000*60*60*24))
				{
					loginDate = null;
				}
				try
				{
					sleep(sleepTime);
				}
				catch(InterruptedException e)
				{
					
				}
			}
		}
	}
	
	public void finish()
	{
		finished = true;
	}
	
	protected ResponseCodes login()
	{
		Header headers[];
		email = Preferences.getEmail();
		password = Preferences.getPassword();
		
		if((email != null) && (password != null) && (loginDate == null))
		{
			/*
		
			List<BasicNameValuePair> nvps= new ArrayList<BasicNameValuePair>();
  			nvps.add(new BasicNameValuePair("email", email));
  			nvps.add(new BasicNameValuePair("password", password));
*/
		
  			HttpParams params = new BasicHttpParams();
  			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
  			params.setBooleanParameter("http.protocol.expect-continue", false);
  			SSLSocketFactory fact =  SSLSocketFactory.getSocketFactory();
  			fact.setHostnameVerifier(new BrowserCompatHostnameVerifier());
  			SchemeRegistry schemeRegistry = new SchemeRegistry ();
  			schemeRegistry.register (new Scheme ("http", PlainSocketFactory.getSocketFactory (), 80));
  			schemeRegistry.register (new Scheme ("https",fact, 443));

  			ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager( params, schemeRegistry);

  			DefaultHttpClient client = new DefaultHttpClient(cm, params); 
  			HttpPost httppost = new HttpPost(baseurl + loginurl);

  			HttpResponse response;
  			String encoded;
			try 
			{
				encoded = Base64Coder.encodeString("email=" + email + "&password=" + password);
				httppost.setEntity(new StringEntity(encoded));
				response = client.execute(httppost);
				AlertDialog.Builder builder = new AlertDialog.Builder(context);

				if(response.getStatusLine().getStatusCode() == 200)
				{
					headers = response.getHeaders("Set-cookie");
					int start, end;
					String auth="auth=";
					for(Header h : headers)
					{
						encoded = h.getValue();
						if(encoded.contains(auth))
						{
							start=encoded.indexOf(auth) + auth.length();
							end = encoded.indexOf(";", start);
							authToken= encoded.subSequence(start, end).toString();
						}
					}
					encoded = null;
					
					/*
					builder.setTitle("Title")
					.setMessage("The response code is "  + response.getStatusLine().getStatusCode())
			  	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() 
			  	      {
							@Override
							public void onClick(DialogInterface arg0, int arg1)
							{
							
							}
			  	      })
			  	      .setCancelable(false)
			  	      .show();
			  	      */
				}
				else if(response.getStatusLine().getStatusCode() == 403)
				{
					builder.setTitle(R.string.Sort_Order_Dialog_Title)
			  	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() 
			  	      {
							@Override
							public void onClick(DialogInterface arg0, int arg1)
							{
							
							}
			  	      })
			  	      .setCancelable(false)
			  	      .show();
					return ResponseCodes.RESPONSE_LOGIN_FAILED;
				}
			}
			catch (Throwable t)
			{
				
			}		
		}
		return ResponseCodes.RESPONSE_OK;
	}
	
	
	protected ResponseCodes getNotesFromServer()
	{

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		params.setBooleanParameter("http.protocol.expect-continue", false);
		SSLSocketFactory fact =  SSLSocketFactory.getSocketFactory();
		fact.setHostnameVerifier(new BrowserCompatHostnameVerifier());
		SchemeRegistry schemeRegistry = new SchemeRegistry ();
		schemeRegistry.register (new Scheme ("http", PlainSocketFactory.getSocketFactory (), 80));
		schemeRegistry.register (new Scheme ("https",fact, 443));

		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager( params, schemeRegistry);

		DefaultHttpClient client = new DefaultHttpClient(cm, params); 

		HttpResponse response;
		HttpEntity responseEntity;
		String encoded;
		
		try 
		{
			encoded = baseurl+indexurl + "?auth=" + authToken + "&email=" + Preferences.getEmail();
			HttpGet httpGet = new HttpGet(encoded);
//				httpGet.setEntity(new StringEntity(encoded));
			response = client.execute(httpGet);
			responseEntity = response.getEntity();
			BufferedReader in = new BufferedReader(new InputStreamReader(responseEntity.getContent()));
			String line;
			StringBuffer sb = new StringBuffer("");
			while((line = in.readLine()) != null)
			{
				sb.append(line);
			}
			JSONArray jArray = new JSONArray(line = sb.toString());
			JSONObject jObject;
			String modifyString,keyString;
			boolean deleted;
			
			for(int i = 0; i < jArray.length(); i++)
			{
				jObject = jArray.getJSONObject(i);
				modifyString = jObject.getString("modify");
				keyString = jObject.getString("key");
				deleted = jObject.getBoolean("deleted");
				
				if(!deleted)
				{
					
				}
			}
			
			line.length();
		}
		catch (Throwable t)
		{
			return ResponseCodes.RESPONSE_RETRIEVE_FAILED;
		}
		return ResponseCodes.RESPONSE_OK;
	}
	
	public void sync()
	{	
		if (syncState > 0)
		{
			throw new SyncInProgressException();
			//NSLog(@"sync in progress cannot continue - syncState=%d", syncState);
		}

		if (email == null || email.length() < 1 || password == null || password.length() < 1)
		{
			throw new SyncAccountInfoMissingException();
		}
//		localEnum = nil;
//		webEnum = nil;
		notesToDelete = new Vector<Note>();
//		localNotes = SimpleNote.getLocalNotes();
		
		// begin sync
//		[[NSNotificationCenter defaultCenter] postNotificationName:syncStarted object:self];	
		syncState = 1;
		syncPhase = 0;


	  	List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
/*	 
		nvps.add(new BasicNameValuePair("api", "1"));
		nvps.add(new BasicNameValuePair("locale", "en-US"));
*/			
		nvps.add(new BasicNameValuePair("email", email));
		nvps.add(new BasicNameValuePair("password", password));
		

		doRequestWithURL(baseurl + loginurl, "email=" + email + "&password=" + password );

	}
	
	public void createAccount(String email, String password)
	{
		
		if (syncState > 0)
		{
			throw new SyncInProgressException();
		}
//eric		String email = (NSString *)CFPreferencesCopyAppValue(CFSTR("email"), kCFPreferencesCurrentApplication);
//eric		String password = (NSString *)CFPreferencesCopyAppValue(CFSTR("password"), kCFPreferencesCurrentApplication);
		
		if (email == null || email.length() < 1 || password == null || password.length() < 1)
		{
			throw new SyncAccountInfoMissingException();
		}	
	
		// begin sync
		syncState = 8;
		syncPhase = 0;
//		String createdata = "api=1&email=" + email +  "&password="+ password;
		List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
//		nvps.add(new BasicNameValuePair("api", "1"));
		nvps.add(new BasicNameValuePair("email", email));
		nvps.add(new BasicNameValuePair("password", password));
			doRequestWithURL(baseurl + createurl, nvps.toString());
	}
	
	public void doRequestWithURL(String url)
	{
		doRequestWithURL(url, null);
	}
	
	public void doRequestWithURL(String url, String data )
	{
	

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		params.setBooleanParameter("http.protocol.expect-continue", false);
		SSLSocketFactory fact =  SSLSocketFactory.getSocketFactory();
		fact.setHostnameVerifier(new BrowserCompatHostnameVerifier());
		SchemeRegistry schemeRegistry = new SchemeRegistry ();
		schemeRegistry.register (new Scheme ("http", PlainSocketFactory.getSocketFactory (), 80));
		schemeRegistry.register (new Scheme ("https",fact, 443));

		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager( params, schemeRegistry);

		DefaultHttpClient client = new DefaultHttpClient(cm, params); 
		HttpPost httppost = new HttpPost(url);

		HttpResponse response;
		HttpEntity responseEntity;
		String encoded;
		
		if (data != null)
		{
			try 
			{
				encoded = Base64Coder.encodeString(data);
				httppost.setEntity(new StringEntity(encoded));
				response = client.execute(httppost);
				responseEntity = response.getEntity();


				Header headers[] = response.getHeaders("Set-cookie");
				int start, end;
				String auth="auth=";
				for(Header h : headers)
				{
					encoded = h.getValue();
					if(encoded.contains(auth))
					{
						start=encoded.indexOf(auth) + auth.length();
						end = encoded.indexOf(";", start);
						authToken= encoded.subSequence(start, end).toString();
						auth.length();
					}
					encoded.length();
				}
				encoded = null;
				
				
				encoded = baseurl+indexurl + "?auth=" + authToken + "&email=" + Preferences.getEmail();
				HttpGet httpGet = new HttpGet(encoded);
//				httpGet.setEntity(new StringEntity(encoded));
				response = client.execute(httpGet);
				responseEntity = response.getEntity();
				BufferedReader in = new BufferedReader(new InputStreamReader(responseEntity.getContent()));
				String line;
				StringBuffer sb = new StringBuffer("");
				while((line = in.readLine()) != null)
				{
					sb.append(line);
				}
				encoded = null;
			}
			catch (Throwable t)
			{
				syncState = -3;
			}
		}
	}
	/*
	public void connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
	{
		//NSLog(@"connection didReceiveResponse");
		[receivedData setLength:0];
		NSHTTPURLResponse *httpresponse = (NSHTTPURLResponse *)response;
		//NSLog(@"responsecode = %d", httpresponse.statusCode);
		if (httpresponse.statusCode != 200)
		{
			switch (syncState)
			{
				case 1:
					if (httpresponse.statusCode == 403)
						[[NSNotificationCenter defaultCenter] postNotificationName:syncLoginFailedNeedsVerification object:self];
					else
						[[NSNotificationCenter defaultCenter] postNotificationName:syncLoginFailed object:self];
					break;
				case 8:
					[[NSNotificationCenter defaultCenter] postNotificationName:syncAccountCreateFailed object:self];
					break;
				default:
					[[NSNotificationCenter defaultCenter] postNotificationName:syncCommunicationError object:self];
					break;
			}
			NSLog(@"service returned an error cannot continue");
			syncState = -1;
		}
		if (syncState == 7)
		{
			NSHTTPURLResponse *http = (NSHTTPURLResponse *)response;
			NSDictionary *headers = [http allHeaderFields];
			[currentWebNote release];
			currentWebNote = [[NSMutableArray alloc] init];
			NSString *note_key = nil;
			NSDate *note_create = nil;
			NSDate *note_modify = nil;
			NSString *note_deleted = nil;
			for (NSString *key in headers)
			{
				NSString *value = [headers objectForKey:key];
				NSLog(@"key: %@ value:%@", key, value);
				key = [key lowercaseString];
				if ([key hasPrefix:@"note-key"])
				{
					note_key = value;
				}
				else if ([key hasPrefix:@"note-createdate"])
				{
					note_create = [dateFormatter dateFromString:value];
				}
				else if ([key hasPrefix:@"note-modifydate"])
				{
					note_modify = [dateFormatter dateFromString:value];
				}
				else if ([key hasPrefix:@"note-deleted"])
				{
					if ([[value lowercaseString] hasPrefix:@"true"])
						note_deleted = [NSString stringWithString:@"t"];
					else
						note_deleted = [NSString stringWithString:@"f"];
				}
			}
			if (note_key != nil && note_modify != nil && note_deleted != nil && note_create != nil)
			{			
				[currentWebNote addObject:note_key];
				[currentWebNote addObject:note_modify];
				[currentWebNote addObject:note_deleted];
				[currentWebNote addObject:note_create];
			}
			else
			{
				[currentWebNote release];
				currentWebNote = nil;
			}
		}
		return;
	}
	
	public void connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
	{
		//NSLog(@"connection didReceiveData");
		[receivedData appendData:data];
		return;
	}
	public void connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
	{
		//NSLog(@"connection didFailWithError");
		// release the connection, and the data object
	    [connection release];
	    [receivedData release];	
		syncState = -4;	// no connection
	    // inform the user
		[[NSNotificationCenter defaultCenter] postNotificationName:syncCommunicationError object:self];
	    NSLog(@"Connection failed! Error - %@ %@",
		  [error localizedDescription],
		  [[error userInfo] objectForKey:NSErrorFailingURLStringKey]);
		return;
	}
	
	public void connectionDidFinishLoading:(NSURLConnection *)connection
	{
		//NSLog(@"connectionDidFinishLoading");
		//NSLog(@"Succeeded! Received %d bytes of data", [receivedData length]);
		NSString *data = [[NSString alloc] initWithBytes:[receivedData mutableBytes] length:[receivedData length] encoding:NSUTF8StringEncoding];
		//NSLog(@"Data =\n%@\n.\n", data);
		[connection release];
		[receivedData release];
		
		switch (syncState)
		{
			case 1:
				//logged in now get index
				syncState = 2;
				
				[[NSNotificationCenter defaultCenter] postNotificationName:syncLoginSucceeded object:self];			
				[self doRequestWithURL:[baseurl stringByAppendingString:indexurl]];
				break;
			case 2:
				syncState = 3;
				[webNotes release];
				webNotes = [[NSMutableArray alloc] init];
				NSString *trimmed = [data stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"[]"]];
				NSArray *items = [trimmed componentsSeparatedByString:@"},"];
				for (NSString *item in items)
				{
					NSString *item_deleted = nil;
					NSDate *item_modify = nil;
					NSString *item_key = nil;
					
					NSString *s = [item stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"{} "]];
	//				NSLog(@"item: <%@>", s);
					NSArray *args = [s componentsSeparatedByString:@","];
					for (NSString *arg in args)
					{
						NSString *key = nil;
						NSString *value = nil;
						
						NSArray *kv = [arg componentsSeparatedByString:@": "];
						for (NSString *k in kv)
						{
							NSString *val = [k stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"\" "]];
							if (key == nil) 
								key = val;
							else 
								value = val;
						}
						if ([key hasPrefix:@"deleted"])
						{
							if ([value hasPrefix:@"true"])
								item_deleted = [NSString stringWithString:@"t"];
							else
								item_deleted = [NSString stringWithString:@"f"];
						}
						else if ([key hasPrefix:@"key"])
						{
							item_key = [NSString stringWithString:value];
						}
						else if ([key hasPrefix:@"modify"])
						{
							item_modify = [dateFormatter dateFromString:value];
						}
					}
	//				NSLog(@"key: %@ date: %@ deleted: %@", item_key, item_modify, item_deleted);
					if (item_key != nil && item_modify != nil && item_deleted != nil)
					{
						NSArray *values = [NSArray arrayWithObjects:item_key, item_modify, item_deleted, nil];
						[webNotes addObject:values];
					}
				}
				[webEnum release];
				webEnum = [webNotes objectEnumerator];
				[webEnum retain];
				[webNotes retain];
				syncState = 4;
				[self syncWebNotesToLocal];
				break;
			case 4:
				// do stuff
				[self syncWebNotesToLocal];
				break;
				
			case 5: // DELETE
				// finished deleting note on web, delete same locally
				//NSLog(@"finished deleting something...");
				if (currentNote != nil)
				{
					NSLog(@"there is a current note, adding to notesToDelete");
					currentNote.remoteId = @"";
					currentNote.deleted = YES;
					if ([notesToDelete indexOfObject:currentNote] == NSNotFound)
						[notesToDelete addObject:currentNote];
				}
				if (syncPhase == 0)
					[self syncWebNotesToLocal];
				else
					[self syncLocalNotesToWeb];
				break;
			case 6: // UPDATE remote with local (store)
				if (syncPhase == 0)
					[self syncWebNotesToLocal];
				else
				{
					//NSLog(@"created new note got key: %@", data);
					currentNote.remoteId = data;
					//[currentNote dehydrate];
					[self syncLocalNotesToWeb];
				}
				break;
			case 7: // UPDATE local with remote (retrieve)
				if (currentWebNote != nil)
				{
					NSString *note_key = [currentWebNote objectAtIndex:0];
					NSDate *note_modify = [currentWebNote objectAtIndex:1];
					NSString *note_deleted = [currentWebNote objectAtIndex:2];
					NSDate *note_create = [currentWebNote objectAtIndex:3];
					Note *found = nil;
					for (Note *note in localNotes)
					{
	//					NSLog(@"comparing local key: %@ web key: %@", note.key, note_key);
						if ([note_key isEqualToString:note.remoteId])
						{
							found = note;
						}
					}
					if (found == nil)
					{
						found = [appDelegate addNoteWithContent:@"" creationDate:note_create
											   modificationDate:note_modify remoteId:note_key];
						//NSLog(@"added new note key %@ content %@", found.remoteId, found.content);
					}
					//NSLog(@"saving updated note");
					
					found.remoteId = note_key;
					found.creationDate = note_create;
					found.modificationDate = note_modify;
					if (note_deleted == @"t")
						found.deleted = YES;
					else
						found.deleted = NO;
					NSData *b64 = [NSData dataWithBase64EncodedString:data];
					NSString *s = [[NSString alloc] initWithData:b64 encoding:NSUTF8StringEncoding];
					found.content = s;
					[found dehydrate];
					//NSLog(@"finished retrieve");
					[self syncWebNotesToLocal];
				}
				break;
			case 8: // CREATE account
				// account created and verification should be sent
				syncState = 0;
				[[NSNotificationCenter defaultCenter] postNotificationName:syncAccountCreated object:self];
				break;
			default:
				//NSLog(@"connectionfinished: unhandled syncstate %d", syncState);
				syncState = -2;
				break;
		}
		[data release];
		return;
	}
	public void syncLocalNotesToWeb()
	{
		NSString *key = nil;
		NSDate *modify = nil;
		BOOL deleted = NO;
		//NSLog(@"syncLocalNotesToWeb");
		
		while ((currentNote = [localEnum nextObject]))
		{
	
			BOOL found = NO;
			if (currentNote.deleted == YES)
			{
				//NSLog(@"currentNote is deleted adding to notesToDelete");
				if ([notesToDelete indexOfObject:currentNote] == NSNotFound)
					[notesToDelete addObject:currentNote];
				continue;
			}
			for (currentWebNote in webNotes)
			{
				key = [currentWebNote objectAtIndex:0];
				modify = [currentWebNote objectAtIndex:1];
				NSString *d = [currentWebNote objectAtIndex:2];
				deleted = NO;
				if ([d hasPrefix:@"t"])
					deleted = YES;
				
				if ([key isEqualToString:currentNote.remoteId])
					found = YES;
			}
			if (!found)
			{
				//NSLog(@"post- not found: %@", currentNote.remoteId);
				syncState = 6;
				NSString *params = [NSString stringWithFormat:@"?modify=%@&create=%@",
									[dateFormatter stringFromDate:currentNote.modificationDate],
									[dateFormatter stringFromDate:currentNote.creationDate]];
				NSString *url = [NSString stringWithFormat:@"%@%@%@", baseurl, storeurl,
								 [params stringByAddingPercentEscapesUsingEncoding:NSASCIIStringEncoding]];
				NSData *data = [currentNote.content dataUsingEncoding:NSUTF8StringEncoding];
				NSData *encoded = [[data base64Encoding] dataUsingEncoding:NSUTF8StringEncoding];
				[self doRequestWithURL:url postData:encoded];
				return;
			}
		}
	
		//NSLog(@"deleting notes in notesToDelete");
		for (Note *note in notesToDelete)
		{
			//NSLog(@"deleting note key: %@", note);
			[appDelegate removeNote:note deleteNote:YES];
		}
		//NSLog(@"finished syncing!");
		syncState = 0;
		[[NSNotificationCenter defaultCenter] postNotificationName:syncCompleted object:self];
	}
	
	public void syncWebNotesToLocal()
	{
		NSString *key = nil;
		NSDate *modify = nil;
		BOOL deleted = NO;
		currentNote = nil;
		currentWebNote = [webEnum nextObject];
		if (currentWebNote == nil)
		{
			syncPhase = 1;
			localEnum = [localNotes objectEnumerator];
			[localEnum retain];
			// do something else
			//NSLog(@"syncWebNotesToLocal finished");
			[self syncLocalNotesToWeb];
			return;
		}
		key = [currentWebNote objectAtIndex:0];
		modify = [currentWebNote objectAtIndex:1];
		NSString *d = [currentWebNote objectAtIndex:2];
		deleted = NO;
		if ([d hasPrefix:@"t"])
			deleted = YES;
		bool found = false;
		for (Note *note in localNotes)
		{
			if ([note.remoteId isEqualToString:key])
			{
				currentNote = note;
				found = true;
			}
		}
		if (found)
		{
	//		NSLog(@"compareNote: note found!");
			if ([modify compare:currentNote.modificationDate] == NSOrderedDescending &&
				fabs([modify timeIntervalSinceDate:currentNote.modificationDate]) > 1)
			{
				//NSLog(@"compareNote: web version is more recent");
				// web is more recent
				if (deleted)
				{
					//NSLog(@"compareNote: web version is deleted, deleting note");
					syncState = 5;
					[self doRequestWithURL:[NSString stringWithFormat:@"%@%@?dead=1&key=%@", baseurl, deleteurl, key]];
				}
				else
				{
					//NSLog(@"compareNote: web version not deleted, retrieving from web");
					syncState = 7;
					[self doRequestWithURL:[NSString stringWithFormat:@"%@%@?key=%@&encode=base64", baseurl, storeurl, key]];
				}
			}
			else if ([modify compare:currentNote.modificationDate] == NSOrderedAscending &&
					 fabs([modify timeIntervalSinceDate:currentNote.modificationDate]) > 1)
			{
				//NSLog(@"compareNote: local version is more recent local:%@ web:%@", currentNote.modificationDate, modify);
				if (currentNote.deleted)
				{
					//NSLog(@"compareNote: local version is deleted, deleting note");
					syncState = 5;
					[self doRequestWithURL:[NSString stringWithFormat:@"%@%@?dead=1&key=%@", baseurl, deleteurl, key]];
				}
				else
				{
					//NSLog(@"compareNote: local version not deleted, storing to web");
					syncState = 6;
					NSString *params = [NSString stringWithFormat:@"?key=%@&modify=%@&create=%@", key,
										[dateFormatter stringFromDate:currentNote.modificationDate],
										[dateFormatter stringFromDate:currentNote.creationDate]];
					NSString *url = [NSString stringWithFormat:@"%@%@%@", baseurl, storeurl,
									 [params stringByAddingPercentEscapesUsingEncoding:NSASCIIStringEncoding]];
					NSData *data = [currentNote.content dataUsingEncoding:NSUTF8StringEncoding];
					NSData *encoded = [[data base64Encoding] dataUsingEncoding:NSUTF8StringEncoding];
					[self doRequestWithURL:url postData:encoded];
				}
			}
			else
			{
	//			NSLog(@"compareNote: modify date is equal!");
				[self syncWebNotesToLocal];
			}
		}
		else
		{
			//NSLog(@"compareNote: note not found!");
			if (deleted)
			{
				//NSLog(@"compareNote: web version is deleted, deleting note");
				syncState = 5;
				[self doRequestWithURL:[NSString stringWithFormat:@"%@%@?dead=1&key=%@", baseurl, deleteurl, key]];
			}
			else
			{
				//NSLog(@"compareNote: web version is new note, retrieving from web");
				syncState = 7;
				[self doRequestWithURL:[NSString stringWithFormat:@"%@%@?key=%@&encode=base64", baseurl, storeurl, key]];
			}
		}
	}
	
	*/
}