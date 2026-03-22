package com.princeraj.campustaxipooling.admin;

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
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Admin Rides. Uses the standard ride layout where possible,
 * but reuses our custom MyRidesAdapter layout tricks to inject a delete button.
 */
public class AdminRideAdapter extends RecyclerView.Adapter<AdminRideAdapter.AdminRideViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(Ride ride);
    }

    private final List<Ride> rides;
    private final OnDeleteClickListener onDelete;

    private static final SimpleDateFormat FMT = new SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault());

    public AdminRideAdapter(List<Ride> rides, OnDeleteClickListener onDelete) {
        this.rides = rides;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public AdminRideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // We reuse the ride item from the Home feed which has a basic card structure
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_card, parent, false);
        return new AdminRideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminRideViewHolder holder, int position) {
        holder.bind(rides.get(position), onDelete);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class AdminRideViewHolder extends RecyclerView.ViewHolder {

        private final TextView sourceText, destinationText;
        private final TextView timeText, fareText, seatsText;
        private final TextView posterName;
        private final MaterialButton actionBtn;

        AdminRideViewHolder(@NonNull View itemView) {
            super(itemView);
            sourceText = itemView.findViewById(R.id.sourceText);
            destinationText = itemView.findViewById(R.id.destinationText);
            timeText = itemView.findViewById(R.id.timeText);
            fareText = itemView.findViewById(R.id.fareText);
            seatsText = itemView.findViewById(R.id.seatsText);
            posterName = itemView.findViewById(R.id.posterName);
            actionBtn = itemView.findViewById(R.id.viewRideBtn);
        }

        void bind(Ride ride, OnDeleteClickListener onDelete) {
            sourceText.setText(ride.getSource());
            destinationText.setText(ride.getDestination());
            
            if (ride.getJourneyDateTime() != null) {
                timeText.setText(FMT.format(ride.getJourneyDateTime().toDate()));
            }

            fareText.setText("₹" + ride.getTotalFare());
            seatsText.setText(ride.getSeatsRemaining() + "/" + ride.getTotalSeats());
            posterName.setText(ride.getPostedByName() != null ? ride.getPostedByName() : "Deleted User");

            if (ride.isDeleted()) {
                actionBtn.setText("DELETED");
                actionBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF333333));
                actionBtn.setEnabled(false);
            } else {
                actionBtn.setText("Force Delete");
                actionBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5370));
                actionBtn.setEnabled(true);
                actionBtn.setOnClickListener(v -> onDelete.onDelete(ride));
            }
        }
    }
}
