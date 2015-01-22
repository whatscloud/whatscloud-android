package com.whatscloud.activities.tutorial;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;
import com.bugsense.trace.BugSenseHandler;
import com.whatscloud.R;
import com.whatscloud.activities.Main;
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.security.AES;
import com.whatscloud.ui.dialogs.DialogManager;
import com.whatscloud.utils.networking.HTTP;
import com.whatscloud.utils.strings.StringUtils;

import org.json.JSONObject;

import me.pushy.sdk.Pushy;

public class DeleteAccount extends SherlockActivity
{
    Button mDelete;
    boolean mIsDeleting;

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

        setContentView(R.layout.delete_account);

        //-----------------------------
        // Cache view
        //-----------------------------

        mDelete = (Button)findViewById(R.id.delete);

        //-----------------------------
        // Set up on click listeners
        //-----------------------------

        initializeListeners();
    }

    void initializeListeners()
    {
        //-----------------------------
        // Delete button onclick
        //-----------------------------

        mDelete.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Do it
                //-----------------------------

                new DeleteAccountAsync().execute();
            }
        });
    }

    void deleteAccount() throws Exception
    {
        //---------------------------------
        // Delete account
        //---------------------------------

        String delete = HTTP.get(WhatsCloud.API_URL + "/users?do=delete&key=" + User.getAPIKey(this));

        //---------------------------------
        // Empty string - no internet
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(delete) )
        {
            //---------------------------------
            // Log error
            //---------------------------------

            throw new Exception(getString(R.string.noInternetDesc));
        }

        //-----------------------------
        // Create a JSON object
        //-----------------------------

        JSONObject signInJSON = new JSONObject(delete);

        //-----------------------------
        // Did we get back an error?
        //-----------------------------

        if ( delete.contains( "error" ) )
        {
            //----------------------------------
            // Extract server error
            //----------------------------------

            String serverMessage = signInJSON.get("error").toString();

            //----------------------------------
            // Send it to DialogManager
            //----------------------------------

            throw new Exception( serverMessage );
        }

        //----------------------------------
        // Success! Save all credentials
        //----------------------------------

        User.saveCredentials(DeleteAccount.this, "", "", "", "");
    }

    public class DeleteAccountAsync extends AsyncTask<String, String, Integer>
    {
        ProgressDialog mLoading;

        public DeleteAccountAsync()
        {
            //---------------------------------
            // Prevent double click
            //---------------------------------

            mIsDeleting = true;

            //--------------------------------
            // Progress bar
            //--------------------------------

            mLoading = new ProgressDialog( DeleteAccount.this );

            //--------------------------------
            // Prevent cancel
            //--------------------------------

            mLoading.setCancelable(false);

            //--------------------------------
            // Set default message
            //--------------------------------

            mLoading.setMessage(getString(R.string.loading));

            //--------------------------------
            // Show the progress dialog
            //--------------------------------

            mLoading.show();
        }

        @Override
        protected Integer doInBackground(String... parameters)
        {
            //---------------------------------
            // Try to log in!
            //---------------------------------

            try
            {
                deleteAccount();
            }
            catch( Exception exc )
            {
                //---------------------------------
                // Set server message
                //---------------------------------

                DialogManager.setErrorMessage(exc.getMessage());

                //---------------------------------
                // Return hash for unique dialog
                //---------------------------------

                return exc.getMessage().hashCode();
            }

            //---------------------------------
            // Success!
            //---------------------------------

            return 0;
        }

        @Override
        protected void onPostExecute(Integer errorCode)
        {
            //---------------------------------
            // No longer logging in
            //---------------------------------

            mIsDeleting = false;

            //--------------------------------
            // Activity dead?
            //--------------------------------

            if ( isFinishing() )
            {
                return;
            }

            //--------------------------------
            // Hide loading
            //--------------------------------

            if (mLoading.isShowing())
            {
                mLoading.dismiss();
            }

            //-----------------------------------
            // Error?
            //-----------------------------------

            if ( errorCode == 0 )
            {
                //---------------------------------
                // Show main window
                //---------------------------------

                mainWindow();
            }
            else
            {
                //---------------------------------
                // Show dialog
                //---------------------------------

                showDialog(errorCode);
            }
        }
    }

    void mainWindow()
    {
        //---------------------------------
        // Show main activity
        //---------------------------------

        startActivity(new Intent().setClass(DeleteAccount.this, Main.class));

        //---------------------------------
        // Exit this activity
        //---------------------------------

        finish();
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
