package com.automattic.simplenote.smoke.pages;

import com.automattic.simplenote.R;

public class SearchPage extends BasePage {

    private static final Integer INPUT_SEARCH = R.id.search_src_text;

    public SearchPage search(String searchParam) {
        enterText(INPUT_SEARCH, searchParam);

        return this;
    }
}
