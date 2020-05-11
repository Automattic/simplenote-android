package com.automattic.simplenote.screenshots;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.appcompat.widget.SearchView;
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
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<NotesActivity> mActivityTestRule = new ActivityTestRule<>(NotesActivity.class);

    @Test
    public void screenshotTest() throws InterruptedException {
        logoutIfNeeded();
        login();

        // Wait for notes to load.
        // TODO: this should be some kind of loop/polling code, not a dumb and fragile sleep
        Thread.sleep(5000);

        selectNoteFromNotesList();

        Screengrab.screenshot("note");

        dismissNoteEditor();

        loadSearchFromNotesList("Recipe");

        Screengrab.screenshot("search");

        dismissSearch();

        enableDarkModeFromNotesList();

        dismissSettings();

        Screengrab.screenshot("notes-list");

        loadSideMenuFromNotesList();

        Screengrab.screenshot("tags");

        dismissSideMenu();

        loadSideMenuFromNotesList();

        disableDarkModeFromNotesList();
    }

    private void selectNoteFromNotesList() {
        onView(allOf(withId(R.id.note_title), withText("# Lemon Cake & Blueberry"))).perform(click());
    }

    private void dismissNoteEditor() {
        onView(withContentDescription("Navigate up")).perform(click());
    }

    private void loadSearchFromNotesList(String query) {
        // Tap the search button in the toolbar
        onView(withId(R.id.menu_search)).perform(click());
        // Type the search query
        onView(withId(R.id.search_src_text)).perform(typeSearchViewText(query));

    }

    private void dismissSearch() {
        onView(withContentDescription("Collapse")).perform(click());
    }

    private void loadSideMenuFromNotesList() {
        // There is no R.id for the menu drawer button
        onView(allOf(withContentDescription("Open drawer"))).perform(click());
    }

    private void dismissSideMenu() {
        // Same logic as loading it actually...
        loadSideMenuFromNotesList();
    }

    private void loadSettingsFromNotesList() {
        loadSideMenuFromNotesList();

        // Tap on settings
        //
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

    private Integer themePosition = 6;
    private Integer logoutPosition = 14;

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

    private void enableDarkModeFromNotesList() {
        loadThemeSwitcherFromNotesList();
        // The options have no id, and I couldn't find a way to access them by their text
        onView(childAtPosition(withId(R.id.select_dialog_listview), 1)).perform(click());
    }

    private void disableDarkModeFromNotesList() {
        loadThemeSwitcherFromNotesList();
        // The options have no id, and I couldn't find a way to access them by their text
        onView(childAtPosition(withId(R.id.select_dialog_listview), 0)).perform(click());
    }

    private void loadThemeSwitcherFromNotesList() {
        loadSettingsFromNotesList();
        selectSettingsOption(R.string.theme, 6);
    }

    private void dismissSettings() {
        onView(withContentDescription("Navigate up")).perform(click());
    }

    private void logoutIfNeeded() throws InterruptedException {
        if (isViewDisplayed(getViewById(R.id.list_root)) == false) {
            return;
        }

        loadSettingsFromNotesList();

        // Swipe to perform a scroll (because I couldn't get a reference to a scrollable view)
        // that will reveal the logout button.
        onView(withId(R.id.preferences_container)).perform(swipeUp());

        // Logout
        selectSettingsOption(R.string.log_out, logoutPosition);

        // Give time to the logout to finish
        // TODO: this should be some kind of loop/polling code, not a dumb and fragile sleep
        Thread.sleep(3000);
    }

    private void login() {
        getViewById(R.id.button_login).perform(click());
        getViewById(R.id.button_email).perform(click());

        getViewById(R.id.input_email)
                .perform(click())
                .perform(replaceTextInCustomInput("test@example.com"))
                .perform(ViewActions.closeSoftKeyboard());

        getViewById(R.id.input_password)
                .perform(click())
                .perform(replaceTextInCustomInput("password"))
                .perform(ViewActions.closeSoftKeyboard());

        getViewById(R.id.button).perform(click());

        // This waits for the notes container to load, that is the login has been successful.
        // We still have to wait for the notes to load from the backend, though.
        waitForViewToBeDisplayed(R.id.list, 10000);
    }

    private ViewInteraction getViewById(Integer id) {
        return onView(allOf(ViewMatchers.withId(id), isDisplayed()));
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
}
