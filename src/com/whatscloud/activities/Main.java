package com.whatscloud.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.whatscloud.R;
import com.whatscloud.config.ads.AdMob;
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.integration.WhatsAppInterface;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.sync.SyncStatus;
import com.whatscloud.logic.sync.manager.SyncManager;
import com.whatscloud.utils.strings.StringUtils;
import com.whatscloud.utils.networking.HTTP;
import com.whatscloud.utils.objects.Singleton;
import com.whatscloud.ui.dialogs.DialogManager;
import com.whatscloud.services.SyncScheduler;
import org.json.JSONObject;

public class Main extends SherlockActivity
{
    ImageView mIcon;
    EditText mEncryptionKey;
    RelativeLayout mAdContainer;

    public static int MENU_SIGN_OUT = 1;

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

        //---------------------------------
        // Verify WhatsApp is installed
        //---------------------------------

        if ( ! isWhatsAppInstalled() )
        {
            //---------------------------------
            // Go to market
            //---------------------------------

            downloadWhatsApp();

            //---------------------------------
            // Stop execution
            //---------------------------------

            return;
        }

        //-----------------------------
        // Make sure we are logged in
        //-----------------------------

        initializeAccount();

        //-----------------------------
        // Load UI elements
        //-----------------------------

        initializeUI();

        //---------------------------------
        // Initialize advertisements
        //---------------------------------

