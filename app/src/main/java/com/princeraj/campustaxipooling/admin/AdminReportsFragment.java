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
import com.google.firebase.firestore.DocumentSnapshot;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Report;
import com.princeraj.campustaxipooling.repository.ReportRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows all PENDING reports in real-time.
 * Admin can Dismiss (mark DISMISSED) or Ban User (mark ACTIONED + ban).
 */
public class AdminReportsFragment extends Fragment {

    private RecyclerView reportsRecyclerView;
    private LinearLayout emptyStateView;
    private TextView pendingCountLabel;

    private AdminReportAdapter adapter;
    private final List<Report> reports = new ArrayList<>();

    private final ReportRepository reportRepo = ReportRepository.getInstance();

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
        reportRepo.getPendingReports()
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    reports.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Report report = doc.toObject(Report.class);
                        if (report != null) {
                            // Inject the document ID as reportId
                            report.setReportId(doc.getId());
                            reports.add(report);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    int count = reports.size();
                    pendingCountLabel.setText(count + " pending");

                    boolean empty = reports.isEmpty();
                    reportsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
                });
    }

    private void onDismiss(Report report) {
        reportRepo.reviewReport(report.getReportId(), "DISMISSED",
                "Reviewed by admin — no action required")
                .addOnSuccessListener(v ->
                        Snackbar.make(reportsRecyclerView,
                                "Report dismissed.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Snackbar.make(reportsRecyclerView,
                                "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
    }

    private void onBanUser(Report report) {
        if (report.getTargetUid() == null || report.getTargetUid().isEmpty()) {
            Snackbar.make(reportsRecyclerView,
                    "No target user to ban.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String reason = "Banned: " + report.getCategory() + " — " + report.getDescription();

        // Atomically: ban user + mark report as ACTIONED
        reportRepo.banUser(report.getTargetUid(), reason)
                .addOnSuccessListener(v ->
                        reportRepo.reviewReport(report.getReportId(), "ACTIONED",
                                "User banned by admin")
                                .addOnSuccessListener(v2 ->
                                        Snackbar.make(reportsRecyclerView,
                                                "User banned and report resolved.",
                                                Snackbar.LENGTH_LONG).show()))
                .addOnFailureListener(e ->
                        Snackbar.make(reportsRecyclerView,
                                "Failed to ban: " + e.getMessage(),
                                Snackbar.LENGTH_SHORT).show());
    }
}
