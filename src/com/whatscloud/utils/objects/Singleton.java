package com.whatscloud.utils.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Singleton
{
    static ObjectMapper mMapper;
    static Resources mResources;
    static SharedPreferences mSettings;

    public static ObjectMapper getJackson()
    {
        if ( mMapper == null )
        {
            //---------------------------------
            // Get Jackson instance
            //---------------------------------

            mMapper = new ObjectMapper();

            //---------------------------------
            // Ignore unknown properties
            //---------------------------------

            mMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        return mMapper;
    }

    public static SharedPreferences getSettings(Context context)
    {
        if ( mSettings == null )
        {
            //---------------------------------
            // Open shared preferences
            //---------------------------------

            mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        }

        return mSettings;
    }

    public static Resources getResources(Context context)
    {
        if ( mResources == null )
        {
            //---------------------------------
            // Initialize resources if
            // first time
            //---------------------------------

            mResources = context.getResources();
        }

        return mResources;
    }
}
