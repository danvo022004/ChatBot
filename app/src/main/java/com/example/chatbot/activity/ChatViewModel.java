package com.example.chatbot.activity;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chatbot.model.Message;
import com.example.chatbot.model.MessageEntity;
import com.example.chatbot.model.Session;
import com.example.chatbot.repository.ChatRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChatViewModel extends AndroidViewModel {
    private ChatRepository repository;
    private final MediatorLiveData<List<Message>> messageListLiveData = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> isTyping = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final List<Message> messageList = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long currentSessionId = -1; // ID phiên bản hiện tại

    public ChatViewModel(@NonNull Application application) {
        super(application);
        try {
            repository = new ChatRepository(application);
            //Không tải lịch sử cho MainActivity
            messageListLiveData.setValue(new ArrayList<>());
            //Tạo session mới khi khởi tạo
            startNewSession();
        } catch (Exception e) {
            Log.e("ChatViewModel", "Lỗi khởi tạo repository", e);
            mainHandler.post(() -> errorMessage.setValue("Không thể khởi tạo cơ sở dữ liệu"));
        }
    }

    private void startNewSession() {
        String previewText = "New session"; //Mặc định sẽ cập nhật sau
        Session session = new Session(previewText, System.currentTimeMillis());
        repository.insertSession(session, new ChatRepository.SessionInsertCallback() {
            @Override
            public void onSessionInserted(long sessionId) {
                mainHandler.post(() -> {
                    currentSessionId = sessionId;
                    messageList.clear();
                    messageListLiveData.setValue(new ArrayList<>(messageList));
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> errorMessage.setValue(error));
            }
        });
    }

    public LiveData<List<Message>> getMessageList() {
        return messageListLiveData;
    }

    public LiveData<Boolean> getIsTyping() {
        return isTyping;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<List<Session>> getAllSessions() {
        return repository.getAllSessions();
    }

    public LiveData<List<Message>> getMessagesBySession(long sessionId) {
        MediatorLiveData<List<Message>> sessionMessages = new MediatorLiveData<>();
        LiveData<List<MessageEntity>> messagesLiveData = repository.getMessagesBySession(sessionId);
        sessionMessages.addSource(messagesLiveData, entities -> {
            if (entities != null) {
                List<Message> messages = new ArrayList<>();
                for (MessageEntity entity : entities) {
                    messages.add(Message.fromEntity(entity));
                }
                sessionMessages.setValue(messages);
            }
        });
        return sessionMessages;
    }

    public void deleteSession(long sessionId) {
        repository.deleteSession(sessionId, new ChatRepository.DeleteCallback() {
            @Override
            public void onDeleted() {
                mainHandler.post(() ->{

                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> errorMessage.setValue(error));
            }
        });
    }

    public void sendMessage(String question) {
        if (question == null || question.trim().isEmpty() || currentSessionId == -1) {
            return;
        }

        // Cập nhật previewText cho session nếu đây là tin nhắn đầu tiên
        if (messageList.isEmpty()) {
            String previewText = question.length() > 50 ? question.substring(0, 50) + "..." : question;
            Session updateSession = new Session(previewText, System.currentTimeMillis());
            updateSession.id = currentSessionId;
            repository.insertSession(updateSession, new ChatRepository.SessionInsertCallback() {
                @Override
                public void onSessionInserted(long sessionId) {

                }

                @Override
                public void onError(String error) {
                    mainHandler.post(() -> errorMessage.setValue(error));
                }
            }); // cập nhật session
        }

        Message userMessage = new Message(currentSessionId, question.trim(), Message.SENT_BY_ME, System.currentTimeMillis());
        messageList.add(userMessage);
        messageListLiveData.setValue(new ArrayList<>(messageList));
        repository.insertMessage(userMessage, currentSessionId);

        mainHandler.post(() -> isTyping.setValue(true));
        repository.sendMessage(question, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API", "Lỗi kết nối", e);
                mainHandler.post(() -> {
                    isTyping.setValue(false);
                    errorMessage.setValue("Không thể kết nối đến máy chủ: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                mainHandler.post(() -> {
                    isTyping.setValue(false);
                    if (response.isSuccessful() && !responseBody.isEmpty()) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            if (!jsonObject.isNull("choices")) {
                                JSONArray jsonArray = jsonObject.getJSONArray("choices");
                                if (jsonArray.length() > 0) {
                                    JSONObject choice = jsonArray.getJSONObject(0);
                                    if (!choice.isNull("message")) {
                                        JSONObject messageObj = choice.getJSONObject("message");
                                        if (!messageObj.isNull("content")) {
                                            String result = messageObj.getString("content").trim();
                                            if (!result.isEmpty()) {
                                                Message botMessage = new Message(result, Message.SENT_BY_BOT, System.currentTimeMillis());
                                                messageList.add(botMessage);
                                                messageListLiveData.setValue(new ArrayList<>(messageList));
                                                repository.insertMessage(botMessage, currentSessionId);
                                            } else {
                                                errorMessage.setValue("Phản hồi từ bot rỗng");
                                            }
                                        } else {
                                            errorMessage.setValue("Không tìm thấy nội dung tin nhắn trong phản hồi");
                                        }
                                    } else {
                                        errorMessage.setValue("Không tìm thấy tin nhắn trong phản hồi");
                                    }
                                } else {
                                    errorMessage.setValue("Không có lựa chọn nào trong phản hồi API");
                                }
                            } else {
                                errorMessage.setValue("Phản hồi API không hợp lệ");
                            }
                        } catch (Exception e) {
                            Log.e("API", "Lỗi phân tích JSON", e);
                            errorMessage.setValue("Lỗi phân tích phản hồi: " + e.getMessage());
                        }
                    } else {
                        Log.e("API", "HTTP " + response.code() + ": " + responseBody);
                        String errorMsg;
                        switch (response.code()) {
                            case 429:
                                errorMsg = "Đã vượt giới hạn yêu cầu miễn phí. Vui lòng thử lại sau.";
                                break;
                            case 401:
                                errorMsg = "Khóa API không hợp lệ. Vui lòng kiểm tra lại.";
                                break;
                            case 400:
                                errorMsg = "Yêu cầu không hợp lệ. Kiểm tra tham số.";
                                break;
                            default:
                                errorMsg = "Lỗi máy chủ: HTTP " + response.code();
                        }
                        errorMessage.setValue(errorMsg);
                    }
                });
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}