package com.princeraj.campustaxipooling;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.SeatRequest;
import com.princeraj.campustaxipooling.repository.RideRepository;
import com.princeraj.campustaxipooling.ui.requests.SeatRequestAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Shown to the RIDE POSTER.
 * Displays all PENDING seat requests with Accept / Reject actions.
 *
 * Accept uses an atomic Firestore transaction via RideRepository that:
 *   1. Sets request status → ACCEPTED
 *   2. Decrements seatsRemaining on the ride
 *   3. Sets ride status → FULL if no seats left
 *   4. Creates a Connection document (unlocks chat)
 */
public class SeatRequestsActivity extends BaseActivity {

    private TextView rideRouteSubtitle, seatsRemainingInfo, rideDateInfo, pendingCountBadge;
    private RecyclerView requestsRecyclerView;
    private LinearLayout emptyStateView;

    private SeatRequestAdapter adapter;
    private final List<SeatRequest> pendingRequests = new ArrayList<>();

    private final RideRepository rideRepo = RideRepository.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String rideId;
    private Ride currentRide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_requests);

        rideId = getIntent().getStringExtra("rideId");
        if (rideId == null) { finish(); return; }

        bindViews();

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        loadRide();
        listenToRequests();
    }

    // ── Bind ──────────────────────────────────────────────────────────────────

    private void bindViews() {
        rideRouteSubtitle = findViewById(R.id.rideRouteSubtitle);
        seatsRemainingInfo = findViewById(R.id.seatsRemainingInfo);
        rideDateInfo = findViewById(R.id.rideDateInfo);
        pendingCountBadge = findViewById(R.id.pendingCountBadge);
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        emptyStateView = findViewById(R.id.emptyStateView);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        adapter = new SeatRequestAdapter(
                pendingRequests,
                this::handleAccept,
                this::handleReject
        );
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestsRecyclerView.setAdapter(adapter);
    }

    // ── Load Ride ─────────────────────────────────────────────────────────────

    private void loadRide() {
        db.collection("rides").document(rideId).get()
                .addOnSuccessListener(doc -> {
                    currentRide = doc.toObject(Ride.class);
                    if (currentRide != null) {
                        updateRideHeader(currentRide);
                    }
                });
    }

    private void updateRideHeader(Ride ride) {
        rideRouteSubtitle.setText(ride.getSource() + " → " + ride.getDestination());
        seatsRemainingInfo.setText(
                "💺 " + ride.getSeatsRemaining() + " of " + ride.getTotalSeats()
                        + " seats remaining");
        if (ride.getJourneyDateTime() != null) {
            Date date = ride.getJourneyDateTime().toDate();
            rideDateInfo.setText(new SimpleDateFormat("dd MMM · hh:mm a",
                    Locale.getDefault()).format(date));
        }
    }

    // ── Live Requests Listener ────────────────────────────────────────────────

    private void listenToRequests() {
        rideRepo.getPendingRequestsForRide(rideId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    pendingRequests.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        SeatRequest req = doc.toObject(SeatRequest.class);
                        if (req != null) {
                            pendingRequests.add(req);
                        }
                    }

                    // Sort locally by requestedAt (ASCENDING) to avoid Firestore composite index requirement
                    pendingRequests.sort((r1, r2) -> {
                        if (r1.getRequestedAt() == null || r2.getRequestedAt() == null) return 0;
                        return r1.getRequestedAt().compareTo(r2.getRequestedAt());
                    });

                    adapter.notifyDataSetChanged();

                    // Update count badge
                    int count = pendingRequests.size();
                    pendingCountBadge.setText(String.valueOf(count));
                    pendingCountBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

                    // Show/hide empty state
                    boolean empty = pendingRequests.isEmpty();
                    requestsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);

                    if (empty) {
                        ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateEmoji)).setText("📭");
                        ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateTitle)).setText("No pending requests");
                        ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateSubtitle)).setText("Students' ride requests will appear here.");
                    }
                });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleAccept(SeatRequest request) {
        if (currentRide == null) return;

        if (currentRide.getSeatsRemaining() <= 0) {
            Snackbar.make(requestsRecyclerView,
                    "No seats remaining to accept.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Atomic transaction: accept request + decrement seat + create connection
        rideRepo.acceptSeatRequest(
                        rideId,
                        request.getRequestId(),
                        request.getRequesterUid(),
                        request.getRequesterName(),
                        currentUid)

                .addOnSuccessListener(aVoid -> {
                    // Update local ride state so next acceptance checks correct seat count
                    currentRide.setSeatsRemaining(currentRide.getSeatsRemaining() - 1);
                    if (currentRide.getSeatsRemaining() <= 0) {
                        currentRide.setStatus("FULL");
                    }
                    updateRideHeader(currentRide);

                    Snackbar.make(requestsRecyclerView,
                            "✅ " + request.getRequesterName() + " accepted! Chat unlocked.",
                            Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(requestsRecyclerView,
                                "Failed to accept: " + e.getMessage(),
                                Snackbar.LENGTH_SHORT).show());
    }

    private void handleReject(SeatRequest request) {
        rideRepo.rejectSeatRequest(rideId, request.getRequestId(), request.getRequesterUid())
                .addOnSuccessListener(aVoid ->
                        Snackbar.make(requestsRecyclerView,
                                "Request rejected.",
                                Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Snackbar.make(requestsRecyclerView,
                                "Failed to reject: " + e.getMessage(),
                                Snackbar.LENGTH_SHORT).show());
    }
}
