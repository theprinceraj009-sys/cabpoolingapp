package com.princeraj.campustaxipooling.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Report;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the admin pending-reports list.
 * Maps category codes to emoji labels.
 * Double-tap protection via an actioned ID set.
 */
public class AdminReportAdapter extends
        RecyclerView.Adapter<AdminReportAdapter.ReportViewHolder> {

    public interface OnReportActionListener {
        void onAction(Report report);
    }

    private final List<Report> reports;
    private final OnReportActionListener onDismiss;
    private final OnReportActionListener onBanUser;
    private final java.util.Set<String> actioned = new java.util.HashSet<>();

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault());

    public AdminReportAdapter(List<Report> reports,
                              OnReportActionListener onDismiss,
                              OnReportActionListener onBanUser) {
        this.reports = reports;
        this.onDismiss = onDismiss;
        this.onBanUser = onBanUser;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reports.get(position), onDismiss, onBanUser, actioned);
    }

    @Override
    public int getItemCount() { return reports.size(); }

    static class ReportViewHolder extends RecyclerView.ViewHolder {

        private final TextView categoryChip, typeChip, timeText, reporterInfo, description;
        private final MaterialButton dismissBtn, banUserBtn;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryChip = itemView.findViewById(R.id.reportCategoryChip);
            typeChip = itemView.findViewById(R.id.reportTypeChip);
            timeText = itemView.findViewById(R.id.reportTimeText);
            reporterInfo = itemView.findViewById(R.id.reporterInfo);
            description = itemView.findViewById(R.id.reportDescription);
            dismissBtn = itemView.findViewById(R.id.dismissBtn);
            banUserBtn = itemView.findViewById(R.id.banUserBtn);
        }

        void bind(Report report, OnReportActionListener onDismiss,
                  OnReportActionListener onBan, java.util.Set<String> actioned) {

            categoryChip.setText(friendlyCategory(report.getCategory()));
            typeChip.setText(report.getTargetType() != null
                    ? report.getTargetType() : "RIDE");

            if (report.getReportedAt() != null) {
                timeText.setText(FMT.format(report.getReportedAt().toDate()));
            }

            // Reporter info — show truncated UIDs; Phase 7 can denormalize names
            String target = report.getTargetUid() != null
                    ? "Target: " + shortId(report.getTargetUid())
                    : "Ride: " + shortId(report.getTargetRideId());
            reporterInfo.setText("Reporter: " + shortId(report.getReporterUid())
                    + "  ·  " + target);

            // Description
            String desc = report.getDescription();
            if (desc != null && !desc.isEmpty()) {
                description.setText("\"" + desc + "\"");
                description.setVisibility(View.VISIBLE);
            } else {
                description.setVisibility(View.GONE);
            }

            // Double-tap protection
            String id = report.getReportId();
            boolean done = id != null && actioned.contains(id);
            dismissBtn.setEnabled(!done);
            banUserBtn.setEnabled(!done);

            if (!done) {
                dismissBtn.setOnClickListener(v -> {
                    if (id != null) actioned.add(id);
                    dismissBtn.setEnabled(false);
                    banUserBtn.setEnabled(false);
                    onDismiss.onAction(report);
                });

                banUserBtn.setOnClickListener(v -> {
                    if (id != null) actioned.add(id);
                    dismissBtn.setEnabled(false);
                    banUserBtn.setEnabled(false);
                    onBan.onAction(report);
                });
            }
        }

        private String friendlyCategory(String cat) {
            if (cat == null) return "OTHER";
            switch (cat) {
                case "HARASSMENT":   return "🚫 Harassment";
                case "FAKE_LISTING": return "❌ Fake Listing";
                case "SCAM":         return "💸 Scam";
                case "INAPPROPRIATE": return "⚠️ Inappropriate";
                default:             return "📌 Other";
            }
        }

        private String shortId(String uid) {
            if (uid == null) return "N/A";
            return uid.length() > 8 ? uid.substring(0, 8) + "…" : uid;
        }
    }
}
