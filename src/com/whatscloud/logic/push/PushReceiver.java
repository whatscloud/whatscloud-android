package com.whatscloud.logic.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.bugsense.trace.BugSenseHandler;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.sync.manager.SyncManager;
import com.whatscloud.services.SendService;
import com.whatscloud.utils.strings.StringUtils;

public class PushReceiver extends BroadcastReceiver
{
    Context mContext;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        //--------------------------------
        // Save context for later
        //--------------------------------

        this.mContext = context;

        //---------------------------------
        // Initialize bug tracking
        //---------------------------------

        BugSenseHandler.initAndStartSession(context, BugSense.API_KEY);

        //--------------------------------
        // Not logged in? Stop.
        //--------------------------------

        if (!User.isSignedIn(context) )
        {
            return;
        }

        //--------------------------------
        // Handle the push
        //--------------------------------

        handlePush(context, intent);
    }

    void handlePush(Context context, Intent intent)
    {
        //---------------------------------
        // Log the push
        //---------------------------------

        Log.d(Logging.TAG_NAME, "Received push");

        //--------------------------------
        // Get do
        //--------------------------------

        String method = intent.getStringExtra("do");

        //--------------------------------
        // No method, ignore
        //--------------------------------

        if (StringUtils.stringIsNullOrEmpty(method))
        {
            return;
        }

        //--------------------------------
        // Are we resetting unread count?
        //--------------------------------

        if ( method.equals("unread") )
        {
            //--------------------------------
            // Reset unread count
            //--------------------------------

            resetUnreadCount(context);
        }
        else
        {
            //--------------------------------
            // Async and network-related
            // Let a service take care of it
            //--------------------------------

            context.startService(new Intent(context, SendService.class));
        }
    }

    void resetUnreadCount(Context context)
    {
        //--------------------------------
        // Call upon our sync manager
        //--------------------------------

        SyncManager manager = new SyncManager(context, false);

        //--------------------------------
        // Send the pending messages
        //--------------------------------

        try
        {
            manager.resetTotalUnreadCount();
        }
        catch( Exception exc )
        {
            //--------------------------------
            // Log the error
            //--------------------------------

            Log.e(Logging.TAG_NAME, exc.getMessage());
        }
    }
}
