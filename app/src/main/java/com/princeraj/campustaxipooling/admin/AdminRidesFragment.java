package com.princeraj.campustaxipooling.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Ride;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin Ride Management list.
 * Allows searching by route, filtering by ACTIVE/FULL/DELETED,
 * and clicking to hard-delete fully if harmful.
 */
public class AdminRidesFragment extends Fragment {

    private RecyclerView ridesRecyclerView;
    private TextInputEditText searchEt;
    private ChipGroup statusChipGroup;

    private AdminRideAdapter adapter;
    private final List<Ride> allRides = new ArrayList<>();
    private final List<Ride> filteredRides = new ArrayList<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String currentStatus = "ACTIVE"; // "ACTIVE" | "FULL" | "DELETED" | "ALL"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_rides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ridesRecyclerView = view.findViewById(R.id.ridesRecyclerView);
        searchEt = view.findViewById(R.id.searchEt);
        statusChipGroup = view.findViewById(R.id.statusChipGroup);

        adapter = new AdminRideAdapter(filteredRides, this::onForceDeleteRide);
        ridesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ridesRecyclerView.setAdapter(adapter);

        setupSearch();
        setupFilterChips();
        loadAllRides();
    }

    private void setupSearch() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                applyFilter(s.toString());
            }
        });
    }

    private void setupFilterChips() {
        statusChipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) {
                currentStatus = "ALL";
            } else {
                int id = ids.get(0);
                if (id == R.id.chipActive) currentStatus = "ACTIVE";
                else if (id == R.id.chipFull) currentStatus = "FULL";
                else if (id == R.id.chipDeleted) currentStatus = "DELETED";
            }

            applyFilter(searchEt.getText() != null ? searchEt.getText().toString() : "");
        });
    }

    private void loadAllRides() {
        db.collection("rides")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    
                    allRides.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Ride ride = doc.toObject(Ride.class);
                        if (ride != null) allRides.add(ride);
                    }
                    
                    applyFilter(searchEt.getText() != null ? searchEt.getText().toString() : "");
                });
    }

    private void applyFilter(String query) {
        filteredRides.clear();
        String lower = query.toLowerCase();

        for (Ride ride : allRides) {

            // Text match
            boolean matchesSearch = lower.isEmpty() ||
                (ride.getSource() != null && ride.getSource().toLowerCase().contains(lower)) ||
                (ride.getDestination() != null && ride.getDestination().toLowerCase().contains(lower));

            // Status match
            boolean matchesStatus;
            if (currentStatus.equals("ALL")) {
                matchesStatus = true;
            } else if (currentStatus.equals("DELETED")) {
                matchesStatus = ride.isDeleted();
            } else {
                matchesStatus = !ride.isDeleted() && currentStatus.equals(ride.getStatus());
            }

            if (matchesSearch && matchesStatus) {
                filteredRides.add(ride);
            }
        }
        
        adapter.notifyDataSetChanged();
    }

    private void onForceDeleteRide(Ride ride) {
        // Soft delete via Firestore if not already
        db.collection("rides").document(ride.getRideId())
                .update("isDeleted", true)
                .addOnSuccessListener(a -> 
                    Snackbar.make(ridesRecyclerView, "Ride deleted.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                    Snackbar.make(ridesRecyclerView, "Failed to delete: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
    }
}
