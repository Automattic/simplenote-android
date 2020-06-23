package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;
import com.automattic.simplenote.smoke.utils.TestUtils;

public class SearchPage extends BasePage {

    private static final Integer INPUT_SEARCH = R.id.search_src_text;
    private static final Integer TEXT_NOTE_TITLE = R.id.note_title;
    private static final Integer TEXT_NOTE_CONTENT = R.id.note_content;
    private static final Integer TEXT_SUGGESTION = R.id.suggestion_text;
    private static final Integer BUTTON_CLOSE_SEARCH = R.id.search_close_btn;

    public SearchPage search(String searchParam) {
        enterText(INPUT_SEARCH, searchParam);
        clickButton(TEXT_SUGGESTION, searchParam);
        return this;
    }

    /**
     * Checks for the search results that contain given searchParam. If search returns more than one data, it will check the first occurrence.
     *
     * @param searchParam can be part of the text. Checks with the containString() matcher.
     * @return current SearchPage instance
     */
    public SearchPage checkSearchResultsTitleAndContent(String searchParam) {

        checkElementDisplayedWithTextAtPosition(TEXT_NOTE_TITLE, searchParam, 0);
        checkElementDisplayedWithTextAtPosition(TEXT_NOTE_CONTENT, searchParam, 0);

        return this;
    }

    public MainPage cancelSearchAndGoBack() {
        clickButton(BUTTON_CLOSE_SEARCH);
        TestUtils.idleForAShortPeriod();
        pressBack();
        pressBack();

        return new MainPage();
    }

    /*
    // Sample code https://medium.com/mindorks/some-useful-custom-espresso-matchers-in-android-33f6b9ca2240
    public SearchPage checkSearchResultsColors() {
        // R.attr.listSearchHighlightBackgroundColor
        // listSearchHighlightForegroundColor
        // textView.check(matches(textViewTextColorMatcher(R.attr.listSearchHighlightForegroundColor)));
        // textView.check(matches(allOf(withText(containsString("Corona")), textViewTextColorMatcher(R.attr.listSearchHighlightForegroundColor))));

        // checkTextOnViews(TEXT_NOTE_TITLE, searchParam);
    }
    */
}
