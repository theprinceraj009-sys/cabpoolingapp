package com.princeraj.campustaxipooling.sync;

import android.util.Log;

import androidx.annotation.NonNull;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import dagger.hilt.android.qualifiers.ApplicationContext;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import com.princeraj.campustaxipooling.db.CampusTaxiDatabase;
import com.princeraj.campustaxipooling.db.entity.MessageEntity;
import com.princeraj.campustaxipooling.db.entity.RideEntity;
import com.princeraj.campustaxipooling.db.entity.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SyncManager: Synchronizes offline writes with Firestore.
 *
 * Responsibility:
 * 1. Queue offline writes to Room database
 * 2. Retry failed uploads when network returns
 * 3. Mark synced on successful upload
 * 4. Handle conflicts with ConflictResolver
 *
 * Phase 3: Offline Resilience Implementation
 */
@Singleton
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String RIDES_COLLECTION = "rides";
    private static final String MESSAGES_SUB = "messages";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;
    private final CampusTaxiDatabase database;
    private final ConflictResolver conflictResolver;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public SyncManager(@ApplicationContext Context context, FirebaseFirestore db, CampusTaxiDatabase database, 
                       ConflictResolver conflictResolver) {
        this.context = context;
        this.db = db;
        this.database = database;
        this.conflictResolver = conflictResolver;
    }

    public Context getContext() {
        return context;
    }

    // ── Main Sync Operations ──────────────────────────────────────────────────

    /**
     * Syncs all unsynced rides to Firestore.
     * Called when network becomes available.
     */
    public Task<Void> syncOfflineRides() {
        return Tasks.call(executor, () -> {
            Log.d(TAG, "Starting ride sync...");

            // Step 1: Get unsynced rides from Room
            List<RideEntity> unsyncedRides = database.rideDao().getUnsyncedRides();
            if (unsyncedRides.isEmpty()) {
                Log.d(TAG, "No rides to sync");
                return null;
            }

            Log.d(TAG, "Found " + unsyncedRides.size() + " unsynced rides");

            // Step 2: Try to upload each ride
            List<Task<Void>> tasks = new ArrayList<>();
            for (RideEntity ride : unsyncedRides) {
                tasks.add(uploadRideToFirestore(ride));
            }

            Tasks.await(Tasks.whenAll(tasks));
            return null;
        });
    }

    /**
     * Syncs all unsynced messages to Firestore.
     */
    public Task<Void> syncOfflineMessages() {
        return Tasks.call(executor, () -> {
            Log.d(TAG, "Starting message sync...");

            List<MessageEntity> unsyncedMessages = database.messageDao().getUnsyncedMessages();
            if (unsyncedMessages.isEmpty()) {
                Log.d(TAG, "No messages to sync");
                return null;
            }

            Log.d(TAG, "Found " + unsyncedMessages.size() + " unsynced messages");

            List<Task<Void>> tasks = new ArrayList<>();
            for (MessageEntity message : unsyncedMessages) {
                tasks.add(uploadMessageToFirestore(message));
            }

            Tasks.await(Tasks.whenAll(tasks));
            return null;
        });
    }

    /**
     * Syncs all unsynced user profile updates.
     */
    public Task<Void> syncOfflineUsers() {
        return Tasks.call(executor, () -> {
            Log.d(TAG, "Starting user sync...");

            List<UserEntity> unsyncedUsers = database.userDao().getUnsyncedUsers();
            if (unsyncedUsers.isEmpty()) {
                Log.d(TAG, "No users to sync");
                return null;
            }

            Log.d(TAG, "Found " + unsyncedUsers.size() + " unsynced users");

            List<Task<Void>> tasks = new ArrayList<>();
            for (UserEntity user : unsyncedUsers) {
                tasks.add(uploadUserToFirestore(user));
            }

            Tasks.await(Tasks.whenAll(tasks));
            return null;
        });
    }

    /**
     * Syncs everything offline: rides, messages, users.
     * Call this when network returns.
     */
    public Task<Void> syncAll() {
        Log.d(TAG, "Syncing all offline data...");

        return Tasks.whenAll(
                syncOfflineRides(),
                syncOfflineMessages(),
                syncOfflineUsers()
        ).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Complete sync successful!");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Complete sync had failures", e);
        });
    }

    // ── Individual Upload Methods ─────────────────────────────────────────────

    /**
     * Uploads a single ride to Firestore.
     */
    @NonNull
    private Task<Void> uploadRideToFirestore(@NonNull RideEntity ride) {
        Log.d(TAG, "Uploading ride: " + ride.getRideId());

        return db.collection(RIDES_COLLECTION)
                .document(ride.getRideId())
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) return Tasks.forException(task.getException());

                    com.google.firebase.firestore.DocumentSnapshot snapshot = task.getResult();
                    if (snapshot.exists()) {
                        com.princeraj.campustaxipooling.model.Ride remote = 
                                snapshot.toObject(com.princeraj.campustaxipooling.model.Ride.class);
                        
                        if (remote != null) {
                            RideEntity merged = conflictResolver.resolveRideConflict(ride, remote);
                            if (conflictResolver.isValidMerge(merged)) {
                                return uploadRideData(merged);
                            }
                            return Tasks.forResult(null);
                        }
                    }
                    return uploadRideData(ride);
                })
                .addOnSuccessListener(aVoid -> {
                    ride.setSyncedAt(System.currentTimeMillis());
                    executor.execute(() -> database.rideDao().updateRide(ride));
                    Log.d(TAG, "Ride synced: " + ride.getRideId());
                });
    }

    /**
     * Uploads a single message to Firestore.
     */
    @NonNull
    private Task<Void> uploadMessageToFirestore(@NonNull MessageEntity message) {
        return db.collection("connections")
                .document(message.getConnectionId())
                .collection(MESSAGES_SUB)
                .document(message.getMessageId())
                .set(messageEntityToMap(message))
                .addOnSuccessListener(aVoid -> {
                    message.setSyncedAt(System.currentTimeMillis());
                    executor.execute(() -> database.messageDao().updateMessage(message));
                });
    }

    /**
     * Uploads a single user to Firestore.
     */
    @NonNull
    private Task<Void> uploadUserToFirestore(@NonNull UserEntity user) {
        return db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userEntityToMap(user))
                .addOnSuccessListener(aVoid -> {
                    user.setSyncedAt(System.currentTimeMillis());
                    executor.execute(() -> database.userDao().updateUser(user));
                });
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    private Task<Void> uploadRideData(@NonNull RideEntity ride) {
        return db.collection(RIDES_COLLECTION)
                .document(ride.getRideId())
                .set(rideEntityToMap(ride));
    }

    private java.util.Map<String, Object> rideEntityToMap(@NonNull RideEntity ride) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("rideId", ride.getRideId());
        map.put("postedByUid", ride.getPostedByUid());
        map.put("postedByName", ride.getPostedByName());
        map.put("campusId", ride.getCampusId());
        map.put("source", ride.getSource());
        map.put("destination", ride.getDestination());
        map.put("routeDescription", ride.getRouteDescription());
        if (ride.getJourneyDateTime() != null) {
            map.put("journeyDateTime", new Timestamp(ride.getJourneyDateTime(), 0));
        }
        map.put("totalFare", ride.getTotalFare());
        map.put("totalSeats", ride.getTotalSeats());
        map.put("seatsRemaining", ride.getSeatsRemaining());
        map.put("preferences", ride.getPreferences());
        map.put("proofUrl", ride.getProofUrl());
        map.put("status", ride.getStatus());
        map.put("isDeleted", ride.isDeleted());
        map.put("updatedAt", Timestamp.now());
        return map;
    }

    private java.util.Map<String, Object> messageEntityToMap(@NonNull MessageEntity message) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("senderUid", message.getSenderUid());
        map.put("text", message.getText());
        map.put("isFlagged", message.isFlagged());
        map.put("flagReason", message.getFlagReason());
        map.put("isBlocked", message.isBlocked());
        map.put("sentAt", Timestamp.now());
        return map;
    }

    private java.util.Map<String, Object> userEntityToMap(@NonNull UserEntity user) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("uid", user.getUid());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("rollNumber", user.getRollNumber());
        map.put("department", user.getDepartment());
        map.put("role", user.getRole());
        map.put("campusId", user.getCampusId());
        map.put("profilePhotoUrl", user.getProfilePhotoUrl());
        map.put("phoneNumber", user.getPhoneNumber());
        map.put("subscriptionTier", user.getSubscriptionTier());
        map.put("isBanned", user.isBanned());
        map.put("isAdmin", user.isAdmin());
        map.put("banReason", user.getBanReason());
        map.put("reportCount", user.getReportCount());
        map.put("fcmToken", user.getFcmToken());
        map.put("updatedAt", Timestamp.now());
        return map;
    }

    public void clearOfflineData() {
        executor.execute(() -> {
            database.rideDao().clearAllRides();
            database.messageDao().clearAllMessages();
            database.userDao().clearAllUsers();
        });
    }
}
