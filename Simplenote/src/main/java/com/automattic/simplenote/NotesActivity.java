package com.automattic.simplenote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.TagsAdapter;
import com.automattic.simplenote.utils.TypefaceSpan;
import com.automattic.simplenote.utils.UndoBarController;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;

import com.simperium.Simperium;
import com.simperium.android.LoginActivity;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.Query;
import com.simperium.client.User;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.Calendar;

public class NotesActivity extends Activity implements
        NoteListFragment.Callbacks, User.StatusChangeListener, Simperium.OnUserCreatedListener, UndoBarController.UndoListener,
        Bucket.Listener<Note> {

    private boolean mIsLargeScreen, mIsLandscape;
    private UndoBarController mUndoBarController;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private NoteListFragment mNoteListFragment;
    private NoteEditorFragment mNoteEditorFragment;
    private Note mCurrentNote;
    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;
    private int TRASH_SELECTED_ID = 1;
    private ActionBar mActionBar;

    // Menu drawer
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private TagsAdapter mTagsAdapter;
    private TagsAdapter.TagMenuItem mSelectedTag;
    private CharSequence mActionBarTitle;

    public static String TAG_NOTE_LIST = "noteList";
    public static String TAG_NOTE_EDITOR = "noteEditor";

    // Google Analytics tracker
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        Simplenote currentApp = (Simplenote) getApplication();
        mNotesBucket = currentApp.getNotesBucket();
        mTagsBucket = currentApp.getTagsBucket();
        setContentView(R.layout.activity_notes);

        EasyTracker.getInstance().activityStart(this); // Add this method.
        mTracker = EasyTracker.getTracker();

        if (savedInstanceState == null) {
            mNoteListFragment = new NoteListFragment();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.noteFragmentContainer, mNoteListFragment, TAG_NOTE_LIST);
            fragmentTransaction.commit();
        } else {
            mNoteListFragment = (NoteListFragment) getFragmentManager().findFragmentByTag(TAG_NOTE_LIST);
        }

        Configuration config = getBaseContext().getResources().getConfiguration();
        if ((config.screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            mIsLargeScreen = true;

            if (getFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR) != null) {
                mNoteEditorFragment = (NoteEditorFragment) getFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR);
            } else if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mIsLandscape = true;
                addEditorFragment();
            }
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        // Set up the menu drawer
        mTagsAdapter = new TagsAdapter(this, mNotesBucket);
        mDrawerList.setAdapter(mTagsAdapter);
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (mSelectedTag == null)
            mSelectedTag = mTagsAdapter.getDefaultItem();

        // enable ActionBar app icon to behave as action to toggle nav drawer
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.open_drawer,
                R.string.close_drawer
        ) {
            public void onDrawerClosed(View view) {
                setTitle(mActionBarTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                setTitleWithCustomFont(getString(R.string.app_name));
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mUndoBarController = new UndoBarController(findViewById(R.id.undobar), this);

        if (PrefUtils.getBoolPref(this, PrefUtils.PREF_FIRST_LAUNCH, true)) {
            // Create the welcome note
            Note welcomeNote = mNotesBucket.newObject("welcome-android");
            welcomeNote.setCreationDate(Calendar.getInstance());
            welcomeNote.setModificationDate(welcomeNote.getCreationDate());
            welcomeNote.setContent(getString(R.string.welcome_note));
            welcomeNote.getTitle();
            welcomeNote.save();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PrefUtils.PREF_FIRST_LAUNCH, false);
            editor.commit();
        }

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            // Check share action
            Intent intent = getIntent();
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && !text.equals("")) {
                if (subject != null && !subject.equals("")) {
                    text = subject + "\n\n" + text;
                }
                Note note = mNotesBucket.newObject();
                note.setCreationDate(Calendar.getInstance());
                note.setModificationDate(note.getCreationDate());
                note.setContent(text);
                note.save();
                onNoteSelected(note.getSimperiumKey(), true);
                mTracker.sendEvent("note", "create_note", "external_share", null);
            }
        }
        currentApp.getSimperium().setOnUserCreatedListener(this);
        currentApp.getSimperium().setUserStatusChangeListener(this);
        checkForUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNotesBucket.start();
        mTagsBucket.start();

        mNotesBucket.addOnNetworkChangeListener(this);
        mNotesBucket.addOnSaveObjectListener(this);
        mNotesBucket.addOnDeleteObjectListener(this);
        mTagsBucket.addListener(mTagsMenuUpdater);

        updateNavigationDrawerItems();
        setSelectedTagActive();
        checkForCrashes();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mTagsBucket.removeListener(mTagsMenuUpdater);
        mNotesBucket.stop();
        mTagsBucket.stop();

        mNotesBucket.removeOnNetworkChangeListener(this);
        mNotesBucket.removeOnSaveObjectListener(this);
        mNotesBucket.removeOnDeleteObjectListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void setTitle(CharSequence title) {
        mActionBarTitle = (title != null) ? title : "";
        setTitleWithCustomFont(mActionBarTitle);
    }

    private void setTitleWithCustomFont(CharSequence title) {
        SpannableString s = new SpannableString(title);
        s.setSpan(new TypefaceSpan(this), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mActionBar.setTitle(s);
    }

    private void updateNavigationDrawerItems() {
        Bucket.ObjectCursor<Tag> tagCursor = Tag.allWithCount(mTagsBucket).execute();
        mTagsAdapter.changeCursor(tagCursor);
    }

    private void setSelectedTagActive() {
        if (mSelectedTag == null)
            mSelectedTag = mTagsAdapter.getDefaultItem();

        setTitle(mSelectedTag.name);
        mDrawerList.setItemChecked(mTagsAdapter.getPosition(mSelectedTag), true);
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            mSelectedTag = mTagsAdapter.getItem(position);
            checkEmptyListText(false);
            // Update checked item in navigation drawer and close it
            setSelectedTagActive();
            mDrawerLayout.closeDrawer(mDrawerList);

            getNoteListFragment().refreshListFromNavSelect();
            if (position > 1)
                mTracker.sendEvent("tag", "viewed_notes_for_tag", "selected_tag_in_navigation_drawer", null);
        }
    }

    public TagsAdapter.TagMenuItem getSelectedTag() {
        return mSelectedTag;
    }

    private void addEditorFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mNoteEditorFragment = new NoteEditorFragment();
        ft.add(R.id.noteFragmentContainer, mNoteEditorFragment, TAG_NOTE_EDITOR);
        ft.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }

    /**
     * Checks for a previously valid user that is now not authenticated
     *
     * @return true if user has invalid authorization
     */
    private boolean userAuthenticationIsInvalid() {
        Simplenote currentApp = (Simplenote) getApplication();
        Simperium simperium = currentApp.getSimperium();
        User user = simperium.getUser();
        return user.hasAccessToken() && user.getStatus().equals(User.Status.NOT_AUTHORIZED);
    }

    private void checkForCrashes() {
        CrashManager.register(this, Config.hockey_app_id);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, Config.hockey_app_id);
    }

    public void setCurrentNote(Note note) {
        mCurrentNote = note;
    }

    // received a change from the network, refresh the list and the editor
    @Override
    public void onChange(Bucket<Note> bucket, final Bucket.ChangeType type, String key) {
        final boolean resetEditor = mCurrentNote != null && mCurrentNote.getSimperiumKey().equals(key);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (type == Bucket.ChangeType.INDEX)
                    setProgressBarIndeterminateVisibility(false);
                mNoteListFragment.refreshList();
                if (!resetEditor) return;
                if (mNoteEditorFragment != null) {
                    mNoteEditorFragment.refreshContent(true);
                }
            }
        });
    }

    @Override
    public void onSaveObject(Bucket<Note> bucket, Note object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNoteListFragment.refreshList();
            }
        });
    }

    @Override
    public void onDeleteObject(Bucket<Note> bucket, Note object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNoteListFragment.refreshList();
            }
        });
    }

    // Tags bucket listener
    private Bucket.Listener<Tag> mTagsMenuUpdater = new Bucket.Listener<Tag>() {
        void updateNavigationDrawer() {
            runOnUiThread(new Runnable() {
                public void run() {
                    updateNavigationDrawerItems();
                }
            });
        }

        public void onSaveObject(Bucket<Tag> bucket, Tag tag) {
            updateNavigationDrawer();
        }

        public void onDeleteObject(Bucket<Tag> bucket, Tag tag) {
            updateNavigationDrawer();
        }

        public void onChange(Bucket<Tag> bucket, Bucket.ChangeType type, String key) {
            updateNavigationDrawer();
        }
    };

    public boolean isLargeScreenLandscape() {
        return mIsLargeScreen && mIsLandscape;
    }

    // nbradbury 01-Apr-2013
    private NoteListFragment getNoteListFragment() {
        return mNoteListFragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notes_list, menu);

        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();

        // Set a custom search view drawable
        int searchPlateId = mSearchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = mSearchView.findViewById(searchPlateId);
        if (searchPlate != null)
            searchPlate.setBackgroundResource(R.drawable.search_view_selector);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (mSearchMenuItem.isActionViewExpanded())
                    getNoteListFragment().searchNotes(newText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String queryText) {
                getNoteListFragment().searchNotes(queryText);
                return true;
            }

        });
        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                showDetailPlaceholder();
                checkEmptyListText(true);
                getNoteListFragment().hideWelcomeView();
                mTracker.sendEvent("note", "searched_notes", "action_bar_search_tap", null);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                // Show all notes again
                checkEmptyListText(false);
                getNoteListFragment().clearSearch();
                getNoteListFragment().setWelcomeViewVisibility();
                return true;
            }
        });

        Simplenote currentApp = (Simplenote) getApplication();
        menu.findItem(R.id.menu_sign_in).setVisible(currentApp.getSimperium().needsAuthorization());


        MenuItem trashItem = menu.findItem(R.id.menu_delete).setTitle(R.string.undelete);
        if (mCurrentNote != null && mCurrentNote.isDeleted())
            trashItem.setTitle(R.string.undelete);
        else
            trashItem.setTitle(R.string.delete);

        if (isLargeScreenLandscape()) {
            menu.findItem(R.id.menu_create_note).setVisible(!drawerOpen);
            menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
            menu.findItem(R.id.menu_preferences).setVisible(!drawerOpen);
            if (mCurrentNote != null) {
                menu.findItem(R.id.menu_share).setVisible(!drawerOpen);
                trashItem.setVisible(true);
            } else {
                menu.findItem(R.id.menu_share).setVisible(false);
                trashItem.setVisible(false);
            }
            menu.findItem(R.id.menu_edit_tags).setVisible(!drawerOpen);
            menu.findItem(R.id.menu_empty_trash).setVisible(false);
        } else {
            menu.findItem(R.id.menu_create_note).setVisible(!drawerOpen);
            menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
            menu.findItem(R.id.menu_preferences).setVisible(!drawerOpen);
            menu.findItem(R.id.menu_share).setVisible(false);
            trashItem.setVisible(false);
            menu.findItem(R.id.menu_edit_tags).setVisible(!drawerOpen);
            menu.findItem(R.id.menu_empty_trash).setVisible(false);
        }

        // Are we looking at the trash? Adjust menu accordingly.
        if (mDrawerList.getCheckedItemPosition() == TRASH_SELECTED_ID) {
            MenuItem emptyTrashItem = menu.findItem(R.id.menu_empty_trash);
            emptyTrashItem.setVisible(!drawerOpen);

            // Disable the trash icon if there are no notes trashed.
            Simplenote application = (Simplenote) getApplication();
            Bucket<Note> noteBucket = application.getNotesBucket();
            Query<Note> query = Note.allDeleted(noteBucket);
            if (query.count() == 0) {
                emptyTrashItem.setIcon(R.drawable.ab_icon_empty_trash_disabled);
                emptyTrashItem.setEnabled(false);
            } else {
                emptyTrashItem.setIcon(R.drawable.ab_icon_empty_trash);
                emptyTrashItem.setEnabled(true);
            }

            menu.findItem(R.id.menu_create_note).setVisible(false);
            menu.findItem(R.id.menu_search).setVisible(false);
            menu.findItem(R.id.menu_share).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_preferences:
                // nbradbury - use startActivityForResult so onActivityResult can detect when user returns from preferences
                Intent i = new Intent(this, PreferencesActivity.class);
                startActivityForResult(i, Simplenote.INTENT_PREFERENCES);
                return true;
            case R.id.menu_sign_in:
                startLoginActivity(true);
                return true;
            case R.id.menu_edit_tags:
                Intent editTagsIntent = new Intent(this, TagsListActivity.class);
                startActivity(editTagsIntent);
                return true;
            case R.id.menu_create_note:
                getNoteListFragment().addNote();
                mTracker.sendEvent("note", "create_note", "action_bar_button", null);
                return true;
            case R.id.menu_share:
                if (mCurrentNote != null) {
                    Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mCurrentNote.getContent());
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_note)));
                    mTracker.sendEvent("note", "shared_note", "action_bar_share_button", null);
                }
                return true;
            case R.id.menu_delete:
                if (mNoteEditorFragment != null) {
                    if (mCurrentNote != null) {
                        mCurrentNote.setDeleted(!mCurrentNote.isDeleted());
                        mCurrentNote.setModificationDate(Calendar.getInstance());
                        mCurrentNote.save();

                        if (mCurrentNote.isDeleted()) {
                            mUndoBarController.setDeletedNoteId(mCurrentNote.getSimperiumKey());
                            mUndoBarController.showUndoBar(false, getString(R.string.note_deleted), null);
                            mTracker.sendEvent("note", "deleted_note", "overflow_menu", null);
                        } else {
                            mTracker.sendEvent("note", "restored_note", "overflow_menu", null);
                        }
                        showDetailPlaceholder();
                    }
                    NoteListFragment fragment = getNoteListFragment();
                    if (fragment != null) {
                        fragment.getPrefs();
                        fragment.refreshList();
                    }
                }
                return true;
            case R.id.menu_empty_trash:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle(R.string.empty_trash);
                alert.setMessage(R.string.confirm_empty_trash);
                alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new emptyTrashTask().execute();
                        mTracker.sendEvent("note", "trash_emptied", "overflow_menu", null);
                    }
                });
                alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing, just closing the dialog
                    }
                });
                alert.show();
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Callback method from {@link NoteListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
    @Override
    public void onNoteSelected(String noteID, boolean isNew) {

        if (!isLargeScreenLandscape()) {
            // Launch the editor activity
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, noteID);
            arguments.putBoolean(NoteEditorFragment.ARG_NEW_NOTE, isNew);
            Intent editNoteIntent = new Intent(this, NoteEditorActivity.class);
            editNoteIntent.putExtras(arguments);
            startActivityForResult(editNoteIntent, Simplenote.INTENT_EDIT_NOTE);
        } else {
            mNoteEditorFragment.setIsNewNote(isNew);
            mNoteEditorFragment.setNote(noteID);
            getNoteListFragment().setNoteSelected(noteID);
        }

        mTracker.sendEvent("note", "viewed_note", "note_list_row_tap", null);
    }

    @Override
    public void onUserCreated(User user) {
        // New account created
        mTracker.sendEvent("user", "new_account_created", "account_created_from_login_activity", null);
    }

    public void onUserStatusChange(User.Status status) {
        switch (status) {

            // successfully used access token to connect to simperium bucket
            case AUTHORIZED:
                mTracker.sendEvent("user", "signed_in", "signed_in_from_login_activity", null);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidateOptionsMenu();
                        if (!mNotesBucket.hasChangeVersion()) {
                            setProgressBarIndeterminateVisibility(true);
                        }
                    }
                });
                break;

            // NOT_AUTHORIZED means we attempted to connect but the token was not valid
            case NOT_AUTHORIZED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startLoginActivity(true);
                    }
                });
                break;

            // Default starting state of User, don't do anything we allow use of app while not signed in so don't do anything
            case UNKNOWN:
                break;
        }
    }

    public void startLoginActivity(boolean signInFirst) {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        if (signInFirst)
            loginIntent.putExtra(LoginActivity.EXTRA_SIGN_IN_FIRST, true);
        startActivityForResult(loginIntent, Simperium.SIGNUP_SIGNIN_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Simplenote.INTENT_PREFERENCES:
                // nbradbury - refresh note list when user returns from preferences (in case they changed anything)
                NoteListFragment fragment = getNoteListFragment();
                if (fragment != null) {
                    fragment.getPrefs();
                    fragment.refreshList();
                }
                break;
            case Simplenote.INTENT_EDIT_NOTE:
                if (resultCode == RESULT_OK && data != null && data.hasExtra(Simplenote.DELETED_NOTE_ID)) {
                    String noteId = data.getStringExtra(Simplenote.DELETED_NOTE_ID);
                    if (noteId != null) {
                        mUndoBarController.setDeletedNoteId(noteId);
                        mUndoBarController.showUndoBar(false, getString(R.string.note_deleted), null);
                    }
                }
                break;
            /*case Simperium.SIGNUP_SIGNIN_REQUEST:
                if (resultCode == Activity.RESULT_CANCELED && userAuthenticationIsInvalid()) {
                    finish();
                }
                break;*/
        }
    }

    @Override
    public void onUndo(Parcelable p) {
        if (mUndoBarController != null && mUndoBarController.getDeletedNoteId() != null) {
            Note deletedNote = null;
            try {
                deletedNote = mNotesBucket.get(mUndoBarController.getDeletedNoteId());
            } catch (BucketObjectMissingException e) {
                return;
            }
            if (deletedNote != null) {
                deletedNote.setDeleted(false);
                deletedNote.setModificationDate(Calendar.getInstance());
                deletedNote.save();
                NoteListFragment fragment = getNoteListFragment();
                if (fragment != null) {
                    fragment.getPrefs();
                    fragment.refreshList();
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mDrawerToggle.onConfigurationChanged(newConfig);

        if (mIsLargeScreen) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Add the editor fragment
                mIsLandscape = true;
                addEditorFragment();
                if (mNoteListFragment != null) {
                    mNoteListFragment.setActivateOnItemClick(true);
                    mNoteListFragment.setDividerVisible(true);
                }
                // Select the current note on a tablet
                if (mCurrentNote != null)
                    onNoteSelected(mCurrentNote.getSimperiumKey(), false);
                else {
                    mNoteEditorFragment.setPlaceholderVisible(true);
                    mNoteListFragment.getListView().clearChoices();
                }
                invalidateOptionsMenu();
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && mNoteEditorFragment != null) {
                // Remove the editor fragment when rotating back to portrait
                mIsLandscape = false;
                mCurrentNote = null;
                if (mNoteListFragment != null) {
                    mNoteListFragment.setActivateOnItemClick(false);
                    mNoteListFragment.setDividerVisible(false);
                    mNoteListFragment.setActivatedPosition(ListView.INVALID_POSITION);
                    mNoteListFragment.refreshList();
                }
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.remove(mNoteEditorFragment);
                mNoteEditorFragment = null;
                ft.commitAllowingStateLoss();
                fm.executePendingTransactions();
                invalidateOptionsMenu();
            }
        }
    }

    public void checkEmptyListText(boolean isSearch) {
        if (isSearch) {
            getNoteListFragment().setEmptyListMessage(getString(R.string.no_notes_found));
            getNoteListFragment().setEmptyListViewClickable(false);
        } else if (mDrawerList.getCheckedItemPosition() == TRASH_SELECTED_ID) {
            getNoteListFragment().setEmptyListMessage(getString(R.string.trash_is_empty));
            EasyTracker.getTracker().sendEvent("note", "viewed_trash", "trash_filter_selected", null);
            getNoteListFragment().setEmptyListViewClickable(false);
        } else {
            getNoteListFragment().setEmptyListMessage(getString(R.string.no_notes));
            getNoteListFragment().setEmptyListViewClickable(true);
        }
    }

    public void showDetailPlaceholder() {
        if (isLargeScreenLandscape() && mNoteEditorFragment != null) {
            mCurrentNote = null;
            mNoteEditorFragment.setPlaceholderVisible(true);
        }
    }

    public void stopListeningToNotesBucket() {
        mNotesBucket.removeOnNetworkChangeListener(this);
        mNotesBucket.removeOnSaveObjectListener(this);
        mNotesBucket.removeOnDeleteObjectListener(this);
    }

    private class emptyTrashTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Simplenote application = (Simplenote) getApplication();
            Bucket<Note> noteBucket = application.getNotesBucket();
            Query<Note> query = Note.allDeleted(noteBucket);
            Bucket.ObjectCursor c = query.execute();
            while (c.moveToNext()) {
                c.getObject().delete();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            invalidateOptionsMenu();
            showDetailPlaceholder();
        }
    }

}