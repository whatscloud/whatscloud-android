package com.whatscloud.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.whatscloud.config.functionality.Sync;

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

    public static void scheduleSync(Context context)
    {
        //---------------------------------
        // Get alarm manager
        //---------------------------------

        AlarmManager manager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);

        //---------------------------------
        // Create intent linking to Service
        //---------------------------------

        Intent intent = new Intent(context, SyncOperation.class);

        //---------------------------------
        // Create pending intent
        //---------------------------------

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        //---------------------------------
        // Set repetition using interval
        //---------------------------------

        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + Sync.INTERVAL, Sync.INTERVAL, pendingIntent);
    }
}