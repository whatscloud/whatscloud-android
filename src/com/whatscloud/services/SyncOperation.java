package com.whatscloud.services;

import android.app.IntentService;
import android.content.Intent;

import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.sync.SyncStatus;
import com.whatscloud.logic.sync.manager.SyncManager;

public class SyncOperation extends IntentService
{
    public SyncOperation()
    {
        //---------------------------------
        // Call super with service name
        //---------------------------------

        super("SyncOperation");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        //---------------------------------
        // Are we logged in?
        //---------------------------------

        if ( User.isSignedIn(this) )
        {
            //---------------------------------
            // Already synced first time?
            //---------------------------------

            if ( User.isInitialSyncComplete(this) )
            {
                //---------------------------------
                // Not syncing right now?
                //---------------------------------

                if ( ! SyncStatus.isSyncing(this) )
                {
                    //---------------------------------
                    // Sync!
                    //---------------------------------

                    sync();
                }
            }
        }
    }

    void sync()
    {
        //--------------------------------
        // Prevent simultaneous sync
        //--------------------------------

        SyncStatus.setSyncing(this, true);

        //--------------------------------
        // Call upon our sync manager
        //--------------------------------

        SyncManager manager = new SyncManager(this, true);

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
            // Log the error
            //--------------------------------

            //Log.e(Main.TAG_NAME, Exc.getMessage());
        }

        //--------------------------------
        // No longer syncing
        //--------------------------------

        SyncStatus.setSyncing(this, false);
    }
}