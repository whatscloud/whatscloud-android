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
}
