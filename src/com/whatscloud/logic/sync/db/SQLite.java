package com.whatscloud.logic.sync.db;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.whatscloud.R;
import com.whatscloud.config.db.SQLite3;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.integration.WhatsAppInterface;
import com.whatscloud.logic.root.RootCommand;
import com.whatscloud.root.RootTools;
import com.whatscloud.root.execution.Command;

import java.io.*;
import java.util.*;

public class SQLite
{
    Context mContext;

    public SQLite(Context context)
    {
        //--------------------------------
        // Save context instance
        //--------------------------------

        this.mContext = context;
    }

    public String getSQLCommand(String sql, String db)
    {
        //--------------------------------
        // Build SQL command
        //--------------------------------

        return SQLite3.PATH_TO_SQLITE3_BINARY + " " + SQLite3.SEPARATOR_PARAM + " " + db + " \"" + sql + "\"";
    }

    public List<HashMap<String, String>> select(final String[] columns, String tableName, String whereClause, String dbName) throws Exception
    {
        //--------------------------------
        // Make sure we installed binaries!
        //--------------------------------

        installBinaries();

        //--------------------------------
        // Create a list of rows
        //--------------------------------

        final List<HashMap<String, String>> rows = new ArrayList<HashMap<String, String>>();

        //--------------------------------
        // Dirty hack to fix line breaks
        //--------------------------------

        String columnsList = TextUtils.join(", ", columns).replace( "data", "replace( replace( data, X'0D', '' ), X'0A', '" + SQLite3.LINE_BREAK_CHAR + "' )" );

        //--------------------------------
        // Generate SQL statement
        //--------------------------------

        String sql = "SELECT " + columnsList + " FROM " + tableName + " WHERE " + whereClause;

        //--------------------------------
        // Execute the query
        //--------------------------------

        Command sqlCommand = new Command(0, false, getSQLCommand(sql, dbName))
        {
            @Override
            public void commandOutput(int id, String line)
            {
                //--------------------------------
                // Add row to rows list
                //--------------------------------

                rows.add(convertRowToHashMap(columns, line));
            }

            @Override
            public void commandTerminated(int id, String error)
            {
                //--------------------------------
                // Show error
                //--------------------------------

                Log.e(Logging.TAG_NAME, error);
            }

            @Override
            public void commandCompleted(int id, int exitCode)
            {}
        };

        //--------------------------------
        // Execute the command
        //--------------------------------

        RootTools.getShell(true).add(sqlCommand);

        //--------------------------------
        // Wait for it...
        //--------------------------------

        RootCommand.waitForFinish(sqlCommand);

        //--------------------------------
        // Return rows
        //--------------------------------

        return rows;
    }

    public String prepareForInsert(String data)
    {
        //--------------------------------
        // Clean and wrap with quotes
        //--------------------------------

        return "'" + data.replace("'", "''").replace("\"", "\\\"" ).replace("$", "\\$" ) + "'";
    }

    public void insert(HashMap<String, String> row, String tableName, String dbName) throws Exception
    {
        //--------------------------------
        // Make sure we installed binaries!
        //--------------------------------

        installBinaries();

        //--------------------------------
        // Escape single-quotes
        //--------------------------------

        for ( Map.Entry<String, String> column : row.entrySet() )
        {
            //--------------------------------
            // Apparently, SQLite3 escapes
            // single-quotes with another quote
            //--------------------------------

            row.put(column.getKey(), prepareForInsert(column.getValue()));
        }

        //--------------------------------
        // Convert columns to string
        //--------------------------------

        String columnsList = TextUtils.join(", ", row.keySet());

        //--------------------------------
        // Convert values to string
        //--------------------------------

        String valuesList = TextUtils.join(", ", row.values());

        //--------------------------------
        // Generate SQL statement
        //--------------------------------

        String sql = "INSERT INTO " + tableName + " (" + columnsList + ") VALUES (" + valuesList + ")";

        //--------------------------------
        // Execute the query
        //--------------------------------

        RootCommand.execute(getSQLCommand(sql, dbName));
    }

