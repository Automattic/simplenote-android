package com.automattic.simplenote;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.models.Tag;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.TagsAdapter;
import com.automattic.simplenote.utils.Typefaces;
import com.simperium.client.Bucket;
import com.simperium.client.Bucket.ObjectCursor;
import com.simperium.client.Query;
import com.simperium.client.Query.SortType;

import java.util.Calendar;

/**
 * A list fragment representing a list of Notes. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link NoteEditorFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends ListFragment {

	private NotesCursorAdapter mNotesAdapter;
    private TextView mEmptyListTextView;
    private LinearLayout mDividerShadow;
	private int mNumPreviewLines;
    private String mSearchString;
    private ViewSwitcher mWelcomeViewSwitcher;
    private String mSelectedNoteId;

	/**
	 * The preferences key representing the activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String TAG_BUTTON_SIGNIN = "sign_in";
    private static final String TAG_BUTTON_SIGNUP = "sign_up";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

    public void setEmptyListViewClickable(boolean isClickable) {
        if (mEmptyListTextView != null) {
            mEmptyListTextView.setClickable(isClickable);
        }
    }

    /**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when a note has been selected.
		 */
		public void onNoteSelected(String noteID, boolean isNew);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sCallbacks = new Callbacks() {
		@Override
		public void onNoteSelected(String noteID, boolean isNew) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Simplenote application = (Simplenote) getActivity().getApplication();

		mNotesAdapter = new NotesCursorAdapter(getActivity().getBaseContext(), null, 0);
		setListAdapter(mNotesAdapter);

	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    // nbradbury - load values from preferences
	protected void getPrefs() {
        boolean condensedList = PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_CONDENSED_LIST, false);
		mNumPreviewLines = (condensedList) ? 0 : 2;
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.notes_list, container, false);
        return view;
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        NotesActivity notesActivity = (NotesActivity)getActivity();

        LinearLayout emptyView = (LinearLayout)view.findViewById(android.R.id.empty);
        emptyView.setVisibility(View.GONE);
        mEmptyListTextView = (TextView)view.findViewById(R.id.empty_message);
        mEmptyListTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNote();
            }
        });
        mDividerShadow = (LinearLayout)view.findViewById(R.id.divider_shadow);
        mWelcomeViewSwitcher = (ViewSwitcher)view.findViewById(R.id.welcome_view_switcher);

        TextView welcomeTextView = (TextView)view.findViewById(R.id.welcome_textview);
        TextView laterTextView = (TextView)view.findViewById(R.id.welcome_later_textview);

        Button signInButton = (Button)view.findViewById(R.id.welcome_sign_in);
        signInButton.setTag(TAG_BUTTON_SIGNIN);
        signInButton.setOnClickListener(signInClickListener);
        Button signUpButton = (Button)view.findViewById(R.id.welcome_sign_up);
        signUpButton.setTag(TAG_BUTTON_SIGNUP);
        signUpButton.setOnClickListener(signInClickListener);

        // Set custom typeface
        mEmptyListTextView.setTypeface(Typefaces.get(getActivity(), Simplenote.CUSTOM_FONT_PATH));
        welcomeTextView.setTypeface(Typefaces.get(getActivity(), Simplenote.CUSTOM_FONT_PATH));
        laterTextView.setTypeface(Typefaces.get(getActivity(), Simplenote.CUSTOM_FONT_PATH));
        signInButton.setTypeface(Typefaces.get(getActivity(), Simplenote.CUSTOM_FONT_PATH));
        signUpButton.setTypeface(Typefaces.get(getActivity(), Simplenote.CUSTOM_FONT_PATH));

        if (notesActivity.isLargeScreenLandscape()) {
            setActivateOnItemClick(true);
            mDividerShadow.setVisibility(View.VISIBLE);
        }

        welcomeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeViewSwitcher.showNext();
            }
        });
        laterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeViewSwitcher.showNext();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PrefUtils.PREF_APP_TRIAL, true);
                editor.commit();
            }
        });

        getListView().setDivider(getResources().getDrawable(R.drawable.list_divider));
        getListView().setDividerHeight(1);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onResume() {
		super.onResume();
        Log.i("SIMPLENOTE", "LIST FRAGMENT RESUMED");
        getPrefs();

        setWelcomeViewVisibility();

        refreshList();

        if (!PrefUtils.getBoolPref(getActivity(), PrefUtils.PREF_APP_TRIAL, false)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mWelcomeViewSwitcher.showNext();
                }
            }, 100);
        }
	}

    public void setWelcomeViewVisibility() {
        if (mWelcomeViewSwitcher != null && getActivity() != null) {
            Simplenote currentApp = (Simplenote) getActivity().getApplication();
            if (currentApp.getSimperium().needsAuthorization()) {
                int bottomMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) getListView().getLayoutParams();
                mlp.setMargins(0, 0, 0, bottomMargin);
                getListView().getEmptyView().setLayoutParams(mlp);
                mWelcomeViewSwitcher.setVisibility(View.VISIBLE);
            } else {
                mWelcomeViewSwitcher.setVisibility(View.GONE);
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) getListView().getLayoutParams();
                mlp.setMargins(0, 0, 0, 0);
                getListView().getEmptyView().setLayoutParams(mlp);
            }
        }
    }

    public void hideWelcomeView() {
        if (mWelcomeViewSwitcher != null)
            mWelcomeViewSwitcher.setVisibility(View.GONE);
    }

    private Button.OnClickListener signInClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((NotesActivity)getActivity()).startLoginActivity(v.getTag().equals(TAG_BUTTON_SIGNIN) ? true : false);
        }
    };

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sCallbacks;
	}

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    public void setEmptyListMessage(String message) {
        if (mEmptyListTextView != null && message != null)
            mEmptyListTextView.setText(Html.fromHtml(message));
    }

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

        NoteViewHolder holder = (NoteViewHolder)view.getTag();
        String noteID = holder.getNoteId();
        if (noteID != null)
            mCallbacks.onNoteSelected(noteID, false);

        mActivatedPosition = position;
	}

    /**
     * Selects first row in the list if available
     */
    public void selectFirstNote() {
        if (mNotesAdapter.getCount() > 0) {
            Note selectedNote = mNotesAdapter.getItem(0);
            mCallbacks.onNoteSelected(selectedNote.getSimperiumKey(), false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
	}

	public void setActivatedPosition(int position) {
        if (getListView() != null) {
            if (position == ListView.INVALID_POSITION) {
                getListView().setItemChecked(mActivatedPosition, false);
            } else {
                getListView().setItemChecked(position, true);
            }

            mActivatedPosition = position;
        }
	}

    public void setDividerVisible(boolean visible) {
        if (visible)
            mDividerShadow.setVisibility(View.VISIBLE);
        else
            mDividerShadow.setVisibility(View.GONE);
    }

	public void refreshList() {
        Log.d(Simplenote.TAG, "Refresh the list");
        new refreshListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
	}

    public void refreshListFromNavSelect() {
        Log.d(Simplenote.TAG, "Refresh the list");
        new refreshListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
    }

    public ObjectCursor<Note> queryNotes(){
        NotesActivity notesActivity = (NotesActivity)getActivity();
        Query<Note> query = notesActivity.getSelectedTag().query();
        if (mSearchString != null) {
            query.where(Note.CONTENT_PROPERTY, Query.ComparisonType.LIKE, String.format("%%%s%%", mSearchString));
        }
        query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME, Note.PINNED_INDEX_NAME, Note.MODIFICATION_DATE_PROPERTY);
        sortNoteQuery(query);
        return query.execute();
    }

	public void addNote() {

        // Prevents jarring 'New note...' from showing in the list view when creating a new note
        NotesActivity notesActivity = (NotesActivity)getActivity();
        if (!notesActivity.isLargeScreenLandscape())
            notesActivity.stopListeningToNotesBucket();

		// Create & save new note
		Simplenote simplenote = (Simplenote) getActivity().getApplication();
		Bucket<Note> notesBucket = simplenote.getNotesBucket();
		Note note = notesBucket.newObject();
        note.setCreationDate(Calendar.getInstance());
        note.setModificationDate(note.getCreationDate());

        if (notesActivity.getSelectedTag() != null && notesActivity.getSelectedTag().name != null) {
            String tagName = notesActivity.getSelectedTag().name;
            if (!tagName.equals(getString(R.string.notes)) && !tagName.equals(getString(R.string.trash)))
                note.setTagString(tagName);
        }

		note.save();
		
		// nbradbury - call onNoteSelected() directly rather than using code below, since code below may not always select the correct note depending on user's sort preference
		mCallbacks.onNoteSelected(note.getSimperiumKey(), true);
	}

    public void setNoteSelected(String selectedNoteID) {
        // Loop through notes and set note selected if found
        ObjectCursor<Note> cursor = (ObjectCursor<Note>)mNotesAdapter.getCursor();
        if (cursor == null || cursor.getCount() == 0)
            return;
        for(int i=0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            String noteKey = cursor.getSimperiumKey();
            if (noteKey != null && noteKey.equals(selectedNoteID)) {
                setActivatedPosition(i);
                return;
            }
        }

        // Didn't find the note, let's try again after the cursor updates (see refreshListTask)
        mSelectedNoteId = selectedNoteID;
    }

	public class NotesCursorAdapter extends CursorAdapter {
        private ObjectCursor<Note> mCursor;
        public NotesCursorAdapter(Context context, ObjectCursor<Note> c, int flags) {
            super(context, c, flags);
            mCursor = c;
        }

        public void changeCursor(ObjectCursor<Note> cursor){
            mCursor = cursor;
            super.changeCursor(cursor);
        }

        @Override
        public Note getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getObject();
        }

        /*
        *  nbradbury - implemented "holder pattern" to boost performance with large note lists
        */
		@Override
		public View getView(int position, View view, ViewGroup parent) {

			NoteViewHolder holder;
			if (view == null) {
				view = View.inflate(getActivity().getBaseContext(), R.layout.note_list_row, null);
				holder = new NoteViewHolder();
				holder.titleTextView = (TextView) view.findViewById(R.id.note_title);
                holder.titleTextView.setTypeface(Typefaces.get(getActivity().getBaseContext(), Simplenote.CUSTOM_FONT_PATH));
				holder.contentTextView = (TextView) view.findViewById(R.id.note_content);
                holder.contentTextView.setTypeface(Typefaces.get(getActivity().getBaseContext(), Simplenote.CUSTOM_FONT_PATH));
				holder.pinImageView = (ImageView) view.findViewById(R.id.note_pin);
				view.setTag(holder);
			} else {
				holder = (NoteViewHolder) view.getTag();
			}

            if (position == getListView().getCheckedItemPosition())
                view.setActivated(true);
            else
                view.setActivated(false);

            // for performance reasons we are going to get indexed values
            // from the cursor instead of instantiating the entire bucket object
            holder.contentTextView.setMaxLines(mNumPreviewLines);
            mCursor.moveToPosition(position);
            holder.setNoteId(mCursor.getSimperiumKey());
            int pinned = mCursor.getInt(mCursor.getColumnIndex("pinned"));
            holder.pinImageView.setVisibility(pinned == 1 ? View.VISIBLE : View.GONE);

            String title = mCursor.getString(mCursor.getColumnIndex("title"));
            if (title == null || title.equals("")) {
                title = getString(R.string.new_note_list);
            }
            holder.titleTextView.setText(title);

            if (mNumPreviewLines > 0) {
                holder.contentTextView.setText(mCursor.getString(mCursor.getColumnIndex("contentPreview")));
            }
			
			return view;
		}

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

        }

	}
	
	// view holder for NotesCursorAdapter
	private static class NoteViewHolder {
		TextView titleTextView;
		TextView contentTextView;
		ImageView pinImageView;
        private String mNoteId;

        public void setNoteId(String noteId) {
            mNoteId = noteId;
        }

        public String getNoteId() {
            return mNoteId;
        }
	}

	public void searchNotes(String searchString) {
        if (!searchString.equals(mSearchString)){
            mSearchString = searchString;
            refreshList();
        }
	}

    /**
     * Clear search and load all notes
     */
    public void clearSearch() {
        if (mSearchString != null && !mSearchString.equals("")){
            mSearchString = null;
            refreshList();
        }
    }

    public void sortNoteQuery(Query<Note> noteQuery){
        noteQuery.order("pinned", SortType.DESCENDING);
		int sortPref = PrefUtils.getIntPref(getActivity(), PrefUtils.PREF_SORT_ORDER);
		switch (sortPref) {
        case 0:
            noteQuery.order("modificationDate", SortType.DESCENDING);
            break;
		case 1:
            noteQuery.order("modificationDate", SortType.ASCENDING);
			break;
		case 2:
            noteQuery.order("creationDate", SortType.DESCENDING);
			break;
		case 3:
            noteQuery.order("creationDate", SortType.ASCENDING);
			break;
		case 4:
            noteQuery.order("content", SortType.ASCENDING);
			break;
		case 5:
            noteQuery.order("content", SortType.DESCENDING);
			break;
		}
    }

    private class refreshListTask extends AsyncTask<Boolean, Void, ObjectCursor<Note>> {
        boolean mIsFromNavSelect;

        @Override
        protected ObjectCursor<Note> doInBackground(Boolean ... args) {
            Log.d(Simplenote.TAG, "Querying in background");
            mIsFromNavSelect = args[0];
            return queryNotes();
        }

        @Override
        protected void onPostExecute(ObjectCursor<Note> cursor) {
            if (getActivity() == null || getActivity().isFinishing())
                return;
            Log.d(Simplenote.TAG, "Changing cursor");
            mNotesAdapter.changeCursor(cursor);
            Log.d(Simplenote.TAG, "Cursor changed");

            if (mIsFromNavSelect) {
                NotesActivity notesActivity = (NotesActivity)getActivity();
                if (notesActivity != null && notesActivity.isLargeScreenLandscape()) {
                    if (mNotesAdapter.getCount() == 0) {
                        notesActivity.showDetailPlaceholder();
                    } else {
                        // Select the first note
                        selectFirstNote();
                    }
                }
            }

            if (mSelectedNoteId != null) {
                setNoteSelected(mSelectedNoteId);
                mSelectedNoteId = null;
            }
        }
    }
}
