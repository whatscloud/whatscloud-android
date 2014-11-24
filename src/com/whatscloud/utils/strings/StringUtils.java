package com.whatscloud.utils.strings;

import android.content.Context;

import com.whatscloud.utils.objects.Singleton;

public class StringUtils
{
    public static boolean stringIsNullOrEmpty(String input)
    {
        //---------------------------------
        // String is null? return true
        //---------------------------------

        if ( input == null )
        {
            return true;
        }

        //---------------------------------
        // String is empty? true
        //---------------------------------

        if ( input.trim().equals("") )
        {
            return true;
        }

        //---------------------------------
        // String is not empty
        //---------------------------------

        return false;
    }
}
