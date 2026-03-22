package com.princeraj.campustaxipooling.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Connection;

import java.util.List;

/**
 * RecyclerView adapter for the chat/connections list.
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ConnectionViewHolder> {

    public interface OnConnectionClickListener {
        void onConnectionClick(Connection connection);
    }

    private final List<Connection> connections;
    private final OnConnectionClickListener listener;

    public ChatListAdapter(List<Connection> connections, OnConnectionClickListener listener) {
        this.connections = connections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConnectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection_card, parent, false);
        return new ConnectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectionViewHolder holder, int position) {
        holder.bind(connections.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return connections.size();
    }

    static class ConnectionViewHolder extends RecyclerView.ViewHolder {

        private final TextView partnerInitial;
        private final TextView partnerName;
        private final TextView rideRouteSnippet;

        ConnectionViewHolder(@NonNull View itemView) {
            super(itemView);
            partnerInitial = itemView.findViewById(R.id.partnerInitial);
            partnerName = itemView.findViewById(R.id.partnerName);
            rideRouteSnippet = itemView.findViewById(R.id.rideRouteSnippet);
        }

        void bind(Connection connection, OnConnectionClickListener listener) {
            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

            // Determine who the "other" participant is
            String partnerId = connection.getPosterUid().equals(currentUid)
                    ? connection.getJoinerUid()
                    : connection.getPosterUid();

            // Show first letter of partner UID as avatar placeholder
            // (In Phase 5, we'll denormalize the partner name onto the connection doc)
            String label = partnerName != null ? "Partner" : "?";
            String initial = partnerId.isEmpty() ? "?" : String.valueOf(
                    partnerId.charAt(0)).toUpperCase();

            partnerInitial.setText(initial);
            partnerName.setText("Ride Partner");  // Will be replaced with denormalized name
            rideRouteSnippet.setText("🚗  Tap to open chat");

            itemView.setOnClickListener(v -> listener.onConnectionClick(connection));
        }
    }
}
