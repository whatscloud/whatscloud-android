package com.whatscloud.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.sync.SyncStatus;
import com.whatscloud.logic.sync.manager.SyncManager;

public class SyncService extends IntentService
{
    public SyncService()
    {
        //---------------------------------
        // Call super with service name
        //---------------------------------

        super(SyncService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        //---------------------------------
        // Try to sync
        //---------------------------------

        sync(this);
    }

    public static void sync(Context context)
    {
        //---------------------------------
        // Are we logged in?
        //---------------------------------

        if ( User.isSignedIn(context) )
        {
            //---------------------------------
            // Already synced first time?
            //---------------------------------

            if ( User.isInitialSyncComplete(context) )
            {
                //---------------------------------
                // Not syncing right now?
                //---------------------------------

                if ( ! SyncStatus.isSyncing(context) )
                {
                    //---------------------------------
                    // Sync!
                    //---------------------------------

                    startSyncManager(context);
                }
            }
        }
    }

    public static void startSyncManager(Context context)
    {
        //--------------------------------
        // Prevent simultaneous sync
        //--------------------------------

        SyncStatus.setSyncing(context, true);

        //--------------------------------
        // Call upon our sync manager
        //--------------------------------

        SyncManager manager = new SyncManager(context, true);

        //--------------------------------
        // Actually sync!
        //--------------------------------

        try
        {
            manager.sync();
        }
        catch( Exception exc )
        {
            //--------------------------------
            // Ignore sync errors for now
            //--------------------------------

            //Log.e(Main.TAG_NAME, Exc.getMessage());
        }

        //--------------------------------
        // No longer syncing
        //--------------------------------

        SyncStatus.setSyncing(context, false);
    }
}