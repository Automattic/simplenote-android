## Testing

### Setup 

- [ ] Sign up using mixed-capitalization in the email address. Use this account to run all tests below.

#### Login/Signup

- [ ] Welcome note is shown for newly signed-up user.
- [ ] Logout.
- [ ] Login with wrong password fails.
- [ ] Login with correct password succeeds.

#### Sync

- [ ] Created note appears in other device.
- [ ] Changes to new note sync to/from other device.
- [ ] New tag immediately syncs to/from other device.
- [ ] Removed tag immediately syncs to/from other device.
- [ ] Note publishes with link.
- [ ] Note unpublishes.
- [ ] Note publish change syncs _from_ other device (visible with dialog open).
- [ ] Markdown setting syncs to/from other device.
- [ ] Preview mode disappears/reappears when receiving remote changes to markdown setting.
- [ ] Note pinning syncs immediately to/from other device.
- [ ] Note pinning works regardless if done from list view or note info.
- [ ] Viewing history on one device leaves note unchanged on other device.
- [ ] Restoring history immediately syncs note to/from other device.
- [ ] After disabling network connectivity and making changes, selecting Log Out triggers an `Unsynced Notes Detected` alert.
- [ ] After going back online, changes sync.

#### Note editor

- [ ] Can preview markdown with Preview tab (👁 button on landscape tablet).
- [ ] Can flip to edit mode with Edit tab (👁 button on landscape tablet).
- [ ] Can toggle sidebar (in landscape tablet).
- [ ] Tapping the `Insert checklist` icon inserts a checklist.
- [ ] Typing `- [x]` creates a checked checklist item.
- [ ] Typing `- [ ]` created an unchecked checklist item.
- [ ] Typing `-` creates a list.
- [ ] All list bullet types render to markdown lists.
- [ ] Added URL is linkified.
- [ ] Tapping on link shows Link bar with options to view in browser, copy, and share.

#### Tags & search

- [ ] Can filter by tag when clicking on tag in tag drawer.
- [ ] Can add tag to note and have it appear in filtered tag view when previously not in filter.
- [ ] Searching in the search field does a global search.
- [ ] Searching in the search field highlights matches in note list.
- [ ] Searching in the search field highlights matches in the note editor.
- [ ] Clearing the search field immediately updates filtered notes.
- [ ] Clicking on different tags or `All Notes` or `Trash` immediately updates filtered notes.
- [ ] Can search by keyword, with search results shown after submitting the search.
- [ ] Tag auto-completes appear when typing in search field (e.g. searching for `test` shows tag results such as `tag:test` and `tag:testing`).
- [ ] Tag suggestions suggest tags regardless of case.
- [ ] Typing `tag:` shows all tags with `tag:` prefixed.
- [ ] Search field updates with results of `tag:test` format search string.

#### Trash

- [ ] Can view trashed notes by clicking on `Trash`.
- [ ] Can restore note from trash screen.
- [ ] Can trash note.

#### Settings

- [ ] Can change analytics sharing setting.
- [ ] Changing `Condensed note list` mode immediately updates and reflects in note list.
- [ ] Changing `Sort Order` immediately updates and reflects in note list.
- [ ] Changing `Tag Sorting` immediately updates and reflects in tag list.
- [ ] For each sort type the pinned notes appear first in the note list.
- [ ] Changing `Theme` immediately updates app for desired color scheme.
- [ ] After turning on passcode lock, passcode is required to resume the app (from lock screen, from background, after force-closing it).
- [ ] Can turn passcode lock off with correct 4-digit passcode.

#### Widgets

- [ ] Adding note widget to home screen brings up note list to select the note for the widget.
- [ ] Tapping the note widget opens that note in the note editor.
- [ ] Adding note list widget to home screen shows scrollable note list.
- [ ] Tapping a note in the note list widget opens that note in the note editor.
- [ ] Resizing note list widget (with long press) shows new note button when widget is large enough.
- [ ] Tapping new note button in note list widget creates a new note and shows it in the note editor.
