package com.example.chatbot.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbot.R;
import com.example.chatbot.model.Session;

import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {
    private final List<Session> sessionList;
    private final OnSessionClickListener listener;
    private long selectedSessionId = -1; // ID phiên bản được chọn
    public interface OnSessionClickListener {
        void onSessionClick(Session session);
        void onSessionSelect(long sessionId);
    }

    public SessionAdapter(OnSessionClickListener listener) {
        this.sessionList = new ArrayList<>();
        this.listener = listener;
    }

    public void setSessions(List<Session> session) {
        this.sessionList.clear();
        this.sessionList.addAll(session);
        notifyDataSetChanged();
    }

    public void setSelectedSessionId(long sessionId) {
        selectedSessionId = sessionId;
        notifyDataSetChanged();
    }

    public long getSelectedSessionId() {
        return selectedSessionId;
    }
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessionList.get(position);
        holder.bind(session, session.id == selectedSessionId);
    }


    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView previewTextView;
        private final TextView timestampTextView;
        private final View itemView;
        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            previewTextView = itemView.findViewById(R.id.preview_text);
            timestampTextView = itemView.findViewById(R.id.timestamp);
            itemView.setOnClickListener(v-> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Session session = sessionList.get(position);
                    listener.onSessionSelect(session.id);
                    setSelectedSessionId(session.id);
                }
            });
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onSessionClick(sessionList.get(position));
                    return true;
                }
                return false;
            });
        }

        void bind(Session session, boolean isSelected) {
            previewTextView.setText(session.previewText);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            timestampTextView.setText(sdf.format(new Date(session.timestamp)));
            itemView.setBackgroundColor(isSelected ? 0xFFBBDEFB : 0xFFFFFFFF);// Xanh nhạt nếu được chọn, trắng nếu không
        }
    }
}
