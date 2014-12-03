package com.whatscloud.logic.sync.integration;

import android.content.Context;
import android.util.Log;

import com.whatscloud.config.db.SQLite3;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.functionality.Sync;
import com.whatscloud.config.integration.WhatsAppInterface;
import com.whatscloud.logic.sync.db.SQLite;
import com.whatscloud.logic.root.RootCommand;
import com.whatscloud.model.Chat;
import com.whatscloud.model.ChatMessage;
import com.whatscloud.utils.strings.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class WhatsApp
{
    SQLite mSQLite;
    Context mContext;

    public WhatsApp(Context context)
    {
        //--------------------------------
        // Save context instance
        //--------------------------------

        this.mContext = context;

        //--------------------------------
        // Create database connector
        //--------------------------------

        this.mSQLite = new SQLite( context );
    }

    private void resetUnreadCount(ChatMessage message) throws Exception
    {
        //--------------------------------
        // Initialize contact hash map
        //--------------------------------

        HashMap<String, String> contact = new HashMap<String, String>();

        //--------------------------------
        // Reset unseen msg count
        //--------------------------------

        contact.put("unseen_msg_count", "0");

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        mSQLite.update("wa_contacts", contact, "jid = '" + message.jid + "'", WhatsAppInterface.CONTACTS_DB);
    }

    public void resetTotalUnreadCount() throws Exception
    {
        //--------------------------------
        // Initialize contact hash map
        //--------------------------------

        HashMap<String, String> contact = new HashMap<String, String>();

        //--------------------------------
        // Reset unseen msg count
        //--------------------------------

        contact.put("unseen_msg_count", "0");

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        mSQLite.update("wa_contacts", contact, "1 = 1", WhatsAppInterface.CONTACTS_DB);

        //--------------------------------
        // Restart WhatsApp!
        //--------------------------------

        restartWhatsApp();
    }

    private int getLastChatMessageIDByJID(String jid) throws Exception
    {
        //--------------------------------
        // Get back last id for this chat
        //--------------------------------

        String[] columns = new String[]
                {
                        "_id"
                };

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        List<HashMap<String, String>> rows = mSQLite.select(columns, "messages", "key_remote_jid = '" + jid + "' ORDER BY _id DESC LIMIT 0, 1", WhatsAppInterface.MESSAGE_DB);

        //--------------------------------
        // Get last id
        //--------------------------------

        int id = 0;

        try
        {
            id = Integer.parseInt(rows.get(0).get("_id"));
        }
        catch( Exception exc )
        {
            //--------------------------------
            // Log it
            //--------------------------------

            Log.e(Logging.TAG_NAME, "parseInt() failed in getLastChatMessageIDByJID(): " + rows.get(0).get("_id"));
        }

        //--------------------------------
        // Return id
        //--------------------------------

        return id;
    }

    public int getTotalUnreadCount() throws Exception
    {
        //--------------------------------
        // Get back last id for this chat
        //--------------------------------

        String[] columns = new String[]
                {
                        "sum(unseen_msg_count)"
                };

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        List<HashMap<String, String>> rows = mSQLite.select(columns, "wa_contacts", "1 = 1", WhatsAppInterface.CONTACTS_DB);

        //--------------------------------
        // Get unread count
        //--------------------------------

        int unread = 0;

        try
        {
            unread = Integer.parseInt(rows.get(0).get("sum(unseen_msg_count)"));
        }
        catch( Exception exc )
        {
            //--------------------------------
            // Log it
            //--------------------------------

            Log.e(Logging.TAG_NAME, "parseInt() failed in getTotalUnreadCount(): " + rows.get(0).get("_id"));
        }

        //--------------------------------
        // Return count
        //--------------------------------

        return unread;
    }

    private void updateChatLastMessageID(ChatMessage message) throws Exception
    {
        //--------------------------------
        // Get last id
        //--------------------------------

        int lastID = getLastChatMessageIDByJID(message.jid);

        //--------------------------------
        // Initialize contact hash map
        //--------------------------------

        HashMap<String, String> chat = new HashMap<String, String>();

        //--------------------------------
        // Reset unseen msg count
        //--------------------------------

        chat.put("message_table_id", lastID + "");

        //--------------------------------
        // Update sort timestamp
        // to sort WhatsApp chat list
        // according to last sent message
        //--------------------------------

        chat.put("sort_timestamp", message.timeStamp + "");

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        mSQLite.update("chat_list", chat, "key_remote_jid = '" + message.jid + "'", WhatsAppInterface.MESSAGE_DB);
    }

    private void insertMessageDB(ChatMessage message) throws Exception
    {
        //--------------------------------
        // Initialize row hash map
        //--------------------------------

        HashMap<String, String> row = new HashMap<String, String>();

        //--------------------------------
        // Add message-specific fields
        //--------------------------------

        row.put("data", message.data);
        row.put("key_remote_jid", message.jid);
        row.put("timestamp", message.timeStamp);
        row.put("key_from_me", message.fromMe + "");
        row.put("received_timestamp", message.timeStamp);

        //--------------------------------
        // Add default fields
        //--------------------------------

        row.put("key_id", getKeyID(message));
        row.put("status", "0");
        row.put("needs_push", "0");
        row.put("media_size", "0");
        row.put("origin", "0");
        row.put("recipient_count", "");
        row.put("media_wa_type", "0");
        row.put("media_duration", "0");
        row.put("send_timestamp", "-1");
        row.put("latitude", "0.0");
        row.put("longitude", "0.0");
        row.put("receipt_server_timestamp", "-1");
        row.put("receipt_device_timestamp", "-1");

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        mSQLite.insert(row, "messages", WhatsAppInterface.MESSAGE_DB);
    }

    private void doSendMessage(ChatMessage message) throws Exception
    {
        //--------------------------------
        // Execute SQL query
        //--------------------------------

        insertMessageDB(message);

        //--------------------------------
        // Reset chat unread counter
        //--------------------------------

        resetUnreadCount(message);

        //--------------------------------
        // Update last id (to display
        // the last-inserted message)
        //--------------------------------

        updateChatLastMessageID(message);
    }

    public void sendMessages(List<ChatMessage> messages) throws Exception
    {
        //--------------------------------
        // Sort by id ascending
        //--------------------------------

        Collections.sort(messages);

        //--------------------------------
        // Iterate over chat messages
        //--------------------------------

        for ( ChatMessage message : messages )
        {
            //--------------------------------
            // This function takes care of
            // everything!
            //--------------------------------

            doSendMessage(message);
        }

        //--------------------------------
        // Restart WhatsApp!
        //--------------------------------

        restartWhatsApp();
    }

    void restartWhatsApp() throws Exception
    {
        //--------------------------------
        // Stop WhatsApp!
        //--------------------------------

        RootCommand.execute(WhatsAppInterface.STOP_WHATSAPP_COMMAND);

        //--------------------------------
        // Wait 200ms
        //--------------------------------

        Thread.sleep(200);

        //--------------------------------
        // Start Messaging Service
        //--------------------------------

        RootCommand.execute(WhatsAppInterface.START_MESSAGING_SERVICE_COMMAND);
    }

    String getKeyID(ChatMessage message)
    {
        //--------------------------------
        // Get a solid id
        //--------------------------------

        return message.timeStamp + "-1";
    }

    public List<ChatMessage> getMessages(int minMessageID, String jid, int limit) throws Exception
    {
        //--------------------------------
        // Query WhatsApp's DB
        //--------------------------------

        List<ChatMessage> messages = new ArrayList<ChatMessage>();

        //--------------------------------
        // Get new messages
        //--------------------------------

        String[] columns = new String[]
                {
                        "_id",
                        "data",
                        "status",
                        "media_url",
                        "timestamp",
                        "key_from_me",
                        "media_caption",
                        "media_wa_type",
                        "remote_resource",
                        "key_remote_jid"
                };

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        List<HashMap<String, String>> rows = mSQLite.select(columns, "messages", "_id > " + minMessageID + " AND media_wa_type != 4 AND status != -1" + ((jid != null) ? " AND key_remote_jid = '" + jid + "'" : "") + " ORDER BY timestamp " + ((jid != null) ? "DESC" : "ASC") + ((limit > 0) ? " LIMIT 0, " + limit : ""), WhatsAppInterface.MESSAGE_DB);

        //--------------------------------
        // Loop over returned rows
        //--------------------------------

        for ( HashMap<String, String> row : rows )
        {
            //--------------------------------
            // Create new generic message
            //--------------------------------

            ChatMessage message = new ChatMessage();

            //--------------------------------
            // Extract integers from row
            //--------------------------------

            try
            {
                message.id = Integer.parseInt(row.get("_id"));
                message.type = Integer.parseInt(row.get("media_wa_type"));
                message.fromMe = Integer.parseInt( row.get("key_from_me") );
            }
            catch( Exception exc )
            {
                //--------------------------------
                // Log it
                //--------------------------------

                Log.e(Logging.TAG_NAME, "parseInt() failed in getMessages(): " + row.get("_id") + ", " + row.get("media_wa_type") + ", " + row.get("key_from_me"));

                //--------------------------------
                // Just skip for now
                //--------------------------------

                continue;
            }

            //--------------------------------
            // Extract strings from row
            //--------------------------------

            message.data = row.get("data");
            message.status = row.get("status");
            message.jid = row.get("key_remote_jid");
            message.mediaURL = row.get("media_url");
            message.timeStamp = row.get("timestamp");
            message.sender = row.get("remote_resource");

            //--------------------------------
            // Convert line break back
            //--------------------------------

            message.data = message.data.replace( SQLite3.LINE_BREAK_CHAR, "\n" );

            //--------------------------------
            // Sync media caption
            //--------------------------------

            if (! StringUtils.stringIsNullOrEmpty( message.mediaURL ) )
            {
                message.data = row.get("media_caption");
            }

            //--------------------------------
            // Add to list of messages
            //--------------------------------

            messages.add(message);
        }

        //--------------------------------
        // Return messages
        //--------------------------------

        return messages;
    }

    public List<Chat> getChats(int lastChatID) throws Exception
    {
        //--------------------------------
        // Query WhatsApp's DB
        //--------------------------------

        List<Chat> chats = new ArrayList<Chat>();

        //--------------------------------
        // Get new messages
        //--------------------------------

        String[] columns = new String[]
                {
                        "_id",
                        "is_whatsapp_user",
                        "jid",
                        "display_name",
                        "number",
                        "status"
                };

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        List<HashMap<String, String>> rows = mSQLite.select(columns, "wa_contacts", "_id > " + lastChatID + " ORDER BY _id ASC LIMIT 0, " + Sync.MAX_ITEMS_PER_SYNC, WhatsAppInterface.CONTACTS_DB);

        //--------------------------------
        // Loop over returned rows
        //--------------------------------

        for ( HashMap<String, String> row : rows )
        {
            //--------------------------------
            // Create new generic chat
            //--------------------------------

            Chat chat = new Chat();

            //--------------------------------
            // Extract integers from row
            //--------------------------------

            try
            {
                chat.id = Integer.parseInt(row.get("_id"));
                chat.whatsAppUser = Integer.parseInt(row.get("is_whatsapp_user"));
            }
            catch( Exception exc )
            {
                //--------------------------------
                // Log it
                //--------------------------------

                Log.e(Logging.TAG_NAME, "parseInt() failed in getChats(): " + row.get("_id") + ", " + row.get("is_whatsapp_user"));

                //--------------------------------
                // Just skip for now
                //--------------------------------

                continue;
            }

            //--------------------------------
            // Extract strings from row
            //--------------------------------

            chat.jid = row.get("jid");
            chat.name = row.get("display_name");
            chat.number = row.get("number");
            chat.status = row.get("status");

            //--------------------------------
            // Add to list of chats
            //--------------------------------

            chats.add(chat);
        }

        //--------------------------------
        // Return chats
        //--------------------------------

        return chats;
    }

    public List<Chat> getChatList() throws Exception
    {
        //--------------------------------
        // Query WhatsApp's DB
        //--------------------------------

        List<Chat> chats = new ArrayList<Chat>();

        //--------------------------------
        // Define columns
        //--------------------------------

        String[] columns = new String[]
                {
                        "_id",
                        "key_remote_jid",
                };

        //--------------------------------
        // Execute SQL query
        //--------------------------------

        List<HashMap<String, String>> rows = mSQLite.select(columns, "chat_list", "1 = 1 ORDER BY message_table_id DESC", WhatsAppInterface.MESSAGE_DB);

        //--------------------------------
        // Loop over returned rows
        //--------------------------------

        for ( HashMap<String, String> row : rows )
        {
            //--------------------------------
            // Create new generic chat
            //--------------------------------

            Chat chat = new Chat();

            //--------------------------------
            // Extract integers from row
            //--------------------------------

            try
            {
                chat.id = Integer.parseInt(row.get("_id"));
            }
            catch( Exception exc )
            {
                //--------------------------------
                // Log it
                //--------------------------------

                Log.e(Logging.TAG_NAME, "parseInt() failed in getChatList(): " + row.get("_id"));

                //--------------------------------
                // Just skip for now
                //--------------------------------

                continue;
            }

            //--------------------------------
            // Extract strings from row
            //--------------------------------

            chat.jid = row.get("key_remote_jid");

            //--------------------------------
            // Add to list of chats
            //--------------------------------

            chats.add(chat);
        }

        //--------------------------------
        // Return chats
        //--------------------------------

        return chats;
    }
}
