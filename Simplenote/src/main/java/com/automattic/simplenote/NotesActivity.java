package com.automattic.simplenote;

import static com.automattic.simplenote.NoteListFragment.TAG_PREFIX;
import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_NOTE;
import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_TAG;
import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_USER;
import static com.automattic.simplenote.analytics.AnalyticsTracker.CATEGORY_WIDGET;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.EDITOR_NOTE_RESTORED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.LIST_NOTES_SEARCHED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.LIST_NOTE_CREATED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.LIST_NOTE_DELETED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.LIST_NOTE_OPENED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.LIST_TAG_VIEWED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.LIST_TRASH_EMPTIED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.LIST_TRASH_VIEWED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_LIST_WIDGET_BUTTON_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_LIST_WIDGET_SIGN_IN_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_LIST_WIDGET_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.NOTE_WIDGET_SIGN_IN_TAPPED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.USER_ACCOUNT_CREATED;
import static com.automattic.simplenote.analytics.AnalyticsTracker.Stat.USER_SIGNED_IN;
import static com.automattic.simplenote.utils.DisplayUtils.disableScreenshotsIfLocked;
import static com.automattic.simplenote.utils.TagsAdapter.ALL_NOTES_ID;
import static com.automattic.simplenote.utils.TagsAdapter.DEFAULT_ITEM_POSITION;
import static com.automattic.simplenote.utils.TagsAdapter.SETTINGS_ID;
import static com.automattic.simplenote.utils.TagsAdapter.TAGS_ID;
import static com.automattic.simplenote.utils.TagsAdapter.TRASH_ID;
import static com.automattic.simplenote.utils.TagsAdapter.UNTAGGED_NOTES_ID;
import static com.automattic.simplenote.utils.WidgetUtils.KEY_LIST_WIDGET_CLICK;
import static com.automattic.simplenote.utils.WidgetUtils.KEY_WIDGET_CLICK;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.authentication.SimplenoteAuthenticationActivity;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.repositories.CollaboratorsRepository;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.AppLog.Type;
import com.automattic.simplenote.utils.AuthUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.HtmlCompat;
import com.automattic.simplenote.utils.NetworkUtils;
import com.automattic.simplenote.utils.NoteUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.StrUtils;
import com.automattic.simplenote.utils.TagsAdapter;
import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.utils.UndoBarController;
import com.google.android.material.navigation.NavigationView;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.Query;
import com.simperium.client.User;

