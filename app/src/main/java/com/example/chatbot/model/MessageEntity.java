package com.example.chatbot.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String messages;
    public String sentBy;
    public long timestamp;

    public MessageEntity(String messages, String sentBy, long timestamp) {
        this.messages = messages;
        this.sentBy = sentBy;
        this.timestamp = timestamp;
    }
}
