package com.automattic.simplenote;

import android.os.Bundle;
import android.text.SpannableString;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.automattic.simplenote.utils.ThemeUtils;

import static com.automattic.simplenote.utils.DisplayUtils.disableScreenshotsIfLocked;

public class TagsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tags);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SpannableString title = new SpannableString(getString(R.string.edit_tags));

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

    @Override
    protected void onResume() {
        super.onResume();
        disableScreenshotsIfLocked(this);
    }
}