    public void update(String tableName, HashMap<String, String> row, String whereClause, String dbName) throws Exception
    {
        //--------------------------------
        // Make sure we installed binaries!
        //--------------------------------

        installBinaries();

        //--------------------------------
        // Prepare set array
        //--------------------------------

        List<String> set = new ArrayList<String>();

        //--------------------------------
        // Add items, escape single-quotes
        //--------------------------------

        for ( Map.Entry<String, String> column : row.entrySet() )
        {
            //--------------------------------
            // Apparently, SQLite3 escapes
            // single-quotes with another quote
            //--------------------------------

            set.add(column.getKey() + " = '" + column.getValue().replace("'", "''") + "'");
        }

        //--------------------------------
        // Convert to CSV string
        //--------------------------------

        String setList = TextUtils.join(", ", set);

        //--------------------------------
        // Generate SQL statement
        //--------------------------------

        String sql = "UPDATE " + tableName + " SET " + setList + " WHERE " + whereClause;

        //--------------------------------
        // Execute the query
        //--------------------------------

        RootCommand.execute(getSQLCommand(sql, dbName));
    }

    public HashMap<String, String> convertRowToHashMap(String[] columns, String line)
    {
        //--------------------------------
        // Split row into columns
        //--------------------------------

        List<String> values = new ArrayList<String>(Arrays.asList(line.split(SQLite3.SEPARATOR_CHAR)));

        //--------------------------------
        // Not enough values?
        //--------------------------------

        if ( values.size() < columns.length )
        {
            //--------------------------------
            // Add empty values
            //--------------------------------

            for ( int i = values.size(); i < columns.length; i++ )
            {
                values.add( "" );
            }
        }

        //--------------------------------
        // Create a new hash map
        //--------------------------------

        HashMap<String, String> row = new HashMap<String, String>();

        //--------------------------------
        // Add all columns to hash map
        //--------------------------------

        for ( int i = 0; i < values.size(); i++ )
        {
            row.put(columns[i], values.get(i));
        }

        //--------------------------------
        // Return row
        //--------------------------------

        return row;
    }

    public void installBinaries() throws Exception
    {
        //--------------------------------
        // Make sure we have root!
        //--------------------------------

        if (!RootTools.isRootAvailable())
        {
            throw new Exception(mContext.getString(R.string.noRootError));
        }

        //--------------------------------
        // Make sure we have permission
        //--------------------------------

        if (!RootTools.isAccessGiven())
        {
            throw new Exception(mContext.getString(R.string.noRootGrantedError));
        }

        //--------------------------------
        // Install busybox binary
        //--------------------------------

        installBinary(WhatsAppInterface.PATH_TO_BUSYBOX_BINARY, R.raw.busybox_binary);

        //--------------------------------
        // Get default sqlite3 binary
        //--------------------------------

        int resource = R.raw.sqlite3_binary_5x;

        //--------------------------------
        // Add support pre-5.x devices
        // (Pre-PIE enforcement)
        //--------------------------------

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
        {
            resource = R.raw.sqlite3_binary_4x;
        }

        //--------------------------------
        // Install sqlite3 binary
        //--------------------------------

        installBinary(SQLite3.PATH_TO_SQLITE3_BINARY, resource);
    }

    void installBinary(String path, int resource) throws Exception
    {
        //--------------------------------
        // Get path to file
        //--------------------------------

        File installationPath = new File( path );

        //--------------------------------
        // File exists?
        //--------------------------------

        if ( installationPath.exists() )
        {
            return;
        }

        //--------------------------------
        // Open file from raw assets
        //--------------------------------

        InputStream rawResource = mContext.getResources().openRawResource(resource);

        //--------------------------------
        // Create output stream
        //--------------------------------

        OutputStream internalStorage = new FileOutputStream(installationPath);

        //--------------------------------
        // Define temp length variable
        //--------------------------------

        int length;

        //--------------------------------
        // Define temporary byte buffer
        //--------------------------------

        byte[] buffer = new byte[1024];

        //--------------------------------
        // Read from raw resource
        // until we reach EOF
        //--------------------------------

        while ( ( length = rawResource.read( buffer ) ) > 0 )
        {
            //--------------------------------
            // Write to internal storage
            //--------------------------------

            internalStorage.write(buffer, 0, length);
        }

        //--------------------------------
        // Close both streams
        //--------------------------------

        rawResource.close();
        internalStorage.close();

        //--------------------------------
        // CHMOD 777 (so we can exec)
        //--------------------------------

        String chmodCommand = "chmod 777 " + installationPath.getAbsolutePath();

        //--------------------------------
        // Execute the command
        //--------------------------------

        RootCommand.execute(chmodCommand);
    }
}
