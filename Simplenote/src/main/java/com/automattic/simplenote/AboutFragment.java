package com.automattic.simplenote;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Ondrej Ruttkay on 24/03/2016.
 */
public class AboutFragment extends Fragment {

    private static final String SIMPLENOTE_BLOG_URL = "http://simplenote.com/blog";
    private static final String SIMPLENOTE_TWITTER_HANDLE = "simplenoteapp";
    private static final String SIMPLENOTE_HIRING_HANDLE = "https://automattic.com/work-with-us/";
    private static final String TWITTER_PROFILE_URL = "https://twitter.com/#!/";
    private static final String TWITTER_APP_URI = "twitter://user?screen_name=";
    private static final String PLAY_STORE_URL = "http://play.google.com/store/apps/details?id=";
    private static final String PLAY_STORE_URI = "market://details?id=";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView version = (TextView) view.findViewById(R.id.about_version);
        View blog = view.findViewById(R.id.about_blog);
        View twitter = view.findViewById(R.id.about_twitter);
        View playStore = view.findViewById(R.id.about_play_store);
        View hiring = view.findViewById(R.id.about_hiring);

        version.setText(String.format("%s %s", getString(R.string.version), BuildConfig.VERSION_NAME));

        blog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SIMPLENOTE_BLOG_URL)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
            }
        });

        twitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(TWITTER_APP_URI + SIMPLENOTE_TWITTER_HANDLE)));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(TWITTER_PROFILE_URL + SIMPLENOTE_TWITTER_HANDLE)));
                }
            }
        });

        playStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(PLAY_STORE_URI + getActivity().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(PLAY_STORE_URL + getActivity().getPackageName())));
                }
            }
        });

        hiring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SIMPLENOTE_HIRING_HANDLE)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }
}
