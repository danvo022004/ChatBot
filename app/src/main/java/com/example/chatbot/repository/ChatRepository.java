package com.example.chatbot.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.chatbot.dao.MessageDao;
import com.example.chatbot.dao.SessionDao;
import com.example.chatbot.database.AppDatabase;
import com.example.chatbot.model.Message;
import com.example.chatbot.model.MessageEntity;
import com.example.chatbot.model.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ChatRepository {
    private final MessageDao messageDao;
    private final SessionDao sessionDao;
    private final ExecutorService executorService;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    public ChatRepository(Context context) {
      try {
          AppDatabase database = AppDatabase.getDatabase(context);
          this.messageDao = database.messageDao();
          this.sessionDao = database.sessionDao();
          this.executorService = Executors.newSingleThreadExecutor();
          this.client = new OkHttpClient.Builder()
                  .connectTimeout(30, TimeUnit.SECONDS)
                  .readTimeout(30, TimeUnit.SECONDS)
                  .build();
      } catch (Exception e) {
          Log.e("ChatRepository", "Lỗi khi khởi tạo ChatRepository", e);
          throw new RuntimeException("Lỗi khi khởi tạo ChatRepository", e);
      }
    }

    public interface SessionInsertCallback {
        void onSessionInserted(long sessionId);
        void onError(String error);
    }

    public void insertSession(Session session, SessionInsertCallback callback) {
        executorService.execute(() -> {
            try {
                long sessionId = sessionDao.insert(session);
                callback.onSessionInserted(sessionId);
            } catch (Exception e) {
                Log.e("ChatRepository", "Lỗi khi lưu session", e);
                callback.onError("Lỗi khi lưu session: " + e.getMessage());
            }
        });
    }
    public LiveData<List<Session>> getAllSessions() {
        return sessionDao.getAllSessions();
    }
    public void insertMessage(Message message, long sessionId) {
        try {
            executorService.execute(() -> messageDao.insert(message.toEntity(sessionId)));
        } catch (Exception e) {
            Log.e("ChatRepository", "Lỗi khi lưu tin nhắn vào Room", e);
        }
    }

    public LiveData<List<MessageEntity>> getMessagesBySession(long sessionId) {
        return messageDao.getMessagesBySession(sessionId);
    }

    public interface DeleteCallback {
        void onDeleted();
        void onError(String error);
    }

    public void deleteSession(long sessionId, DeleteCallback callback) {
        executorService.execute(() -> {
            try {
                sessionDao.deleteSession(sessionId);
                callback.onDeleted();
            } catch (Exception e) {
                Log.e("ChatRepository", "Lỗi khi xóa session", e);
                callback.onError("Lỗi khi xóa session: " + e.getMessage());
            }
        });
    }

    public void sendMessage(String question, Callback callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "deepseek/deepseek-r1-distill-llama-70b:free");
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.put(userMessage);
            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0.7);
        } catch (Exception e) {
            callback.onFailure(null, new IOException("Lỗi khi tạo JSON body", e));
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
