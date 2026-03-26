package com.princeraj.campustaxipooling.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import com.princeraj.campustaxipooling.model.Connection;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.SeatRequest;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.util.NotificationApi;
import com.princeraj.campustaxipooling.util.SafeResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import com.princeraj.campustaxipooling.util.AppConfig;
import com.princeraj.campustaxipooling.util.FirestoreLogger;

/**
 * Refactored RideRepository with SafeResult wrapper.
 *
 * Key improvements:
 * - Returns LiveData<SafeResult<T>> for all operations (consistent error handling)
 * - All Firebase operations are decoupled from ViewModels
 * - Easy to mock in unit tests
 *
 * Architecture: Repository pattern
 * - ViewModel observes LiveData and updates UI automatically
 * - All Firebase operations are decoupled from ViewModels
 * - Easy to mock in unit tests
 */
@javax.inject.Singleton
public class RideRepositoryImpl implements IRideRepository {

    private static final String TAG = "RideRepository";
    private static final String RIDES_COLLECTION = "rides";
    private static final String SEAT_REQUESTS_SUB = "seatRequests";
    private static final String CONNECTIONS_COLLECTION = "connections";

    private final FirebaseFirestore db;
    private final com.princeraj.campustaxipooling.db.CampusTaxiDatabase database;
    private final com.princeraj.campustaxipooling.sync.SyncManager syncManager;
    private final com.princeraj.campustaxipooling.util.AppExecutors executors;
    private final java.util.List<com.google.firebase.firestore.ListenerRegistration> activeListeners = new java.util.ArrayList<>();

    @javax.inject.Inject
    public RideRepositoryImpl(FirebaseFirestore db, 
                            com.princeraj.campustaxipooling.db.CampusTaxiDatabase database, 
                            com.princeraj.campustaxipooling.sync.SyncManager syncManager,
                            com.princeraj.campustaxipooling.util.AppExecutors executors) {
        this.db = db;
        this.database = database;
        this.syncManager = syncManager;
        this.executors = executors;
    }

    // ── Ride CRUD ─────────────────────────────────────────────────────────────

