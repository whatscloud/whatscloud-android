package com.whatscloud.listeners;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.integration.WhatsAppInterface;
import com.whatscloud.services.SyncService;
import com.whatscloud.receivers.SyncScheduler;

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
        // Sync every X minutes, to
        // update the unread status
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
        // Try to sync
        //---------------------------------

        SyncService.sync(this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}