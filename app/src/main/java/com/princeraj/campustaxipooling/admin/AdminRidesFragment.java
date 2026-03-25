package com.princeraj.campustaxipooling.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.repository.IAdminRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Admin Ride Management list.
 * Allows searching by route, filtering by ACTIVE/FULL/DELETED,
 * and clicking to hard-delete fully if harmful.
 */
@AndroidEntryPoint
public class AdminRidesFragment extends Fragment {

    private RecyclerView ridesRecyclerView;
    private EditText searchEt;
    private ChipGroup statusChipGroup;

    private AdminRideAdapter adapter;
    private final List<Ride> allRides = new ArrayList<>();
    private final List<Ride> filteredRides = new ArrayList<>();

    @Inject
    IAdminRepository adminRepo;

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
        statusChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) {
                currentStatus = "ALL";
            } else {
                if (checkedId == R.id.chipActive) currentStatus = "ACTIVE";
                else if (checkedId == R.id.chipFull) currentStatus = "FULL";
                else if (checkedId == R.id.chipDeleted) currentStatus = "DELETED";
                else currentStatus = "ALL";
            }

            applyFilter(searchEt.getText() != null ? searchEt.getText().toString() : "");
        });
    }

    private void loadAllRides() {
        adminRepo.getAllRides().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess() && result.getData() != null) {
                allRides.clear();
                allRides.addAll(result.getData());
                applyFilter(searchEt.getText() != null ? searchEt.getText().toString() : "");
            }
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
                matchesStatus = !ride.isDeleted() && currentStatus.equalsIgnoreCase(ride.getStatus());
            }

            if (matchesSearch && matchesStatus) {
                filteredRides.add(ride);
            }
        }
        
        adapter.notifyDataSetChanged();
    }

    private void onForceDeleteRide(Ride ride) {
        adminRepo.reviewReport(ride.getRideId(), "DELETED", "Hard deleted by admin")
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                         Snackbar.make(ridesRecyclerView, "Ride deleted.", Snackbar.LENGTH_SHORT).show();
                    } else if (result.isError()) {
                         Snackbar.make(ridesRecyclerView, "Failed: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }
}
