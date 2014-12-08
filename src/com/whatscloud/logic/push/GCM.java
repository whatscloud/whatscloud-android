package com.whatscloud.logic.push;

import android.content.Context;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.whatscloud.config.push.GCMParameters;
import com.whatscloud.utils.objects.Singleton;
import com.whatscloud.utils.strings.StringUtils;

public class GCM
{
    public static String getPushToken(Context context) throws Exception
    {
        //---------------------------------
        // Get cached push token
        //---------------------------------

        String pushToken = Singleton.getSettings(context).getString("push", "");

        //---------------------------------
        // First time? Generate it!
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(pushToken) )
        {
            //---------------------------------
            // Get an instance of GCM
            //---------------------------------

            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

            //---------------------------------
            // Try to sign up
            //---------------------------------

            pushToken = gcm.register(GCMParameters.SENDER_ID);
        }

        return pushToken;
    }
}
