package com.whatscloud.config.integration;

import android.os.Environment;

import com.whatscloud.config.app.WhatsCloud;

public class WhatsAppInterface
{
    public static String PACKAGE = "com.whatsapp";
    public static String MESSAGE_DB = "/data/data/" + PACKAGE + "/databases/msgstore.db";
    public static String CONTACTS_DB = "/data/data/" + PACKAGE + "/databases/wa.db";
    public static String PATH_TO_BUSYBOX_BINARY = "/data/data/" + WhatsCloud.PACKAGE + "/files/busybox.bin";
    public static String STOP_WHATSAPP_COMMAND = PATH_TO_BUSYBOX_BINARY + " killall " + PACKAGE;
    public static String WHATSAPP_PICTURES_SOURCE_FOLDER = "/data/data/" + PACKAGE + "/files/Avatars";
    public static String WHATSAPP_PICTURES_DESTINATION_FOLDER = Environment.getExternalStorageDirectory() + "/WhatsApp/Avatars";
    public static String START_MESSAGING_SERVICE_COMMAND = "am startservice -n com.whatsapp/com.whatsapp.messaging.MessageService -a com.whatsapp.MessageService.START";
}
