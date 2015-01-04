package com.whatscloud.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.sync.SyncStatus;
import com.whatscloud.logic.sync.manager.SyncManager;
import com.whatscloud.utils.networking.HTTP;

public class SendService extends IntentService
{
    public SendService()
    {
        //---------------------------------
        // Call super with service name
        //---------------------------------

        super("SendService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        //---------------------------------
        // Get instance to power manager
        //---------------------------------

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        //---------------------------------
        // Create a CPU wake lock
        //---------------------------------

        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WhatsCloud.PACKAGE);

        //---------------------------------
        // Acquire it
        //---------------------------------

        wakeLock.acquire();

        //---------------------------------
        // Send pending messages
        //---------------------------------

        sendPendingMessages();

        //---------------------------------
        // Release the wake lock
        //---------------------------------

        wakeLock.release();
    }

    void sendPendingMessages()
    {
        //--------------------------------
        // Log service start
        //--------------------------------

        Log.d(Logging.TAG_NAME, "Sending outgoing messages...");

        //--------------------------------
        // Currently syncing?
        // Please wait...
        //--------------------------------

        while ( SyncStatus.isSyncing(this) )
        {
            //Thread.sleep( 200 );
        }

        //--------------------------------
        // Set syncing to true to prevent
        // other process from syncing
        //--------------------------------

        SyncStatus.setSyncing(this, true);

        //--------------------------------
        // Get back pending messages
        //--------------------------------

        String responseJSON = HTTP.get(WhatsCloud.API_URL + "/messages?do=pending&key=" + User.getAPIKey(this));

        //--------------------------------
        // Log request completion
        //--------------------------------

        Log.d(Logging.TAG_NAME, "Retrieved pending messages from server");

        //--------------------------------
        // Call upon our sync manager
        //--------------------------------

        SyncManager manager = new SyncManager(this, false);

        //--------------------------------
        // Send messages
        //--------------------------------

        try
        {
            manager.sendPendingMessages(responseJSON);
        }
        catch (Exception exc)
        {
            //--------------------------------
            // Log the error
            //--------------------------------

            Log.e(Logging.TAG_NAME, exc.getMessage());
        }

        //--------------------------------
        // Set syncing to false
        //--------------------------------

        SyncStatus.setSyncing(this, false);
    }
}