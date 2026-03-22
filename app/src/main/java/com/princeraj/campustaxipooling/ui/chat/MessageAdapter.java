package com.princeraj.campustaxipooling.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for chat messages.
 * Uses two view types: SENT (right) and RECEIVED (left).
 * Never shows isBlocked=true messages — those are filtered in ChatActivity.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT     = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUid;

    public MessageAdapter(List<Message> messages, String currentUid) {
        this.messages = messages;
        this.currentUid = currentUid;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        return currentUid.equals(msg.getSenderUid()) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_SENT)
                ? R.layout.item_message_sent
                : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;
        private final TextView timeText;
        private static final SimpleDateFormat TIME_FORMAT =
                new SimpleDateFormat("hh:mm a", Locale.getDefault());

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        void bind(Message message) {
            messageText.setText(message.getText());

            if (message.getSentAt() != null) {
                Date date = message.getSentAt().toDate();
                timeText.setText(TIME_FORMAT.format(date));
            } else {
                timeText.setText("Sending…");
            }
        }
    }
}
