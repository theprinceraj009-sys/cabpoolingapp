package com.princeraj.campustaxipooling.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.princeraj.campustaxipooling.model.Report;
import com.princeraj.campustaxipooling.util.SafeResult;

import dagger.hilt.android.scopes.ActivityScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Refactored ReportRepository with SafeResult wrapper.
 * Handles all report submission and admin moderation operations.
 */
public class ReportRepositoryImpl implements IReportRepository {

    private static final String TAG = "ReportRepository";
    private static final String REPORTS_COLLECTION = "reports";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();

    @Inject
    public ReportRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public LiveData<SafeResult<Void>> submitReport(Report report) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        report.setReportedAt(Timestamp.now());
        report.setStatus("PENDING");

        WriteBatch batch = db.batch();
        var reportRef = db.collection(REPORTS_COLLECTION).document();
        batch.set(reportRef, report);

        // Increment reportCount on the reported user (if user report)
        if (report.getTargetUid() != null && !report.getTargetUid().isEmpty()) {
            var userRef = db.collection(USERS_COLLECTION).document(report.getTargetUid());
            batch.update(userRef, "reportCount", FieldValue.increment(1));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Report submitted successfully");
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit report", e);
                    liveData.setValue(SafeResult.error(e, "Failed to submit report. Please try again."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<List<Report>>> getPendingReports() {
        MutableLiveData<SafeResult<List<Report>>> liveData = new MutableLiveData<>(SafeResult.loading());

        ListenerRegistration listener = db.collection(REPORTS_COLLECTION)
                .whereEqualTo("status", "PENDING")
                .orderBy("reportedAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching pending reports", error);
                        liveData.setValue(SafeResult.error(error, "Failed to load reports."));
                        return;
                    }

                    if (snapshot == null) {
                        liveData.setValue(SafeResult.success(new ArrayList<>()));
                        return;
                    }

                    List<Report> reports = snapshot.toObjects(Report.class);
                    liveData.setValue(SafeResult.success(reports));
                });

        activeListeners.add(listener);
        return liveData;
    }

    @Override
    public LiveData<SafeResult<List<Report>>> getReportsForUser(String targetUid) {
        MutableLiveData<SafeResult<List<Report>>> liveData = new MutableLiveData<>(SafeResult.loading());

        ListenerRegistration listener = db.collection(REPORTS_COLLECTION)
                .whereEqualTo("targetUid", targetUid)
                .orderBy("reportedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching reports for user", error);
                        liveData.setValue(SafeResult.error(error, "Failed to load reports."));
                        return;
                    }

                    if (snapshot == null) {
                        liveData.setValue(SafeResult.success(new ArrayList<>()));
                        return;
                    }

                    List<Report> reports = snapshot.toObjects(Report.class);
                    liveData.setValue(SafeResult.success(reports));
                });

        activeListeners.add(listener);
        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> reviewReport(String reportId, String status, String adminNote) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> update = new HashMap<>();
        update.put("status", status);
        update.put("adminNote", adminNote);
        update.put("reviewedAt", Timestamp.now());

        db.collection(REPORTS_COLLECTION)
                .document(reportId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Report reviewed: " + reportId);
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to review report", e);
                    liveData.setValue(SafeResult.error(e, "Failed to review report."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> banUser(String targetUid, String reason) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> update = new HashMap<>();
        update.put("isBanned", true);
        update.put("banReason", reason);
        update.put("updatedAt", Timestamp.now());

        db.collection(USERS_COLLECTION)
                .document(targetUid)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User banned: " + targetUid);
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to ban user", e);
                    liveData.setValue(SafeResult.error(e, "Failed to ban user."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> unbanUser(String targetUid) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> update = new HashMap<>();
        update.put("isBanned", false);
        update.put("banReason", null);
        update.put("updatedAt", Timestamp.now());

        db.collection(USERS_COLLECTION)
                .document(targetUid)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User unbanned: " + targetUid);
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to unban user", e);
                    liveData.setValue(SafeResult.error(e, "Failed to unban user."));
                });

        return liveData;
    }

    /**
     * Cleanup: remove all active listeners when repository is destroyed.
     */
    public void cleanup() {
        for (ListenerRegistration listener : activeListeners) {
            listener.remove();
        }
        activeListeners.clear();
    }
}

