package com.whatscloud.config.db;

import com.whatscloud.config.app.WhatsCloud;

public class SQLite3
{
    public static String SEPARATOR_CHAR = (char)006 + "";
    public static String LINE_BREAK_CHAR = (char)007 + "";
    public static String SEPARATOR_PARAM = "-separator '" + SEPARATOR_CHAR + "'";
    public static String PATH_TO_SQLITE3_BINARY = "/data/data/" + WhatsCloud.PACKAGE + "/files/sqlite3.bin";
}
