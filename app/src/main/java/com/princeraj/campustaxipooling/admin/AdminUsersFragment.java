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
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.repository.IAdminRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Admin user management list.
 * Supports search by name/email, and filter by All / Banned / Flagged (3+ reports).
 * Ban/Unban is done inline via AdminUserAdapter.
 */
@AndroidEntryPoint
public class AdminUsersFragment extends Fragment {

    private RecyclerView usersRecyclerView;
    private EditText searchEt;
    private ChipGroup filterChipGroup;

    private AdminUserAdapter adapter;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();

    @Inject
    IAdminRepository adminRepo;

    // Filter state
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usersRecyclerView = view.findViewById(R.id.usersRecyclerView);
        searchEt = view.findViewById(R.id.searchEt);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        adapter = new AdminUserAdapter(filteredUsers, adminRepo);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        usersRecyclerView.setAdapter(adapter);

        setupSearch();
        setupFilterChips();
        loadUsers();
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
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) return;
            
            if (checkedId == R.id.chipBanned) currentFilter = "BANNED";
            else if (checkedId == R.id.chipFlagged) currentFilter = "FLAGGED";
            else currentFilter = "ALL";

            String query = searchEt.getText() != null
                    ? searchEt.getText().toString() : "";
            applyFilter(query);
        });
    }

    private void loadUsers() {
        adminRepo.getAllUsers().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess() && result.getData() != null) {
                allUsers.clear();
                allUsers.addAll(result.getData());
                applyFilter(searchEt.getText() != null
                        ? searchEt.getText().toString() : "");
            }
        });
    }

    private void applyFilter(String query) {
        filteredUsers.clear();
        String lower = query.toLowerCase();

        for (User user : allUsers) {
            // Text search
            boolean matchesSearch = lower.isEmpty()
                    || (user.getName() != null && user.getName().toLowerCase().contains(lower))
                    || (user.getEmail() != null && user.getEmail().toLowerCase().contains(lower));

            // Chip filter
            boolean matchesFilter;
            switch (currentFilter) {
                case "BANNED":
                    matchesFilter = user.isBanned();
                    break;
                case "FLAGGED":
                    // reportCount >= 3 counts as flagged
                    matchesFilter = user.getReportCount() >= 3 && !user.isBanned();
                    break;
                default:
                    matchesFilter = true;
            }

            if (matchesSearch && matchesFilter) {
                filteredUsers.add(user);
            }
        }

        adapter.notifyDataSetChanged();
    }
}
