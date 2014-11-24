package com.whatscloud.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;

import com.whatscloud.R;
import com.whatscloud.utils.strings.StringUtils;

public class DialogManager
{
    static String mServerMessage;

    public static final int NO_GCM = 1;
    public static final int NO_WHATSAPP = 2;
    public static final int NO_INTERNET = 3;
    public static final int SYNC_FAILED = 4;
    public static final int SYNC_COMPLETE = 5;

    public static void BuildDialog( AlertDialog dialog, int dialogID, Context activity )
    {
        //---------------------------------
        // Set text according to the
        // received dialog id
        //---------------------------------

        switch( dialogID )
        {
            case SYNC_COMPLETE:
                dialog.setIcon(android.R.color.transparent);
                dialog.setTitle(activity.getString(R.string.syncComplete));
                dialog.setMessage(activity.getString(R.string.syncCompleteDesc));
                break;
            case NO_INTERNET:
                dialog.setTitle(activity.getString(R.string.noInternet));
                dialog.setMessage(activity.getString(R.string.noInternetDesc));
                break;
            case NO_WHATSAPP:
                dialog.setTitle(activity.getString(R.string.noWhatsApp));
                dialog.setMessage(activity.getString(R.string.noWhatsAppDesc));
                break;
            case SYNC_FAILED:
                dialog.setTitle(activity.getString(R.string.syncFailed));
                dialog.setMessage(activity.getString(R.string.syncFailedDesc));
                break;
            case NO_GCM:
                dialog.setTitle(activity.getString(R.string.gcmError));
                dialog.setMessage(activity.getString(R.string.gcmErrorDesc));
                break;
            default:
                dialog.setTitle(activity.getString(R.string.error));
                dialog.setMessage(mServerMessage);
                break;
        }
    }

    public static void setErrorMessage(String message)
    {
        //---------------------------------
        // Set error message
        //---------------------------------

        mServerMessage = message;
    }
}
