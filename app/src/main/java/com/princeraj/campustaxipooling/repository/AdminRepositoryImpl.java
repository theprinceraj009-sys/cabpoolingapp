package com.princeraj.campustaxipooling.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.Report;
import com.princeraj.campustaxipooling.util.SafeResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdminRepositoryImpl implements IAdminRepository {

    private final FirebaseFirestore db;
    private static final String USERS_COL = "users";
    private static final String RIDES_COL = "rides";
    private static final String REPORTS_COL = "reports";

    @Inject
    public AdminRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public LiveData<SafeResult<Map<String, Long>>> getSystemStats() {
        MutableLiveData<SafeResult<Map<String, Long>>> statsLiveData = new MutableLiveData<>();
        statsLiveData.setValue(SafeResult.loading());

        Map<String, Long> stats = new HashMap<>();
        
        // Use parallel fetch for speed (Admin needs real-time global state)
        db.collection(USERS_COL).get().addOnSuccessListener(userSnaps -> {
            stats.put("totalUsers", (long) userSnaps.size());
            
            db.collection(RIDES_COL).whereEqualTo("status", "ACTIVE").get().addOnSuccessListener(rideSnaps -> {
                stats.put("activeRides", (long) rideSnaps.size());
                
                db.collection(REPORTS_COL).whereEqualTo("status", "PENDING").get().addOnSuccessListener(reportSnaps -> {
                    stats.put("pendingReports", (long) reportSnaps.size());
                    statsLiveData.setValue(SafeResult.success(stats));
                });
            });
        }).addOnFailureListener(e -> statsLiveData.setValue(SafeResult.error(e.getMessage())));

        return statsLiveData;
    }

    @Override
    public LiveData<SafeResult<List<User>>> getAllUsers() {
        MutableLiveData<SafeResult<List<User>>> userLiveData = new MutableLiveData<>();
        userLiveData.setValue(SafeResult.loading());

        db.collection(USERS_COL)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        userLiveData.setValue(SafeResult.error(error.getMessage()));
                        return;
                    }
                    if (snapshots != null) {
                        userLiveData.setValue(SafeResult.success(snapshots.toObjects(User.class)));
                    }
                });

        return userLiveData;
    }

    @Override
    public void updateUserBanStatus(String uid, boolean isBanned, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isBanned", isBanned);
        updates.put("banReason", reason);
        if (isBanned) {
            updates.put("role", "BANNED");
        } else {
            updates.put("role", "STUDENT"); // Restore default
        }

        db.collection(USERS_COL).document(uid).update(updates);
    }

    @Override
    public LiveData<SafeResult<List<Ride>>> getAllRides() {
        MutableLiveData<SafeResult<List<Ride>>> rideLiveData = new MutableLiveData<>();
        rideLiveData.setValue(SafeResult.loading());

        db.collection(RIDES_COL)
                .orderBy("journeyDateTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        rideLiveData.setValue(SafeResult.error(error.getMessage()));
                        return;
                    }
                    if (snapshots != null) {
                        rideLiveData.setValue(SafeResult.success(snapshots.toObjects(Ride.class)));
                    }
                });

        return rideLiveData;
    }

    @Override
    public LiveData<SafeResult<List<Report>>> getPendingReports() {
        MutableLiveData<SafeResult<List<Report>>> reportLiveData = new MutableLiveData<>();
        reportLiveData.setValue(SafeResult.loading());

        db.collection(REPORTS_COL)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        reportLiveData.setValue(SafeResult.error(error.getMessage()));
                        return;
                    }
                    if (snapshots != null) {
                        reportLiveData.setValue(SafeResult.success(snapshots.toObjects(Report.class)));
                    }
                });

        return reportLiveData;
    }

    @Override
    public LiveData<SafeResult<Void>> reviewReport(String reportId, String status, String adminNote) {
        MutableLiveData<SafeResult<Void>> result = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("adminNote", adminNote);
        updates.put("reviewedAt", com.google.firebase.Timestamp.now());

        db.collection(REPORTS_COL).document(reportId).update(updates)
                .addOnSuccessListener(aVoid -> result.setValue(SafeResult.success(null)))
                .addOnFailureListener(e -> result.setValue(SafeResult.error(e.getMessage())));
        
        return result;
    }

    @Override
    public LiveData<SafeResult<Void>> banUser(String uid, String reason) {
        MutableLiveData<SafeResult<Void>> result = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> updates = new HashMap<>();
        updates.put("isBanned", true);
        updates.put("banReason", reason);
        updates.put("role", "BANNED");

        db.collection(USERS_COL).document(uid).update(updates)
                .addOnSuccessListener(aVoid -> result.setValue(SafeResult.success(null)))
                .addOnFailureListener(e -> result.setValue(SafeResult.error(e.getMessage())));

        return result;
    }

    @Override
    public LiveData<SafeResult<Void>> deleteRide(String rideId) {
        MutableLiveData<SafeResult<Void>> result = new MutableLiveData<>(SafeResult.loading());

        db.collection(RIDES_COL).document(rideId).delete()
                .addOnSuccessListener(aVoid -> result.setValue(SafeResult.success(null)))
                .addOnFailureListener(e -> result.setValue(SafeResult.error(e.getMessage())));

        return result;
    }
}
