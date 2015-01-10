package com.whatscloud.logic.global;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import com.whatscloud.config.app.WhatsCloud;

public class App extends Application
{
    boolean mIsSyncing;

    public boolean isSyncing()
    {
        //---------------------------------
        // Return syncing state
        //---------------------------------

        return mIsSyncing;
    }

    public void setIsSyncing(boolean value)
    {
        //---------------------------------
        // Set syncing state
        //---------------------------------

        mIsSyncing = value;
    }

    public static int getVersionCode(Context context)
    {
        //---------------------------------
        // Attempt to get version code
        //---------------------------------

        try
        {
            //---------------------------------
            // Access package info
            //---------------------------------

            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            //---------------------------------
            // Return minimum code
            //---------------------------------

            return WhatsCloud.MINIMUM_LOGIN_VERSION_CODE;
        }
    }

    @Override
    public void onCreate()
    {
        //---------------------------------
        // Call super
        //---------------------------------

        super.onCreate();

        //---------------------------------
        // Attempt to fix Play Services bug
        // https://groups.google.com/forum/#!topic/google-admob-ads-sdk/_x12qmjWI7M
        //---------------------------------

        try
        {
            Class.forName("android.os.AsyncTask");
        }
        catch(Throwable ignore)
        {
            //---------------------------------
            // Failed to load for some reason
            //---------------------------------
        }
    }
}
