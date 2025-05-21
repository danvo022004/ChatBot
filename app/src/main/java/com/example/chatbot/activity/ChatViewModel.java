package com.example.chatbot.activity;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chatbot.model.ChatRepository;
import com.example.chatbot.model.Message;
import com.example.chatbot.model.MessageEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repository;
    private final MediatorLiveData<List<Message>> messageListLiveData = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> isTyping = new MediatorLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MediatorLiveData<>();
    private final List<Message> messageList = new ArrayList<>();


    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
        LiveData<List<MessageEntity>> messagesLiveData = repository.getAllMessages();
        messageListLiveData.addSource(messagesLiveData, entities -> {
            messageList.clear();
            for (MessageEntity entity : entities) {
                messageList.add(Message.fromEntity(entity));
            }
            messageListLiveData.setValue(messageList);
        });
    }

    public LiveData<List<Message>> getMessages() {
        return messageListLiveData;
    }
    public LiveData<Boolean> getIsTyping() {
        return isTyping;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void sendMessage(String question) {
        if (question == null || question.trim().isEmpty()) {
            return;
        }

        Message userMessage = new Message(question.trim(), Message.SENT_BY_ME, System.currentTimeMillis());
        messageList.add(userMessage);
        messageListLiveData.setValue(messageList);
        repository.insertMessage(userMessage);

        isTyping.setValue(true);
        repository.sendMessage(question, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API", "Lỗi kết nối", e);
                isTyping.setValue(false);
                errorMessage.setValue("Không thể kết nối đến máy chủ: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                isTyping.setValue(false);
                String responseBody = response.body() != null ? response.body().string(): "";
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        Message botMessage = new Message(result, Message.SENT_BY_BOT, System.currentTimeMillis());
                        messageList.add(botMessage);
                        messageListLiveData.postValue(messageList);
                        repository.insertMessage(botMessage);
                } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }else {
                    Log.e("API", "Lỗi phản hồi từ máy chủ: ");
                    errorMessage.setValue("Lỗi phản hồi từ máy chủ: " + response.code());
                }
        });
    }
}