        initializeAds();
    }

    void initializeAds()
    {
        //---------------------------------
        // Create a Smart Banner
        //---------------------------------

        AdView adView = new AdView(this, AdSize.SMART_BANNER, AdMob.UNIT_ID);

        //---------------------------------
        // Load the ad
        //---------------------------------

        adView.loadAd(new AdRequest());

        //---------------------------------
        // Remove previous ads (if any)
        //---------------------------------

        mAdContainer.removeAllViews();

        //---------------------------------
        // Add this ad view
        //---------------------------------

        mAdContainer.addView(adView);
    }

    void downloadWhatsApp()
    {
        //-----------------------------
        // Show toast
        //-----------------------------

        Toast.makeText(this, getString(R.string.installWhatsApp), Toast.LENGTH_LONG).show();

        //-----------------------------
        // Go to market (May fail!)
        //-----------------------------

        try
        {
            startActivity( new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + WhatsAppInterface.PACKAGE) ) );
        }
        catch( Exception exc )
        {
            //-----------------------------
            // Show error dialog
            //-----------------------------

            showDialog(DialogManager.NO_WHATSAPP);
        }

        //-----------------------------
        // Finish up
        //-----------------------------

        finish();
    }

    private boolean isWhatsAppInstalled()
    {
        //-----------------------------
        // Get package manager
        //-----------------------------

        PackageManager packages = getPackageManager();

        try
        {
            //-----------------------------
            // Locate package by name
            //-----------------------------

            packages.getPackageInfo(WhatsAppInterface.PACKAGE, PackageManager.GET_ACTIVITIES);

            //-----------------------------
            // If we are still here,
            // the app exists
            //-----------------------------

            return true;
        }
        catch (Exception exc)
        {
            //-----------------------------
            // Failed - no such package
            //-----------------------------

            return false;
        }
    }

    void initializeAccount()
    {
        //-----------------------------
        // Both empty? Go to sign in
        //-----------------------------

        if (! User.isSignedIn(this))
        {
            navigateToSplash();
        }
    }

    void sync()
    {
        //--------------------------------
        // Logged in?
        //--------------------------------

        if ( User.isSignedIn(this) )
        {
            //--------------------------------
            // Not syncing?
            //--------------------------------

            if ( ! SyncStatus.isSyncing(this) )
            {
                //--------------------------------
                // First time? Sync with progress
                //--------------------------------

                if ( ! User.isInitialSyncComplete(this) )
                {
                    //--------------------------------
                    // Show initial dialog
                    //--------------------------------

                    showInitialSyncDialog();
                }
                else
                {
                    //--------------------------------
                    // Sync pending outgoing messages
                    //--------------------------------

                    new syncPendingMessages().execute();

                    //--------------------------------
                    // Sync changes every 3 seconds
                    //--------------------------------

                    SyncScheduler.scheduleSync(Main.this);
                }
            }
        }
    }

    @Override
    protected void onResume()
    {
        //--------------------------------
        // Call parent function
        //--------------------------------

        super.onResume();

        //--------------------------------
        // Sync WhatsApp
        //--------------------------------

        sync();
    }

    void showInitialSyncDialog()
    {
        //--------------------------------
        // Create dialog builder
        //--------------------------------

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //--------------------------------
        // Set title, message and buttons
        //--------------------------------

        builder.setTitle(R.string.rootAccess).setMessage(R.string.rootAccessDesc).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                //--------------------------------
                // Sync asynchronously
                //--------------------------------

                new InitialSync().execute();
            }
       }).setNegativeButton(R.string.cancel, null);

        //--------------------------------
        // Prevent cancellation
        //--------------------------------

        builder.setCancelable(false);

        //--------------------------------
        // Show the dialog
        //--------------------------------

        builder.show();
    }

    public class InitialSync extends AsyncTask<Integer, String, Integer>
    {
        ProgressDialog mLoading;

        public InitialSync()
        {
            //--------------------------------
            // Prevent simultaneous sync
            //--------------------------------

            SyncStatus.setSyncing(Main.this, true);

            //-------------------------------
            // Register for changes
            //-------------------------------

            Singleton.getSettings(Main.this).registerOnSharedPreferenceChangeListener(mPropertyChanged);

            //--------------------------------
            // Progress bar
            //--------------------------------

            mLoading = new ProgressDialog( Main.this );

            //--------------------------------
            // Prevent cancel
            //--------------------------------

            mLoading.setCancelable(false);

            //--------------------------------
            // Set default message
            //--------------------------------

            mLoading.setMessage(getString(R.string.syncing));

            //--------------------------------
            // Show the progress dialog
            //--------------------------------

            mLoading.show();
        }

        @Override
        protected Integer doInBackground(Integer...parameter)
        {
            //--------------------------------
            // Call upon our sync manager
            //--------------------------------

            SyncManager manager = new SyncManager(Main.this, false);

            //--------------------------------
            // Actually sync!
            //--------------------------------

            try
            {
                manager.sync();
            }
            catch( Exception exc )
            {
                //---------------------------------
                // Set error message
                //---------------------------------

                DialogManager.setErrorMessage(getString(R.string.syncFailed) + ": " + exc.getMessage() + getString(R.string.syncFailedDesc));

                //---------------------------------
                // Return hash for unique dialog
                //---------------------------------

                return exc.getMessage().hashCode();
            }

            //--------------------------------
            // Initial sync done!
            //--------------------------------

            User.setInitialSyncComplete(Main.this, true);

            //--------------------------------
            // Success!
            //--------------------------------

            return 0;
        }

        @Override
        protected void onPostExecute(Integer errorCode)
        {
            //--------------------------------
            // No longer syncing
            //--------------------------------

            SyncStatus.setSyncing(Main.this, false);

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

            //-------------------------------
            // Unregister for changes
            //-------------------------------

            Singleton.getSettings(getBaseContext()).unregisterOnSharedPreferenceChangeListener(mPropertyChanged);

            //--------------------------------
            // Show error
            //--------------------------------

            if ( errorCode == 0 )
            {
                //--------------------------------
                // Show success
                //--------------------------------

                showDialog(DialogManager.SYNC_COMPLETE);

                //--------------------------------
                // Start syncing every X seconds
                //--------------------------------

                SyncScheduler.scheduleSync(Main.this);
            }
            else
            {
                //--------------------------------
                // Show error
                //--------------------------------

               showDialog(errorCode);
            }
        }

        SharedPreferences.OnSharedPreferenceChangeListener mPropertyChanged = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
            {
                //-------------------------------
                // Register for progress changed
                //-------------------------------

                if (key.equalsIgnoreCase("SyncProgress"))
                {
                    //-------------------------------
                    // Get done and total
                    //-------------------------------

                    int done = Singleton.getSettings(Main.this).getInt("sync_done", 0);
                    int total = Singleton.getSettings(Main.this).getInt("sync_total", 1);

                    //-------------------------------
                    // Update loading text
                    //-------------------------------

                    mLoading.setMessage(getString(R.string.syncing) + " (" + (int) (100 * (double) done / total) + "%)");
                }
            }
        };
    }

    public class SignOutAsync extends AsyncTask<Integer, String, Integer>
    {
        ProgressDialog mLoading;

        public SignOutAsync()
        {
            //--------------------------------
            // Progress bar
            //--------------------------------

            mLoading = new ProgressDialog( Main.this );

            //--------------------------------
            // Prevent cancel
            //--------------------------------

            mLoading.setCancelable(false);

            //--------------------------------
            // Set default message
            //--------------------------------

            mLoading.setMessage(getString(R.string.loggingOut));

            //--------------------------------
            // Show the progress dialog
            //--------------------------------

            mLoading.show();
        }

        @Override
        protected Integer doInBackground(Integer...parameter)
        {
            //---------------------------------
            // Try to sign out!
            //---------------------------------

            try
            {
                signOut();
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

            //--------------------------------
            // Success!
            //--------------------------------

            return 0;
        }

        @Override
        protected void onPostExecute(Integer errorCode)
        {
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
            //--------------------------------
            // Show error
            //--------------------------------

            if ( errorCode == 0 )
            {
                //----------------------------------
                // Show sign in form
                //----------------------------------

                initializeAccount();
            }
            else
            {
                //----------------------------------
                // Show error
                //----------------------------------

                showDialog(errorCode);
            }
        }

        SharedPreferences.OnSharedPreferenceChangeListener mPropertyChanged = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
            {
                //-------------------------------
                // Register for progress changed
                //-------------------------------

                if (key.equalsIgnoreCase("SyncProgress"))
                {
                    //-------------------------------
                    // Get done and total
                    //-------------------------------

                    int done = Singleton.getSettings(Main.this).getInt("sync_done", 0);
                    int total = Singleton.getSettings(Main.this).getInt("sync_total", 1);

                    //-------------------------------
                    // Update loading text
                    //-------------------------------

                    mLoading.setMessage(getString(R.string.syncing) + " (" + (int) (100 * (double) done / total) + "%)");
                }
            }
        };
    }

    public class syncPendingMessages extends AsyncTask<Integer, String, Integer>
    {
        public syncPendingMessages()
        {
            //--------------------------------
            // Prevent simultaneous sync
            //--------------------------------

            SyncStatus.setSyncing(Main.this, true);
        }

        @Override
        protected Integer doInBackground(Integer...parameter)
        {
            //--------------------------------
            // Get back pending messages
            //--------------------------------

            String responseJSON = HTTP.get(WhatsCloud.API_URL + "/messages?do=pending&key=" + User.getAPIKey(Main.this));

            //--------------------------------
            // Call upon our sync manager
            //--------------------------------

            SyncManager manager = new SyncManager(Main.this, false);

            //--------------------------------
            // Actually send the messages
            //--------------------------------

            try
            {
                manager.sendPendingMessages(responseJSON, 0);
            }
            catch( Exception exc )
            {
                //---------------------------------
                // Set error message
                //---------------------------------

                DialogManager.setErrorMessage(getString(R.string.syncFailed) + ": " + exc.getMessage() + getString(R.string.syncFailedDesc));

                //---------------------------------
                // Return hash for unique dialog
                //---------------------------------

                return exc.getMessage().hashCode();
            }

            //--------------------------------
            // Success!
            //--------------------------------

            return 0;
        }

        @Override
        protected void onPostExecute(Integer errorCode)
        {
            //--------------------------------
            // No longer syncing
            //--------------------------------

            SyncStatus.setSyncing(Main.this, false);

            //--------------------------------
            // Activity dead?
            //--------------------------------

            if ( isFinishing() )
            {
                return;
            }

            //--------------------------------
            // Show error
            //--------------------------------

            if ( errorCode != 0 )
            {
               showDialog(errorCode);
            }
        }
    }

    void navigateToSplash()
    {
        //-----------------------------
        // Start splash activity
        //-----------------------------

        startActivity(new Intent().setClass(Main.this, Splash.class));

        //-----------------------------
        // Exit this one
        //-----------------------------

        finish();
    }

    void initializeUI()
    {
        //-----------------------------
        // Set default layout
        //-----------------------------

        setContentView(R.layout.main);

        //-----------------------------
        // Get icon & ad layout
        //-----------------------------

        mIcon = (ImageView)findViewById(R.id.icon);
        mEncryptionKey = (EditText)findViewById(R.id.encryptionKey);
        mAdContainer = (RelativeLayout)findViewById(R.id.adContainer);

        //---------------------------------
        // Display encryption key
        //---------------------------------

        mEncryptionKey.setText(User.getEncryptionKey(this));

        //--------------------------------
        // Set click listeners
        //--------------------------------

        initializeListeners();
    }

    void initializeListeners()
    {
        //--------------------------------
        // Set app icon click
        //--------------------------------

        mIcon.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //--------------------------------
                // Sync WhatsApp
                //--------------------------------

                sync();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        //--------------------------------
        // Call super function
        //--------------------------------

        super.onConfigurationChanged(newConfig);

        //---------------------------------
        // Reload the Smart Banner
        //---------------------------------

        initializeAds();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        //--------------------------------
        // Create sign out menu item
        //--------------------------------

        menu.add(0, MENU_SIGN_OUT, 0, getString(R.string.signOut));

        //--------------------------------
        // Handled
        //--------------------------------

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        //--------------------------------
        // Did we sign out?
        //--------------------------------

        if (item.getItemId() == MENU_SIGN_OUT)
        {
            //--------------------------------
            // Sign out of app
            //--------------------------------

            new SignOutAsync().execute();

            //--------------------------------
            // Handle event
            //--------------------------------

            return true;
        }

        //--------------------------------
        // Unhandled event
        //--------------------------------

        return false;
    }

    void signOut() throws Exception
    {
        //--------------------------------
        // Sign out from server
        //--------------------------------

        String responseJSON = HTTP.get(WhatsCloud.API_URL + "/users?do=logout&key=" + User.getAPIKey(this));

        //---------------------------------
        // Empty string - no internet
        //---------------------------------

        if ( StringUtils.stringIsNullOrEmpty(responseJSON) )
        {
            //---------------------------------
            // Log error
            //---------------------------------

            throw new Exception(getString(R.string.noInternetDesc));
        }

        //-----------------------------
        // Create a JSON object
        //-----------------------------

        JSONObject signInJSON = new JSONObject(responseJSON);

        //-----------------------------
        // Did we get back an error?
        //-----------------------------

        if ( responseJSON.contains( "error" ) )
        {
            //----------------------------------
            // Extract server error
            //----------------------------------

            String serverMessage = signInJSON.get("error").toString();

            //--------------------------------
            // Log error, but don't stop
            //--------------------------------

            Log.e(Logging.TAG_NAME, serverMessage);
        }

        //----------------------------------
        // Reset all credentials
        //----------------------------------

        User.saveCredentials(this, null, null, null, null);
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
