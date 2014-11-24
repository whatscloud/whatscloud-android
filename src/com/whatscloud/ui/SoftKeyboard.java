package com.whatscloud.ui;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class SoftKeyboard
{
    public static void hide(Context context, View view)
    {
        try
        {
            //-----------------------------
            // Hide the soft keyboard
            // focused on the TargetView
            //-----------------------------

            InputMethodManager input = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            input.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        catch( Exception exc )
        {
            //-----------------------------
            // Do nothing, it's not the
            // end of the world...
            //-----------------------------
        }
    }
}
