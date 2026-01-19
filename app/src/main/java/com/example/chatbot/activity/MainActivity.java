package com.example.chatbot.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.chatbot.R;
import com.example.chatbot.adapter.MessageAdapter;
import com.example.chatbot.adapter.SessionAdapter;
import com.example.chatbot.model.Message;
import com.example.chatbot.model.Session;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView welcomeTextView;
    private EditText messageEditText;
    private ImageButton sendButton, menuButton;
    private MessageAdapter messageAdapter;
    private ChatViewModel viewModel;
    private final List<Message> messageList = new ArrayList<>();
    private SessionAdapter sessionAdapter; // Biến instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Liên kết các thành phần giao diện
        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);
        menuButton = findViewById(R.id.menu_btn);

        // Thiết lập RecyclerView
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        // Thiết lập LottieAnimationView
        LottieAnimationView animationView = findViewById(R.id.menuicon1);
        animationView.setFailureListener(throwable -> {
            Toast.makeText(this, "Lỗi tải animation", Toast.LENGTH_SHORT).show();
        });
        animationView.addLottieOnCompositionLoadedListener(composition -> {
            // Có thể bỏ qua log
        });

        // Khởi tạo ViewModel
        try {
            viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi tạo ViewModel: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Quan sát danh sách tin nhắn hiện tại
        viewModel.getMessageList().observe(this, messages -> {
            if (messages != null) {
                executeOnUiThread(() -> {
                    messageList.clear();
                    messageList.addAll(messages);
                    messageAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                        welcomeTextView.setVisibility(View.GONE);
                    } else {
                        welcomeTextView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        // Quan sát trạng thái Typing
        viewModel.getIsTyping().observe(this, isTyping -> {
            if (isTyping != null) {
                executeOnUiThread(() -> {
                    if (isTyping) {
                        Message typingMessage = new Message("Typing...", Message.SENT_BY_BOT, System.currentTimeMillis());
                        messageList.add(typingMessage);
                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    } else {
                        if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessage().equals("Typing...")) {
                            messageList.remove(messageList.size() - 1);
                            messageAdapter.notifyItemRemoved(messageList.size());
                        }
                    }
                });
            }
        });

        // Quan sát lỗi
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                executeOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_LONG).show());
            }
        });

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
            viewModel.sendMessage(question);
            messageEditText.setText("");
        }
    }

    private void showChatHistoryDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_chat_history);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.8

                )
        );

        RecyclerView historyRecyclerView = dialog.findViewById(R.id.history_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        historyRecyclerView.setLayoutManager(layoutManager);

        // Khởi tạo sessionAdapter
        sessionAdapter = new SessionAdapter(new SessionAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(Session session) {
                showSessionMessagesDialog(session);
            }

            @Override
            public void onSessionSelect(long sessionId) {
                if (sessionAdapter != null) {
                    sessionAdapter.setSelectedSessionId(sessionId);
                }
            }
        });
        historyRecyclerView.setAdapter(sessionAdapter);

        // Quan sát danh sách sessions
        viewModel.getAllSessions().observe(this, sessions -> {
            if (sessions != null && sessionAdapter != null) {
                executeOnUiThread(() -> sessionAdapter.setSessions(sessions));
            }
        });

        Button closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        Button deleteButton = dialog.findViewById(R.id.btn_delete);
        deleteButton.setOnClickListener(v -> {
            if (sessionAdapter == null) {
                Toast.makeText(MainActivity.this, "Adapter chưa được khởi tạo", Toast.LENGTH_SHORT).show();
                return;
            }
            long selectedSessionId = sessionAdapter.getSelectedSessionId();
            if (selectedSessionId == -1) {
                Toast.makeText(MainActivity.this, "Vui lòng chọn một phiên để xóa", Toast.LENGTH_SHORT).show();
            } else {
                viewModel.deleteSession(selectedSessionId);
                sessionAdapter.setSelectedSessionId(-1); // Bỏ chọn sau prevented from executing code in the UI thread
                Toast.makeText(MainActivity.this, "Đã xóa phiên", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showSessionMessagesDialog(Session session) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_session_messages);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.8)
        );

        RecyclerView messagesRecyclerView = dialog.findViewById(R.id.messages_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);

        MessageAdapter messageAdapter = new MessageAdapter(new ArrayList<>());
        messagesRecyclerView.setAdapter(messageAdapter);

        // Quan sát tin nhắn của session
        viewModel.getMessagesBySession(session.id).observe(this, messages -> {
            if (messages != null) {
                executeOnUiThread(() -> {
                    messageAdapter.setMessages(messages);
                    if (!messages.isEmpty()) {
                        messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                });
            }
        });

        Button closeButton = dialog.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void executeOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}