package com.example.chatbot.model;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.chatbot.dao.MessageDao;
import com.example.chatbot.database.AppDatabase;

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
    private final ExecutorService executorService;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    public ChatRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.messageDao = (MessageDao) AppDatabase.getDatabase(context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public LiveData<List<MessageEntity>> getAllMessages() {
        return messageDao.getAllMessages();
    }

    public void insertMessage(Message message) {
        executorService.execute(() -> messageDao.insert(message));
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
