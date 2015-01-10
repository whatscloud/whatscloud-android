package com.whatscloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Chat
{
    @JsonProperty("ID")
    public int id;

    @JsonProperty("JID")
    public String jid;

    @JsonProperty("Name")
    public String name;

    @JsonProperty("Number")
    public String number;

    @JsonProperty("Status")
    public String status;

    @JsonProperty("Picture")
    public String picture;

    @JsonProperty("WhatsAppUser")
    public int whatsAppUser;
}
