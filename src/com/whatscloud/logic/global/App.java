package com.whatscloud.logic.global;

import android.app.Application;

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