import org.wordpress.passcodelock.AppLockManager;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotesActivity extends ThemedAppCompatActivity implements NoteListFragment.Callbacks,
    User.StatusChangeListener, Simperium.OnUserCreatedListener, UndoBarController.UndoListener,
    Bucket.Listener<Note> {
    public static String TAG_NOTE_LIST = "noteList";
    public static String TAG_NOTE_EDITOR = "noteEditor";

    public static final String KEY_ALREADY_LOGGED_IN = "KEY_ALREADY_LOGGED_IN";

    private static String STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED = "STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED";

    protected Bucket<Note> mNotesBucket;
    protected Bucket<Tag> mTagsBucket;
    private boolean mHasTappedNoteListWidgetButton;
    private boolean mIsSettingsClicked;
    private boolean mIsShowingMarkdown;
    private boolean mIsTabletFullscreen;
    private boolean mShouldSelectNewNote;

    private Menu mMenu;
    private String mTabletSearchQuery;
    private UndoBarController mUndoBarController;
    private View mFragmentsContainer;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private NoteListFragment mNoteListFragment;
    private NoteEditorFragment mNoteEditorFragment;
    private Note mCurrentNote;
    private MenuItem mEmptyTrashMenuItem;
    private Handler mInvalidateOptionsMenuHandler = new Handler();
    private Runnable mInvalidateOptionsMenuRunnable = new Runnable() {
        @Override
        public void run() {
            invalidateOptionsMenu();
        }
    };

    // Menu drawer
    private static final int GROUP_PRIMARY = 100;
    private static final int GROUP_SECONDARY = 101;
    private static final int GROUP_TERTIARY = 102;
    private DrawerLayout mDrawerLayout;
    private Menu mNavigationMenu;
    private ActionBarDrawerToggle mDrawerToggle;
    private TagsAdapter mTagsAdapter;
    private TagsAdapter.TagMenuItem mSelectedTag;
    @Inject CollaboratorsRepository collaboratorsRepository;
    // Tags bucket listener
    private Bucket.Listener<Tag> mTagsMenuUpdater = new Bucket.Listener<Tag>() {
        @Override
        public void onSyncObject(Bucket<Tag> bucket, String key) {

        }

        @Override
        public void onLocalQueueChange(Bucket<Tag> bucket, Set<String> queuedObjects) {

        }

        void updateNavigationDrawer() {
            runOnUiThread(new Runnable() {
                public void run() {
                    updateNavigationDrawerItems();
                }
            });
        }

        @Override
        public void onSaveObject(Bucket<Tag> bucket, Tag tag) {
            updateNavigationDrawer();
        }

        @Override
        public void onDeleteObject(Bucket<Tag> bucket, Tag tag) {
            updateNavigationDrawer();
        }

        @Override
        public void onNetworkChange(Bucket<Tag> bucket, Bucket.ChangeType type, String key) {
            updateNavigationDrawer();
        }

        @Override
        public void onBeforeUpdateObject(Bucket<Tag> bucket, Tag object) {
            // noop
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppLog.add(Type.NETWORK, NetworkUtils.getNetworkInfo(NotesActivity.this));
        AppLog.add(Type.SCREEN, "Created (NotesActivity)");
        setContentView(R.layout.activity_notes);

        mFragmentsContainer = findViewById(R.id.note_fragment_container);

        Simplenote currentApp = (Simplenote) getApplication();
        if (mNotesBucket == null) {
            mNotesBucket = currentApp.getNotesBucket();
        }

        if (mTagsBucket == null) {
            mTagsBucket = currentApp.getTagsBucket();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        configureNavigationDrawer(toolbar);

        if (savedInstanceState == null) {
            mNoteListFragment = new NoteListFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.note_fragment_container, mNoteListFragment, TAG_NOTE_LIST);
            fragmentTransaction.commit();
        } else {
            mHasTappedNoteListWidgetButton = savedInstanceState.getBoolean(STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED);
            mNoteListFragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_LIST);
        }

        mIsTabletFullscreen = mNoteListFragment.isHidden();

        if (DisplayUtils.isLargeScreen(this)) {
            if (getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR) != null) {
                mNoteEditorFragment = (NoteEditorFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR);
            } else if (DisplayUtils.isLandscape(this)) {
                addEditorFragment();
            }
        }

        // enable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);

            // Add loading indicator to show when indexing
            ProgressBar progressBar = (ProgressBar) getLayoutInflater().inflate(R.layout.progressbar_toolbar, null);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(progressBar);
            setToolbarProgressVisibility(false);
        }

        mUndoBarController = new UndoBarController(this);

        // Creates 'Welcome' note
        checkForFirstLaunch();

        if (savedInstanceState == null) {
            checkForSharedContent(getIntent());
        }

        currentApp.getSimperium().setOnUserCreatedListener(this);
        currentApp.getSimperium().setUserStatusChangeListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkForSharedContent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();

        // Ensure user has valid authorization
        if (userAuthenticationIsInvalid()) {
            startLoginActivity();

            if (intent.hasExtra(KEY_WIDGET_CLICK) && intent.getExtras() != null &&
                intent.getExtras().getSerializable(KEY_WIDGET_CLICK) == NOTE_WIDGET_SIGN_IN_TAPPED) {
                AnalyticsTracker.track(
                    NOTE_WIDGET_SIGN_IN_TAPPED,
                    CATEGORY_WIDGET,
                    "note_widget_sign_in_tapped"
                );
            }

            if (intent.hasExtra(KEY_LIST_WIDGET_CLICK) && intent.getExtras() != null &&
                intent.getExtras().getSerializable(KEY_LIST_WIDGET_CLICK) == NOTE_LIST_WIDGET_SIGN_IN_TAPPED) {
                AnalyticsTracker.track(
                    NOTE_LIST_WIDGET_SIGN_IN_TAPPED,
                    CATEGORY_WIDGET,
                    "note_list_widget_sign_in_tapped"
                );
            }
        }

        if (intent.getBooleanExtra(KEY_ALREADY_LOGGED_IN, false)) {
            intent.removeExtra(KEY_ALREADY_LOGGED_IN);
            showAlreadyLoggedInDialog();
        }

        if (intent.hasExtra(KEY_LIST_WIDGET_CLICK) && intent.getExtras() != null) {
            if (intent.getExtras().getSerializable(KEY_LIST_WIDGET_CLICK) == NOTE_LIST_WIDGET_TAPPED) {
                AnalyticsTracker.track(
                    NOTE_LIST_WIDGET_TAPPED,
                    CATEGORY_WIDGET,
                    "note_list_widget_tapped"
                );
            } else if (intent.getExtras().getSerializable(KEY_LIST_WIDGET_CLICK) == NOTE_LIST_WIDGET_BUTTON_TAPPED && !mHasTappedNoteListWidgetButton) {
                mHasTappedNoteListWidgetButton = true;
                AnalyticsTracker.track(
                    NOTE_LIST_WIDGET_BUTTON_TAPPED,
                    CATEGORY_WIDGET,
                    "note_list_widget_button_tapped"
                );
                AnalyticsTracker.track(
                    LIST_NOTE_CREATED,
                    CATEGORY_NOTE,
                    "note_list_widget"
                );
                intent.removeExtra(KEY_LIST_WIDGET_CLICK);
                getNoteListFragment().addNote("");
            }
        }

        disableScreenshotsIfLocked(this);

        mNotesBucket.addOnNetworkChangeListener(this);
        mNotesBucket.addOnSaveObjectListener(this);
        mNotesBucket.addOnDeleteObjectListener(this);
        AppLog.add(Type.SYNC, "Added note bucket listener (NotesActivity)");
        mTagsBucket.addListener(mTagsMenuUpdater);
        AppLog.add(Type.SYNC, "Added tag bucket listener (NotesActivity)");

        updateNavigationDrawerItems();

        // if the user is not authenticated and the tag doesn't exist revert to default drawer selection
        if (userIsUnauthorized()) {
            if (mTagsAdapter.getPosition(mSelectedTag) == -1) {
                mSelectedTag = null;
                mNavigationMenu.getItem(DEFAULT_ITEM_POSITION).setChecked(true);
            }
        }

        if (mSelectedTag != null) {
            filterListBySelectedTag();
        }

        if (mCurrentNote != null && mShouldSelectNewNote) {
            onNoteSelected(mCurrentNote.getSimperiumKey(), null, mCurrentNote.isMarkdownEnabled(), mCurrentNote.isPreviewEnabled());
            mShouldSelectNewNote = false;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (DisplayUtils.isLargeScreenLandscape(this)) {
            if (mIsTabletFullscreen) {
                ft.hide(mNoteListFragment);
            } else {
                ft.show(mNoteListFragment);
            }
        } else {
            ft.show(mNoteListFragment);
        }
        ft.commitNow();
    }

    private void showAlreadyLoggedInDialog() {
        String email = ((Simplenote) getApplication()).getSimperium().getUser().getEmail();
        Spanned alreadyLoggedInText = Html.fromHtml(getString(
            R.string.dialog_already_logged_in_message,
            "<b>" + email + "</b>"
        ));
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_already_logged_in_title)
            .setMessage(alreadyLoggedInText)
            .setPositiveButton(R.string.log_out, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AuthUtils.logOut((Simplenote) getApplication());
                    finish();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    protected void onPause() {
        super.onPause();  // Always call the superclass method first
        mTagsBucket.removeListener(mTagsMenuUpdater);
        AppLog.add(Type.SYNC, "Removed tag bucket listener (NotesActivity)");

        mNotesBucket.removeOnNetworkChangeListener(this);
        mNotesBucket.removeOnSaveObjectListener(this);
        mNotesBucket.removeOnDeleteObjectListener(this);
        AppLog.add(Type.SYNC, "Removed note bucket listener (NotesActivity)");
        AppLog.add(Type.SCREEN, "Paused (NotesActivity)");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED, mHasTappedNoteListWidgetButton);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setTitle(CharSequence title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActionModeCreated() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onActionModeDestroyed() {
        if (mSearchMenuItem != null && !mSearchMenuItem.isActionViewExpanded()) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    private ColorStateList getIconSelector() {
        int[][] states = new int[][] {
            new int[] {android.R.attr.state_checked}, // checked
            new int[] {-android.R.attr.state_checked}  // unchecked
        };

        int[] colors = new int[] {
            ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.colorAccent),
            ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.toolbarIconColor)
        };

        return new ColorStateList(states, colors);
    }

    private ColorStateList getTextSelector() {
        int[][] states = new int[][] {
            new int[] {-android.R.attr.state_enabled}, // disabled
            new int[] {android.R.attr.state_checked}, // checked
            new int[] {-android.R.attr.state_checked}  // unchecked
        };

        int[] colors = new int[] {
            getResources().getColor(R.color.text_title_disabled, getTheme()),
            ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.colorAccent),
            ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.noteTitleColor)
        };

        return new ColorStateList(states, colors);
    }

    private void configureNavigationDrawer(Toolbar toolbar) {
        ColorStateList iconSelector = getIconSelector();
        ColorStateList textSelector = getTextSelector();
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.getLayoutParams().width = ThemeUtils.getOptimalDrawerWidth(this);
        navigationView.setItemIconTintList(iconSelector);
        navigationView.setItemTextColor(textSelector);
        navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);

                    if (item.getItemId() == SETTINGS_ID) {
                        AnalyticsTracker.track(
                            LIST_TAG_VIEWED,
                            CATEGORY_TAG,
                            "selected_tag_in_navigation_drawer",
                            new HashMap<String, String>(1) {{
                                put("tag", "settings");
                            }}
                        );
                        mIsSettingsClicked = true;
                        return false;
                    } else {
                        mSelectedTag = mTagsAdapter.getTagFromItem(item);
                        filterListBySelectedTag();
                        return true;
                    }
                }
            }
        );

        mNavigationMenu = navigationView.getMenu();
        mNavigationMenu.add(GROUP_PRIMARY, ALL_NOTES_ID, Menu.NONE, getString(R.string.all_notes)).setIcon(R.drawable.ic_notes_24dp).setCheckable(true);
        mNavigationMenu.add(GROUP_PRIMARY, TRASH_ID, Menu.NONE, getString(R.string.trash)).setIcon(R.drawable.ic_trash_24dp).setCheckable(true);
        mNavigationMenu.add(GROUP_PRIMARY, SETTINGS_ID, Menu.NONE, getString(R.string.settings)).setIcon(R.drawable.ic_settings_24dp).setCheckable(false);
        mTagsAdapter = new TagsAdapter(this, mNotesBucket);

        if (mSelectedTag == null) {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_drawer,
            R.string.close_drawer) {
            public void onDrawerClosed(View view) {
                supportInvalidateOptionsMenu();

                if (mIsSettingsClicked) {
                    Intent intent = new Intent(NotesActivity.this, PreferencesActivity.class);
                    startActivityForResult(intent, Simplenote.INTENT_PREFERENCES);
                    mIsSettingsClicked = false;
                }
            }

            public void onDrawerOpened(View drawerView) {
                // noop
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0f);
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void filterListBySelectedTag() {
        MenuItem selectedMenuItem = mNavigationMenu.findItem((int) mSelectedTag.id);

        if (selectedMenuItem != null) {
            mSelectedTag = mTagsAdapter.getTagFromItem(selectedMenuItem);
        } else {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        checkEmptyListText(mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded());

        if (mNoteListFragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.show(mNoteListFragment);
            fragmentTransaction.commitNowAllowingStateLoss();
        }

        // Disable long press on notes when viewing Trash.
        if (mSelectedTag.id == TRASH_ID) {
            getNoteListFragment().getListView().setLongClickable(false);
        } else {
            getNoteListFragment().getListView().setLongClickable(true);
        }

        getNoteListFragment().refreshListFromNavSelect();

        Map<String, String> properties = new HashMap<>(1);

        switch ((int) mSelectedTag.id) {
            case ALL_NOTES_ID:
                properties.put("tag", "all_notes");
                break;
            case TRASH_ID:
                properties.put("tag", "trash");
                break;
            case UNTAGGED_NOTES_ID:
                properties.put("tag", "untagged_notes");
                break;
            default:
                properties = null;
                break;
        }

        AnalyticsTracker.track(
            LIST_TAG_VIEWED,
            CATEGORY_TAG,
            "selected_tag_in_navigation_drawer",
            properties
        );

        setSelectedTagActive();
    }

    private void checkForFirstLaunch() {
        if (PrefUtils.getBoolPref(this, PrefUtils.PREF_FIRST_LAUNCH, true)) {
            // Create the welcome note
            try {
                Note welcomeNote = mNotesBucket.newObject("welcome-android");
                welcomeNote.setCreationDate(Calendar.getInstance());
                welcomeNote.setModificationDate(welcomeNote.getCreationDate());
                welcomeNote.setContent(getString(R.string.welcome_note));
                welcomeNote.getTitle();
                welcomeNote.save();
            } catch (BucketObjectNameInvalid e) {
                // this won't happen because welcome-android is a valid name
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PrefUtils.PREF_FIRST_LAUNCH, false);
            editor.putBoolean(PrefUtils.PREF_ACCOUNT_REQUIRED, true);
            editor.apply();
        }
    }

    private void checkForSharedContent(Intent intent) {
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            // Check share action
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            // Don't add the 'Note to self' subject or open the note if this was shared from a voice search
            String intentAction = StrUtils.notNullStr(intent.getAction());
            boolean isVoiceShare = intentAction.equals("com.google.android.gm.action.AUTO_SEND");
            if (!TextUtils.isEmpty(text)) {
                if (!TextUtils.isEmpty(subject) && !isVoiceShare) {
                    text = subject + "\n\n" + text;
                }
                Note note = mNotesBucket.newObject();
                note.setCreationDate(Calendar.getInstance());
                note.setModificationDate(note.getCreationDate());
                note.setContent(text);
                note.save();
                setCurrentNote(note);
                mShouldSelectNewNote = true;

                AnalyticsTracker.track(
                    LIST_NOTE_CREATED,
                    CATEGORY_NOTE,
                    "external_share"
                );

                if (!DisplayUtils.isLargeScreenLandscape(this)) {
                    // Disable the lock screen when sharing content and opening NoteEditorActivity
                    // Lock screen activities are enabled again in NoteEditorActivity.onPause()
                    if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
                        AppLockManager.getInstance().getAppLock().setExemptActivities(
                            new String[] {"com.automattic.simplenote.NotesActivity",
                                "com.automattic.simplenote.NoteEditorActivity"});
                        AppLockManager.getInstance().getAppLock().setOneTimeTimeout(0);
                    }
                }
            }
        }
    }

    private List<Tag> getTagsFromCursor(Bucket.ObjectCursor<Tag> tagCursor) {
        List<Tag> tags = new ArrayList<>();

        for (int i = 0; i < tagCursor.getCount(); i++) {
            tagCursor.moveToNext();
            Tag tag = tagCursor.getObject();
            if (!collaboratorsRepository.isValidCollaborator(tag.getName())) {
                tags.add(tag);
            }
        }

        return tags;
    }

    private void updateNavigationDrawerItems() {
        boolean isAlphaSort = PrefUtils.getBoolPref(this, PrefUtils.PREF_SORT_TAGS_ALPHA);
        List<Tag> tags;
        if (isAlphaSort) {
            tags = getTagsFromCursor(Tag.allSortedAlphabetically(mTagsBucket).execute());
        } else {
            tags = getTagsFromCursor(Tag.allWithName(mTagsBucket).execute());
        }

        mTagsAdapter.submitList(tags);
        mNavigationMenu.removeGroup(GROUP_SECONDARY);
        mNavigationMenu.removeGroup(GROUP_TERTIARY);

        View emptyTagsDivider = findViewById(R.id.empty_tags_hint_divider);
        View emptyTagsHint = findViewById(R.id.empty_tags_hint_text);

        if (mTagsAdapter.getCountCustom() > 0) {
            MenuItem tagsHeader = mNavigationMenu.add(GROUP_SECONDARY, TAGS_ID, Menu.NONE, getString(R.string.tags)).setActionView(R.layout.drawer_action_edit).setEnabled(false);
            tagsHeader.getActionView().findViewById(R.id.edit).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(NotesActivity.this, TagsActivity.class));
                    }
                }
            );

            for (int i = 0; i < mTagsAdapter.getCount(); i++) {
                String name = mTagsAdapter.getItem(i).name;
                int id = (int) mTagsAdapter.getItem(i).id;

                if (id >= 0) { // Custom tags have a positive ID.
                    mNavigationMenu.add(GROUP_SECONDARY, id, Menu.NONE, name).setCheckable(true);
                }
            }

            mNavigationMenu.add(GROUP_TERTIARY, UNTAGGED_NOTES_ID, Menu.NONE, getString(R.string.untagged_notes)).setIcon(R.drawable.ic_untagged_24dp).setCheckable(true);
            setSelectedTagActive();
            emptyTagsDivider.setVisibility(View.GONE);
            emptyTagsHint.setVisibility(View.GONE);
        } else {
            emptyTagsDivider.setVisibility(View.VISIBLE);
            emptyTagsHint.setVisibility(View.VISIBLE);
        }
    }

    public void createNewNote(View view) {
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.SEARCH_EMPTY_TAPPED,
            AnalyticsTracker.CATEGORY_SEARCH,
            "search_empty_tapped_new_note"
        );
        getNoteListFragment().createNewNote(
            isSearchQueryNotNull() ? mSearchView.getQuery().toString() : "", "new_note_search"
        );
    }

    private boolean isSearchQueryNotNull() {
        return mSearchView != null && mSearchView.getQuery() != null;
    }

    private void setSelectedTagActive() {
        if (mSelectedTag == null) {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        MenuItem selectedMenuItem = mNavigationMenu.findItem((int) mSelectedTag.id);

        if (selectedMenuItem != null) {
            selectedMenuItem.setChecked(true);
        } else {
            mNavigationMenu.findItem(ALL_NOTES_ID).setChecked(true);
        }

        setTitle(mSelectedTag.name);
    }

    public TagsAdapter.TagMenuItem getSelectedTag() {
        if (mSelectedTag == null) {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        return mSelectedTag;
    }

    // Set trash action bar button enabled/disabled and icon based on deleted notes or not.
    public void updateTrashMenuItem() {
        if (mEmptyTrashMenuItem == null || mNotesBucket == null) {
            return;
        }

        // Disable trash icon if there are no trashed notes.
        Query<Note> query = Note.allDeleted(mNotesBucket);

        if (query.count() == 0) {
            mEmptyTrashMenuItem.setIcon(R.drawable.ic_trash_disabled_24dp);
            mEmptyTrashMenuItem.setEnabled(false);
        } else {
            mEmptyTrashMenuItem.setIcon(R.drawable.av_trash_empty_24dp);
            mEmptyTrashMenuItem.setEnabled(true);
        }
    }

    public void updateTrashMenuItem(boolean shouldWaitForAnimation) {
        if (shouldWaitForAnimation) {
            new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        updateTrashMenuItem();
                    }
                },
                getResources().getInteger(R.integer.time_animation)
            );
        } else {
            updateTrashMenuItem();
        }
    }

    private void addEditorFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mNoteEditorFragment = new NoteEditorFragment();
        ft.add(R.id.note_fragment_container, mNoteEditorFragment, TAG_NOTE_EDITOR);
        ft.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }

    private boolean userAccountRequired() {
        return PrefUtils.getBoolPref(this, PrefUtils.PREF_ACCOUNT_REQUIRED, false);
    }

    /**
     * Checks for a previously valid user that is now not authenticated
     * Also checks if user account is required (added in version 1.5.6)
     *
     * @return true if user has invalid authorization
     */
    private boolean userAuthenticationIsInvalid() {
        Simplenote currentApp = (Simplenote) getApplication();
        Simperium simperium = currentApp.getSimperium();
        User user = simperium.getUser();
        boolean isNotAuthorized = user.getStatus().equals(User.Status.NOT_AUTHORIZED);
        if (user.hasAccessToken() && isNotAuthorized) {
            AppLog.add(Type.ACCOUNT, "Access token not authorized");
            return true;
        }
        if (userAccountRequired() && isNotAuthorized) {
            AppLog.add(Type.ACCOUNT, "Access token missing");
            return true;
        }
        return false;
    }

    public boolean userIsUnauthorized() {
        Simplenote currentApp = (Simplenote) getApplication();
        return currentApp.getSimperium().getUser().getStatus() == User.Status.NOT_AUTHORIZED;
    }

    public void setCurrentNote(Note note) {
        mCurrentNote = note;
    }

    public NoteListFragment getNoteListFragment() {
        return mNoteListFragment;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notes_list, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        mMenu = menu;

        // restore the search query if on a landscape tablet
        String searchQuery = null;
        if (DisplayUtils.isLargeScreenLandscape(this) && mSearchView != null) {
            searchQuery = mSearchView.getQuery().toString();
        }

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        LinearLayout searchEditFrame = mSearchView.findViewById(R.id.search_edit_frame);
        ((LinearLayout.LayoutParams) searchEditFrame.getLayoutParams()).leftMargin = 0;

        if (!TextUtils.isEmpty(searchQuery)) {
            mSearchView.setQuery(searchQuery, false);
            mSearchMenuItem.expandActionView();
        } else {
            // Workaround for setting the search placeholder text color
            @SuppressWarnings("ResourceType")
            String hintHexColor = getString(R.color.text_title_disabled).replace("ff", "");
            mSearchView.setQueryHint(
                HtmlCompat.fromHtml(
                    String.format(
                        "<font color=\"%s\">%s</font>",
                        hintHexColor,
                        getString(R.string.search_hint)
                    )
                )
            );
        }

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (mSearchMenuItem.isActionViewExpanded()) {
                    getNoteListFragment().searchNotes(newText, false);
                }

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String queryText) {
                getNoteListFragment().searchNotes(queryText, true);
                getNoteListFragment().addSearchItem(queryText, 0);
                checkEmptyListText(true);
                return true;
            }
        });

        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                getNoteListFragment().searchNotes("", false);

                if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
                    updateActionsForLargeLandscape(menu);
                }

                checkEmptyListText(true);

                // Hide floating action button and list bottom padding.
                if (mNoteListFragment != null) {
                    mNoteListFragment.setFloatingActionButtonVisible(false);
                    mNoteListFragment.showListPadding(false);
                }

                AnalyticsTracker.track(
                    LIST_NOTES_SEARCHED,
                    CATEGORY_NOTE,
                    "action_bar_search_tap"
                );
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

                if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
                    updateActionsForLargeLandscape(menu);
                }

                // Show floating action button and list bottom padding.
                if (mNoteListFragment != null) {
                    mNoteListFragment.setFloatingActionButtonVisible(true);
                    mNoteListFragment.showListPadding(true);
                }

                mTabletSearchQuery = "";
                mSearchView.setQuery("", false);
                checkEmptyListText(false);
                getNoteListFragment().clearSearch();
                return true;
            }
        });

        mSearchMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (!mSearchMenuItem.isActionViewExpanded()) {
                    showDetailPlaceholder();
                }
                return false;
            }
        });

        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        MenuItem trashItem = menu.findItem(R.id.menu_trash);

        // Show delete action only when note is in Trash.
        // Change trash action to restore when note is in Trash.
        if (mCurrentNote != null && mCurrentNote.isDeleted()) {
            deleteItem.setVisible(true);
            trashItem.setTitle(R.string.restore);
        } else {
            deleteItem.setVisible(false);
            trashItem.setTitle(R.string.trash);
        }

        if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
            // Restore the search query on landscape tablets
            if (!TextUtils.isEmpty(mTabletSearchQuery)) {
                mSearchMenuItem.expandActionView();
                mSearchView.setQuery(mTabletSearchQuery, true);
                mSearchView.clearFocus();
            }

            updateActionsForLargeLandscape(menu);
        } else {
            menu.findItem(R.id.menu_search).setVisible(true);
            menu.findItem(R.id.menu_share).setVisible(false);
            menu.findItem(R.id.menu_info).setVisible(false);
            menu.findItem(R.id.menu_checklist).setVisible(false);
            menu.findItem(R.id.menu_markdown_preview).setVisible(false);
            menu.findItem(R.id.menu_collaborators).setVisible(false);
            menu.findItem(R.id.menu_sidebar).setVisible(false);
            trashItem.setVisible(false);
            menu.findItem(R.id.menu_empty_trash).setVisible(false);
            menu.setGroupVisible(R.id.group_1, false);
            menu.setGroupVisible(R.id.group_2, false);
            menu.setGroupVisible(R.id.group_3, false);
        }

        if (mSelectedTag != null && mSelectedTag.id == TRASH_ID) {
            mEmptyTrashMenuItem = menu.findItem(R.id.menu_empty_trash);
            mEmptyTrashMenuItem.setVisible(true);

            updateTrashMenuItem(false);

            menu.findItem(R.id.menu_search).setVisible(false);
            menu.findItem(R.id.menu_checklist).setVisible(false);
        }

        DrawableUtils.tintMenuItemWithAttribute(this, menu.findItem(R.id.menu_search), R.attr.toolbarIconColor);

        if (mDrawerLayout != null && mSearchMenuItem != null) {
            mDrawerLayout.setDrawerLockMode(mSearchMenuItem.isActionViewExpanded() ?
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
                DrawerLayout.LOCK_MODE_UNLOCKED
            );
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_sidebar:
                toggleSidebar(item);
                return true;
            case R.id.menu_markdown_preview:
                togglePreview(item);
                return true;
            case R.id.menu_delete:
                NoteUtils.showDialogDeletePermanently(NotesActivity.this, mCurrentNote);
                return true;
            case R.id.menu_trash:
                if (mNoteEditorFragment != null && mCurrentNote != null) {
                    trashNote(mCurrentNote);
                }

                return true;
            case R.id.menu_empty_trash:
                AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Dialog));

                alert.setTitle(R.string.empty_trash);
                alert.setMessage(R.string.confirm_empty_trash);
                alert.setNegativeButton(R.string.cancel, null);
                alert.setPositiveButton(R.string.empty, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        new EmptyTrashTask(NotesActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        setIconAfterAnimation(item, R.drawable.ic_trash_disabled_24dp, R.string.empty_trash);
                        AnalyticsTracker.track(
                            LIST_TRASH_EMPTIED,
                            CATEGORY_NOTE,
                            "overflow_menu"
                        );
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
            updateActionsForLargeLandscape(menu);
        }

        MenuItem pinItem = menu.findItem(R.id.menu_pin);
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        MenuItem historyItem = menu.findItem(R.id.menu_history);
        MenuItem publishItem = menu.findItem(R.id.menu_publish);
        MenuItem copyLinkItem = menu.findItem(R.id.menu_copy);
        MenuItem markdownItem = menu.findItem(R.id.menu_markdown);
        MenuItem markdownPreviewItem = menu.findItem(R.id.menu_markdown_preview);
        MenuItem collaboratorsItem = menu.findItem(R.id.menu_collaborators);

        if (mIsShowingMarkdown) {
            markdownPreviewItem.setIcon(R.drawable.av_visibility_off_on_24dp);
            markdownPreviewItem.setTitle(R.string.markdown_hide);
        } else {
            markdownPreviewItem.setIcon(R.drawable.av_visibility_on_off_24dp);
            markdownPreviewItem.setTitle(R.string.markdown_show);
        }

        if (mCurrentNote != null) {
            pinItem.setChecked(mCurrentNote.isPinned());
            publishItem.setChecked(mCurrentNote.isPublished());
            markdownItem.setChecked(mCurrentNote.isMarkdownEnabled());

            if (mCurrentNote.isDeleted()) {
                pinItem.setEnabled(false);
                shareItem.setEnabled(false);
                historyItem.setEnabled(false);
                publishItem.setEnabled(false);
                copyLinkItem.setEnabled(false);
                markdownItem.setEnabled(false);
                collaboratorsItem.setEnabled(false);
            } else {
                pinItem.setEnabled(true);
                shareItem.setEnabled(true);
                historyItem.setEnabled(true);
                publishItem.setEnabled(true);
                copyLinkItem.setEnabled(mCurrentNote.isPublished());
                markdownItem.setEnabled(true);
                collaboratorsItem.setEnabled(true);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void setIconAfterAnimation(final MenuItem item, final @DrawableRes int drawable, final @StringRes int string) {
        DrawableUtils.startAnimatedVectorDrawable(item.getIcon());
        new Handler().postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    item.setIcon(drawable);
                    item.setTitle(string);

                    if (item == mEmptyTrashMenuItem) {
                        invalidateOptionsMenu();
                    }
                }
            },
            getResources().getInteger(R.integer.time_animation)
        );
    }

    public void submitSearch(String selection) {
        if (mSearchView != null) {
            String query = mSearchView.getQuery().toString();

            if (query.endsWith(TAG_PREFIX)) {
                mSearchView.setQuery(query.substring(0, query.lastIndexOf(TAG_PREFIX)) + selection, true);
            } else {
                mSearchView.setQuery(selection, true);
            }
        }
    }

    private void updateActionsForLargeLandscape(Menu menu) {
        if (mCurrentNote != null) {
            menu.findItem(R.id.menu_checklist).setVisible(true);
            menu.findItem(R.id.menu_markdown_preview).setVisible(mCurrentNote.isMarkdownEnabled());
            menu.findItem(R.id.menu_sidebar).setVisible(true);
            menu.findItem(R.id.menu_info).setVisible(true);
            menu.setGroupVisible(R.id.group_1, true);
            menu.setGroupVisible(R.id.group_2, true);
            menu.setGroupVisible(R.id.group_3, true);
        } else {
            menu.findItem(R.id.menu_checklist).setVisible(false);
            menu.findItem(R.id.menu_markdown_preview).setVisible(false);
            menu.findItem(R.id.menu_sidebar).setVisible(false);
            menu.findItem(R.id.menu_info).setVisible(false);
            menu.setGroupVisible(R.id.group_1, false);
            menu.setGroupVisible(R.id.group_2, false);
            menu.setGroupVisible(R.id.group_3, false);
        }

        menu.findItem(R.id.menu_empty_trash).setVisible(mSelectedTag != null && mSelectedTag.id == TRASH_ID);
    }

    public void trashNote(Note note) {
        if (note == null || isFinishing()) {
            return;
        }

        note.setDeleted(!note.isDeleted());
        note.setModificationDate(Calendar.getInstance());
        note.save();

        if (note.isDeleted()) {
            List<String> deletedNoteIds = new ArrayList<>();
            deletedNoteIds.add(note.getSimperiumKey());
            mUndoBarController.setDeletedNoteIds(deletedNoteIds);
            mUndoBarController.showUndoBar(getUndoView(), getString(R.string.note_deleted));
            AnalyticsTracker.track(
                LIST_NOTE_DELETED,
                CATEGORY_NOTE,
                "overflow_menu"
            );
        } else {
            AnalyticsTracker.track(
                EDITOR_NOTE_RESTORED,
                CATEGORY_NOTE,
                "overflow_menu"
            );
        }

        if (getNoteListFragment() != null) {
            NoteListFragment fragment = getNoteListFragment();
            if (DisplayUtils.isLargeScreenLandscape(this)) {
                fragment.updateSelectionAfterTrashAction();
            }
            fragment.getPrefs();
            fragment.refreshList();
        }

        if (mInvalidateOptionsMenuHandler != null) {
            mInvalidateOptionsMenuHandler.removeCallbacks(mInvalidateOptionsMenuRunnable);
            mInvalidateOptionsMenuHandler.postDelayed(
                mInvalidateOptionsMenuRunnable,
                getResources().getInteger(android.R.integer.config_shortAnimTime)
            );
        }
    }

    public void setMarkdownShowing(boolean isMarkdownShowing) {
        mIsShowingMarkdown = isMarkdownShowing;

        if (mNoteEditorFragment != null) {
            if (isMarkdownShowing) {
                mNoteEditorFragment.showMarkdown();
            } else {
                mNoteEditorFragment.hideMarkdown();
            }
        }

        new Handler().postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    invalidateOptionsMenu();
                }
            },
            getResources().getInteger(R.integer.time_animation)
        );
    }

    /**
     * Callback method from {@link NoteListFragment.Callbacks} indicating that
     * the item with the given ID was selected. Used for tablets only.
     */
    @Override
    public void onNoteSelected(String noteID, String matchOffsets, boolean isMarkdownEnabled, boolean isPreviewEnabled) {
        if (!DisplayUtils.isLargeScreenLandscape(this)) {
            // Launch the editor activity
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, noteID);
            arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, isMarkdownEnabled);
            arguments.putBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED, isPreviewEnabled);

            if (matchOffsets != null) {
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS, matchOffsets);
            }

            Intent editNoteIntent = new Intent(this, NoteEditorActivity.class);
            editNoteIntent.putExtras(arguments);

            startActivityForResult(editNoteIntent, Simplenote.INTENT_EDIT_NOTE);
        } else {
            mNoteEditorFragment.removeScrollListener();
            mNoteEditorFragment.setNote(noteID, matchOffsets);
            getNoteListFragment().setNoteSelected(noteID);
            setMarkdownShowing(isPreviewEnabled && matchOffsets == null);

            if (isSearchQueryNotNull()) {
                mTabletSearchQuery = mSearchView.getQuery().toString();
            }

            mNoteEditorFragment.clearMarkdown();

            if (!isMarkdownEnabled && mIsShowingMarkdown) {
                setMarkdownShowing(false);
            }

            invalidateOptionsMenu();
        }

        AnalyticsTracker.track(
            LIST_NOTE_OPENED,
            CATEGORY_NOTE,
            "note_list_row_tap"
        );
    }

    @Override
    public void onUserCreated(User user) {
        // New account created
        AnalyticsTracker.track(
            USER_ACCOUNT_CREATED,
            CATEGORY_USER,
            "account_created_from_login_activity"
        );
    }

    public void selectDefaultTag() {
        mSelectedTag = mTagsAdapter.getDefaultItem();
        MenuItem selectedMenuItem = mNavigationMenu.findItem((int) mSelectedTag.id);

        if (selectedMenuItem != null) {
            mSelectedTag = mTagsAdapter.getTagFromItem(selectedMenuItem);
        } else {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        checkEmptyListText(mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded());

        if (mNoteListFragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.show(mNoteListFragment);
            fragmentTransaction.commitNowAllowingStateLoss();
        }

        getNoteListFragment().refreshList();
        setSelectedTagActive();
    }

    public void scrollToSelectedNote(String id) {
        if (mNoteListFragment != null && mNoteListFragment.getListAdapter() != null) {
            ListAdapter listAdapter = mNoteListFragment.getListAdapter();
            ListView listView = mNoteListFragment.getListView();

            for (int i = 0; i < listAdapter.getCount(); i++) {
                if (((Note) listAdapter.getItem(i + listView.getHeaderViewsCount())).getSimperiumKey().equals(id)) {
                    listView.smoothScrollToPosition(i);
                    break;
                }
            }
        }
    }

    public void onUserStatusChange(User.Status status) {
        switch (status) {
            // successfully used access token to connect to simperium bucket
            case AUTHORIZED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!mNotesBucket.hasChangeVersion()) {
                            setToolbarProgressVisibility(true);
                        }
                    }
                });
                break;

            // NOT_AUTHORIZED means we attempted to connect but the token was not valid
            case NOT_AUTHORIZED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppLog.add(Type.ACCOUNT, "Access token not authorized");
                        startLoginActivity();
                    }
                });
                break;

            // Default starting state of User, don't do anything we allow use of app while not signed in so don't do anything
            case UNKNOWN:
                break;
        }
    }

    private void setToolbarProgressVisibility(boolean isVisible) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(isVisible);
        }
    }

    public void startLoginActivity() {
        // Clear account-specific data
        AuthUtils.logOut((Simplenote) getApplication());

        Intent intent = new Intent(NotesActivity.this, SimplenoteAuthenticationActivity.class);
        startActivityForResult(intent, Simperium.SIGNUP_SIGNIN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Simplenote.INTENT_PREFERENCES:
                // nbradbury - refresh note list when user returns from preferences (in case they changed anything)
                invalidateOptionsMenu();
                NoteListFragment fragment = getNoteListFragment();
                if (fragment != null) {
                    fragment.getPrefs();
                    fragment.refreshList();
                }

                break;
            case Simplenote.INTENT_EDIT_NOTE:
                if (resultCode == RESULT_OK && data != null) {
                    if (data.hasExtra(Simplenote.DELETED_NOTE_ID)) {
                        String noteId = data.getStringExtra(Simplenote.DELETED_NOTE_ID);
                        if (noteId != null) {
                            List<String> deletedNoteIds = new ArrayList<>();
                            deletedNoteIds.add(noteId);
                            mUndoBarController.setDeletedNoteIds(deletedNoteIds);
                            mUndoBarController.showUndoBar(getUndoView(), getString(R.string.note_deleted));
                        }
                    } else if (DisplayUtils.isLargeScreenLandscape(this) && data.hasExtra(Simplenote.SELECTED_NOTE_ID)) {
                        String selectedNoteId = data.getStringExtra(Simplenote.SELECTED_NOTE_ID);
                        boolean isPreviewEnabled = data.getBooleanExtra(NoteEditorFragment.ARG_PREVIEW_ENABLED, false);
                        boolean isMarkdownEnabled = data.getBooleanExtra(NoteEditorFragment.ARG_MARKDOWN_ENABLED, false);
                        onNoteSelected(selectedNoteId, null, isMarkdownEnabled, isPreviewEnabled);

                        // Relaunch shortcut dialog if it was showing in editor (Chrome OS).
                        if (data.getBooleanExtra(ShortcutDialogFragment.DIALOG_VISIBLE, false)) {
                            ShortcutDialogFragment.showShortcuts(NotesActivity.this, false);
                        }
                    }
                }
                break;
            case Simperium.SIGNUP_SIGNIN_REQUEST:
                invalidateOptionsMenu();

                Simplenote app = (Simplenote) getApplication();
                AnalyticsTracker.refreshMetadata(app.getSimperium().getUser().getEmail());

                AnalyticsTracker.track(
                    USER_SIGNED_IN,
                    CATEGORY_USER,
                    "signed_in_from_login_activity"
                );

                if (resultCode == Activity.RESULT_CANCELED && userAuthenticationIsInvalid()) {
                    finish();
                }

                break;
        }
    }

    @Override
    public void onUndo() {
        if (mUndoBarController == null) return;

        List<String> deletedNoteIds = mUndoBarController.getDeletedNoteIds();
        if (deletedNoteIds != null) {
            for (int i = 0; i < deletedNoteIds.size(); i++) {
                Note deletedNote;
                try {
                    deletedNote = mNotesBucket.get(deletedNoteIds.get(i));
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
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);

        // Relaunch shortcut dialog when window is maximized or restored (Chrome OS).
        if (getSupportFragmentManager().findFragmentByTag(ShortcutDialogFragment.DIALOG_TAG) != null) {
            ShortcutDialogFragment.showShortcuts(NotesActivity.this, false);
        }

        if (DisplayUtils.isLargeScreen(this)) {
            mIsShowingMarkdown = false;

            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Add the editor fragment
                if (getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR) != null) {
                    mNoteEditorFragment = (NoteEditorFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR);
                } else if (DisplayUtils.isLandscape(this)) {
                    addEditorFragment();
                }

                if (mNoteListFragment != null) {
                    mNoteListFragment.setActivateOnItemClick(true);
                    mNoteListFragment.setDividerVisible(true);
                }

                // Select the current note on a tablet
                if (mCurrentNote != null) {
                    onNoteSelected(mCurrentNote.getSimperiumKey(), null, mCurrentNote.isMarkdownEnabled(), mCurrentNote.isPreviewEnabled());
                } else {
                    mNoteEditorFragment.setPlaceholderVisible(true);
                    mNoteListFragment.getListView().clearChoices();
                }

                invalidateOptionsMenu();
            // Go to NoteEditorActivity if a note was selected and orientation was switched to portrait
            } else if (mCurrentNote != null) {
                onNoteSelected(mCurrentNote.getSimperiumKey(), null, mCurrentNote.isMarkdownEnabled(), mCurrentNote.isPreviewEnabled());
                overridePendingTransition(0, 0);
            }
        } else {
            // Show list/sidebar when it was hidden while in landscape orientation.
            if (mNoteListFragment.isHidden()) {
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.show(mNoteListFragment);
                fragmentTransaction.commitNowAllowingStateLoss();
                mIsTabletFullscreen = mNoteListFragment.isHidden();
            }

            // Dismiss all bottom sheet dialogs when going from editor to list view.
            HistoryBottomSheetDialog dialogHistory = (HistoryBottomSheetDialog) getSupportFragmentManager().findFragmentByTag(HistoryBottomSheetDialog.TAG);

            if (dialogHistory != null) {
                dialogHistory.dismiss();
            }

            InfoBottomSheetDialog dialogInfo = (InfoBottomSheetDialog) getSupportFragmentManager().findFragmentByTag(InfoBottomSheetDialog.TAG);

            if (dialogInfo != null) {
                dialogInfo.dismiss();
            }

            ShareBottomSheetDialog dialogShare = (ShareBottomSheetDialog) getSupportFragmentManager().findFragmentByTag(ShareBottomSheetDialog.TAG);

            if (dialogShare != null) {
                dialogShare.dismiss();
            }
        }

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && mNoteEditorFragment != null) {
            // Remove the editor fragment when rotating back to portrait
            mCurrentNote = null;
            if (mNoteListFragment != null) {
                mNoteListFragment.setActivateOnItemClick(false);
                mNoteListFragment.setDividerVisible(false);
                mNoteListFragment.setActivatedPosition(ListView.INVALID_POSITION);
                mNoteListFragment.refreshList();
            }
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(mNoteEditorFragment);
            mNoteEditorFragment = null;
            ft.commitAllowingStateLoss();
            fm.executePendingTransactions();
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_C:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.insertChecklist();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_toggle_checklist_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_COMMA:
                if (event.isCtrlPressed()) {
                    ShortcutDialogFragment.showShortcuts(NotesActivity.this, false);
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_H:
                if (event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.showHistory();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_show_history_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_I:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    getNoteListFragment().createNewNote("", "keyboard_shortcut");
                    return true;
                } else if (event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.showInfo();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_show_information_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_L:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        toggleSidebar(mMenu.findItem(R.id.menu_sidebar));
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_toggle_list_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_P:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mCurrentNote != null && mCurrentNote.isMarkdownEnabled()) {
                            togglePreview(mMenu.findItem(R.id.menu_markdown_preview));
                        } else {
                            Toast.makeText(NotesActivity.this, R.string.item_action_toggle_preview_enable_error, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_toggle_preview_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_S:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (mSearchMenuItem != null && mSearchView != null) {
                        mSearchMenuItem.expandActionView();
                        mSearchView.requestFocus();
                    }

                    return true;
                } else if (event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.shareNote();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_show_share_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private boolean isLargeLandscapeAndNoteSelected() {
        return DisplayUtils.isLargeScreenLandscape(NotesActivity.this) && mNoteEditorFragment != null && !mNoteEditorFragment.isPlaceholderVisible();
    }

    public void checkEmptyListText(boolean isSearch) {
        if (isSearch) {
            getNoteListFragment().setEmptyListButton(
                isSearchQueryNotNull() ?
                    getString(R.string.empty_notes_search_button, mSearchView.getQuery().toString()) :
                    getString(R.string.empty_notes_search_button_default)
            );
            getNoteListFragment().setEmptyListImage(-1);
            getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_search));
        } else if (mSelectedTag != null) {
            getNoteListFragment().setEmptyListButton("");

            if (mSelectedTag.id == ALL_NOTES_ID) {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_notes_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_all));
            } else if (mSelectedTag.id == TRASH_ID) {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_trash_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_trash));
                AnalyticsTracker.track(
                    LIST_TRASH_VIEWED,
                    CATEGORY_NOTE,
                    "trash_filter_selected"
                );
            } else if (mSelectedTag.id == UNTAGGED_NOTES_ID) {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_untagged_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_untagged));
            } else {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_tag_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_tag, mSelectedTag.name));
            }
        } else {
            getNoteListFragment().setEmptyListButton("");
            getNoteListFragment().setEmptyListImage(R.drawable.ic_notes_24dp);
            getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_all));
        }
    }

    public void showDetailPlaceholder() {
        if (DisplayUtils.isLargeScreenLandscape(this) && mNoteEditorFragment != null) {
            mCurrentNote = null;
            mNoteEditorFragment.setPlaceholderVisible(true);
            mNoteEditorFragment.clearMarkdown();
            mNoteEditorFragment.hideMarkdown();
            mIsShowingMarkdown = false;
        }
    }

    public void stopListeningToNotesBucket() {
        mNotesBucket.removeOnNetworkChangeListener(this);
        mNotesBucket.removeOnSaveObjectListener(this);
        mNotesBucket.removeOnDeleteObjectListener(this);
        AppLog.add(Type.SYNC, "Removed note bucket listener (NotesActivity)");
    }

    // Returns the appropriate view to show the undo bar within
    private View getUndoView() {
        View undoView = mFragmentsContainer;
        if (!DisplayUtils.isLargeScreenLandscape(this) &&
            getNoteListFragment() != null &&
            getNoteListFragment().getRootView() != null) {
            undoView = getNoteListFragment().getRootView();
        }

        return undoView;
    }

    public void showUndoBarWithNoteIds(List<String> noteIds) {
        if (mUndoBarController != null) {
            mUndoBarController.setDeletedNoteIds(noteIds);
            mUndoBarController.showUndoBar(
                getUndoView(),
                getResources().getQuantityString(R.plurals.trashed_notes, noteIds.size(), noteIds.size())
            );
        }
    }

    private void togglePreview(MenuItem item) {
        if (mIsShowingMarkdown) {
            setIconAfterAnimation(item, R.drawable.av_visibility_on_off_24dp, R.string.markdown_show);
            setMarkdownShowing(false);
            mCurrentNote.setPreviewEnabled(false);
        } else {
            setIconAfterAnimation(item, R.drawable.av_visibility_off_on_24dp, R.string.markdown_hide);
            setMarkdownShowing(true);
            mCurrentNote.setPreviewEnabled(true);
        }

        mCurrentNote.save();
    }

    private void toggleSidebar(MenuItem item) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (mNoteListFragment.isHidden()) {
            ft.show(mNoteListFragment);
            setIconAfterAnimation(item, R.drawable.av_list_hide_show_24dp, R.string.list_hide);
        } else {
            ft.hide(mNoteListFragment);
            setIconAfterAnimation(item, R.drawable.av_list_show_hide_24dp, R.string.list_show);
        }

        ft.commitNowAllowingStateLoss();
        mIsTabletFullscreen = mNoteListFragment.isHidden();
    }

    /* Simperium Bucket Listeners */
    // received a change from the network, refresh the list
    @Override
    public void onNetworkChange(Bucket<Note> bucket, final Bucket.ChangeType type, String key) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (type == Bucket.ChangeType.INDEX) {
                    setToolbarProgressVisibility(false);
                }
                mNoteListFragment.refreshList();
            }
        });
    }

    @Override
    public void onSaveObject(Bucket<Note> bucket, Note note) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNoteListFragment.refreshList();
            }
        });

        if (note.equals(mCurrentNote)) {
            mCurrentNote = note;

            new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        invalidateOptionsMenu();
                    }
                },
                getResources().getInteger(R.integer.time_animation)
            );
        }

        AppLog.add(
            Type.SYNC,
            "Saved note callback in NotesActivity (ID: " + note.getSimperiumKey() +
                " / Title: " + note.getTitle() +
                " / Characters: " + NoteUtils.getCharactersCount(note.getContent()) +
                " / Words: " + NoteUtils.getWordCount(note.getContent()) + ")"
        );
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

    @Override
    public void onBeforeUpdateObject(Bucket<Note> bucket, Note note) {
        // noop, NoteEditorFragment will handle this
    }

    @Override
    public void onLocalQueueChange(Bucket<Note> bucket, Set<String> queuedObjects) {

    }

    @Override
    public void onSyncObject(Bucket<Note> bucket, String key) {

    }

    private static class EmptyTrashTask extends AsyncTask<Void, Void, Void> {
        private SoftReference<NotesActivity> mNotesActivityReference;

        EmptyTrashTask(NotesActivity context) {
            mNotesActivityReference = new SoftReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            NotesActivity activity = mNotesActivityReference.get();

            if (activity.mNotesBucket == null) {
                return null;
            }

            Query<Note> query = Note.allDeleted(activity.mNotesBucket);
            Bucket.ObjectCursor cursor = query.execute();

            while (cursor.moveToNext()) {
                cursor.getObject().delete();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            NotesActivity activity = mNotesActivityReference.get();

            if (activity != null) {
                activity.showDetailPlaceholder();
            }
        }
    }
}
