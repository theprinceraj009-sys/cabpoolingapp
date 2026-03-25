package com.princeraj.campustaxipooling.ui.myrides;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.princeraj.campustaxipooling.PostRideActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.RideDetailActivity;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.Ride;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Shows all rides posted by the current user.
 * Integrated with Phase 3 offline cache support.
 */
@AndroidEntryPoint
public class MyRidesFragment extends Fragment {

    private ShimmerFrameLayout shimmerViewContainer;
    private RecyclerView myRidesRecyclerView;
    private LinearLayout emptyStateView;
    private MaterialButton postNewRideBtn;

    private MyRidesAdapter adapter;
    private final List<Ride> myRides = new ArrayList<>();

    @Inject
    com.princeraj.campustaxipooling.repository.IRideRepository rideRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_rides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer);
        myRidesRecyclerView = view.findViewById(R.id.myRidesRecyclerView);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        postNewRideBtn = view.findViewById(R.id.postNewRideBtn);

        postNewRideBtn.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PostRideActivity.class)));

        setupRecyclerView();
        loadMyRides();
    }

    private void setupRecyclerView() {
        adapter = new MyRidesAdapter(myRides,
                // Card tap → view detail
                ride -> startActivity(
                        new Intent(requireContext(), RideDetailActivity.class)
                                .putExtra("rideId", ride.getRideId())),
                // Cancel → soft-delete via repo
                ride -> rideRepo.cancelRide(ride.getRideId()).observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                         com.google.android.material.snackbar.Snackbar.make(requireView(), "Ride cancelled successfully", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    } else if (result.isError()) {
                         com.google.android.material.snackbar.Snackbar.make(requireView(), "Error cancelling ride.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    }
                }),
                // View Requests → SeatRequestsActivity
                ride -> startActivity(
                        new Intent(requireContext(),
                                com.princeraj.campustaxipooling.SeatRequestsActivity.class)
                                .putExtra("rideId", ride.getRideId()))
        );
        myRidesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        myRidesRecyclerView.setAdapter(adapter);
    }

    private void loadMyRides() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Phase 3: Observe LiveData from repository
        rideRepo.getMyPostedRides(uid).observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading() && myRides.isEmpty()) {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.startShimmer();
                    shimmerViewContainer.setVisibility(View.VISIBLE);
                }
                return;
            }

            // Stop and hide shimmer
            if (shimmerViewContainer != null) {
                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
            }

            if (result.getData() != null) {
                myRides.clear();
                myRides.addAll(result.getData());
                
                adapter.notifyDataSetChanged();

                boolean empty = myRides.isEmpty();
                myRidesRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);

                if (empty) {
                    ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateEmoji)).setText("🚗");
                    ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateTitle)).setText(getString(R.string.empty_title));
                    ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateSubtitle)).setText(getString(R.string.empty_desc));
                }
            } else if (result.isError() && myRides.isEmpty()) {
                 myRidesRecyclerView.setVisibility(View.GONE);
                 emptyStateView.setVisibility(View.VISIBLE);
            }
        });
    }
}
