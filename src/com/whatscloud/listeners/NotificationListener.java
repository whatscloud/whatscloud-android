package com.whatscloud.listeners;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.integration.WhatsAppInterface;
import com.whatscloud.services.SyncOperation;
import com.whatscloud.services.SyncScheduler;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)

public class NotificationListener extends NotificationListenerService
{
    @Override
    public void onCreate()
    {
        //---------------------------------
        // Call super
        //---------------------------------

        super.onCreate();

        //---------------------------------
        // Stop using the old-fashioned
        // polling method
        //---------------------------------

        SyncScheduler.cancelScheduledSync( this );
    }

    @Override
    public void onDestroy()
    {
        //---------------------------------
        // Call super
        //---------------------------------

        super.onDestroy();

        //---------------------------------
        // Start using the old-fashioned
        // polling method
        //---------------------------------

        SyncScheduler.scheduleSync(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification)
    {
        //---------------------------------
        // We want WhatsApp notifications
        // only, ignore the rest
        //---------------------------------

        if ( ! notification.getPackageName().equals( WhatsAppInterface.PACKAGE ) )
        {
            return;
        }

        //---------------------------------
        // Log the notification
        //---------------------------------

        Log.d(Logging.TAG_NAME, "WhatsApp notification detected");

        //---------------------------------
        // Cancel scheduled sync
        // if for some reason it's active
        //---------------------------------

        SyncScheduler.cancelScheduledSync( this );

        //---------------------------------
        // Try to sync
        //---------------------------------

        SyncOperation.sync(this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}