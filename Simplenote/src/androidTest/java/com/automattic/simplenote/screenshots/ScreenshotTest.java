package com.automattic.simplenote.screenshots;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.appcompat.widget.SearchView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.automattic.simplenote.BuildConfig;
import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static tools.fastlane.screengrab.cleanstatusbar.BarsMode.TRANSPARENT;
import static tools.fastlane.screengrab.cleanstatusbar.IconVisibility.SHOW;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {
    private static final int LAUNCH_TIMEOUT = 5000;

    private static final String NOTE_FOR_EDITOR_SHOT_TITLE = "Lemon Cake & Blueberry";
    private static final String NOTE_FOR_INTERLINKING_SHOT_TITLE = "# Colors";
    private static final String NOTE_FOR_INTERLINKED_NOTE_SHOT_TITLE = "Blueberry Recipes";

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @BeforeClass
    public static void beforeAll() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        new CleanStatusBar()
                .setBatteryLevel(100)
                .setClock("1231")
                .setWifiVisibility(SHOW)
                .setBarsMode(TRANSPARENT)
                .enable();
    }

    @Test
    public void screenshotTest() throws InterruptedException {
        // Pre-checks if the state is dirty
        enterThenDisablePasscodeIfNeeded();
        dismissVerifyEmailScreenIfNeeded();

        logoutIfNeeded();

        login();

        waitForNotesInNotesListToLoad();

        // I have no idea why this is the case, but something changed between February and April
        // 2021, either in our code or Fastlane, resulting in the link highlighting not rendering
        // in the first run. The only solution I found is... logging out and in again?!
        dismissVerifyEmailScreenIfNeeded();
        logoutIfNeeded();
        login();
        waitForNotesInNotesListToLoad();

        selectNoteAndTakeScreenshotFromNotesList(NOTE_FOR_EDITOR_SHOT_TITLE, "01-note", true);

        // What we'd like to do is take a screenshot of the interlinking interface with:
        //
        // takeInterNoteLinkingScreenshotFromNotesList(NOTE_FOR_INTERLINKING_SHOT_TITLE, "03-inter-note-linking");
        //
        // But that would be confusing on the Play Store page because we don't have descriptions for
        // the screenshots. So, we take a screenshot of a note with note links in the body instead.
        selectNoteAndTakeScreenshotFromNotesList(NOTE_FOR_INTERLINKED_NOTE_SHOT_TITLE, "03-inter-linked-note", false);

        loadSearchFromNotesList("Recipe");
        // Make sure the results have been rendered
        waitForViewMatching(allOf(withId(R.id.note_title), withText(NOTE_FOR_EDITOR_SHOT_TITLE)), 1000);

        if (!isPhone()) {
            // On tablet, because of the landscape setup, select a note and wait for the keyboard to dismiss
            selectNoteFromNotesList(NOTE_FOR_EDITOR_SHOT_TITLE);
            Thread.sleep(1000);
        }

        Screengrab.screenshot("05-search");

        dismissSearch();

        enableDarkModeFromNotesList();

        dismissSettings();

        // On the tablet, at this point of the flow, there is no note selected. That would make for
        // an "empty" screenshot. Select one note to make it more interesting.
        if (!isPhone()) {
            selectNoteFromNotesList(NOTE_FOR_EDITOR_SHOT_TITLE);
        }

        Screengrab.screenshot("02-notes-list");

        loadSideMenuFromNotesList();

        Screengrab.screenshot("04-tags");

        loadSettingsFromSideMenu();

        loadPasscodeSetterFromSettings();

        // Set
        typeFullPasscode();
        // Confirm
        typeFullPasscode();

        // Relaunch Simplenote to show the pin screen
        relaunchSimplenote();

        // We want only 3 numbers in the passcode screenshot. Also, as soon as we press the fourth,
        // the screen is dismissed, so we wouldn't be able to take the screenshot.
        tapPasscodeKeypad();
        tapPasscodeKeypad();
        tapPasscodeKeypad();
        Screengrab.screenshot("06-pin");
        tapPasscodeKeypad();

        loadSettingsFromNotesList();
        // Disable passcode
        loadPasscodeUnsetterFromSettings();
        typeFullPasscode();
        // Disable darkmode
        enableLightModeFromSettings();
    }

    @AfterClass
    public static void afterAll() {
        CleanStatusBar.disable();
    }

    // Flows

    private void dismissVerifyEmailScreenIfNeeded() {
        // This is quite brittle. Would be good to have a unique identifier instead.
        final String contentDescription = "Close";

        if (isViewDisplayed(getViewByContent(contentDescription))) {
            onView(withContentDescription(contentDescription)).perform(click());
        }
    }

    private void logoutIfNeeded() {
        if (!isViewDisplayed(getViewById(R.id.list_root))) {
            return;
        }

        loadSettingsFromNotesList();

        // The logout option is down at the bottom of the list, offscreen.
        scrollDownSettingsScreen();

        // Logout
        selectSettingsOption(R.string.log_out, logoutPosition);

        // Give time to the logout to finish
        waitForViewMatching(withId(R.id.button_login), 1000);
    }

    private void enterThenDisablePasscodeIfNeeded() {
        if (!isViewDisplayed(getViewById(R.id.button1))) {
            return;
        }

        typeFullPasscode();

        loadSettingsFromNotesList();
        loadPasscodeUnsetterFromSettings();
        typeFullPasscode();
        dismissSettings();
    }

    private void login() {
        getViewById(R.id.button_login).perform(click());
        getViewById(R.id.button_email).perform(click());

        getViewById(R.id.input_email)
                .perform(click())
                .perform(replaceTextInCustomInput(BuildConfig.SCREENSHOT_EMAIL))
                .perform(ViewActions.closeSoftKeyboard());

        getViewById(R.id.input_password)
                .perform(click())
                .perform(replaceTextInCustomInput(BuildConfig.SCREENSHOT_PASSWORD))
                .perform(ViewActions.closeSoftKeyboard());

        getViewById(R.id.button).perform(click());
    }

    private void relaunchSimplenote() {
        // Taken straight out of:
        // https://developer.android.com/training/testing/ui-testing/uiautomator-testing#java
        // Initialize UiDevice instance
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Start from the home screen
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        // Launch the app
        Context context = ApplicationProvider.getApplicationContext();
        String packageName = context.getPackageName();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), LAUNCH_TIMEOUT);
    }

    // Notes List Screen

    private void waitForNotesInNotesListToLoad() throws InterruptedException {
        // Not 100% sure why, but without this little sleep call, the call to wait for the note that
        // we need to interact with to be loaded in the list fails.
        //
        // Currently, this method is called right after a login, so I suspect it might be due to the
        // app changing root activity (from login to notes list) but I haven't had the time to
        // research it properly
        Thread.sleep(2000);
        dismissVerifyEmailScreenIfNeeded();
        // We want the full list of notes to be on screen when taking the screenshots, so let's wait
        // for enough notes to be on screen to be relatively confident that's happened.
        //
        // Obviously, it would be better to have something like "wait for notes to load" but I
        // wasn't able to find a way to achieve this – Gio
        List<String> noteTitles = Arrays.asList(
                NOTE_FOR_EDITOR_SHOT_TITLE,
                "Bret Victor's Quote Collection",
                NOTE_FOR_INTERLINKING_SHOT_TITLE
        );
        for (String title:noteTitles) {
            waitForViewMatching(allOf(withId(R.id.note_title), withText(title)), 5000);
        }
    }

    private void selectNoteAndTakeScreenshotFromNotesList(String noteTitle, String screenshotName, Boolean fullscrenOnTablet) {
        selectNoteFromNotesList(noteTitle);

        // It can happen that the email verification screen appears on the note editor instead of
        // the note list screen, so look for one and dismiss it if found.
        dismissVerifyEmailScreenIfNeeded();

        // On the table, we take screenshots in landscape and there's a side list view. We need a
        // different behavior depending on the device.
        if (isPhone()) {
            Screengrab.screenshot(screenshotName);
            dismissNoteEditor();
        } else {
            final String hideDescription = "Hide List";
            final String showDescription = "Show List";

            if (fullscrenOnTablet) {
                onView(withContentDescription(hideDescription)).perform(click());
                // Give time to the animation to run...
                waitForViewToBeDisplayed(withContentDescription(showDescription), 1000);
            }

            Screengrab.screenshot(screenshotName);

            if (fullscrenOnTablet) {
                onView(withContentDescription(showDescription)).perform(click());
                // Give time to the animation to run...
                waitForViewToBeDisplayed(withContentDescription(hideDescription), 1000);
            }
        }
    }

    private void selectNoteFromNotesList(String title) {
        onView(allOf(withId(R.id.note_title), withText(title))).perform(click());
    }

    private void loadSearchFromNotesList(String query) {
        // Tap the search button in the toolbar
        final int searchButtonId = R.id.menu_search;
        waitForViewMatching(withId(searchButtonId), 5000);
        onView(withId(searchButtonId)).perform(click());

        // Type the search query
        final int searchViewId = R.id.search_src_text;
        onView(withId(searchViewId)).perform(typeSearchViewText(query));
        onView(withId(searchViewId)).perform(pressImeActionButton());
    }

    private void loadSideMenuFromNotesList() {
        // There is no R.id for the menu drawer button
        final String contentDescription = "Open drawer";

        waitForViewToBeDisplayed(withContentDescription(contentDescription), 2000);
        onView(allOf(withContentDescription(contentDescription))).perform(click());
    }

    private void loadSettingsFromNotesList() {
        loadSideMenuFromNotesList();
        loadSettingsFromSideMenu();
    }

    private void enableDarkModeFromNotesList() {
        loadThemeSwitcherFromNotesList();
        // The options have no id, and I couldn't find a way to access them by their text
        onView(childAtPosition(withId(R.id.select_dialog_listview), 1)).perform(click());
    }

    private void loadThemeSwitcherFromNotesList() {
        loadSettingsFromNotesList();
        loadThemeSwitcherFromSettings();
    }

    private void takeInterNoteLinkingScreenshotFromNotesList(String noteName, String screenshotName) throws InterruptedException {
        /*
        The code in this method is rather hacky, unfortunately.

        Try as I might, I couldn't find a way to directly add text at the end fo the note.

        Selecting the note places the cursor in the center of the screen, a spot that would vary
        depending on the screen size and that we cannot use as a reference to move through the text.
        I tried synthesizing arrow key presses to go to the bottom, but because we don't know where
        the bottom is, the only way to know it to wait till the Markdown preview screen is
        displayed. From there one can go back but... the cursor is back in the middle of the text!

        The only reliable option I could come up with was creating a new note every time. By adding
        all the text in one go, we ensure that the cursor is actually at the proper location.

        The approach adds the extra overhead of having to delete the note to avoid polluting notes
        list and also emptying the trash.
        – Gio 2021/02
         */
        onView(withContentDescription("New Note")).perform(click());

        String noteText = noteName + "\n" +
                "\n" +
                // The original note has this quote wrapped in this kind of quotes: “”, but trying
                // to type them make the tests crash. They're irrelevant for the end result in the
                // screenshot, so they've been omitted.
                "Color is so much a matter of direct and immediate perception that any discussion of theory needs to be accompanied by experiments with the colors themselves.\n" +
                "\n" +
                "### Blue\n" +
                "\n" +
                "Blue is the only color which maintains its own character in all its tones it will always stay blue; whereas yellow is blackened in its shades, and fades away when lightened; red when darkened becomes brown, and diluted with white is no longer red, but another color.";

        // We need to tap everything into a single string otherwise the cursor will jump back to the
        // center of the text area and mess everything up 🤦‍♂️.
        onView(withId(R.id.note_content)).perform(typeText(noteText + "\n\n[l"));

        // Give the inter-note linking picker time to appear before taking the screenshot
        Thread.sleep(500);
        Screengrab.screenshot(screenshotName);

        onView(withContentDescription("More Options")).perform(click());

        onView(withText("Trash")).perform(click());

        // Clean up the the trash
        loadSideMenuFromNotesList();
        onView(withText("Trash")).perform(click());
        onView(withId(R.id.menu_empty_trash)).perform(click());
        onView(withText("Empty")).perform(click());

        // Go back to notes list
        loadSideMenuFromNotesList();    // The trash activity is obviously not the notes list one,
                                        // but this method works anyways
        onView(withText("All Notes")).perform(click());
    }

    // Notes Editor Screen

    private void dismissNoteEditor() {
        final String contentDescripion = "Navigate up";
        waitForViewToBeDisplayed(withContentDescription(contentDescripion), 5000);
        onView(withContentDescription(contentDescripion)).perform(click());
    }

    // Search Screen

    private void dismissSearch() {
        onView(withContentDescription("Collapse")).perform(click());
    }

    // Side Menu

    private void loadSettingsFromSideMenu() {
        // Note: I couldn't find a way to get a straight reference to the settings item, so I
        // was left with this brittle position based matching.
        ViewInteraction navigationMenuItemView = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.navigation_view),
                                        0)),
                        3),
                        isDisplayed()));
        navigationMenuItemView.perform(click());
    }

    // Settings Screen

    private int themePosition = 6;
    private int logoutPosition = 14;
    private int passcodePosition = 11;

    private void scrollUpSettingsScreen() {
        // Swipe to perform a scroll, because I couldn't get a reference to a scrollable view.
        onView(withId(R.id.preferences_container)).perform(swipeDown());
    }

    private void scrollDownSettingsScreen() {
        // Swipe to perform a scroll, because I couldn't get a reference to a scrollable view.
        onView(withId(R.id.preferences_container)).perform(swipeUp());
    }

    // Note: I couldn't find a way to get a straight reference to the item, so I was left with this
    // brittle position based matching.
    private void selectSettingsOption(Integer textId, Integer position) {
        // Also note: I had to use that withId + hasDescendant because simply using the
        // recycler view id produced multiple views.
        //
        // Inspired by https://stackoverflow.com/a/37247925/809944.
        onView(
                allOf(
                        withId(androidx.preference.R.id.recycler_view),
                        hasDescendant(withText(textId))
                )
        )
                .perform(RecyclerViewActions.actionOnItemAtPosition(position, click()));
    }

    private void loadThemeSwitcherFromSettings() {
        // Scroll up in case we're on a 7" tablet and the option is offscreen
        scrollUpSettingsScreen();
        selectSettingsOption(R.string.theme, themePosition);
    }

    private void enableLightModeFromSettings() {
        loadThemeSwitcherFromSettings();
        // The options have no id, and I couldn't find a way to access them by their text
        onView(childAtPosition(withId(R.id.select_dialog_listview), 0)).perform(click());
    }

    private void loadPasscodeSetterFromSettings() {
        if (!isPhone()) {
            // When on landscape on a 7" tablet, the option is not on screen. On the 10", scrolling
            // doesn't disrupt the flow, but on the portrait phone screen it does.
            scrollDownSettingsScreen();
        }
        selectSettingsOption(R.string.passcode_turn_on, passcodePosition);
    }

    private void loadPasscodeUnsetterFromSettings() {
        if (!isPhone()) {
            // When on landscape on a 7" tablet, the option is not on screen. On the 10", scrolling
            // doesn't disrupt the flow, but on the portrait phone screen it does.
            scrollDownSettingsScreen();
        }
        selectSettingsOption(R.string.passcode_turn_off, passcodePosition);
    }

    private void dismissSettings() {
        onView(withContentDescription("Navigate up")).perform(click());
    }

    // Passcode Screen

    private void typeFullPasscode() {
        tapPasscodeKeypad();
        tapPasscodeKeypad();
        tapPasscodeKeypad();
        tapPasscodeKeypad();
    }

    private void tapPasscodeKeypad() {
        onView(withId(R.id.button1)).perform(click());
    }

    // Utils

    private void waitForViewMatching(final Matcher<View> matcher, final long milliseconds) {
        onView(isRoot()).perform(waitForViewToBeDisplayed(matcher, milliseconds));
    }

    private ViewInteraction getViewById(Integer id) {
        return onView(allOf(ViewMatchers.withId(id), isDisplayed()));
    }

    private ViewInteraction getViewByContent(String contentDescription) {
        return onView(allOf(withContentDescription(contentDescription), isDisplayed()));
    }

    private Boolean isViewDisplayed(ViewInteraction view) {
        try {
            view.check(ViewAssertions.matches(isDisplayed()));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // The Espresso recorder generated this.
    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    // Thanks to https://stackoverflow.com/a/47412904/809944
    public static ViewAction replaceTextInCustomInput(final String text) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextInputLayout.class));
            }

            @Override
            public String getDescription() {
                return "Replace text in TextInputLayout view";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextInputLayout) view).getEditText().setText(text);
            }
        };
    }

    // Modified from https://stackoverflow.com/a/48037073/809944
    public static ViewAction typeSearchViewText(final String text) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                // Ensure that only apply if it is a SearchView.SearchAutoComplete and if it is
                // visible.
                return allOf(isDisplayed(), isAssignableFrom(SearchView.SearchAutoComplete.class));
            }

            @Override
            public String getDescription() {
                return "Change view text";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((SearchView.SearchAutoComplete) view).setText(text);
            }
        };
    }

    // Thanks to https://stackoverflow.com/a/49814995/809944
    public static ViewAction waitForViewToBeDisplayed(final int id, final long milliseconds) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Wait for a specific view with id <" + id + "> for " + milliseconds + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + milliseconds;
                final Matcher<View> viewMatcher = withId(id);

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    // Modified from https://stackoverflow.com/a/49814995/809944
    public static ViewAction waitForViewToBeDisplayed(final Matcher<View> matcher, final long milliseconds) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Wait for a view matching the given matcher for " + milliseconds + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + milliseconds;

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found matching view
                        if (matcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    // Modified from https://stackoverflow.com/a/30270939/809944
    private boolean isPhone() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mActivityTestRule.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float widthDp = displayMetrics.widthPixels / displayMetrics.density;
        float heightDp = displayMetrics.heightPixels / displayMetrics.density;
        float screenSw = Math.min(widthDp, heightDp);
        // The threshold should be 600, but on the 7 inch Emulators the value turns out to be 552.
        return screenSw < 552;
    }
}
