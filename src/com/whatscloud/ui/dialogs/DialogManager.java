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
                dialog.setTitle(StringUtils.getString(activity, R.string.syncComplete));
                dialog.setMessage(StringUtils.getString(activity, R.string.syncCompleteDesc));
                break;
            case NO_INTERNET:
                dialog.setTitle(StringUtils.getString(activity, R.string.noInternet));
                dialog.setMessage(StringUtils.getString(activity, R.string.noInternetDesc));
                break;
            case NO_WHATSAPP:
                dialog.setTitle(StringUtils.getString(activity, R.string.noWhatsApp));
                dialog.setMessage(StringUtils.getString(activity, R.string.noWhatsAppDesc));
                break;
            case SYNC_FAILED:
                dialog.setTitle(StringUtils.getString(activity, R.string.syncFailed));
                dialog.setMessage(StringUtils.getString(activity, R.string.syncFailedDesc));
                break;
            case NO_GCM:
                dialog.setTitle(StringUtils.getString(activity, R.string.gcmError));
                dialog.setMessage(StringUtils.getString(activity, R.string.gcmErrorDesc));
                break;
            default:
                dialog.setTitle(StringUtils.getString(activity, R.string.error));
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
