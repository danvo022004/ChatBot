package com.example.chatbot.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages",
        foreignKeys = @ForeignKey(entity = Session.class,
                parentColumns = "id",
                childColumns = "sessionId",
                onDelete = ForeignKey.CASCADE))
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public String messages;
    public String sentBy;
    public long timestamp;

    public MessageEntity(long sessionId, String messages, String sentBy, long timestamp) {
        this.sessionId = sessionId;
        this.messages = messages;
        this.sentBy = sentBy;
        this.timestamp = timestamp;
    }
}
