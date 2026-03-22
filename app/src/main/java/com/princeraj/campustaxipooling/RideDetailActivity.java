package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.SeatRequest;
import com.princeraj.campustaxipooling.repository.RideRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays full details of a ride and allows the user to request a seat.
 */
public class RideDetailActivity extends AppCompatActivity {

    private TextView sourceText, destinationText, timeText, fareText, seatsText;
    private TextView posterInitial, posterName, preferencesText;
    private MaterialButton requestJoinBtn, reportRideBtn;

    private final RideRepository rideRepo = RideRepository.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        backBtn.setOnClickListener(v -> finish());

        requestJoinBtn.setOnClickListener(v -> requestJoin());
        reportRideBtn.setOnClickListener(v -> {
            Intent reportIntent = new Intent(this, ReportActivity.class);
            reportIntent.putExtra(ReportActivity.EXTRA_TARGET_RIDE, rideId);
            reportIntent.putExtra(ReportActivity.EXTRA_TARGET_TYPE, "RIDE");
            if (currentRide != null) {
                reportIntent.putExtra(ReportActivity.EXTRA_TARGET_UID,
                        currentRide.getPostedByUid());
            }
            startActivity(reportIntent);
        });

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
        requestJoinBtn = findViewById(R.id.requestJoinBtn);
        reportRideBtn = findViewById(R.id.reportRideBtn);
    }

    private void loadRide() {
        db.collection("rides").document(rideId).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Ride not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentRide = doc.toObject(Ride.class);
                    if (currentRide != null) {
                        bindRideData(currentRide);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load ride.", Toast.LENGTH_SHORT).show();
                    finish();
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
            posterInitial.setText(
                    String.valueOf(ride.getPostedByName().charAt(0)).toUpperCase());
            posterName.setText(ride.getPostedByName());
        }

        String prefs = ride.getPreferences();
        preferencesText.setText(
                (prefs != null && !prefs.isEmpty()) ? "📋 " + prefs : "No preferences set");

        // Disable join button if no seats or posted by current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentUser != null
                && currentUser.getUid().equals(ride.getPostedByUid());

        if (isOwner) {
            requestJoinBtn.setText("This is your ride");
            requestJoinBtn.setEnabled(false);
        } else if (!ride.hasSeatsAvailable()) {
            requestJoinBtn.setText("Ride is Full");
            requestJoinBtn.setEnabled(false);
        }
    }

    private void requestJoin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentRide == null) return;

        if (currentRide.getSeatsRemaining() <= 0) {
            Snackbar.make(requestJoinBtn, "No seats available.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        requestJoinBtn.setEnabled(false);
        requestJoinBtn.setText("Sending…");

        // Check for duplicate pending request from same user
        db.collection("rides").document(rideId)
                .collection("seatRequests")
                .whereEqualTo("requesterUid", user.getUid())
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(existing -> {
                    if (!existing.isEmpty()) {
                        requestJoinBtn.setText("Request Already Sent");
                        Snackbar.make(requestJoinBtn,
                                "You already have a pending request for this ride.",
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    // Fetch user's display name
                    db.collection("users").document(user.getUid()).get()
                            .addOnSuccessListener(userDoc -> {
                                String name = userDoc.getString("name");
                                if (name == null) name = user.getEmail();

                                SeatRequest request = new SeatRequest(
                                        user.getUid(), name, "");

                                rideRepo.sendJoinRequest(rideId, request)
                                        .addOnSuccessListener(ref -> {
                                            requestJoinBtn.setText("Request Sent ✓");
                                            Snackbar.make(requestJoinBtn,
                                                    "Join request sent! Waiting for approval.",
                                                    Snackbar.LENGTH_LONG).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            requestJoinBtn.setEnabled(true);
                                            requestJoinBtn.setText("Request to Join");
                                            Snackbar.make(requestJoinBtn,
                                                    "Failed to send request: " + e.getMessage(),
                                                    Snackbar.LENGTH_SHORT).show();
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    requestJoinBtn.setEnabled(true);
                    requestJoinBtn.setText("Request to Join");
                    Snackbar.make(requestJoinBtn,
                            "Error checking existing requests.",
                            Snackbar.LENGTH_SHORT).show();
                });
    }
}
