package com.whatscloud.receivers;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.functionality.Sync;
import com.whatscloud.listeners.NotificationListener;
import com.whatscloud.services.SyncService;

public class SyncScheduler extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        //---------------------------------
        // Schedule sync as per interval
        //---------------------------------

        scheduleSync(context);
    }

    static boolean isNotificationListenerActive(Context context)
    {
        //-----------------------------
        // Not using Android 4.3+?
        //-----------------------------

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            return false;
        }

        //---------------------------------
        // Get activity manager
        //---------------------------------

        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

        //---------------------------------
        // Traverse running services
        //---------------------------------

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            //---------------------------------
            // Is our listener running?
            //---------------------------------

            if (NotificationListener.class.getName().equals(service.service.getClassName()))
            {
                //---------------------------------
                // Log this
                //---------------------------------

                Log.d(Logging.TAG_NAME, "Notification listener is running");

                //---------------------------------
                // Return true
                //---------------------------------

                return true;
            }
        }

        //---------------------------------
        // No, it is not running
        //---------------------------------

        return false;
    }

    public static void scheduleSync(Context context)
    {
        //---------------------------------
        // Cancel any previous schedule
        //---------------------------------

        cancelScheduledSync(context);

        //---------------------------------
        // Default interval in case we
        // haven't enabled Notification Access
        //---------------------------------

        long interval = Sync.DEFAULT_INTERVAL;

        //---------------------------------
        // Did we enable notification
        // access in settings?
        //---------------------------------

        if ( isNotificationListenerActive(context) )
        {
            interval = Sync.LISTENER_INTERVAL;
        }

        //---------------------------------
        // Get alarm manager
        //---------------------------------

        AlarmManager manager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);

        //---------------------------------
        // Create intent linking to Service
        //---------------------------------

        Intent intent = new Intent(context, SyncService.class);

        //---------------------------------
        // Create pending intent
        //---------------------------------

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        //---------------------------------
        // Set repetition using interval
        //---------------------------------

        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval, interval, pendingIntent);

        //---------------------------------
        // Output to log
        //---------------------------------

        Log.d(Logging.TAG_NAME, "Scheduled sync (polling) every " + interval + " ms");
    }

    public static void cancelScheduledSync(Context context)
    {
        //---------------------------------
        // Get alarm manager
        //---------------------------------

        AlarmManager manager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);

        //---------------------------------
        // Create intent linking to Service
        //---------------------------------

        Intent intent = new Intent(context, SyncService.class);

        //---------------------------------
        // Create pending intent
        //---------------------------------

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        //---------------------------------
        // Set repetition using interval
        //---------------------------------

        manager.cancel(pendingIntent);

        //---------------------------------
        // Output to log
        //---------------------------------

        Log.d(Logging.TAG_NAME, "Cancelled scheduled sync (polling)");
    }
}