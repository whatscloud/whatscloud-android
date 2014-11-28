package com.whatscloud.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
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
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.push.GCM;
import com.whatscloud.logic.sync.manager.SyncManager;
import com.whatscloud.logic.security.AES;
import com.whatscloud.utils.strings.StringUtils;
import com.whatscloud.utils.networking.HTTP;
import com.whatscloud.ui.SoftKeyboard;
import com.whatscloud.ui.dialogs.DialogManager;
import org.json.JSONObject;

public class SignUp extends SherlockActivity
{
    Button mSignUp;
    EditText mEmail;
    EditText mPassword;
    MenuItem mLoadingItem;
    EditText mPasswordConfirm;

    boolean mIsSigningUp;

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

        setContentView(R.layout.sign_up);

        //-----------------------------
        // Find and cache UI elements
        //-----------------------------

        mEmail = (EditText)findViewById(R.id.email);
        mSignUp = (Button)findViewById(R.id.signUp);
        mPassword = (EditText)findViewById(R.id.password);
        mPasswordConfirm = (EditText)findViewById(R.id.passwordConfirm);

        //-----------------------------
        // Password transformation
        // without weird font
        //-----------------------------

        mPassword.setTypeface(Typeface.DEFAULT);
        mPasswordConfirm.setTypeface(Typeface.DEFAULT);

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
        // Sign up button onclick
        //-----------------------------

        mSignUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //-----------------------------
                // Hide the soft keyboard
                //-----------------------------

                SoftKeyboard.hide(SignUp.this, mPassword);

                //----------------------------
                // Not already logging in?
                //----------------------------

                if (!mIsSigningUp)
                {
                    //-----------------------------
                    // Sign up
                    //-----------------------------

                    new SignUpAsync().execute();
                }
            }
        });

        //-----------------------------
        // Password on editor action
        //-----------------------------

        mPasswordConfirm.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent)
            {
                //----------------------------
                // Sign up
                //----------------------------

                return mSignUp.performClick();
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

        mLoadingItem = optionsMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.signingUp));

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

    void signUp() throws Exception
    {
        //---------------------------------
        // Get e-mail and password
        //---------------------------------

        String emailText = Uri.encode(mEmail.getText().toString().trim());
        String passwordText = Uri.encode(mPassword.getText().toString().trim());
        String passwordConfirmation = Uri.encode(mPasswordConfirm.getText().toString().trim());

        //---------------------------------
        // Empty?
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(emailText) || StringUtils.stringIsNullOrEmpty(passwordText) || StringUtils.stringIsNullOrEmpty(passwordConfirmation) )
        {
            //---------------------------------
            // Show error
            //---------------------------------

            throw new Exception(getString(R.string.signUpErrorDesc));
        }

        //---------------------------------
        // No match?
        //---------------------------------

        if ( ! passwordText.equals( passwordConfirmation ) )
        {
            throw new Exception( getString( R.string.checkPassword ) );
        }

        //---------------------------------
        // Initialize empty variable
        //---------------------------------

        String pushToken;

        try
        {
            //---------------------------------
            // Get cached push token
            //---------------------------------

            pushToken = GCM.getPushToken(SignUp.this);
        }
        catch( Exception exc )
        {
            //---------------------------------
            // Show GCM error
            //---------------------------------

            throw new Exception(getString(R.string.gcmErrorDesc));
        }

        //---------------------------------
        // Generate random key
        //---------------------------------

        User.generateRandomEncryptionKey(this);

        //---------------------------------
        // Get a test encryption string
        // to verify it later on
        //---------------------------------

        String encryptionTest = Uri.encode(AES.encrypt("WhatsCloud", this));

        //---------------------------------
        // Encryption isn't working?
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(encryptionTest) )
        {
            //---------------------------------
            // Log error
            //---------------------------------

            throw new Exception(getString(R.string.encryptionError));
        }

        //---------------------------------
        // Sign up
        //---------------------------------

        String signUp = HTTP.get(WhatsCloud.API_URL + "/users?do=register&email=" + emailText + "&password=" + passwordText + "&push=" + pushToken + "&platform=android" + "&encryption_test=" + encryptionTest);

        //---------------------------------
        // Empty string - no internet
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(signUp) )
        {
            //---------------------------------
            // Log error
            //---------------------------------

            throw new Exception(getString(R.string.noInternetDesc));
        }

        //-----------------------------
        // Create a JSON object
        //-----------------------------

        JSONObject signUpJSON = new JSONObject(signUp);

        //-----------------------------
        // Did we get back an error?
        //-----------------------------

        if ( signUp.contains( "error" ) )
        {
            //----------------------------------
            // Extract server error
            //----------------------------------

            String serverMessage = signUpJSON.get("error").toString();

            //----------------------------------
            // Send it to DialogManager
            //----------------------------------

            throw new Exception( serverMessage );
        }

        //----------------------------------
        // Extract user hash
        //----------------------------------

        String key = signUpJSON.get("hash").toString();

        //---------------------------------
        // Reset last sent message id
        //---------------------------------

        SyncManager.saveLastMessageID(SignUp.this, 0);

        //----------------------------------
        // Success! Save all credentials
        //----------------------------------

        User.saveCredentials(SignUp.this, emailText, passwordText, key, pushToken);
    }

    public class SignUpAsync extends AsyncTask<String, String, Integer>
    {
        ProgressDialog mLoading;

        public SignUpAsync()
        {
            //---------------------------------
            // Prevent double click
            //---------------------------------

            mIsSigningUp = true;

            //---------------------------------
            // Show loading indicator
            //---------------------------------

            toggleProgressBarVisibility(true);

            //--------------------------------
            // Progress bar
            //--------------------------------

            mLoading = new ProgressDialog( SignUp.this );

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
                signUp();
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

            mIsSigningUp = false;

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
                // Show main window
                //---------------------------------

                mainScreen();
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

    void mainScreen()
    {
        //---------------------------------
        // Show main activity
        //---------------------------------

        startActivity(new Intent().setClass(SignUp.this, Main.class));

        //---------------------------------
        // Exit this activity
        //---------------------------------

        finish();
    }

    void signInScreen()
    {
        //---------------------------------
        // Show main activity
        //---------------------------------

        startActivity(new Intent().setClass(SignUp.this, SignIn.class));

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
