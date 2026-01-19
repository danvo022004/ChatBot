package com.example.chatbot.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.chatbot.R;
import com.example.chatbot.model.Message;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private final List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    public void setMessages(List<Message> messages) {
        messageList.clear();
        messageList.addAll(messages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSentBy().equals(Message.SENT_BY_ME)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            View chatView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sender_view, parent, false);
            return new UserViewHolder(chatView);
        } else {
            View chatView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.reciver_view, parent, false);
            return new BotViewHolder(chatView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        // Định dạng thời gian
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String formattedTime = sdf.format(new Date(message.getTimestamp()));

        if (holder.getItemViewType() == VIEW_TYPE_MESSAGE_SENT) {
            UserViewHolder userViewHolder = (UserViewHolder) holder;
            userViewHolder.senderText.setText(message.getMessage());
            userViewHolder.senderTime.setText(formattedTime);

        } else {
            BotViewHolder botViewHolder = (BotViewHolder) holder;
            botViewHolder.receiverText.setText(message.getMessage());
            botViewHolder.receiverTime.setText(formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    //ViewHolder for sender
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView senderText, senderTime;
        ShapeableImageView userAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.sender_text);
            senderTime = itemView.findViewById(R.id.sender_time);
            userAvatar = itemView.findViewById(R.id.user_avatar);
        }
    }

    //ViewHolder for receiver
    public static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView receiverText, receiverTime;
        LottieAnimationView botAvatar;

        public BotViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverText = itemView.findViewById(R.id.receiver_text);
            receiverTime = itemView.findViewById(R.id.receiver_time);
            botAvatar = itemView.findViewById(R.id.menuicon2);
        }
    }

}