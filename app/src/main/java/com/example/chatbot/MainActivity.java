package com.example.chatbot;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.chatbot.adapter.MessageAdapter;
import com.example.chatbot.database.AppDatabase;
import com.example.chatbot.model.Message;
import com.example.chatbot.model.MessageEntity;
import com.example.chatbot.dao.MessageDao;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView welcomeTextView;
    private EditText messageEditText;
    private ImageButton sendButton, menuButton;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private AppDatabase database;
    private MessageDao messageDao;
    private ExecutorService executorService;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo danh sách tin nhắn
        messageList = new ArrayList<>();

        // Liên kết các thành phần giao diện
        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);
        menuButton = findViewById(R.id.menu_btn);

        // Khởi tạo Room database
        database = AppDatabase.getDatabase(this);
        messageDao = database.messageDao();
        executorService = Executors.newSingleThreadExecutor();

        // Thiết lập LottieAnimationView
        LottieAnimationView animationView = findViewById(R.id.menuicon1);
        animationView.setFailureListener(throwable -> {
            Log.e("Lottie", "Failed to load animation", throwable);
        });
        animationView.addLottieOnCompositionLoadedListener(composition -> {
            Log.d("Lottie", "Animation loaded successfully");
        });

        // Thiết lập RecyclerView
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        // Tải lịch sử chat từ Room
        loadChatHistory();

        // Xử lý sự kiện nút gửi
        sendButton.setOnClickListener(v -> sendMessage());

        // Xử lý phím Enter trên bàn phím
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Xử lý nút menu
        menuButton.setOnClickListener(v -> showChatHistoryDialog());
    }

    private void sendMessage() {
        String question = messageEditText.getText().toString().trim();
        if (!question.isEmpty()) {
            Message message = new Message(question, Message.SENT_BY_ME, System.currentTimeMillis());
            addToChat(message);
            messageEditText.setText("");
            callAPI(question);
            welcomeTextView.setVisibility(View.GONE);
        }
    }

    private void addToChat(Message message) {
        executeOnUiThread(() -> {
            messageList.add(message);
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            // Lưu vào Room
            executorService.execute(() -> messageDao.insert(message.toEntity()));
        });
    }

    private void addTypingIndicator() {
        executeOnUiThread(() -> {
            Message typingMessage = new Message("Typing...", Message.SENT_BY_BOT, System.currentTimeMillis());
            messageList.add(typingMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            // Không lưu "Typing..." vào Room
        });
    }

    private void removeTypingIndicator() {
        executeOnUiThread(() -> {
            if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessage().equals("Typing...")) {
                messageList.remove(messageList.size() - 1);
                messageAdapter.notifyItemRemoved(messageList.size());
            }
        });
    }

    private void addResponse(String response) {
        executeOnUiThread(() -> {
            removeTypingIndicator();
            Message responseMessage = new Message(response, Message.SENT_BY_BOT, System.currentTimeMillis());
            addToChat(responseMessage);
        });
    }

    private void loadChatHistory() {
        LiveData<List<MessageEntity>> messagesLiveData = messageDao.getAllMessages();
        messagesLiveData.observe(this, entities -> {
            messageList.clear();
            for (MessageEntity entity : entities) {
                messageList.add(Message.fromEntity(entity));
            }
            messageAdapter.notifyDataSetChanged();
            if (!messageList.isEmpty()) {
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            }
        });
    }

    private void showChatHistoryDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_chat_history);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.8),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.6)
        );

        RecyclerView historyRecyclerView = dialog.findViewById(R.id.history_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        historyRecyclerView.setLayoutManager(layoutManager);

        List<Message> historyList = new ArrayList<>();
        for (Message msg : messageList) {
            if (!msg.getMessage().equals("Typing...")) {
                historyList.add(msg);
            }
        }
        MessageAdapter historyAdapter = new MessageAdapter(historyList);
        historyRecyclerView.setAdapter(historyAdapter);

        Button closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void callAPI(String question) {
        addTypingIndicator();
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
            Log.e("API", "Lỗi khi tạo JSON body", e);
            removeTypingIndicator();
            addResponse("Lỗi khi tạo yêu cầu: " + e.getMessage());
            return;
        }

        String apiKey = "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";
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API", "Lỗi kết nối", e);
                addResponse("Không thể kết nối đến máy chủ: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());
                    } catch (Exception e) {
                        Log.e("API", "Lỗi phân tích JSON", e);
                        addResponse("Lỗi phân tích phản hồi: " + e.getMessage());
                    }
                } else {
                    Log.e("API", "HTTP " + response.code() + ": " + responseBody);
                    String errorMessage;
                    switch (response.code()) {
                        case 429:
                            errorMessage = "Đã vượt giới hạn yêu cầu miễn phí. Vui lòng thử lại sau.";
                            break;
                        case 401:
                            errorMessage = "Khóa API không hợp lệ. Vui lòng kiểm tra lại.";
                            break;
                        case 400:
                            errorMessage = "Yêu cầu không hợp lệ. Kiểm tra tham số.";
                            break;
                        default:
                            errorMessage = "Lỗi máy chủ: HTTP " + response.code();
                    }
                    addResponse(errorMessage);
                }
            }
        });
    }

    private void executeOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}