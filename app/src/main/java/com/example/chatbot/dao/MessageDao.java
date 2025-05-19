package com.example.chatbot.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.chatbot.model.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageEntity message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getAllMessages();

    @Query("DELETE FROM messages")
    void deleteAll();
}
