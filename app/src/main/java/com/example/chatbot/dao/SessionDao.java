package com.example.chatbot.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.chatbot.model.Session;

import java.util.List;
@Dao
public interface SessionDao {
    @Insert
    long insert(Session session);

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    LiveData<List<Session>> getAllSessions();

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    void deleteSession(long sessionId);
}
