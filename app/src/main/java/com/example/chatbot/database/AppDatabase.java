package com.example.chatbot.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Database;

import com.example.chatbot.dao.MessageDao;
import com.example.chatbot.dao.SessionDao;
import com.example.chatbot.model.MessageEntity;
import com.example.chatbot.model.Session;

@Database(entities = {MessageEntity.class, Session.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase{
    public abstract MessageDao messageDao();
    public abstract SessionDao sessionDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "chat_database")
                            .fallbackToDestructiveMigration() // Xóa dữ liệu cũ và tạo lại bảng
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
