package com.whatscloud.logic.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.whatscloud.utils.objects.Singleton;

public class Events
{
    public static void broadcastEvent(Context context, String key)
    {
        //---------------------------------
        // Edit shared preferences
        //---------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(context).edit();

        //---------------------------------
        // Store minutes in shared settings
        //---------------------------------

        editor.putLong(key, System.currentTimeMillis());

        //---------------------------------
        // Save changes
        //---------------------------------

        editor.commit();
    }
}
