package com.princeraj.campustaxipooling.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Report;
import com.princeraj.campustaxipooling.repository.IAdminRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Shows all PENDING reports in real-time.
 * Admin can Dismiss (mark DISMISSED) or Ban User (mark ACTIONED + ban).
 */
@AndroidEntryPoint
public class AdminReportsFragment extends Fragment {

    private RecyclerView reportsRecyclerView;
    private LinearLayout emptyStateView;
    private TextView pendingCountLabel;

    private AdminReportAdapter adapter;
    private final List<Report> reports = new ArrayList<>();

    @Inject
    IAdminRepository adminRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        pendingCountLabel = view.findViewById(R.id.pendingCountLabel);

        adapter = new AdminReportAdapter(
                reports,
                this::onDismiss,
                this::onBanUser
        );
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        reportsRecyclerView.setAdapter(adapter);

        listenToPendingReports();
    }

    private void listenToPendingReports() {
        adminRepo.getPendingReports().observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading()) return;

            if (result.isSuccess() && result.getData() != null) {
                reports.clear();
                reports.addAll(result.getData());
                adapter.notifyDataSetChanged();

                int count = reports.size();
                pendingCountLabel.setText(count + " pending");

                boolean empty = reports.isEmpty();
                reportsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void onDismiss(Report report) {
        adminRepo.reviewReport(report.getReportId(), "DISMISSED",
                "Reviewed by admin — no action required")
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                        Snackbar.make(reportsRecyclerView, "Report dismissed.", Snackbar.LENGTH_SHORT).show();
                    } else if (result.isError()) {
                        Snackbar.make(reportsRecyclerView, "Error: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void onBanUser(Report report) {
        if (report.getTargetUid() == null || report.getTargetUid().isEmpty()) {
            Snackbar.make(reportsRecyclerView, "No target user to ban.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String reason = "Banned: " + report.getCategory() + " — " + report.getDescription();

        // Atomically: ban user + mark report as ACTIONED
        adminRepo.banUser(report.getTargetUid(), reason)
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                         adminRepo.reviewReport(report.getReportId(), "ACTIONED", "User banned by admin")
                                 .observe(getViewLifecycleOwner(), res2 -> {
                                     if (res2.isSuccess()) {
                                         Snackbar.make(reportsRecyclerView, "User banned and report resolved.", Snackbar.LENGTH_LONG).show();
                                     }
                                 });
                    } else if (result.isError()) {
                         Snackbar.make(reportsRecyclerView, "Failed: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }
}
