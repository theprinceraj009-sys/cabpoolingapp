package com.princeraj.campustaxipooling.ui.feed;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.princeraj.campustaxipooling.PostRideActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Ride;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * The main ride browse feed. Shows all ACTIVE rides from the campus.
 * Uses a reactive Repository layer with Room-first caching.
 */
@AndroidEntryPoint
public class RideFeedFragment extends Fragment {

    private ShimmerFrameLayout shimmerViewContainer;
    private RecyclerView ridesRecyclerView;
    private LinearLayout emptyStateView;
    private ExtendedFloatingActionButton postRideFab;
    private TextInputEditText searchEt;

    private RideFeedAdapter adapter;
    private final List<Ride> allRides = new ArrayList<>();
    private final List<Ride> filteredRides = new ArrayList<>();

    @Inject
    com.princeraj.campustaxipooling.repository.IRideRepository rideRepo;

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

        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer);
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

        // Phase 3/4: Use centralized dynamic config
        rideRepo.getRideFeed(com.princeraj.campustaxipooling.util.AppConfig.getCampusId(), uid, 50).observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading() && allRides.isEmpty()) {
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
                allRides.clear();
                allRides.addAll(result.getData());
                
                // If it's a cached result with an error (offline fallback)
                if (result.isError() && result.getData() != null) {
                    com.google.android.material.snackbar.Snackbar.make(requireView(), 
                            "Offline mode. Showing cached rides.", 
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                }

                // Sort locally by journeyDateTime (ASCENDING) - API 21 compatible
                java.util.Collections.sort(allRides, (r1, r2) -> {
                    if (r1.getJourneyDateTime() == null || r2.getJourneyDateTime() == null) return 0;
                    return r1.getJourneyDateTime().compareTo(r2.getJourneyDateTime());
                });

                filterRides(searchEt.getText() != null ? searchEt.getText().toString() : "");
            }
        });
    }

    private void filterRides(String query) {
        filteredRides.clear();
        String sanitizedQuery = query != null ? query.trim().toLowerCase() : "";

        if (sanitizedQuery.isEmpty()) {
            filteredRides.addAll(allRides);
        } else {
            for (Ride ride : allRides) {
                if ((ride.getSource() != null && ride.getSource().toLowerCase().contains(sanitizedQuery))
                        || (ride.getDestination() != null
                        && ride.getDestination().toLowerCase().contains(sanitizedQuery))) {
                    filteredRides.add(ride);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredRides.isEmpty()) {
            ridesRecyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
            
            // Phase 7: Localized labels
            ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateEmoji)).setText("🚕");
            ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateTitle)).setText(getString(R.string.empty_title));
            ((android.widget.TextView)emptyStateView.findViewById(R.id.emptyStateSubtitle))
                    .setText(sanitizedQuery.isEmpty() ? getString(R.string.empty_desc) : "No matches for your search.");
        } else {
            ridesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }
}
