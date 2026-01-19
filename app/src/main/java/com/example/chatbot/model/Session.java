package com.example.chatbot.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class Session {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String previewText; //Text ngắn 50 ký tự
    public long timestamp;

    public Session(String previewText, long timestamp) {
        this.previewText = previewText;
        this.timestamp = timestamp;
    }
}
