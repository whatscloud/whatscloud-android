package com.whatscloud.activities.recovery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.whatscloud.R;
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.utils.strings.StringUtils;
import com.whatscloud.utils.networking.HTTP;
import com.whatscloud.ui.SoftKeyboard;
import com.whatscloud.ui.dialogs.DialogManager;
import org.json.JSONObject;

public class RequestCode extends SherlockActivity
{
    Button mNext;
    EditText mEmail;
    MenuItem mLoadingItem;

    boolean mIsRequestingCode;

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

        //-----------------------------
        // Initialize variables
        //-----------------------------

        initializeVariables();
    }

    void initializeVariables()
    {
        //-----------------------------
        // Get intent extras
        //-----------------------------

        Bundle extras = getIntent().getExtras();

        //-----------------------------
        // No extras?
        //-----------------------------

        if ( extras == null )
        {
            return;
        }

        //-----------------------------
        // Get e-mail
        //-----------------------------

        String emailText = extras.getString("Email");

        //-----------------------------
        // Set it
        //-----------------------------

        mEmail.setText(emailText);
    }

    void initializeUI()
    {
        //-----------------------------
        // Set default layout
        //-----------------------------

        setContentView(R.layout.reset_request_code);

        //-----------------------------
        // Find and cache UI elements
        //-----------------------------

        mNext = (Button)findViewById(R.id.next);
        mEmail = (EditText)findViewById(R.id.email);

        //-----------------------------
        // Set up on click listeners
        //-----------------------------

        initializeListeners();
    }

    void initializeListeners()
    {
        //-----------------------------
        // Set up IME action listener
        //-----------------------------

        mEmail.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent)
            {
                //-----------------------------
                // Click on next button
                //-----------------------------

                return mNext.performClick();
            }
        });

        //-----------------------------
        // Next button onclick
        //-----------------------------

        mNext.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Hide the soft keyboard
                //-----------------------------

                SoftKeyboard.hide(RequestCode.this, mEmail);

                //----------------------------
                // Not already logging in?
                //----------------------------

                if (!mIsRequestingCode)
                {
                    //-----------------------------
                    // Log in
                    //-----------------------------

                    new RequestCodeAsync().execute();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu optionsMenu)
    {
        //----------------------------
        // Add loading indicator
        //----------------------------

        initializeLoadingIndicator(optionsMenu);

        //----------------------------
        // Show the menu!
        //----------------------------

        return true;
    }

    void initializeLoadingIndicator(Menu optionsMenu)
    {
        //----------------------------
        // Add refresh in Action Bar
        //----------------------------

        mLoadingItem = optionsMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, StringUtils.getString(this, R.string.loggingIn));

        //----------------------------
        // Set up the view
        //----------------------------

        mLoadingItem.setActionView(R.layout.loading);

        //----------------------------
        // Specify the show flags
        //----------------------------

        mLoadingItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        //----------------------------
        // Hide by default
        //----------------------------

        mLoadingItem.setVisible(false);
    }

    void toggleProgressBarVisibility(boolean visibility)
    {
        //---------------------------------
        // Set loading visibility
        //---------------------------------

        if ( mLoadingItem != null )
        {
            mLoadingItem.setVisible(visibility);
        }
    }

    void requestCode() throws Exception
    {
        //---------------------------------
        // Get e-mail
        //---------------------------------

        String emailText = Uri.encode(mEmail.getText().toString().trim());

        //---------------------------------
        // Request code
        //---------------------------------

        String json = HTTP.get(WhatsCloud.API_URL + "/users?do=request_code&email=" + emailText);

        //---------------------------------
        // Empty string - no internet
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(json) )
        {
            //---------------------------------
            // Log error
            //---------------------------------

            throw new Exception("No Internet Connection");
        }

        //-----------------------------
        // Create a JSON object
        //-----------------------------

        JSONObject requestJSON = new JSONObject(json);

        //-----------------------------
        // Did we get back an error?
        //-----------------------------

        if ( json.contains( "error" ) )
        {
            //----------------------------------
            // Extract server error
            //----------------------------------

            String serverMessage = requestJSON.get("error").toString();

            //----------------------------------
            // Send it to DialogManager
            //----------------------------------

            throw new Exception( serverMessage );
        }
    }

    public class RequestCodeAsync extends AsyncTask<String, String, Integer>
    {
        ProgressDialog mLoading;

        public RequestCodeAsync()
        {
            //---------------------------------
            // Prevent double click
            //---------------------------------

            mIsRequestingCode = true;

            //---------------------------------
            // Show loading indicator
            //---------------------------------

            toggleProgressBarVisibility(true);

            //--------------------------------
            // Progress bar
            //--------------------------------

            mLoading = new ProgressDialog( RequestCode.this );

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
            // Try to request code!
            //---------------------------------

            try
            {
                requestCode();
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

            mIsRequestingCode = false;

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

            //---------------------------------
            // Hide loading indicator
            //---------------------------------

            toggleProgressBarVisibility(false);

            //-----------------------------------
            // Error?
            //-----------------------------------

            if ( errorCode == 0 )
            {
                //---------------------------------
                // Show password reset window
                //---------------------------------

                resetPassword();
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

    void resetPassword()
    {
        //---------------------------------
        // Prepare intent
        //---------------------------------

        Intent verifyIntent = new Intent();

        //---------------------------------
        // Show reset password activity
        //---------------------------------

        verifyIntent.setClass(RequestCode.this, VerifyCode.class);

        //---------------------------------
        // Send e-mail
        //---------------------------------

        verifyIntent.putExtra("Email", mEmail.getText().toString());

        //---------------------------------
        // Show activity
        //---------------------------------

        startActivity(verifyIntent);

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
