package com.whatscloud.activities.recovery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
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
import com.whatscloud.activities.SignIn;
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.utils.strings.StringUtils;
import com.whatscloud.utils.networking.HTTP;
import com.whatscloud.ui.SoftKeyboard;
import com.whatscloud.ui.dialogs.DialogManager;
import org.json.JSONObject;

public class ResetPassword extends SherlockActivity
{
    Button mReset;
    String mCode;
    String mEmail;
    EditText mPassword;
    MenuItem mLoadingItem;
    EditText mPasswordConfirm;

    boolean mIsResettingPassword;

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
        // Get code
        //-----------------------------

        mCode = extras.getString("Code");

        //-----------------------------
        // Get e-mail
        //-----------------------------

        mEmail = extras.getString("Email");
    }

    void initializeUI()
    {
        //-----------------------------
        // Set default layout
        //-----------------------------

        setContentView(R.layout.reset_change_password);

        //-----------------------------
        // Find and cache UI elements
        //-----------------------------

        mReset = (Button)findViewById(R.id.reset);
        mPassword = (EditText)findViewById(R.id.password);
        mPasswordConfirm = (EditText)findViewById(R.id.passwordConfirm);

        //-----------------------------
        // Password transformation
        // without weird font
        //-----------------------------

        mPassword.setTransformationMethod(new PasswordTransformationMethod());
        mPasswordConfirm.setTransformationMethod(new PasswordTransformationMethod());

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

        mPasswordConfirm.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent)
            {
                //-----------------------------
                // Click on reset button
                //-----------------------------

                return mReset.performClick();
            }
        });

        //-----------------------------
        // Reset button onclick
        //-----------------------------

        mReset.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Hide the soft keyboard
                //-----------------------------

                SoftKeyboard.hide(ResetPassword.this, mPassword);

                //----------------------------
                // Not already logging in?
                //----------------------------

                if (!mIsResettingPassword)
                {
                    //-----------------------------
                    // Log in
                    //-----------------------------

                    new ResetPasswordAsync().execute();
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

        mLoadingItem = optionsMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, getString( R.string.loggingIn));

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

    void resetPassword() throws Exception
    {
        //---------------------------------
        // Get verification code
        //---------------------------------

        String passwordText = mPassword.getText().toString().trim();
        String passwordConfirmation = mPasswordConfirm.getText().toString().trim();

        //---------------------------------
        // No match?
        //---------------------------------

        if ( ! passwordText.equals( passwordConfirmation ) )
        {
            throw new Exception( getString( R.string.checkPassword ) );
        }

        //---------------------------------
        // Reset it
        //---------------------------------

        String json = HTTP.get(WhatsCloud.API_URL + "/users?do=reset_password&email=" + Uri.encode(mEmail) + "&code=" + Uri.encode(mCode) + "&password=" + Uri.encode(passwordText));

        //---------------------------------
        // Empty string - no internet
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(json) )
        {
            //---------------------------------
            // Log error
            //---------------------------------

            throw new Exception(getString(R.string.noInternetDesc));
        }

        //-----------------------------
        // Create a JSON object
        //-----------------------------

        JSONObject resetJSON = new JSONObject(json);

        //-----------------------------
        // Did we get back an error?
        //-----------------------------

        if ( json.contains( "error" ) )
        {
            //----------------------------------
            // Extract server error
            //----------------------------------

            String serverMessage = resetJSON.get("error").toString();

            //----------------------------------
            // Send it to DialogManager
            //----------------------------------

            throw new Exception( serverMessage );
        }
    }

    public class ResetPasswordAsync extends AsyncTask<String, String, Integer>
    {
        ProgressDialog mLoading;

        public ResetPasswordAsync()
        {
            //---------------------------------
            // Prevent double click
            //---------------------------------

            mIsResettingPassword = true;

            //---------------------------------
            // Show loading indicator
            //---------------------------------

            toggleProgressBarVisibility(true);

            //--------------------------------
            // Progress bar
            //--------------------------------

            mLoading = new ProgressDialog( ResetPassword.this );

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
            // Try to verify code
            //---------------------------------

            try
            {
                resetPassword();
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

            mIsResettingPassword = false;

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
                // Show dialog
                //---------------------------------

                showPasswordResetSuccessDialog();
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

    void showPasswordResetSuccessDialog()
    {
        //--------------------------------
        // Create dialog builder
        //--------------------------------

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //--------------------------------
        // Set title, message and buttons
        //--------------------------------

        builder.setTitle(R.string.success).setMessage(R.string.passwordResetSuccessfully).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                //--------------------------------
                // Show sign in screen
                //--------------------------------

                navigateToSignIn();
            }
        });

        //--------------------------------
        // Prevent cancellation
        //--------------------------------

        builder.setCancelable(false);

        //--------------------------------
        // Show the dialog
        //--------------------------------

        builder.show();
    }

    void navigateToSignIn()
    {
        //---------------------------------
        // Show sign in activity
        //---------------------------------

        startActivity(new Intent().setClass(ResetPassword.this, SignIn.class));
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
