package com.whatscloud.activities.tutorial;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockActivity;
import com.bugsense.trace.BugSenseHandler;
import com.whatscloud.R;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.ui.dialogs.DialogManager;

public class NotificationAccessTutorial extends SherlockActivity
{
    Button mDone;
    ImageView mLaunchNotificationAccess;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        //---------------------------------
        // Call super
        //---------------------------------

        super.onCreate(savedInstanceState);

        //---------------------------------
        // Initialize bug tracking
        //---------------------------------

        BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);

        //-----------------------------
        // Load UI elements
        //-----------------------------

        initializeUI();
    }

    void initializeUI()
    {
        //-----------------------------
        // Set default layout
        //-----------------------------

        setContentView(R.layout.notification_access_tutorial);

        //-----------------------------
        // Find and cache UI elements
        //-----------------------------

        mDone = (Button)findViewById(R.id.done);
        mLaunchNotificationAccess = (ImageView)findViewById(R.id.launchNotificationAccess);

        //-----------------------------
        // Set up on click listeners
        //-----------------------------

        initializeListeners();
    }

    void initializeListeners()
    {
        //-----------------------------
        // Set up icon listener
        //-----------------------------

        mLaunchNotificationAccess.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Open SuperSU
                //-----------------------------

                launchNotificationAccess();
            }
        });

        //-----------------------------
        // Set up button listener
        //-----------------------------

        mDone.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Just finish
                //-----------------------------

                finish();
            }
        });
    }

    void launchNotificationAccess()
    {
        try
        {
            //-----------------------------
            // Try to launch
            // the notification access page
            //-----------------------------

            Intent launchIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");

            //-----------------------------
            // Start SuperSU activity
            //-----------------------------

            startActivity( launchIntent );
        }
        catch( Exception exc )
        {
            //-----------------------------
            // In case we fail
            //-----------------------------

            showDialog(DialogManager.SUPERUSER_FAIL);
        }
    }

    @Override
    protected Dialog onCreateDialog( int resource )
    {
        //---------------------------------
        // Create a dialog with error icon
        //---------------------------------

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton(getString(R.string.ok), null)
                .create();

        //-----------------------------
        // Build dialog message
        //-----------------------------

        DialogManager.BuildDialog(dialog, resource, this);

        //-----------------------------
        // Return dialog object
        //----------------------------

        return dialog;
    }
}