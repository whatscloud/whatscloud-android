package com.whatscloud.activities;

import android.annotation.SuppressLint;
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
import com.whatscloud.activities.recovery.RequestCode;
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.push.GCM;
import com.whatscloud.logic.security.AES;
import com.whatscloud.utils.strings.StringUtils;
import com.whatscloud.utils.networking.HTTP;
import com.whatscloud.ui.SoftKeyboard;
import com.whatscloud.ui.dialogs.DialogManager;
import org.json.JSONObject;

public class SignIn extends SherlockActivity
{
    Button mSignIn;
    EditText mEmail;
    TextView mReset;
    MenuItem mLoading;
    EditText mPassword;

    boolean mIsSigningIn;

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

    @SuppressLint("WrongViewCast")
    void initializeUI()
    {
        //-----------------------------
        // Set default layout
        //-----------------------------

        setContentView(R.layout.sign_in);

        //-----------------------------
        // Find and cache UI elements
        //-----------------------------

        mSignIn = (Button)findViewById(R.id.signIn);
        mEmail = (EditText)findViewById(R.id.email);
        mReset = (TextView)findViewById(R.id.reset);
        mPassword = (EditText)findViewById(R.id.password);

        //-----------------------------
        // Password transformation
        // without weird font
        //-----------------------------

        mPassword.setTypeface(Typeface.DEFAULT);
        mPassword.setTransformationMethod(new PasswordTransformationMethod());

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
                // Hide the soft keyboard
                //-----------------------------

                SoftKeyboard.hide(SignIn.this, mPassword);

                //----------------------------
                // Not already logging in?
                //----------------------------

                if (!mIsSigningIn)
                {
                    //-----------------------------
                    // Log in
                    //-----------------------------

                    new SignInAsync().execute();
                }
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
                //----------------------------
                // Not already logging in?
                //----------------------------

                if (!mIsSigningIn)
                {
                    //-----------------------------
                    // Go to reset screen
                    //-----------------------------

                    requestCode();
                }
            }
        });

        //-----------------------------
        // Password on editor action
        //-----------------------------

        mPassword.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent)
            {
                //----------------------------
                // Perform sign in
                //----------------------------

                return mSignIn.performClick();
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

        mLoading = optionsMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.loggingIn));

        //----------------------------
        // Set up the view
        //----------------------------

        mLoading.setActionView(R.layout.loading);

        //----------------------------
        // Specify the show flags
        //----------------------------

        mLoading.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        //----------------------------
        // Hide by default
        //----------------------------

        mLoading.setVisible(false);
    }

    void toggleProgressBarVisibility(boolean visibility)
    {
        //---------------------------------
        // Set loading visibility
        //---------------------------------

        if ( mLoading != null )
        {
            mLoading.setVisible(visibility);
        }
    }

    void signInScreen() throws Exception
    {
        //---------------------------------
        // Get e-mail and password
        //---------------------------------

        String emailText = Uri.encode(mEmail.getText().toString().trim());
        String passwordText = Uri.encode(mPassword.getText().toString().trim());

        //---------------------------------
        // Empty?
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(emailText) || StringUtils.stringIsNullOrEmpty(passwordText) )
        {
            //---------------------------------
            // Show sign in error
            //---------------------------------

            throw new Exception(getString(R.string.signInErrorDesc));
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

            pushToken = GCM.getPushToken(SignIn.this);
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
        // Log in
        //---------------------------------

        String signIn = HTTP.get(WhatsCloud.API_URL + "/users?do=login&email=" + emailText + "&password=" + passwordText + "&push=" + pushToken + "&platform=android" + "&encryption_test=" + encryptionTest);

        //---------------------------------
        // Empty string - no internet
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(signIn) )
        {
            //---------------------------------
            // Log error
            //---------------------------------

            throw new Exception(getString(R.string.noInternetDesc));
        }

        //-----------------------------
        // Create a JSON object
        //-----------------------------

        JSONObject signInJSON = new JSONObject(signIn);

        //-----------------------------
        // Did we get back an error?
        //-----------------------------

        if ( signIn.contains( "error" ) )
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
        // Extract user hash
        //----------------------------------

        String key = signInJSON.get("hash").toString();

        //----------------------------------
        // Success! Save all credentials
        //----------------------------------

        User.saveCredentials(SignIn.this, emailText, passwordText, key, pushToken);
    }

    public class SignInAsync extends AsyncTask<String, String, Integer>
    {
        ProgressDialog mLoading;

        public SignInAsync()
        {
            //---------------------------------
            // Prevent double click
            //---------------------------------

            mIsSigningIn = true;

            //---------------------------------
            // Show loading indicator
            //---------------------------------

            toggleProgressBarVisibility(true);

            //--------------------------------
            // Progress bar
            //--------------------------------

            mLoading = new ProgressDialog( SignIn.this );

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
                signInScreen();
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

            mIsSigningIn = false;

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

        startActivity(new Intent().setClass(SignIn.this, Main.class));

        //---------------------------------
        // Exit this activity
        //---------------------------------

        finish();
    }

    void requestCode()
    {
        //---------------------------------
        // Prepare intent
        //---------------------------------

        Intent requestIntent = new Intent();

        //---------------------------------
        // Show request code activity
        //---------------------------------

        requestIntent.setClass(SignIn.this, RequestCode.class);

        //---------------------------------
        // Send e-mail
        //---------------------------------

        requestIntent.putExtra("Email", mEmail.getText().toString());

        //---------------------------------
        // Show activity
        //---------------------------------

        startActivity(requestIntent);
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
