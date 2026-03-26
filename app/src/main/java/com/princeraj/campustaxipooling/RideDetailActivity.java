package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.SeatRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Displays full details of a ride and allows the user to request a seat.
 * Integrated with Phase 3 offline cache support.
 */
@AndroidEntryPoint
public class RideDetailActivity extends BaseActivity {

    private TextView sourceText, destinationText, timeText, fareText, seatsText;
    private TextView posterInitial, posterName, preferencesText;
    private MaterialButton requestRideBtn, reportRideBtn;

    @Inject
    com.princeraj.campustaxipooling.repository.IRideRepository rideRepo;
    
    @Inject
    com.princeraj.campustaxipooling.repository.IUserRepository userRepo;

    private Ride currentRide;
    private String rideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);

        rideId = getIntent().getStringExtra("rideId");
        if (rideId == null) {
            finish();
            return;
        }

        bindViews();

        ImageView backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        requestRideBtn.setOnClickListener(v -> requestJoin());
        reportRideBtn.setOnClickListener(v ->
                Snackbar.make(requestRideBtn, "Reporting feature coming in next update.", Snackbar.LENGTH_SHORT).show());

        loadRide();
    }

    private void bindViews() {
        sourceText = findViewById(R.id.sourceText);
        destinationText = findViewById(R.id.destinationText);
        timeText = findViewById(R.id.timeText);
        fareText = findViewById(R.id.fareText);
        seatsText = findViewById(R.id.seatsText);
        posterInitial = findViewById(R.id.posterInitial);
        posterName = findViewById(R.id.posterName);
        preferencesText = findViewById(R.id.preferencesText);
        requestRideBtn = findViewById(R.id.requestJoinBtn);
        reportRideBtn = findViewById(R.id.reportRideBtn);
    }

    private void loadRide() {
        rideRepo.getRideById(rideId).observe(this, result -> {
            if (result.isLoading() && currentRide == null) return;
            
            if (result.getData() != null) {
                currentRide = result.getData();
                bindRideData(currentRide);
                
                if (result.isError()) {
                    Snackbar.make(requestRideBtn, "Showing cached ride details", Snackbar.LENGTH_SHORT).show();
                }
            } else if (result.isError()) {
                Toast.makeText(this, "Failed to load ride.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindRideData(Ride ride) {
        sourceText.setText(ride.getSource());
        destinationText.setText(ride.getDestination());

        if (ride.getJourneyDateTime() != null) {
            Date date = ride.getJourneyDateTime().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy · hh:mm a",
                    Locale.getDefault());
            timeText.setText(sdf.format(date));
        }

        fareText.setText("₹" + ride.getTotalFare() + " total  "
                + "(₹" + ride.getPerPersonFare() + "/seat)");
        seatsText.setText(ride.getSeatsRemaining() + " of " + ride.getTotalSeats()
                + " seats available");

        if (ride.getPostedByName() != null && !ride.getPostedByName().isEmpty()) {
            posterInitial.setText(String.valueOf(ride.getPostedByName().charAt(0)).toUpperCase());
            posterName.setText(ride.getPostedByName());
        }

        String prefs = ride.getPreferences();
        preferencesText.setText((prefs != null && !prefs.isEmpty()) ? "📋 " + prefs : "No preferences set");

        com.google.firebase.auth.FirebaseUser currentUser = userRepo.getCurrentFirebaseUser();
        boolean isOwner = currentUser != null && currentUser.getUid().equals(ride.getPostedByUid());

        if (isOwner) {
            requestRideBtn.setText("This is your ride");
            requestRideBtn.setEnabled(false);
            requestRideBtn.setAlpha(0.6f);
        } else if (!ride.hasSeatsAvailable()) {
            requestRideBtn.setText("Ride is Full");
            requestRideBtn.setEnabled(false);
            requestRideBtn.setAlpha(0.6f);
        } else {
            requestRideBtn.setText("Request to Join");
            requestRideBtn.setEnabled(true);
            requestRideBtn.setAlpha(1.0f);
        }
    }

    private void requestJoin() {
        com.google.firebase.auth.FirebaseUser user = userRepo.getCurrentFirebaseUser();
        if (user == null || currentRide == null) return;

        requestRideBtn.setEnabled(false);
        requestRideBtn.setText("Sending…");

        // State guard to prevent multiple requests (Cache + Cloud events).
        final boolean[] requestStarted = {false};

        userRepo.getUserProfile(user.getUid()).observe(this, result -> {
            if (isFinishing() || isDestroyed()) return;
            if (result.isLoading()) return;
            if (requestStarted[0]) return; // Already triggered

            if (result.isSuccess() && result.getData() != null) {
                String name = result.getData().getName();
                SeatRequest request = new SeatRequest(user.getUid(), name != null ? name : user.getEmail(), "");
                
                requestStarted[0] = true; // Mark as started

                rideRepo.sendJoinRequest(rideId, request, currentRide.getPostedByUid()).observe(this, res -> {
                    if (res.isLoading()) return;

                    if (res.isSuccess()) {
                        requestRideBtn.setText("Request Sent ✓");
                        Snackbar.make(requestRideBtn, "Join request sent! Waiting for approval.", Snackbar.LENGTH_LONG).show();
                    } else {
                        requestStarted[0] = false; // Allow retry on error
                        requestRideBtn.setEnabled(true);
                        requestRideBtn.setText("Request to Join");
                        Snackbar.make(requestRideBtn, "Error: " + res.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
            } else if (result.isError()) {
                requestRideBtn.setEnabled(true);
                requestRideBtn.setText("Request to Join");
                Snackbar.make(requestRideBtn, "Failed to load user info: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
