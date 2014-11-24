package com.whatscloud.ui.dialogs;

public class DialogException extends Exception
{
    int mDialogID;

    public DialogException( int dialogID )
    {
        //---------------------------------
        // Set dialog ID
        //---------------------------------

        this.mDialogID = dialogID;
    }

    public int getDialogID()
    {
        //---------------------------------
        // Return dialog ID
        //---------------------------------

        return mDialogID;
    }
}
