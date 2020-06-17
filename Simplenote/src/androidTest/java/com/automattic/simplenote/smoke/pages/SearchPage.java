package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;

public class SearchPage extends BasePage {

    private static final Integer INPUT_SEARCH = R.id.search_src_text;
    private static final Integer TEXT_NOTE_TITLE = R.id.note_title;
    private static final Integer TEXT_NOTE_CONTENT = R.id.note_content;
    private static final Integer LIST_NOTES = R.id.list;

    public SearchPage search(String searchParam) {
        enterText(INPUT_SEARCH, searchParam);
        clickButton(R.id.suggestion_text, searchParam);
        return this;
    }

    /**
     * Checks for the search results that contain given searchParam. If search returns more than one data, it will check the first occurrence.
     *
     * @param searchParam can be part of the text. Checks with the containString() matcher.
     * @return current SearchPage instance
     */
    public SearchPage checkSearchResultsTitleAndContent(String searchParam) {

        elementDisplayedWithTextAtPosition(TEXT_NOTE_TITLE, searchParam, 0);
        elementDisplayedWithTextAtPosition(TEXT_NOTE_CONTENT, searchParam, 0);

        return this;
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
