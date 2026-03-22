package com.princeraj.campustaxipooling.ui.myrides;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Ride;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the "My Rides" tab. Shows the poster's own rides.
 * Cards have Cancel + View Requests buttons for ACTIVE rides.
 */
public class MyRidesAdapter extends RecyclerView.Adapter<MyRidesAdapter.MyRideViewHolder> {

    public interface OnRideActionListener {
        void onAction(Ride ride);
    }

    private final List<Ride> rides;
    private final OnRideActionListener onViewClick;
    private final OnRideActionListener onCancelClick;
    private final OnRideActionListener onViewRequestsClick;

    public MyRidesAdapter(List<Ride> rides,
                          OnRideActionListener onViewClick,
                          OnRideActionListener onCancelClick,
                          OnRideActionListener onViewRequestsClick) {
        this.rides = rides;
        this.onViewClick = onViewClick;
        this.onCancelClick = onCancelClick;
        this.onViewRequestsClick = onViewRequestsClick;
    }

    @NonNull
    @Override
    public MyRideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse item_ride_card, we'll overlay cancel button in the ViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_card, parent, false);
        return new MyRideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyRideViewHolder holder, int position) {
        holder.bind(rides.get(position), onViewClick, onCancelClick, onViewRequestsClick);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class MyRideViewHolder extends RecyclerView.ViewHolder {

        private final TextView posterInitial;
        private final TextView posterName;
        private final TextView posterDept;
        private final TextView statusBadge;
        private final TextView sourceText;
        private final TextView destinationText;
        private final TextView timeText;
        private final TextView fareText;
        private final TextView seatsText;
        private final MaterialButton viewRideBtn;

        MyRideViewHolder(@NonNull View itemView) {
            super(itemView);
            posterInitial = itemView.findViewById(R.id.posterInitial);
            posterName = itemView.findViewById(R.id.posterName);
            posterDept = itemView.findViewById(R.id.posterDept);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            sourceText = itemView.findViewById(R.id.sourceText);
            destinationText = itemView.findViewById(R.id.destinationText);
            timeText = itemView.findViewById(R.id.timeText);
            fareText = itemView.findViewById(R.id.fareText);
            seatsText = itemView.findViewById(R.id.seatsText);
            viewRideBtn = itemView.findViewById(R.id.viewRideBtn);
        }

        void bind(Ride ride, OnRideActionListener onView,
                  OnRideActionListener onCancel,
                  OnRideActionListener onViewRequests) {
            // Poster info
            if (ride.getPostedByName() != null && !ride.getPostedByName().isEmpty()) {
                posterInitial.setText(
                        String.valueOf(ride.getPostedByName().charAt(0)).toUpperCase());
                posterName.setText(ride.getPostedByName());
            }
            posterDept.setText("Posted by you");

            // Status badge
            statusBadge.setText(ride.getStatus() != null ? ride.getStatus() : "ACTIVE");

            // Route
            sourceText.setText(ride.getSource());
            destinationText.setText(ride.getDestination());

            // Time
            if (ride.getJourneyDateTime() != null) {
                Date date = ride.getJourneyDateTime().toDate();
                timeText.setText(new SimpleDateFormat("dd MMM, hh:mm a",
                        Locale.getDefault()).format(date));
            } else {
                timeText.setText("TBD");
            }

            // Fare & seats
            fareText.setText("₹" + ride.getTotalFare());
            seatsText.setText(ride.getSeatsRemaining() + " left");

            // Button behaviour
            if (ride.isActive()) {
                // Primary: View Requests
                viewRideBtn.setText("View Requests");
                viewRideBtn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF5B4FE8));
                viewRideBtn.setEnabled(true);
                viewRideBtn.setOnClickListener(v -> onViewRequests.onAction(ride));

                // Inject a Cancel button below the existing one dynamically via tag trick
                // The layout already has one button; we use its parent to add Cancel
                if (viewRideBtn.getTag() == null) {
                    viewRideBtn.setTag("tagged");
                    MaterialButton cancelBtn = new MaterialButton(
                            itemView.getContext(),
                            null,
                            com.google.android.material.R.attr.borderlessButtonStyle);
                    cancelBtn.setText("Cancel Ride");
                    cancelBtn.setTextColor(0xFFFF5370);
                    cancelBtn.setOnClickListener(v -> onCancel.onAction(ride));
                    if (viewRideBtn.getParent() instanceof android.view.ViewGroup) {
                        ((android.view.ViewGroup) viewRideBtn.getParent()).addView(cancelBtn);
                    }
                }
            } else {
                viewRideBtn.setText("View Details");
                viewRideBtn.setEnabled(true);
                viewRideBtn.setOnClickListener(v -> onView.onAction(ride));
            }

            itemView.setOnClickListener(v -> onView.onAction(ride));
        }
    }
}
