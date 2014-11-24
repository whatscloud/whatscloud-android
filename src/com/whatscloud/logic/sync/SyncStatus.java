package com.whatscloud.logic.sync;

import android.content.Context;

import com.whatscloud.logic.global.App;

public class SyncStatus
{
    public static boolean isSyncing(Context context)
    {
        //--------------------------------
        // Get static app boolean
        //--------------------------------

        return ((App)context.getApplicationContext()).isSyncing();
    }

    public static void setSyncing(Context context, boolean value)
    {
        //--------------------------------
        // Set static app boolean
        //--------------------------------

        ((App)context.getApplicationContext()).setIsSyncing(value);
    }
}
