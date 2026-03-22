package com.princeraj.campustaxipooling.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.princeraj.campustaxipooling.model.Report;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for all report and admin operations.
 *
 * Report pipeline:
 *   1. User submits → report document created (status=PENDING)
 *   2. reportCount on the target User document is incremented (for threshold alerting)
 *   3. Admin reviews → status updated to REVIEWED/DISMISSED/ACTIONED
 *   4. If actioned → admin bans user via banUser()
 */
public class ReportRepository {

    private static final String REPORTS_COLLECTION = "reports";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;
    private static ReportRepository instance;

    private ReportRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static ReportRepository getInstance() {
        if (instance == null) {
            instance = new ReportRepository();
        }
        return instance;
    }

    // ── Report Submission ─────────────────────────────────────────────────────

    /**
     * Submits a report and atomically increments the target user's reportCount.
     * Uses a WriteBatch for consistency.
     */
    public Task<Void> submitReport(Report report) {
        DocumentReference reportRef = db.collection(REPORTS_COLLECTION).document();
        WriteBatch batch = db.batch();

        // 1. Write the report document
        batch.set(reportRef, report);

        // 2. Increment reportCount on the reported user (if user report)
        if (report.getTargetUid() != null && !report.getTargetUid().isEmpty()) {
            DocumentReference userRef = db.collection(USERS_COLLECTION)
                    .document(report.getTargetUid());
            batch.update(userRef, "reportCount", FieldValue.increment(1));
        }

        return batch.commit();
    }

    // ── Admin Operations ──────────────────────────────────────────────────────

    /**
     * Gets all PENDING reports ordered by submission time. Admin only.
     */
    public Query getPendingReports() {
        return db.collection(REPORTS_COLLECTION)
                .whereEqualTo("status", "PENDING")
                .orderBy("reportedAt", Query.Direction.ASCENDING);
    }

    /**
     * Gets all reports made against a specific user. Admin only.
     */
    public Query getReportsForUser(String targetUid) {
        return db.collection(REPORTS_COLLECTION)
                .whereEqualTo("targetUid", targetUid)
                .orderBy("reportedAt", Query.Direction.DESCENDING);
    }

    /**
     * Admin marks a report as reviewed with a note.
     */
    public Task<Void> reviewReport(String reportId, String status, String adminNote) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", status);
        update.put("adminNote", adminNote);
        update.put("reviewedAt", Timestamp.now());
        return db.collection(REPORTS_COLLECTION).document(reportId).update(update);
    }

    /**
     * Admin bans a user. Atomically sets isBanned=true and banReason on the user document.
     */
    public Task<Void> banUser(String targetUid, String reason) {
        Map<String, Object> update = new HashMap<>();
        update.put("isBanned", true);
        update.put("banReason", reason);
        update.put("updatedAt", Timestamp.now());
        return db.collection(USERS_COLLECTION).document(targetUid).update(update);
    }

    /**
     * Admin lifts a ban on a user.
     */
    public Task<Void> unbanUser(String targetUid) {
        Map<String, Object> update = new HashMap<>();
        update.put("isBanned", false);
        update.put("banReason", null);
        update.put("updatedAt", Timestamp.now());
        return db.collection(USERS_COLLECTION).document(targetUid).update(update);
    }
}
