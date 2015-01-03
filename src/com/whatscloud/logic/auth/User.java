package com.whatscloud.logic.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.whatscloud.logic.security.AES;
import com.whatscloud.utils.objects.Singleton;
import com.whatscloud.utils.strings.StringUtils;

public class User
{
    public static boolean isInitialSyncComplete(Context context)
    {
        //----------------------------
        // Query SharedPreferences
        //----------------------------

        return Singleton.getSettings(context).getBoolean("initial_sync", false);
    }

    public static boolean isSignedIn(Context context)
    {
        //--------------------------------
        // Do we have keys stored?
        //--------------------------------

        return ! StringUtils.stringIsNullOrEmpty(getAPIKey(context)) && ! StringUtils.stringIsNullOrEmpty(getEncryptionKey(context));
    }

    public static String getAPIKey(Context context)
    {
        //--------------------------------
        // Get the stored API key
        //--------------------------------

        return Singleton.getSettings(context).getString("api_key", "");
    }

    public static void saveCredentials(Context context, String email, String password, String key, String pushToken)
    {
        //----------------------------------
        // Success! Save all credentials
        //----------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(context).edit();

        //---------------------------------
        // Store API key
        //---------------------------------

        editor.putString("api_key", key);

        //---------------------------------
        // Store user & pass
        //---------------------------------

        editor.putString("email", email);
        editor.putString("password", password);

        //---------------------------------
        // Reset sync counters
        //---------------------------------

        editor.putInt("last_chat", 0);
        editor.putInt("last_message", 0);

        //---------------------------------
        // Reset initial sync status
        //---------------------------------

        editor.putBoolean("initial_sync", false);

        //---------------------------------
        // Save preferences
        //---------------------------------

        editor.commit();
    }

    public static void setInitialSyncComplete(Context context, boolean value)
    {
        //---------------------------------
        // Edit shared preferences
        //---------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(context).edit();

        //---------------------------------
        // Store value in shared preferences
        //---------------------------------

        editor.putBoolean("initial_sync", value);

        //---------------------------------
        // Save changes
        //---------------------------------

        editor.commit();
    }

    public static String getEncryptionKey(Context context)
    {
        //--------------------------------
        // Get the stored enc. key
        //--------------------------------

        return Singleton.getSettings(context).getString("encryption_key", "");
    }

    public static void generateRandomEncryptionKey(Context context)
    {
        //----------------------------------
        // Get preference editor
        //----------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(context).edit();

        //---------------------------------
        // Generate random encryption key
        // and store it
        //---------------------------------

        editor.putString("encryption_key", AES.getRandomEncryptionKey());

        //---------------------------------
        // Save preferences
        //---------------------------------

        editor.commit();
    }
}
