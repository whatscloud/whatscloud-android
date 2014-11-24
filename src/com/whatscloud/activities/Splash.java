package com.whatscloud.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.bugsense.trace.BugSenseHandler;
import com.whatscloud.R;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.logic.auth.User;
import com.whatscloud.ui.dialogs.DialogManager;

public class Splash extends SherlockActivity
{
    Button mSignUp;
    TextView mSignIn;

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

        setContentView(R.layout.splash);

        //-----------------------------
        // Find and cache UI elements
        //-----------------------------

        mSignIn = (TextView)findViewById(R.id.signIn);
        mSignUp = (Button)findViewById(R.id.signUp);

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

        mSignIn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Log in
                //-----------------------------

                navigateToSignIn();
            }
        });

        //-----------------------------
        // Sign up button onclick
        //-----------------------------

        mSignUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Sign up
                //-----------------------------

                navigateToSignUp();
            }
        });
    }

    @Override
    protected void onResume()
    {
        //-----------------------------
        // Call super
        //-----------------------------

        super.onResume();

        //-----------------------------
        // Logged in? Exit
        //-----------------------------

        if (User.isSignedIn(this))
        {
            finish();
        }
    }

    void navigateToSignUp()
    {
        //---------------------------------
        // Show sign up activity
        //---------------------------------

        startActivity(new Intent().setClass(Splash.this, SignUp.class));
    }

    void navigateToSignIn()
    {
        //---------------------------------
        // Show navigateToSignIn activity
        //---------------------------------

        startActivity(new Intent().setClass(Splash.this, SignIn.class));
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
