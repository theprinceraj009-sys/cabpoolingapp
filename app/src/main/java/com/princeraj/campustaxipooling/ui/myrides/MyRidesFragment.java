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

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.princeraj.campustaxipooling.PostRideActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.RideDetailActivity;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.repository.RideRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows all rides posted by the current user.
 * Powered by a real-time Firestore listener.
 * Each card shows cancel option for ACTIVE rides.
 */
public class MyRidesFragment extends Fragment {

    private RecyclerView myRidesRecyclerView;
    private LinearLayout emptyStateView;
    private MaterialButton postNewRideBtn;

    private MyRidesAdapter adapter;
    private final List<Ride> myRides = new ArrayList<>();

    private final RideRepository rideRepo = RideRepository.getInstance();

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
                ride -> rideRepo.cancelRide(ride.getRideId()),
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
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        rideRepo.getMyPostedRides(uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    myRides.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Ride ride = doc.toObject(Ride.class);
                        if (ride != null) {
                            myRides.add(ride);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    boolean empty = myRides.isEmpty();
                    myRidesRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
                });
    }
}
