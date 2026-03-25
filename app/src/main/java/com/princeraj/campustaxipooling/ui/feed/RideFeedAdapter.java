package com.princeraj.campustaxipooling.ui.feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * RecyclerView adapter for the ride feed.
 * Uses an interface callback for click events (no context coupling).
 */
public class RideFeedAdapter extends RecyclerView.Adapter<RideFeedAdapter.RideViewHolder> {

    public interface OnRideClickListener {
        void onRideClick(Ride ride);
    }

    private final List<Ride> rides;
    private final OnRideClickListener listener;

    public RideFeedAdapter(List<Ride> rides, OnRideClickListener listener) {
        this.rides = rides;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_card, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        Ride ride = rides.get(position);
        holder.bind(ride, listener);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class RideViewHolder extends RecyclerView.ViewHolder {

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

        RideViewHolder(@NonNull View itemView) {
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

        void bind(Ride ride, OnRideClickListener listener) {
            // Poster avatar initial
            if (ride.getPostedByName() != null && !ride.getPostedByName().isEmpty()) {
                posterInitial.setText(String.valueOf(ride.getPostedByName().charAt(0)).toUpperCase());
                posterName.setText(ride.getPostedByName());
            }

            // Department (if available — would require denormalized field or join)
            posterDept.setText("Campus Member");

            // Status badge (Auto-calculate completion)
            String status = ride.getStatus() != null ? ride.getStatus() : "ACTIVE";
            boolean isTimePassed = ride.getJourneyDateTime() != null &&
                    ride.getJourneyDateTime().toDate().before(new java.util.Date());

            if ("ACTIVE".equals(status) && isTimePassed) {
                status = "COMPLETED";
            }

            statusBadge.setText(status.toUpperCase());
            
            // Phase 7: Proper semantic coloring for badges
            if ("COMPLETED".equals(status)) {
                statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.status_success)));
            } else if ("CANCELLED".equals(status)) {
                statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.status_error)));
            } else {
                statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.brand_secondary)));
            }

            // Route
            sourceText.setText(ride.getSource());
            destinationText.setText(ride.getDestination());

            // Time — format Firestore Timestamp
            if (ride.getJourneyDateTime() != null) {
                Date date = ride.getJourneyDateTime().toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                timeText.setText(sdf.format(date));
            } else {
                timeText.setText("TBD");
            }

            // Fare per person
            if (ride.getTotalSeats() > 0) {
                long perPerson = ride.getTotalFare() / ride.getTotalSeats();
                fareText.setText("₹" + perPerson + "/seat");
            } else {
                fareText.setText("₹" + ride.getTotalFare());
            }

            // Seats
            seatsText.setText(ride.getSeatsRemaining() + " seat"
                    + (ride.getSeatsRemaining() != 1 ? "s" : "") + " left");

            // Disable button if no seats or if completed
            if ("COMPLETED".equals(status)) {
                viewRideBtn.setText("Ride Ended");
                viewRideBtn.setEnabled(false);
                viewRideBtn.setAlpha(0.6f);
            } else if (!ride.hasSeatsAvailable()) {
                viewRideBtn.setText("Ride Full");
                viewRideBtn.setEnabled(false);
                viewRideBtn.setAlpha(0.6f);
            } else {
                viewRideBtn.setText("View & Request Seat");
                viewRideBtn.setEnabled(true);
                viewRideBtn.setAlpha(1.0f);
            }

            viewRideBtn.setOnClickListener(v -> listener.onRideClick(ride));
            itemView.setOnClickListener(v -> listener.onRideClick(ride));
        }
    }
}
