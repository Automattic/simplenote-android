package com.automattic.simplenote;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;

import com.automattic.simplenote.utils.ThemeUtils;
import com.automattic.simplenote.widgets.TypefaceSpan;

/**
 * Created by Dan Roundhill on 6/26/13. (In Greece!)
 */
public class TagsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        ThemeUtils.setTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tags);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SpannableString title = new SpannableString(getString(R.string.edit_tags));
        title.setSpan(new TypefaceSpan(this), 0, title.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            TagsListFragment tagsListFragment = new TagsListFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.tags_container, tagsListFragment)
                    .commit();
        }
    }
}
