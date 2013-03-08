package com.automattic.simplenote;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.automattic.simplenote.models.Note;
import com.simperium.client.Bucket;
import com.simperium.client.LoginActivity;
import com.simperium.client.Simperium;
import com.simperium.client.User;

/**
 * An activity representing a list of Notes. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link NoteEditorActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link NoteListFragment} and the item details (if present) is a
 * {@link NoteEditorFragment}.
 * <p>
 * This activity also implements the required {@link NoteListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class NoteListActivity extends SherlockFragmentActivity implements
		NoteListFragment.Callbacks, OnNavigationListener, User.AuthenticationListener {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;
	private Bucket<Note> mNotesBucket;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);
		
        Simplenote application = (Simplenote)getApplication();
		mNotesBucket = application.getNotesBucket();

		ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ab.setDisplayShowTitleEnabled(false);

		// TODO load tags here 
		String[] items = { "Notes", "Trash", "RADWHIMPS" };
		SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(
				getSupportActionBar().getThemedContext(),
				R.layout.sherlock_spinner_dropdown_item, items);
		ab.setListNavigationCallbacks(mSpinnerAdapter, this);

		int orientation = getResources().getConfiguration().orientation;
		if (findViewById(R.id.note_detail_container) != null && orientation == Configuration.ORIENTATION_LANDSCAPE ) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			((NoteListFragment) getSupportFragmentManager().findFragmentById(
					R.id.note_list)).setActivateOnItemClick(true);
		}

		Simplenote currentApp = (Simplenote) getApplication();
		if( currentApp.getSimperium().getUser() == null || currentApp.getSimperium().getUser().needsAuthentication() ){
			startLoginActivity();
		}
		currentApp.getSimperium().setAuthenticationListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
		
		if (mTwoPane) {
			inflater.inflate(R.menu.notes_list_twopane, menu);
		} else {
			inflater.inflate(R.menu.notes_list, menu);
		}
		
		SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
	    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener( ) {
	        @Override
	        public boolean onQueryTextChange(String newText) {
	            if (newText != null) {
	            	((NoteListFragment) getSupportFragmentManager().findFragmentById(
	    					R.id.note_list)).searchNotes(newText);
	            }
	            return true;
	        }

	        @Override
	        public boolean onQueryTextSubmit(String queryText) {
	        	if (queryText != null) {
	        		((NoteListFragment) getSupportFragmentManager().findFragmentById(
	    					R.id.note_list)).searchNotes(queryText);
	            }
	            return true;
	        }
	    });
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == R.id.menu_preferences) {
			Intent i = new Intent(this, PreferencesActivity.class);
			this.startActivity(i);
		} else if (item.getItemId() == R.id.menu_create_note) {			
			NoteListFragment noteListFragment = ((NoteListFragment) getSupportFragmentManager().findFragmentById(
					R.id.note_list));
			noteListFragment.addNote();
		}
		
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Callback method from {@link NoteListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onNoteSelected(Note note) {
		String noteKey = note == null ? "none" : note.getSimperiumKey();
		
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putString(NoteEditorFragment.ARG_ITEM_ID, noteKey);
			NoteEditorFragment fragment = new NoteEditorFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.note_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, NoteEditorActivity.class);
			detailIntent.putExtra(NoteEditorFragment.ARG_ITEM_ID, noteKey);
			startActivity(detailIntent);
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	public void onAuthenticationStatusChange(User.AuthenticationStatus status){
		if ( status == User.AuthenticationStatus.NOT_AUTHENTICATED ) {
			startLoginActivity();
		}
	}
	
	public void startLoginActivity(){
		Intent loginIntent = new Intent(this, LoginActivity.class);
		startActivityForResult(loginIntent, Simperium.SIGNUP_SIGNIN_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		 if (requestCode == Simperium.SIGNUP_SIGNIN_REQUEST) {
			 if (resultCode == RESULT_CANCELED) {
				 finish();
			 }
		 }
	}
	
	
}
