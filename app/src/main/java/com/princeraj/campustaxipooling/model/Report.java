package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents a user report submitted against a ride or another user.
 * Stored in Firestore: reports/{reportId}
 */
public class Report {

    @DocumentId
    private String reportId;

    private String reporterUid;
    private String targetUid;      // The user being reported (null if ride report)
    private String targetRideId;   // The ride being reported (null if user report)
    private String targetType;     // "USER" | "RIDE" | "MESSAGE"
    private String category;       // "HARASSMENT" | "FAKE_LISTING" | "SCAM" | "INAPPROPRIATE" | "OTHER"
    private String description;
    private String status;         // "PENDING" | "REVIEWED" | "DISMISSED" | "ACTIONED"
    private String adminNote;
    private Timestamp reportedAt;
    private Timestamp reviewedAt;

    public Report() {}

    public Report(String reporterUid, String targetUid, String targetRideId,
                  String targetType, String category, String description) {
        this.reporterUid = reporterUid;
        this.targetUid = targetUid;
        this.targetRideId = targetRideId;
        this.targetType = targetType;
        this.category = category;
        this.description = description;
        this.status = "PENDING";
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getReportId() { return reportId; }
    public String getReporterUid() { return reporterUid; }
    public String getTargetUid() { return targetUid; }
    public String getTargetRideId() { return targetRideId; }
    public String getTargetType() { return targetType; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getAdminNote() { return adminNote; }
    public Timestamp getReportedAt() { return reportedAt; }
    public Timestamp getReviewedAt() { return reviewedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setReportId(String reportId) { this.reportId = reportId; }
    public void setReporterUid(String reporterUid) { this.reporterUid = reporterUid; }
    public void setTargetUid(String targetUid) { this.targetUid = targetUid; }
    public void setTargetRideId(String targetRideId) { this.targetRideId = targetRideId; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public void setReportedAt(Timestamp reportedAt) { this.reportedAt = reportedAt; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }
}
