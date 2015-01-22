package com.whatscloud.activities.tutorial;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;
import com.bugsense.trace.BugSenseHandler;
import com.whatscloud.R;
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.ui.dialogs.DialogManager;

public class NoRoot extends SherlockActivity
{
    Button mLearnMore;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
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

        setContentView(R.layout.no_root);

        //-----------------------------
        // Cache view
        //-----------------------------

        mLearnMore = (Button)findViewById(R.id.noRootLink);

        //-----------------------------
        // Set up on click listeners
        //-----------------------------

        initializeListeners();
    }

    void initializeListeners()
    {
        //-----------------------------
        // Sign in button onclick
        //-----------------------------

        mLearnMore.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Go to market (May fail!)
                //-----------------------------

                try
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WhatsCloud.ROOT_URL)));
                }
                catch (Exception exc)
                {
                    //-----------------------------
                    // Show error dialog
                    //-----------------------------

                    showDialog(DialogManager.NO_WHATSAPP);
                }
            }
        });
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
