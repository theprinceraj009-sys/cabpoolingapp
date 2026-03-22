package com.princeraj.campustaxipooling.ui.feed;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.PostRideActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.repository.RideRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * The main ride browse feed. Shows all ACTIVE rides from the campus.
 * Uses a real-time Firestore listener so new rides appear without refresh.
 */
public class RideFeedFragment extends Fragment {

    private RecyclerView ridesRecyclerView;
    private LinearLayout emptyStateView;
    private ExtendedFloatingActionButton postRideFab;
    private TextInputEditText searchEt;

    private RideFeedAdapter adapter;
    private final List<Ride> allRides = new ArrayList<>();
    private final List<Ride> filteredRides = new ArrayList<>();

    private final RideRepository rideRepo = RideRepository.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ride_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ridesRecyclerView = view.findViewById(R.id.ridesRecyclerView);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        postRideFab = view.findViewById(R.id.postRideFab);
        searchEt = view.findViewById(R.id.searchEt);

        setupRecyclerView();
        setupSearch();
        loadRides();

        postRideFab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PostRideActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new RideFeedAdapter(filteredRides, ride ->
                // Navigate to RideDetailActivity with rideId
                startActivity(
                        new Intent(requireContext(),
                                com.princeraj.campustaxipooling.RideDetailActivity.class)
                                .putExtra("rideId", ride.getRideId())
                )
        );
        ridesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ridesRecyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRides(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadRides() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Real-time listener on the ride feed query
        rideRepo.getRideFeed("CU_CHANDIGARH", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    allRides.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Ride ride = doc.toObject(Ride.class);
                        if (ride != null) {
                            allRides.add(ride);
                        }
                    }

                    // Apply current search filter
                    String currentQuery = searchEt.getText() != null
                            ? searchEt.getText().toString() : "";
                    filterRides(currentQuery);
                });
    }

    private void filterRides(String query) {
        filteredRides.clear();
        if (query.isEmpty()) {
            filteredRides.addAll(allRides);
        } else {
            String lower = query.toLowerCase();
            for (Ride ride : allRides) {
                if ((ride.getSource() != null && ride.getSource().toLowerCase().contains(lower))
                        || (ride.getDestination() != null
                        && ride.getDestination().toLowerCase().contains(lower))) {
                    filteredRides.add(ride);
                }
            }
        }

        adapter.notifyDataSetChanged();

        // Show/hide empty state
        if (filteredRides.isEmpty()) {
            ridesRecyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            ridesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }
}