    @Override
    public LiveData<SafeResult<String>> postRide(Ride ride) {
        MutableLiveData<SafeResult<String>> liveData = new MutableLiveData<>(SafeResult.loading());

        // 1. Map & Save to Local Room DB (Immediate UI feedback path)
        final String rideId = db.collection(RIDES_COLLECTION).document().getId();
        ride.setRideId(rideId);
        ride.setDeleted(false);

        final com.princeraj.campustaxipooling.db.entity.RideEntity entity = new com.princeraj.campustaxipooling.db.entity.RideEntity();
        entity.setRideId(rideId);
        entity.setPostedByUid(ride.getPostedByUid());
        entity.setPostedByName(ride.getPostedByName());
        entity.setCampusId(ride.getCampusId());
        entity.setSource(ride.getSource());
        entity.setDestination(ride.getDestination());
        entity.setRouteDescription(ride.getRouteDescription());
        if (ride.getJourneyDateTime() != null) {
            entity.setJourneyDateTime(ride.getJourneyDateTime().toDate().getTime());
        }
        entity.setTotalFare(ride.getTotalFare());
        entity.setTotalSeats(ride.getTotalSeats());
        entity.setSeatsRemaining(ride.getSeatsRemaining());
        entity.setStatus(ride.getStatus());
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setSyncedAt(null);
        entity.setDeleted(false);

        executors.diskIO().execute(() -> {
            database.rideDao().insertRide(entity);
            
            // 2. Initiate Firestore upload (Firebase handles its own network threads)
            db.collection(RIDES_COLLECTION)
                    .document(rideId)
                    .set(ride)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Ride synced to Firestore: " + rideId);
                        FirestoreLogger.getInstance().logAction(ride.getPostedByUid(), "RIDE_CREATED", "Ride posted to " + ride.getDestination());
                        entity.setSyncedAt(System.currentTimeMillis());
                        executors.diskIO().execute(() -> database.rideDao().updateRide(entity));
                        liveData.postValue(SafeResult.success(rideId));
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to sync ride to Firestore (will retry background sync)", e);
                        liveData.postValue(SafeResult.success(rideId));
                    });
        });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<List<Ride>>> getRideFeed(String campusId, String currentUserUid, int limit) {
        MutableLiveData<SafeResult<List<Ride>>> liveData = new MutableLiveData<>(SafeResult.loading());

        // ── Phase 5: Query Optimization (Cache-First) ──
        // Immediately fetch from local Room DB to minimize perceived latency
        executors.diskIO().execute(() -> {
            List<com.princeraj.campustaxipooling.db.entity.RideEntity> cachedEntities = 
                    database.rideDao().getRideFeed(campusId, limit);
            if (!cachedEntities.isEmpty()) {
                liveData.postValue(SafeResult.success(mapEntitiesToRides(cachedEntities)));
            }
        });

        // Preferred query: with composite index (campusId + isDeleted + status + journeyDateTime DESC).
        // If the index hasn't been created yet in Firebase Console, this will fail with
        // FAILED_PRECONDITION. In that case we fall back to the simpler query below.
        final ListenerRegistration[] refHolder = new ListenerRegistration[1];

        ListenerRegistration listener = db.collection(RIDES_COLLECTION)
                .whereEqualTo("campusId", campusId)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("isDeleted", false)
                .orderBy("journeyDateTime", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        boolean isMissingIndex = error.getMessage() != null &&
                                (error.getMessage().contains("FAILED_PRECONDITION") ||
                                 error.getMessage().contains("requires an index"));
                        if (isMissingIndex) {
                            // Remove this broken listener and use a fallback query.
                            Log.w(TAG, "Composite index missing for rides feed — falling back " +
                                    "to simple query. Create the index via the URL in Logcat.");
                            if (refHolder[0] != null) {
                                refHolder[0].remove();
                                activeListeners.remove(refHolder[0]);
                            }
                            attachFallbackRideFeedListener(campusId, limit, liveData);
                        } else {
                            Log.w(TAG, "Firestore error in ride feed, continuing with local cache", error);
                        }
                        return;
                    }

                    if (snapshot != null) {
                        List<Ride> rides = snapshot.toObjects(Ride.class);
                        liveData.postValue(SafeResult.success(rides));

                        // ── Update Local Cache (Optimized) ──
                        executors.diskIO().execute(() -> {
                            List<com.princeraj.campustaxipooling.db.entity.RideEntity> entities = new java.util.ArrayList<>();
                            for (Ride ride : rides) {
                                entities.add(mapRideToEntity(ride, true));
                            }
                            database.rideDao().insertRides(entities);
                        });
                    }
                });

        refHolder[0] = listener;
        activeListeners.add(listener);
        return liveData;
    }

    /**
     * Fallback for when the composite index hasn't been created yet.
     * Queries only by campusId + status (no orderBy) — no index required.
     * Filter isDeleted client-side.
     */
    private void attachFallbackRideFeedListener(
            String campusId, int limit,
            MutableLiveData<SafeResult<List<Ride>>> liveData) {

        ListenerRegistration fallback = db.collection(RIDES_COLLECTION)
                .whereEqualTo("campusId", campusId)
                .whereEqualTo("status", "ACTIVE")
                .limit(limit)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Fallback ride feed query also failed", error);
                        return;
                    }
                    if (snapshot != null) {
                        List<Ride> rides = snapshot.toObjects(Ride.class);
                        // Filter out deleted rides client-side
                        List<Ride> filtered = new ArrayList<>();
                        for (Ride r : rides) {
                            if (!r.isDeleted()) filtered.add(r);
                        }
                        liveData.postValue(SafeResult.success(filtered));

                        executors.diskIO().execute(() -> {
                            for (Ride ride : filtered) {
                                database.rideDao().insertRide(mapRideToEntity(ride, true));
                            }
                        });
                    }
                });

        activeListeners.add(fallback);
    }

    @Override
    public LiveData<SafeResult<List<Ride>>> getMyPostedRides(String uid) {
        MutableLiveData<SafeResult<List<Ride>>> liveData = new MutableLiveData<>(SafeResult.loading());

        ListenerRegistration listener = db.collection(RIDES_COLLECTION)
                .whereEqualTo("postedByUid", uid)
                .whereEqualTo("isDeleted", false)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching my rides", error);
                        // ── Phase 3: Offline Fallback ──
                        executors.diskIO().execute(() -> {
                            List<com.princeraj.campustaxipooling.db.entity.RideEntity> entities = 
                                    database.rideDao().getUserRides(uid);
                            List<Ride> rides = mapEntitiesToRides(entities);
                            liveData.postValue(SafeResult.errorWithCache(error, rides, "Network error. Showing local rides."));
                        });
                        return;
                    }

                    if (snapshot != null) {
                        List<Ride> rides = snapshot.toObjects(Ride.class);
                        liveData.setValue(SafeResult.success(rides));

                        // Update cache (Optimized)
                        executors.diskIO().execute(() -> {
                            List<com.princeraj.campustaxipooling.db.entity.RideEntity> entities = new java.util.ArrayList<>();
                            for (Ride ride : rides) {
                                entities.add(mapRideToEntity(ride, true));
                            }
                            database.rideDao().insertRides(entities);
                        });
                    }
                });

        activeListeners.add(listener);
        return liveData;
    }

    /**
     * Map Ride to RideEntity for local storage.
     */
    private com.princeraj.campustaxipooling.db.entity.RideEntity mapRideToEntity(Ride ride, boolean synced) {
        com.princeraj.campustaxipooling.db.entity.RideEntity entity = new com.princeraj.campustaxipooling.db.entity.RideEntity();
        entity.setRideId(ride.getRideId());
        entity.setPostedByUid(ride.getPostedByUid());
        entity.setPostedByName(ride.getPostedByName());
        entity.setCampusId(ride.getCampusId());
        entity.setSource(ride.getSource());
        entity.setDestination(ride.getDestination());
        entity.setRouteDescription(ride.getRouteDescription());
        if (ride.getJourneyDateTime() != null) {
            entity.setJourneyDateTime(ride.getJourneyDateTime().toDate().getTime());
        }
        entity.setTotalFare(ride.getTotalFare());
        entity.setTotalSeats(ride.getTotalSeats());
        entity.setSeatsRemaining(ride.getSeatsRemaining());
        entity.setStatus(ride.getStatus());
        entity.setCreatedAt(ride.getCreatedAt() != null ? ride.getCreatedAt().toDate().getTime() : System.currentTimeMillis());
        entity.setSyncedAt(synced ? System.currentTimeMillis() : null);
        entity.setDeleted(false);
        return entity;
    }

    /**
     * Map local entities back to Ride models for UI.
     */
    private List<Ride> mapEntitiesToRides(List<com.princeraj.campustaxipooling.db.entity.RideEntity> entities) {
        List<Ride> rides = new java.util.ArrayList<>();
        for (com.princeraj.campustaxipooling.db.entity.RideEntity entity : entities) {
            Ride ride = new Ride();
            ride.setRideId(entity.getRideId());
            ride.setPostedByUid(entity.getPostedByUid());
            ride.setPostedByName(entity.getPostedByName());
            ride.setCampusId(entity.getCampusId());
            ride.setSource(entity.getSource());
            ride.setDestination(entity.getDestination());
            ride.setRouteDescription(entity.getRouteDescription());
            if (entity.getJourneyDateTime() != null) {
                ride.setJourneyDateTime(new com.google.firebase.Timestamp(new java.util.Date(entity.getJourneyDateTime())));
            }
            ride.setTotalFare(entity.getTotalFare());
            ride.setTotalSeats(entity.getTotalSeats());
            ride.setSeatsRemaining(entity.getSeatsRemaining());
            ride.setStatus(entity.getStatus());
            rides.add(ride);
        }
        return rides;
    }

    @Override
    public LiveData<SafeResult<Ride>> getRideById(String rideId) {
        MutableLiveData<SafeResult<Ride>> liveData = new MutableLiveData<>(SafeResult.loading());

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Ride ride = snapshot.toObject(Ride.class);
                        liveData.setValue(SafeResult.success(ride));
                    } else {
                        liveData.setValue(SafeResult.error(
                                "NOT_FOUND",
                                null,
                                "Ride not found",
                                false
                        ));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ride " + rideId, e);
                    liveData.setValue(SafeResult.error(e, "Failed to load ride details."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> cancelRide(String rideId) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("status", "CANCELLED");
        updates.put("updatedAt", Timestamp.now());

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ride cancelled: " + rideId);
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to cancel ride", e);
                    liveData.setValue(SafeResult.error(e, "Failed to cancel ride."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> updateRideStatus(String rideId, String newStatus) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", Timestamp.now());

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ride status updated: " + rideId + " -> " + newStatus);
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update ride status", e);
                    liveData.setValue(SafeResult.error(e, "Failed to update ride status."));
                });

        return liveData;
    }

    // ── Seat Requests ─────────────────────────────────────────────────────────

    @Override
    public LiveData<SafeResult<String>> sendJoinRequest(String rideId, SeatRequest request, String posterUid) {
        MutableLiveData<SafeResult<String>> liveData = new MutableLiveData<>(SafeResult.loading());

        request.setRequestedAt(Timestamp.now());

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .collection(SEAT_REQUESTS_SUB)
                .add(request)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Join request sent: " + ref.getId());

                    // Notify poster asynchronously (non-blocking)
                    NotificationApi.notifyJoinRequest(rideId, request.getRequesterName(), posterUid);

                    liveData.setValue(SafeResult.success(ref.getId()));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send join request", e);
                    liveData.setValue(SafeResult.error(e, "Failed to send join request. Please try again."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<List<SeatRequest>>> getPendingRequestsForRide(String rideId) {
        MutableLiveData<SafeResult<List<SeatRequest>>> liveData = new MutableLiveData<>(SafeResult.loading());

        ListenerRegistration listener = db.collection(RIDES_COLLECTION)
                .document(rideId)
                .collection(SEAT_REQUESTS_SUB)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching pending requests", error);
                        liveData.setValue(SafeResult.error(error, "Failed to load requests."));
                        return;
                    }

                    if (snapshot == null) {
                        liveData.setValue(SafeResult.success(new ArrayList<>()));
                        return;
                    }

                    List<SeatRequest> requests = snapshot.toObjects(SeatRequest.class);
                    liveData.setValue(SafeResult.success(requests));
                });

        activeListeners.add(listener);
        return liveData;
    }

    @Override
    public LiveData<SafeResult<SeatRequest>> getSeatRequest(String rideId, String requestId) {
        MutableLiveData<SafeResult<SeatRequest>> liveData = new MutableLiveData<>(SafeResult.loading());

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .collection(SEAT_REQUESTS_SUB)
                .document(requestId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        SeatRequest request = snapshot.toObject(SeatRequest.class);
                        liveData.setValue(SafeResult.success(request));
                    } else {
                        liveData.setValue(SafeResult.error(
                                "NOT_FOUND",
                                null,
                                "Request not found",
                                false
                        ));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching seat request", e);
                    liveData.setValue(SafeResult.error(e, "Failed to load request."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> acceptSeatRequest(
            String rideId,
            String requestId,
            String requesterUid,
            String requesterName,
            String posterUid) {

        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        DocumentReference rideRef = db.collection(RIDES_COLLECTION).document(rideId);
        DocumentReference requestRef = rideRef.collection(SEAT_REQUESTS_SUB).document(requestId);
        DocumentReference connectionRef = db.collection(CONNECTIONS_COLLECTION).document();

        db.runTransaction(transaction -> {
            // ── Step 1: Read latest ride state (prevents overbooking) ──
            DocumentSnapshot rideSnap = transaction.get(rideRef);
            Long currentSeats = rideSnap.getLong("seatsRemaining");

            if (currentSeats == null || currentSeats <= 0) {
                throw new IllegalStateException("No seats remaining to accept this request.");
            }

            // ── Step 2: Update seatRequest status ──
            Map<String, Object> requestUpdate = new HashMap<>();
            requestUpdate.put("status", "ACCEPTED");
            requestUpdate.put("respondedAt", Timestamp.now());
            transaction.update(requestRef, requestUpdate);

            // ── Step 3: Decrement seats ──
            long newSeatsRemaining = currentSeats - 1;
            Map<String, Object> rideUpdate = new HashMap<>();
            rideUpdate.put("seatsRemaining", newSeatsRemaining);
            if (newSeatsRemaining <= 0) {
                rideUpdate.put("status", "FULL");
            }
            rideUpdate.put("updatedAt", Timestamp.now());
            transaction.update(rideRef, rideUpdate);

            // ── Step 4: Create connection document ──
            Map<String, Object> connectionData = new HashMap<>();
            connectionData.put("rideId", rideId);
            connectionData.put("posterUid", posterUid);
            connectionData.put("joinerUid", requesterUid);
            connectionData.put("participants", java.util.Arrays.asList(posterUid, requesterUid));
            connectionData.put("isActive", true);
            connectionData.put("connectedAt", Timestamp.now());
            transaction.set(connectionRef, connectionData);

            return null;
        })
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Seat request accepted: " + requestId);

                    // TODO: Notify joiner asynchronously
                    // NotificationApi.notifyRequestAccepted(requesterUid, requesterName);

                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept request (transaction failed)", e);

                    // Provide user-friendly error message
                    String userMsg = "Failed to accept request";
                    if (e.getMessage() != null && e.getMessage().contains("No seats")) {
                        userMsg = "No seats remaining. Someone else accepted the last seat.";
                    }

                    liveData.setValue(SafeResult.error(e, userMsg));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> rejectSeatRequest(String rideId, String requestId, String rejectionReason) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "REJECTED");
        updates.put("respondedAt", Timestamp.now());
        if (rejectionReason != null) {
            updates.put("rejectionReason", rejectionReason);
        }

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .collection(SEAT_REQUESTS_SUB)
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Seat request rejected: " + requestId);
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reject request", e);
                    liveData.setValue(SafeResult.error(e, "Failed to reject request."));
                });

        return liveData;
    }

    // ── Connections ───────────────────────────────────────────────────────────

    @Override
    public LiveData<SafeResult<List<Connection>>> getMyConnections(String uid) {
        MutableLiveData<SafeResult<List<Connection>>> liveData = new MutableLiveData<>(SafeResult.loading());

        // Keep a reference so we can self-remove on PERMISSION_DENIED (e.g. user logged out)
        final ListenerRegistration[] refHolder = new ListenerRegistration[1];

        ListenerRegistration listener = db.collection(CONNECTIONS_COLLECTION)
                .whereArrayContains("participants", uid)
                .whereEqualTo("isActive", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        // PERMISSION_DENIED happens when the user logs out while the listener
                        // is still active. Safe to swallow — just emit an empty list and
                        // remove the stale listener so it stops firing.
                        if (error.getMessage() != null &&
                                (error.getMessage().contains("PERMISSION_DENIED") ||
                                 error.getMessage().contains("Missing or insufficient"))) {
                            Log.w(TAG, "Connections listener lost permission (user signed out). Removing listener.");
                            if (refHolder[0] != null) {
                                refHolder[0].remove();
                                activeListeners.remove(refHolder[0]);
                            }
                            liveData.postValue(SafeResult.success(new ArrayList<>()));
                            return;
                        }
                        Log.e(TAG, "Error fetching connections", error);
                        liveData.postValue(SafeResult.error(error, "Failed to load chats."));
                        return;
                    }

                    if (snapshot == null) {
                        liveData.postValue(SafeResult.success(new ArrayList<>()));
                        return;
                    }

                    List<Connection> connections = snapshot.toObjects(Connection.class);
                    liveData.postValue(SafeResult.success(connections));
                });

        refHolder[0] = listener;
        activeListeners.add(listener);
        return liveData;
    }

    @Override
    public LiveData<SafeResult<List<Ride>>> getMyJoinedRides(String uid) {
        MutableLiveData<SafeResult<List<Ride>>> liveData = new MutableLiveData<>(SafeResult.loading());

        // 1. Get connections where user is the joiner
        db.collection(CONNECTIONS_COLLECTION)
                .whereEqualTo("joinerUid", uid)
                .whereEqualTo("isActive", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching joined connections", error);
                        liveData.postValue(SafeResult.error(error, "Failed to load joined rides context."));
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        liveData.postValue(SafeResult.success(new ArrayList<>()));
                        return;
                    }

                    List<String> rideIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String rid = doc.getString("rideId");
                        if (rid != null && !rideIds.contains(rid)) rideIds.add(rid);
                    }

                    if (rideIds.isEmpty()) {
                        liveData.postValue(SafeResult.success(new ArrayList<>()));
                        return;
                    }

                    // 2. Fetch the corresponding rides (limit 30 for whereIn)
                    db.collection(RIDES_COLLECTION)
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), rideIds)
                            .get()
                            .addOnSuccessListener(rideSnap -> {
                                List<Ride> joinedRides = rideSnap.toObjects(Ride.class);
                                liveData.postValue(SafeResult.success(joinedRides));
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching joined rides from rideIds", e);
                                liveData.postValue(SafeResult.error(e, "Failed to load ride details for joined rides."));
                            });
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> completeRide(String connectionId) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", false);
        updates.put("completedAt", Timestamp.now());

        db.collection(CONNECTIONS_COLLECTION)
                .document(connectionId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ride completed: " + connectionId);
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to complete ride", e);
                    liveData.setValue(SafeResult.error(e, "Failed to mark ride as complete."));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> submitRating(com.princeraj.campustaxipooling.model.Rating rating) {
        MutableLiveData<SafeResult<Void>> result = new MutableLiveData<>(SafeResult.loading());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference userRef = db.collection("users").document(rating.getToUid());
            com.google.firebase.firestore.DocumentSnapshot userSnap = transaction.get(userRef);
            
            User user = userSnap.toObject(User.class);
            if (user != null) {
                long newCount = user.getRatingCount() + 1;
                double newAvg = ((user.getAverageRating() * user.getRatingCount()) + rating.getScore()) / newCount;
                
                transaction.update(userRef, "ratingCount", newCount);
                transaction.update(userRef, "averageRating", newAvg);
            }
            
            // Save individual rating record
            com.google.firebase.firestore.DocumentReference ratingRef = db.collection("ratings").document();
            transaction.set(ratingRef, rating);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Rating submitted for " + rating.getToUid());
            result.setValue(SafeResult.success(null));
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to submit rating", e);
            result.setValue(SafeResult.error(e, "Failed to submit rating."));
        });

        return result;
    }

    @Override
    public LiveData<SafeResult<Connection>> getConnection(String connectionId) {
        MutableLiveData<SafeResult<Connection>> liveData = new MutableLiveData<>(SafeResult.loading());

        db.collection(CONNECTIONS_COLLECTION)
                .document(connectionId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        liveData.setValue(SafeResult.success(snapshot.toObject(Connection.class)));
                    } else {
                        liveData.setValue(SafeResult.error("NOT_FOUND", null, "Connection no longer exists.", false));
                    }
                })
                .addOnFailureListener(e -> liveData.setValue(SafeResult.error(e, "Failed to load chat details.")));

        return liveData;
    }

    /**
     * Cleanup: remove all active listeners when repository is destroyed.
     * Called by dependency injection framework.
     */
    public void cleanup() {
        for (ListenerRegistration listener : activeListeners) {
            listener.remove();
        }
        activeListeners.clear();
    }
}
