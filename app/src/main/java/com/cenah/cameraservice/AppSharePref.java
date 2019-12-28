package com.cenah.cameraservice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import static android.content.Context.MODE_PRIVATE;

public class AppSharePref {

    private static final String APP_SHARED_PREFS = "Smart-City-preferences";
    private static final String SHARED_MODEL = "SHARED_MODEL";
    private static final String LAST_NOTIFICATION = "LAST_NOTIFICATION";

    private SharedPreferences appSharedPrefs;
    private SharedPreferences.Editor prefsEditor;
    private Gson gson;
    private Context context;

    @SuppressLint("CommitPrefEdits")
    public AppSharePref(Context context) {
        this.appSharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, MODE_PRIVATE);
        this.prefsEditor = appSharedPrefs.edit();
        this.context = context;
        gson = new Gson();
    }


    public void saveSharedInfo(FileModel user) {
        String json = gson.toJson(user);
        prefsEditor.putString(SHARED_MODEL, json);
        prefsEditor.commit();
    }

    public FileModel getSharedInfo() {
        String json = appSharedPrefs.getString(SHARED_MODEL, "");
        return gson.fromJson(json, FileModel.class);

    }

}
