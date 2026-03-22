package com.princeraj.campustaxipooling.ui.requests;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.SeatRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying seat join requests to the ride poster.
 * Cards show requester info and Accept / Reject buttons.
 * After an action, buttons are replaced with a status chip (in-place UI feedback).
 */
public class SeatRequestAdapter extends
        RecyclerView.Adapter<SeatRequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAction(SeatRequest request);
    }

    private final List<SeatRequest> requests;
    private final OnRequestActionListener onAccept;
    private final OnRequestActionListener onReject;

    // Track in-flight actions to prevent double-taps
    // key = requestId, value = true means action submitted
    private final java.util.Set<String> actioned = new java.util.HashSet<>();

    public SeatRequestAdapter(List<SeatRequest> requests,
                              OnRequestActionListener onAccept,
                              OnRequestActionListener onReject) {
        this.requests = requests;
        this.onAccept = onAccept;
        this.onReject = onReject;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seat_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(requests.get(position), onAccept, onReject, actioned);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class RequestViewHolder extends RecyclerView.ViewHolder {

        private final TextView requesterInitial;
        private final TextView requesterName;
        private final TextView requestTime;
        private final TextView requestNote;
        private final TextView statusChip;
        private final LinearLayout actionButtons;
        private final MaterialButton acceptBtn;
        private final MaterialButton rejectBtn;

        private static final SimpleDateFormat TIME_FMT =
                new SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault());

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            requesterInitial = itemView.findViewById(R.id.requesterInitial);
            requesterName = itemView.findViewById(R.id.requesterName);
            requestTime = itemView.findViewById(R.id.requestTime);
            requestNote = itemView.findViewById(R.id.requestNote);
            statusChip = itemView.findViewById(R.id.statusChip);
            actionButtons = itemView.findViewById(R.id.actionButtons);
            acceptBtn = itemView.findViewById(R.id.acceptBtn);
            rejectBtn = itemView.findViewById(R.id.rejectBtn);
        }

        void bind(SeatRequest req,
                  OnRequestActionListener onAccept,
                  OnRequestActionListener onReject,
                  java.util.Set<String> actioned) {

            // Name + avatar
            String name = req.getRequesterName() != null ? req.getRequesterName() : "Unknown";
            requesterInitial.setText(
                    String.valueOf(name.isEmpty() ? "?" : name.charAt(0)).toUpperCase());
            requesterName.setText(name);

            // Time
            if (req.getRequestedAt() != null) {
                requestTime.setText("Requested " + TIME_FMT.format(
                        req.getRequestedAt().toDate()));
            } else {
                requestTime.setText("Just now");
            }

            // Optional note
            String note = req.getMessage();
            if (note != null && !note.isEmpty()) {
                requestNote.setText("💬  " + note);
                requestNote.setVisibility(View.VISIBLE);
            } else {
                requestNote.setVisibility(View.GONE);
            }

            // Already actioned in this session? → show chip, hide buttons
            String reqId = req.getRequestId();
            boolean wasActioned = reqId != null && actioned.contains(reqId);
            if (wasActioned) {
                showStatus("Processing…", 0xFF9E9BBF);
                return;
            }

            // Show action buttons
            actionButtons.setVisibility(View.VISIBLE);
            statusChip.setVisibility(View.GONE);

            acceptBtn.setOnClickListener(v -> {
                if (reqId != null) actioned.add(reqId);
                showStatus("Accepted ✓", 0xFF00D4AA);
                onAccept.onAction(req);
            });

            rejectBtn.setOnClickListener(v -> {
                if (reqId != null) actioned.add(reqId);
                showStatus("Rejected", 0xFFFF5370);
                onReject.onAction(req);
            });
        }

        private void showStatus(String label, int color) {
            actionButtons.setVisibility(View.GONE);
            statusChip.setVisibility(View.VISIBLE);
            statusChip.setText(label);
            statusChip.setTextColor(color);

            // Badge background from bg_badge_active shape
            statusChip.setBackgroundResource(R.drawable.bg_badge_active);
        }
    }
}
