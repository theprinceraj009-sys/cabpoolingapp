package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.SeatRequest;
import com.princeraj.campustaxipooling.ui.requests.SeatRequestAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Shown to the RIDE POSTER.
 * Displays all PENDING seat requests with Accept / Reject actions.
 * Integrated with Phase 3 offline cache support.
 */
@AndroidEntryPoint
public class SeatRequestsActivity extends BaseActivity {

    private TextView rideRouteSubtitle, seatsRemainingInfo, rideDateInfo, pendingCountBadge;
    private RecyclerView requestsRecyclerView;
    private LinearLayout emptyStateView;

    private SeatRequestAdapter adapter;
    private final List<SeatRequest> pendingRequests = new ArrayList<>();

    @Inject
    com.princeraj.campustaxipooling.repository.IRideRepository rideRepo;
    
    @Inject
    com.princeraj.campustaxipooling.repository.IUserRepository userRepo;

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
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        loadRide();
        listenToRequests();
    }

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

    private void loadRide() {
        rideRepo.getRideById(rideId).observe(this, result -> {
            if (result.getData() != null) {
                currentRide = result.getData();
                updateRideHeader(currentRide);
            }
        });
    }

    private void updateRideHeader(Ride ride) {
        rideRouteSubtitle.setText(ride.getSource() + " → " + ride.getDestination());
        seatsRemainingInfo.setText("💺 " + ride.getSeatsRemaining() + " of " + ride.getTotalSeats() + " seats remaining");
        if (ride.getJourneyDateTime() != null) {
            Date date = ride.getJourneyDateTime().toDate();
            rideDateInfo.setText(new SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault()).format(date));
        }
    }

    private void listenToRequests() {
        // Phase 3: Observe LiveData from repository
        rideRepo.getPendingRequestsForRide(rideId).observe(this, result -> {
            if (result.getData() != null) {
                pendingRequests.clear();
                pendingRequests.addAll(result.getData());
                
                // Sort locally by requestedAt (ASCENDING)
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
                    ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateTitle)).setText(getString(R.string.empty_title));
                    ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateSubtitle)).setText(getString(R.string.empty_desc));
                }
            }
        });
    }

    private void handleAccept(SeatRequest request) {
        if (currentRide == null) return;

        if (currentRide.getSeatsRemaining() <= 0) {
            Snackbar.make(requestsRecyclerView, "No seats remaining to accept.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.auth.FirebaseUser user = userRepo.getCurrentFirebaseUser();
        String currentUid = user != null ? user.getUid() : "";

        // Phase 3: Modern Repository call
        rideRepo.acceptSeatRequest(
                        rideId,
                        request.getRequestId(),
                        request.getRequesterUid(),
                        request.getRequesterName(),
                        currentUid)
                .observe(this, result -> {
                    if (result.isLoading()) return;

                    if (result.isSuccess()) {
                        Snackbar.make(requestsRecyclerView, "✅ " + request.getRequesterName() + " accepted! Chat unlocked.", Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(requestsRecyclerView, "Failed to accept: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleReject(SeatRequest request) {
        rideRepo.rejectSeatRequest(rideId, request.getRequestId(), "").observe(this, result -> {
            if (result.isLoading()) return;
            if (result.isSuccess()) {
                Snackbar.make(requestsRecyclerView, "Request rejected.", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(requestsRecyclerView, "Failed to reject: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
