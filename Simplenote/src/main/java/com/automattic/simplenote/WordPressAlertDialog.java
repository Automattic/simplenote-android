package com.automattic.simplenote;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;

import com.automattic.simplenote.utils.WordPressUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WordPressAlertDialog extends AlertDialog {
    protected WordPressAlertDialog(@NonNull Context context) {
        super(context);
        
        loadSites();
        setTitle("Awesome");
    }

    private void loadSites() {
        WordPressUtils.getSites(this.getContext(), new Callback() {
            @Override
            public void onFailure(final Call call, IOException e) {
                // Error

                        /*runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // For the example, you can show an error dialog or a toast
                                // on the main UI thread
                            }
                        });*/
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String resultString = response.body().string();
                    try {
                        JSONArray sites = new JSONObject(resultString).getJSONArray("sites");

                        ArrayList<String> sitesList = new ArrayList<>();
                        for(int i = 0; i < sites.length(); i++){
                            sitesList.add(sites.getJSONObject(i).getString("name"));
                        }

                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                getContext(),
                                android.R.layout.simple_list_item_1,
                                sitesList
                        );

                        getListView().post(new Runnable() {
                            @Override
                            public void run() {
                                getListView().setAdapter(arrayAdapter);
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // Do something with the response
            }
        });
    }
}
