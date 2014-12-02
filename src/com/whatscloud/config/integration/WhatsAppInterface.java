package com.whatscloud.config.integration;

public class WhatsAppInterface
{
    public static String PACKAGE = "com.whatsapp";
    public static String MESSAGE_DB = "/data/data/" + PACKAGE + "/databases/msgstore.db";
    public static String CONTACTS_DB = "/data/data/" + PACKAGE + "/databases/wa.db";
    public static String STOP_WHATSAPP_COMMAND = "kill -9 $(ps | grep " + PACKAGE + " | awk '{ print $2 }')";
    public static String START_MESSAGING_SERVICE_COMMAND = "am startservice -n com.whatsapp/com.whatsapp.messaging.MessageService -a com.whatsapp.MessageService.START";
}
