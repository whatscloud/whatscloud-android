package com.whatscloud.logic.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.logic.global.App;
import com.whatscloud.logic.security.AES;
import com.whatscloud.utils.objects.Singleton;
import com.whatscloud.utils.strings.StringUtils;

import me.pushy.sdk.BuildConfig;

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
        // Outdated login?
        //--------------------------------

        if (getLoginVersionCode(context) < WhatsCloud.MINIMUM_LOGIN_VERSION_CODE)
        {
            return false;
        }

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

    public static int getLoginVersionCode(Context context)
    {
        //--------------------------------
        // Get the stored login version
        //--------------------------------

        return Singleton.getSettings(context).getInt("login_version_code", 0);
    }

    public static void saveCredentials(Context context, String email, String password, String key, String pushToken)
    {
        //----------------------------------
        // Get preferences
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
        // Store current version
        //---------------------------------

        editor.putInt("login_version_code", App.getVersionCode(context));

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
