package com.automattic.simplenote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.UndoBarController;
import com.simperium.client.Bucket;
import com.simperium.client.LoginActivity;
import com.simperium.client.Simperium;
import com.simperium.client.User;

import java.util.Calendar;

/**
 * An activity representing a list of Notes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link NoteListFragment} and the item details (if present) is a
 * {@link NoteEditorFragment}.
 * <p>
 * This activity also implements the required {@link NoteListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class NotesActivity extends Activity implements
		NoteListFragment.Callbacks, ActionBar.OnNavigationListener, User.AuthenticationListener, UndoBarController.UndoListener, FragmentManager.OnBackStackChangedListener {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;
    private UndoBarController mUndoBarController;
    private SearchView mSearchView;
    private NoteEditorFragment mNoteEditorFragment;
    private Note mCurrentNote;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notes);

		ActionBar ab = getActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ab.setDisplayShowTitleEnabled(false);

        mUndoBarController = new UndoBarController(findViewById(R.id.undobar), this);

        getFragmentManager().addOnBackStackChangedListener(this);

		int orientation = getResources().getConfiguration().orientation;
		if (getNoteEditorFragment() != null && orientation == Configuration.ORIENTATION_LANDSCAPE ) {
			mTwoPane = true;
			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			getNoteListFragment().setActivateOnItemClick(true);
            mNoteEditorFragment = getNoteEditorFragment();
		}

		Simplenote currentApp = (Simplenote) getApplication();
		if( currentApp.getSimperium().getUser() == null || currentApp.getSimperium().getUser().needsAuthentication() ){
			startLoginActivity();
		}
		currentApp.getSimperium().setAuthenticationListener(this);

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            // Check share action
            Intent intent = getIntent();
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            if (text != null) {
                Bucket<Note> notesBucket = currentApp.getNotesBucket();
                Note note = notesBucket.newObject();
                note.setContent(text);
                if (title != null)
                    note.setTitle(title);
                note.save();
                onNoteSelected(note);
            }
        }
	}

    @Override
    protected void onResume() {
        super.onResume();
        configureActionBar();
    }

    // nbradbury 01-Apr-2013
	private NoteListFragment getNoteListFragment() {
		return ((NoteListFragment) getFragmentManager().findFragmentById(R.id.note_list));
	}

    private NoteEditorFragment getNoteEditorFragment() {
        return ((NoteEditorFragment) getFragmentManager().findFragmentById(R.id.noteEditorFragment));
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.notes_list, menu);

        if (mSearchView == null) {
		    mSearchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener( ) {
                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText != null)
                        getNoteListFragment().searchNotes(newText);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String queryText) {
                    if (queryText != null)
                        getNoteListFragment().searchNotes(queryText);
                    return true;
                }
            });
        }

        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            menu.findItem(R.id.menu_create_note).setVisible(false);
            menu.findItem(R.id.menu_search).setVisible(false);
            menu.findItem(R.id.menu_preferences).setVisible(false);
            menu.findItem(R.id.menu_share).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else if (mTwoPane) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            menu.findItem(R.id.menu_create_note).setVisible(true);
            menu.findItem(R.id.menu_search).setVisible(true);
            menu.findItem(R.id.menu_preferences).setVisible(true);
            menu.findItem(R.id.menu_share).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else  {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            menu.findItem(R.id.menu_create_note).setVisible(true);
            menu.findItem(R.id.menu_search).setVisible(true);
            menu.findItem(R.id.menu_preferences).setVisible(true);
            menu.findItem(R.id.menu_share).setVisible(false);
            menu.findItem(R.id.menu_delete).setVisible(false);
        }

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_preferences :
			// nbradbury - use startActivityForResult so onActivityResult can detect when user returns from preferences
			Intent i = new Intent(this, PreferencesActivity.class);
			startActivityForResult(i, Simplenote.INTENT_PREFERENCES);
			return true;
		case R.id.menu_create_note :
			getNoteListFragment().addNote();
			return true;
        case R.id.menu_share:
            if (mCurrentNote != null) {
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mCurrentNote.getContent());
                startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_note)));
            }
            return true;
        case R.id.menu_delete:
            if (mNoteEditorFragment != null) {
                if (mCurrentNote != null) {
                    mCurrentNote.setDeleted(true);
                    mCurrentNote.setModificationDate(Calendar.getInstance());
                    // Note will be saved in the onDeleteConfirm method if user doesn't Undo the delete. See UndoBarController.java
                }
                boolean result = ((Simplenote)getApplication()).getNoteDB().update(mCurrentNote);
                if (result) {
                    popNoteDetail();
                    mUndoBarController.setDeletedNote(mCurrentNote);
                    mUndoBarController.showUndoBar(false, getString(R.string.note_deleted), null);
                    NoteListFragment fragment = getNoteListFragment();
                    if (fragment!=null) {
                        fragment.getPrefs();
                        fragment.refreshList();
                    }
                }
            }
            return true;
        case android.R.id.home:
            popNoteDetail();
            invalidateOptionsMenu();
            return true;
		default :
			return super.onOptionsItemSelected(item);
		}
	}

    protected void popNoteDetail() {
        FragmentManager fm = getFragmentManager();
        NoteEditorFragment f = (NoteEditorFragment) fm.findFragmentById(R.id.note_editor);
        if (f == null) {
            try {
                fm.popBackStack();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	/**
	 * Callback method from {@link NoteListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onNoteSelected(Note note) {
        mCurrentNote = note;
		String noteKey = note == null ? "none" : note.getSimperiumKey();

        FragmentManager fm = getFragmentManager();
        NoteEditorFragment f = getNoteEditorFragment();

        if (f == null || !f.isInLayout()) {
            ActionBar ab = getActionBar();
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            ab.setDisplayShowTitleEnabled(true);
            FragmentTransaction ft = fm.beginTransaction();
            ft.hide(getNoteListFragment());
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, noteKey);
            f = new NoteEditorFragment();
            f.setArguments(arguments);
            mNoteEditorFragment = f;
            ft.add(R.id.noteFragmentContainer, f);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(null);
            ft.commit();
            fm.executePendingTransactions();
        } else {
            f.setNote(note);
        }

        invalidateOptionsMenu();
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
		switch (requestCode) {
		case Simperium.SIGNUP_SIGNIN_REQUEST :
			if (resultCode == RESULT_CANCELED) 
				 finish();
			break;
		case Simplenote.INTENT_PREFERENCES :
			// nbradbury - refresh note list when user returns from preferences (in case they changed anything)
			NoteListFragment fragment = getNoteListFragment();
			if (fragment!=null) {
				fragment.getPrefs();
				fragment.refreshList();
			}
			break;
		}
	}

    @Override
    public void onUndo(Parcelable p) {
        if (mUndoBarController != null && mUndoBarController.getDeletedNote() != null) {
            Note deletedNote = mUndoBarController.getDeletedNote();
            if (deletedNote != null) {
                deletedNote.setDeleted(false);
                deletedNote.setModificationDate(Calendar.getInstance());
                Simplenote currentApp = ((Simplenote)getApplication());
                boolean result = currentApp.getNoteDB().update(deletedNote);
                if (result) {
                    NoteListFragment fragment = getNoteListFragment();
                    if (fragment!=null) {
                        fragment.getPrefs();
                        fragment.refreshList();
                    }
                }
            }
        }
    }

    @Override
    public void onDeleteConfirm() {
        if (mUndoBarController != null && mUndoBarController.getDeletedNote() != null) {
            mUndoBarController.getDeletedNote().save();
        }
    }

    @Override
    public void onBackStackChanged() {

        configureActionBar();
        invalidateOptionsMenu();
    }

    /**
     * If a note is changed that is being displayed in the NoteEditorFragment, update its content
     * @param note
     */
    public void onNoteChanged(Note note) {
        if (mCurrentNote != null && mNoteEditorFragment != null && mNoteEditorFragment.isVisible()) {
            if (note.getSimperiumKey().equals(mCurrentNote.getSimperiumKey())) {
                if (note.isDeleted())
                    popNoteDetail();
                else
                    mNoteEditorFragment.setNote(note);
            }
        }
    }

    private void configureActionBar() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            ActionBar ab = getActionBar();
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            ab.setDisplayShowTitleEnabled(true);
        } else {
            ActionBar ab = getActionBar();
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            ab.setDisplayShowTitleEnabled(false);
        }
    }
}

