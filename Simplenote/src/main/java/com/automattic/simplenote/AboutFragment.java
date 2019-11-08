package com.automattic.simplenote;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Locale;

public class AboutFragment extends Fragment {
    private static final String URL_CONTRIBUTE = "https://github.com/Automattic/simplenote-android";
    private static final String URL_PRIVACY = "https://automattic.com/privacy";
    private static final String URL_TERMS = "https://simplenote.com/terms";
    private static final String SIMPLENOTE_BLOG_URL = "https://simplenote.com/blog";
    private static final String SIMPLENOTE_TWITTER_HANDLE = "simplenoteapp";
    private static final String SIMPLENOTE_HIRING_HANDLE = "https://automattic.com/work-with-us/";
    private static final String TWITTER_PROFILE_URL = "https://twitter.com/#!/";
    private static final String TWITTER_APP_URI = "twitter://user?screen_name=";
    private static final String PLAY_STORE_URL = "http://play.google.com/store/apps/details?id=";
    private static final String PLAY_STORE_URI = "market://details?id=";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView version = view.findViewById(R.id.about_version);
        View blog = view.findViewById(R.id.about_blog);
        View twitter = view.findViewById(R.id.about_twitter);
        View store = view.findViewById(R.id.about_store);
        View contribute = view.findViewById(R.id.about_contribute);
        View careers = view.findViewById(R.id.about_careers);
        TextView privacy = view.findViewById(R.id.about_privacy);
        TextView terms = view.findViewById(R.id.about_terms);
        TextView copyright = view.findViewById(R.id.about_copyright);

        String colorLink = Integer.toHexString(ContextCompat.getColor(requireContext(), R.color.blue_5) & 0xffffff);
        version.setText(String.format("%s %s", getString(R.string.version), BuildConfig.VERSION_NAME));
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        copyright.setText(String.format(Locale.getDefault(), getString(R.string.about_copyright), thisYear));

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

        store.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(PLAY_STORE_URI + requireActivity().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(PLAY_STORE_URL + requireActivity().getPackageName())));
                }
            }
        });

        contribute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_CONTRIBUTE)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
            }
        });

        careers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SIMPLENOTE_HIRING_HANDLE)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
            }
        });

        privacy.setText(Html.fromHtml(String.format(
            getResources().getString(R.string.link_privacy),
            "<u><span style=\"color:#",
            colorLink,
            "\">",
            "</span></u>"
        )));
        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
            }
        });

        terms.setText(Html.fromHtml(String.format(
            getResources().getString(R.string.link_terms),
            "<u><span style=\"color:#",
            colorLink,
            "\">",
            "</span></u>"
        )));
        terms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_TERMS)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }
}
