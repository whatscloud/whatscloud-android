package com.whatscloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage implements Comparable
{
    @JsonProperty("mid")
    public int id;

    @JsonProperty("type")
    public int type;

    @JsonProperty("from_me")
    public int fromMe;

    @JsonProperty("jid")
    public String jid;

    @JsonProperty("data")
    public String data;

    @JsonProperty("sender")
    public String sender;

    @JsonProperty("status")
    public String status;

    @JsonProperty("media_url")
    public String mediaURL;

    @JsonProperty("timestamp")
    public String timeStamp;

    @Override
    public int compareTo(Object other)
    {
        return Integer.valueOf(id).compareTo(((ChatMessage)other).id);
    }

    @Override
    public String toString()
    {
        return data;
    }
}
