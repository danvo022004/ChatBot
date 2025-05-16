package com.example.chatbot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.chatbot.adapter.MessageAdapter;
import com.example.chatbot.model.Message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    TextView welcomeTextView;
    EditText messageEditText;

    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        LottieAnimationView animationView = findViewById(R.id.menuicon1);
        animationView.setFailureListener(throwable -> {
            Log.e("Lottie", "Failed to load animation", throwable);
        });
        animationView.addLottieOnCompositionLoadedListener(composition -> {
            Log.d("Lottie", "Animation loaded successfully");
        });

        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener((v)->{
            String question = messageEditText.getText().toString().trim();
            addToChat(question,Message.SENT_BY_ME);
            messageEditText.setText("");
            callAPI(question);
            welcomeTextView.setVisibility(View.GONE);
        });
    }

    public void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message,sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    public void addResponse(String response) {
        messageList.remove(messageList.size()-1);
        addToChat(response,Message.SENT_BY_BOT);
    }

    public void callAPI(String question) {
        // Khởi tạo OkHttp client với timeout
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        // Thêm chỉ báo "Typing..."
        messageList.add(new Message("Typing...", Message.SENT_BY_BOT));

        // Tạo JSON body theo định dạng OpenRouter
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "deepseek/deepseek-r1-distill-llama-70b:free");
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.put(userMessage);
            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 1000); // Giới hạn để tiết kiệm quota
            jsonBody.put("temperature", 0.7);
        } catch (Exception e) {
            e.printStackTrace();
            addResponse("Lỗi khi tạo yêu cầu: " + e.getMessage());
            return;
        }

        // Lấy khóa API từ BuildConfig
        String apiKey = "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";
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        // Gửi yêu cầu bất đồng bộ
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> addResponse("Lỗi kết nối: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        runOnUiThread(() -> addResponse(result.trim()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> addResponse("Lỗi phân tích phản hồi: " + e.getMessage()));
                    }
                } else {
                    Log.e("API", "HTTP " + response.code() + ": " + responseBody);
                    String errorMessage;

                    // Xử lý các lỗi phổ biến
                    if (response.code() == 429) {
                        errorMessage = "Đã vượt giới hạn yêu cầu miễn phí của OpenRouter. Vui lòng thử lại sau hoặc kiểm tra quota.";
                    } else if (response.code() == 401) {
                        errorMessage = "Khóa API OpenRouter không hợp lệ. Vui lòng kiểm tra lại khóa.";
                    } else if (response.code() == 400) {
                        errorMessage = "Yêu cầu không hợp lệ. Kiểm tra mô hình hoặc tham số.";
                    } else {
                        errorMessage = "HTTP " + response.code() + ": " + responseBody;
                    }

                    runOnUiThread(() -> addResponse("Lỗi khi tải phản hồi: " + errorMessage));
                }
            }

            private void runOnUiThread(Runnable runnable) {
                new Handler(Looper.getMainLooper()).post(runnable);
            }
        });
    }
}