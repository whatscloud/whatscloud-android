package com.whatscloud.logic.sync.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.whatscloud.R;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.app.WhatsCloud;
import com.whatscloud.config.functionality.Sync;
import com.whatscloud.logic.auth.User;
import com.whatscloud.logic.sync.integration.WhatsApp;
import com.whatscloud.logic.ui.Events;
import com.whatscloud.model.Chat;
import com.whatscloud.model.ChatMessage;
import com.whatscloud.logic.security.AES;
import com.whatscloud.utils.strings.StringUtils;
import com.whatscloud.utils.networking.HTTP;
import com.whatscloud.utils.objects.Singleton;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class SyncManager
{
    Context mContext;
    WhatsApp mWhatsApp;

    boolean mIsScheduledSync;

    public SyncManager( Context context, boolean isScheduledSync )
    {
        //--------------------------------
        // Save context instance
        //--------------------------------

        this.mContext = context;

        //--------------------------------
        // Save sync type
        //--------------------------------

        this.mIsScheduledSync = isScheduledSync;

        //--------------------------------
        // Create database connector
        //--------------------------------

        this.mWhatsApp = new WhatsApp( context );
    }

    void syncUnread() throws Exception
    {
        //--------------------------------
        // Get last synced unread count
        //--------------------------------

        int lastUnreadCount = getLastUnreadCount(mContext);

        //--------------------------------
        // Get current unread count
        //--------------------------------

        int unreadCount = mWhatsApp.getTotalUnreadCount();

        //--------------------------------
        // Did it change?
        //--------------------------------

        if ( lastUnreadCount != unreadCount )
        {
            //--------------------------------
            // Are there no more unread?
            //--------------------------------

            if ( unreadCount == 0 )
            {
                //----------------------------
                // Sync it with server
                //----------------------------

                saveUnreadCount(unreadCount);
            }
            else
            {
                //--------------------------------
                // Save last unread count
                //--------------------------------

                saveLastUnreadCount(mContext, unreadCount);
            }
        }
    }

    public void sync() throws Exception
    {
        //----------------------------
        // Sync unread count
        //----------------------------

        syncUnread();

        //----------------------------
        // Sync WhatsApp chats
        //----------------------------

        syncChats();

        //----------------------------
        // Sync WhatsApp messages
        //----------------------------

        syncMessages();
    }

    public static int getLastSyncedMessageID(Context context)
    {
        //----------------------------
        // Query SharedPreferences
        //----------------------------

        return Singleton.getSettings(context).getInt("last_message", 0);
    }

    public static int getLastSyncedChatID(Context context)
    {
        //----------------------------
        // Query SharedPreferences
        //----------------------------

        return Singleton.getSettings(context).getInt("last_chat", 0);
    }

    public static int getLastUnreadCount(Context context)
    {
        //----------------------------
        // Query SharedPreferences
        //----------------------------

        return Singleton.getSettings(context).getInt("last_unread", 0);
    }

    void initialMessagesSync() throws Exception
    {
        //--------------------------------
        // Prepare last id temp variable
        //--------------------------------

        int maxID = 0, done = 0;

        //--------------------------------
        // Send the request
        //--------------------------------

        String responseJSON = HTTP.get(WhatsCloud.API_URL + "/sync?do=reset&key=" + User.getAPIKey(mContext));

        //--------------------------------
        // No success? Stop right there!
        //--------------------------------

        if ( ! responseJSON.contains( "success" ) )
        {
            //---------------------------------
            // Return sync failed
            //---------------------------------

            throw new Exception(responseJSON);
        }

        //--------------------------------
        // Get all chats (from wa_contacts)
        //--------------------------------

        List<Chat> chats = mWhatsApp.getChatList();

        //--------------------------------
        // No chats? Halt!
        //--------------------------------

        if ( chats.size() == 0 )
        {
            throw new Exception(mContext.getString(R.string.retrieveChatsError));
        }

        //--------------------------------
        // Log the chats
        //--------------------------------

        Log.d(Logging.TAG_NAME, "Got " + chats.size() + " chats");

        //--------------------------------
        // Temporary list of messages
        // ready for syncing
        //--------------------------------

        List<ChatMessage> syncMessages = new ArrayList<ChatMessage>();

        //--------------------------------
        // For each chat, query last
        // X messages and sync them
        //--------------------------------

        for ( Chat chat : chats )
        {
            //--------------------------------
            // Get last 20 messages
            //--------------------------------

            List<ChatMessage> lastMessages = mWhatsApp.getMessages(0, chat.jid, Sync.MAX_MESSAGES_PER_CHAT_INITIAL);

            //--------------------------------
            // No messages returned? Continue
            //--------------------------------

            if ( lastMessages.size() == 0 )
            {
                continue;
            }

            //--------------------------------
            // Log the chats
            //--------------------------------

            Log.d(Logging.TAG_NAME, "Got " + lastMessages.size() + " messages for " + chat.jid);

            //--------------------------------
            // Loop over messages
            // to find the max id
            //--------------------------------

            for ( ChatMessage message : lastMessages )
            {
                //--------------------------------
                // Is it bigger than MaxID?
                //--------------------------------

                if ( message.id > maxID )
                {
                    //--------------------------------
                    // Update MaxID with new value
                    //--------------------------------

                    maxID = message.id;
                }
            }

            //--------------------------------
            // Add messages to sync list
            //--------------------------------

            syncMessages.addAll(lastMessages);

            //----------------------------
            // Increment counter
            //----------------------------

            done++;

            //--------------------------------
            // Time to sync?
            //--------------------------------

            if ( syncMessages.size() >= Sync.MAX_ITEMS_PER_SYNC || done == chats.size() )
            {
                //--------------------------------
                // Sync the list to server
                //--------------------------------

                saveMessages(syncMessages);

                //--------------------------------
                // Clear the list
                //--------------------------------

                syncMessages.clear();
            }

            //----------------------------
            // Update progress
            //----------------------------

            saveSyncProgress(done, chats.size());
        }

        //---------------------------------
        // Save last synced message id
        //---------------------------------

        saveLastMessageID(mContext, maxID);
    }

    void syncMessages() throws Exception
    {
        //--------------------------------
        // Initial sync? Special algorithm
        //--------------------------------

        if ( ! User.isInitialSyncComplete(mContext) )
        {
            initialMessagesSync();
        }
        else
        {
            //----------------------------
            // Sync until we've synced
            // all device messages!
            //----------------------------

            while ( syncMessagesBulk() > 0 );
        }
    }

    void syncChats() throws Exception
    {
        //----------------------------
        // Sync until we've synced
        // all WhatsApp chats!
        //----------------------------

        while ( syncChatsBulk() > 0 );
    }

    public void saveSyncProgress(int done, int total)
    {
        //----------------------------------
        // Get shared preferences editor
        //----------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(mContext).edit();

        //---------------------------------
        // Store in shared preferences
        //---------------------------------

        editor.putInt("sync_done", done);
        editor.putInt("sync_total", total);

        //---------------------------------
        // Save preferences
        //---------------------------------

        editor.commit();

         //---------------------------------
        // Pop event
        //---------------------------------

        Events.broadcastEvent(mContext, "SyncProgress");
    }

    public static void saveLastMessageID(Context context, int lastMessageID)
    {
        //----------------------------------
        // Get shared preferences editor
        //----------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(context).edit();

        //---------------------------------
        // Store in shared preferences
        //---------------------------------

        editor.putInt("last_message", lastMessageID);

        //---------------------------------
        // Save preferences
        //---------------------------------

        editor.commit();
    }

    public static void saveLastUnreadCount(Context context, int lastUnreadCount)
    {
        //----------------------------------
        // Get shared preferences editor
        //----------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(context).edit();

        //---------------------------------
        // Store in shared preferences
        //---------------------------------

        editor.putInt("last_unread", lastUnreadCount);

        //---------------------------------
        // Save preferences
        //---------------------------------

        editor.commit();
    }

    public static void saveLastChatID(Context context, int lastChatID)
    {
        //----------------------------------
        // Get shared preferences editor
        //----------------------------------

        SharedPreferences.Editor editor = Singleton.getSettings(context).edit();

        //---------------------------------
        // Store in shared preferences
        //---------------------------------

        editor.putInt("last_chat", lastChatID);

        //---------------------------------
        // Save preferences
        //---------------------------------

        editor.commit();
    }

    public int syncMessagesBulk() throws Exception
    {
        //--------------------------------
        // Get last synced message id
        //--------------------------------

        int lastMessageID = getLastSyncedMessageID(mContext);

        //--------------------------------
        // Get new messages
        //--------------------------------

        List<ChatMessage> newMessages = mWhatsApp.getMessages(lastMessageID, null, Sync.MAX_ITEMS_PER_SYNC);

        //--------------------------------
        // Got new messages?
        //--------------------------------

        if ( newMessages.size() > 0 )
        {
            //----------------------------
            // Send messages to server
            //----------------------------

            saveMessages(newMessages);
        }

        //----------------------------
        // Return synced messages
        //----------------------------

        return newMessages.size();
    }

    public int syncChatsBulk() throws Exception
    {
        //--------------------------------
        // Get last synced chat id
        //--------------------------------

        int lastChatID = getLastSyncedChatID(mContext);

        //--------------------------------
        // Get new chats
        //--------------------------------

        List<Chat> newChats = mWhatsApp.getChats(lastChatID);

        //--------------------------------
        // Got new chats?
        //--------------------------------

        if ( newChats.size() > 0 )
        {
            //----------------------------
            // Save them!
            //----------------------------

            saveChats(newChats);

            //---------------------------------
            // Save last synced chat id
            //---------------------------------

            saveLastChatID(mContext, newChats.get(newChats.size() - 1).id);
        }

        //----------------------------
        // Return synced chats
        //----------------------------

        return newChats.size();
    }

    public void saveMessages(List<ChatMessage> messages) throws Exception
    {
        //----------------------------
        // Traverse messages
        //----------------------------

        for ( ChatMessage message : messages )
        {
            //----------------------------
            // Encrypt message data
            //----------------------------

            message.data = AES.encrypt(message.data, mContext);

            //----------------------------
            // Encrypt media URL
            //----------------------------

            message.mediaURL = AES.encrypt(message.mediaURL, mContext);
        }

        //----------------------------
        // Prepare post data
        //----------------------------

        List<NameValuePair> postData = new ArrayList<NameValuePair>();

        //----------------------------
        // Convert messages into JSON
        //----------------------------

        postData.add(new BasicNameValuePair("messages", Singleton.getJackson().writeValueAsString(messages)));

        //--------------------------------
        // Add user's authentication key
        //--------------------------------

        postData.add(new BasicNameValuePair("key", User.getAPIKey(mContext)));

        //--------------------------------
        // Add encryption flag
        //--------------------------------

        postData.add(new BasicNameValuePair("encrypted", "1"));

        //--------------------------------
        // Scheduled sync - for unread
        //--------------------------------

        if (mIsScheduledSync)
        {
            postData.add(new BasicNameValuePair("scheduled", "1"));
        }

        //--------------------------------
        // Send the request
        //--------------------------------

        String responseJSON = HTTP.post(WhatsCloud.API_URL + "/sync?do=messages", postData);

        //--------------------------------
        // Send the pending messages
        //--------------------------------

        sendPendingMessages(responseJSON, messages.get(messages.size() - 1).id);

        //--------------------------------
        // Log the sync
        //--------------------------------

        Log.d(Logging.TAG_NAME, "Synced " + messages.size() + " messages");
    }

    public void saveUnreadCount(int unread) throws Exception
    {
        //--------------------------------
        // Send the request
        //--------------------------------

        String responseJSON = HTTP.get(WhatsCloud.API_URL + "/sync?do=unread&key=" + User.getAPIKey(mContext));

        //--------------------------------
        // No success? Stop right there!
        //--------------------------------

        if ( ! responseJSON.contains( "success" ) )
        {
            //---------------------------------
            // Return sync failed
            //---------------------------------

            throw new Exception(responseJSON);
        }

        //--------------------------------
        // Save last synced unread count
        //--------------------------------

        saveLastUnreadCount(mContext, unread);

        //--------------------------------
        // Log the sync
        //--------------------------------

        Log.d(Logging.TAG_NAME, "Synced unread count");
    }

    public void resetTotalUnreadCount() throws Exception
    {
        //--------------------------------
        // Do it via WhatsApp class
        //--------------------------------

        mWhatsApp.resetTotalUnreadCount();

        //--------------------------------
        // Save last synced unread count
        // To prevent extra request to API
        //--------------------------------

        saveLastUnreadCount(mContext, 0);
    }

    public void sendPendingMessages(String responseJSON, int lastMessageID) throws Exception
    {
        //--------------------------------
        // No Internet?
        //--------------------------------

        if ( StringUtils.stringIsNullOrEmpty(responseJSON))
        {
            throw new Exception(mContext.getString(R.string.noInternetDesc));
        }

        //----------------------------------
        // Convert JSON to object
        //----------------------------------

        List<ChatMessage> pendingMessages = Singleton.getJackson().readValue( responseJSON, new TypeReference<List<ChatMessage>>(){} );

        //--------------------------------
        // Server returned error? Parse it
        //--------------------------------

        if ( pendingMessages == null )
        {
            //---------------------------------
            // Return sync failed
            //---------------------------------

            throw new Exception(responseJSON);
        }

        //---------------------------------
        // Traverse messages
        //---------------------------------

        for ( ChatMessage message : pendingMessages )
        {
            //---------------------------------
            // Decrypt each message
            //---------------------------------

            message.data = AES.decrypt(message.data, mContext);
        }

        //---------------------------------
        // Save last synced message id
        // from previous sync
        //---------------------------------

        if ( lastMessageID > 0 )
        {
            saveLastMessageID(mContext, lastMessageID);
        }

        //----------------------------------
        // Got any messages?
        //----------------------------------

        if ( pendingMessages.size() > 0 )
        {
            //----------------------------------
            // Send them!
            //----------------------------------

            mWhatsApp.sendMessages(pendingMessages);

            //----------------------------------
            // Sync them back to server
            // (to populate the MIDs)
            //----------------------------------

            syncMessages();
        }
    }

    public void saveChats(List<Chat> chats) throws Exception
    {
        //----------------------------
        // Traverse chats
        //----------------------------

        for ( Chat chat : chats )
        {
            //----------------------------
            // Encrypt chat name
            //----------------------------

            chat.name = AES.encrypt(chat.name, mContext);

            //----------------------------
            // Encrypt chat status
            //----------------------------

            chat.status = AES.encrypt(chat.status, mContext);
        }

        //----------------------------
        // Prepare post data
        //----------------------------

        List<NameValuePair> postData = new ArrayList<NameValuePair>();

        //----------------------------
        // Convert chats into JSON
        //----------------------------

        postData.add(new BasicNameValuePair("chats", Singleton.getJackson().writeValueAsString(chats)));

        //--------------------------------
        // Add user's authentication key
        //--------------------------------

        postData.add(new BasicNameValuePair("key", User.getAPIKey(mContext)));

        //--------------------------------
        // Add encryption flag
        //--------------------------------

        postData.add(new BasicNameValuePair("encrypted", "1"));

        //--------------------------------
        // Send the request
        //--------------------------------

        String responseJSON = HTTP.post(WhatsCloud.API_URL + "/sync?do=chats", postData);

        //--------------------------------
        // No Internet?
        //--------------------------------

        if ( StringUtils.stringIsNullOrEmpty(responseJSON))
        {
            throw new Exception(mContext.getString(R.string.noInternetDesc));
        }

        //--------------------------------
        // Server returned error? Parse it
        //--------------------------------

        if ( responseJSON.contains( "error" ) )
        {
            //---------------------------------
            // Return sync failed
            //---------------------------------

            throw new Exception(responseJSON);
        }
    }
}
