package com.automattic.simplenote;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.simperium.client.*;
import com.simperium.client.storage.MemoryStore;


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
public class NoteListActivity extends SherlockFragmentActivity implements NoteListFragment.Callbacks, OnNavigationListener, User.AuthenticationListener{

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;
	
    private Simperium simperium;
    static private final String SIMPERIUM_APP_ID  = "XXXX";
    static private final String SIMPERIUM_API_KEY = "ROBERTO!!!";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);

		ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ab.setDisplayShowTitleEnabled(false);

		String[] items = { "Notes", "Trash", "RADWHIMPS" };
		SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(),
				R.layout.sherlock_spinner_dropdown_item, items);
		ab.setListNavigationCallbacks(mSpinnerAdapter, this);

		if (findViewById(R.id.note_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			((NoteListFragment) getSupportFragmentManager().findFragmentById(R.id.note_list)).setActivateOnItemClick(true);
		}

		// TODO: If exposing deep links into your app, handle intents here.
		
        simperium = new Simperium(
                SIMPERIUM_APP_ID,
                SIMPERIUM_API_KEY,
                getApplicationContext(),
                new MemoryStore(),
                this
            );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.notes_list, menu);
		return true;
	}

    public void onAuthenticationStatusChange(User.AuthenticationStatus status){
        if ( status == User.AuthenticationStatus.NOT_AUTHENTICATED ) {
            startLoginActivity();
        }
    }
	
    public void startLoginActivity(){
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(
            Intent.FLAG_ACTIVITY_SINGLE_TOP |
            Intent.FLAG_ACTIVITY_NEW_TASK
        );
    
        startActivity(loginIntent);
    }
    
	/**
	 * Callback method from {@link NoteListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(String id) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putString(NoteEditorFragment.ARG_ITEM_ID, id);
			NoteEditorFragment fragment = new NoteEditorFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().replace(R.id.note_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, NoteEditorActivity.class);
			detailIntent.putExtra(NoteEditorFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// TODO Auto-generated method stub
		return false;
	}
}
