package com.princeraj.campustaxipooling.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.princeraj.campustaxipooling.model.Connection;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.SeatRequest;
import com.princeraj.campustaxipooling.util.NotificationApi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for all ride, seat request, and connection operations.
 */
public class RideRepository {

    private static final String RIDES_COLLECTION = "rides";
    private static final String SEAT_REQUESTS_SUB = "seatRequests";
    private static final String CONNECTIONS_COLLECTION = "connections";

    private final FirebaseFirestore db;

    private static RideRepository instance;

    private RideRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static RideRepository getInstance() {
        if (instance == null) {
            instance = new RideRepository();
        }
        return instance;
    }

    // ── Ride CRUD ─────────────────────────────────────────────────────────────

    /**
     * Posts a new ride to the rides collection.
     */
    public Task<DocumentReference> postRide(Ride ride) {
        return db.collection(RIDES_COLLECTION).add(ride);
    }

    /**
     * Gets the paginated, active ride feed for a campus.
     * Excludes the current user's own rides.
     */
    public Query getRideFeed(String campusId, String currentUserUid) {
        return db.collection(RIDES_COLLECTION)
                .whereEqualTo("campusId", campusId)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("deleted", false)
                .whereNotEqualTo("postedByUid", currentUserUid)
                .orderBy("postedByUid")  // required before orderBy on different field after whereNotEqualTo
                .orderBy("journeyDateTime", Query.Direction.ASCENDING)
                .limit(20);
    }

    /**
     * Gets all rides posted by the current user.
     */
    public Query getMyPostedRides(String uid) {
        return db.collection(RIDES_COLLECTION)
                .whereEqualTo("postedByUid", uid)
                .whereEqualTo("deleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    /**
     * Soft-deletes and cancels a ride.
     */
    public Task<Void> cancelRide(String rideId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("status", "CANCELLED");
        updates.put("updatedAt", Timestamp.now());
        return db.collection(RIDES_COLLECTION).document(rideId).update(updates);
    }

    // ── Seat Requests ─────────────────────────────────────────────────────────

    /**
     * Sends a join request for a ride and notifies the poster.
     *
     * @param posterUid UID of the ride poster (for push notification).
     */
    public Task<DocumentReference> sendJoinRequest(String rideId, SeatRequest request, String posterUid) {
        return db.collection(RIDES_COLLECTION)
                .document(rideId)
                .collection(SEAT_REQUESTS_SUB)
                .add(request)
                .addOnSuccessListener(ref ->
                        NotificationApi.notifyJoinRequest(
                                rideId,
                                request.getRequesterName(),
                                posterUid
                        )
                );
    }

    /**
     * Gets all PENDING seat requests for a ride (poster view).
     */
    public Query getPendingRequestsForRide(String rideId) {
        return db.collection(RIDES_COLLECTION)
                .document(rideId)
                .collection(SEAT_REQUESTS_SUB)
                .whereEqualTo("status", "PENDING")
                .orderBy("requestedAt", Query.Direction.ASCENDING);
    }

    /**
     * Accepts a seat request using a Firestore transaction.
     * Atomically: updates request status, decrements seatsRemaining,
     * updates ride status if full, creates a connection document.
     */
    public Task<Void> acceptSeatRequest(String rideId, String requestId,
                                         String requesterUid, String requesterName,
                                         String posterUid, int currentSeatsRemaining) {
        DocumentReference rideRef = db.collection(RIDES_COLLECTION).document(rideId);
        DocumentReference requestRef = rideRef.collection(SEAT_REQUESTS_SUB).document(requestId);
        DocumentReference connectionRef = db.collection(CONNECTIONS_COLLECTION).document();

        return db.runTransaction(transaction -> {
            // 1. Update seatRequest status
            Map<String, Object> requestUpdate = new HashMap<>();
            requestUpdate.put("status", "ACCEPTED");
            requestUpdate.put("respondedAt", Timestamp.now());
            transaction.update(requestRef, requestUpdate);

            // 2. Decrement seats
            int newSeatsRemaining = currentSeatsRemaining - 1;
            Map<String, Object> rideUpdate = new HashMap<>();
            rideUpdate.put("seatsRemaining", newSeatsRemaining);
            if (newSeatsRemaining <= 0) {
                rideUpdate.put("status", "FULL");
            }
            rideUpdate.put("updatedAt", Timestamp.now());
            transaction.update(rideRef, rideUpdate);

            // 3. Create connection document
            Connection connection = new Connection(
                    rideId, posterUid, requesterUid,
                    Arrays.asList(posterUid, requesterUid)
            );
            transaction.set(connectionRef, connection);

            return null;
        }).addOnSuccessListener(unused ->
                NotificationApi.notifyRequestUpdate(rideId, "ACCEPTED", requesterUid)
        );
    }

    /**
     * Rejects a seat request and notifies the joiner.
     *
     * @param joinerUid UID of the requester (for push notification).
     */
    public Task<Void> rejectSeatRequest(String rideId, String requestId, String joinerUid) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "REJECTED");
        updates.put("respondedAt", Timestamp.now());
        return db.collection(RIDES_COLLECTION)
                .document(rideId)
                .collection(SEAT_REQUESTS_SUB)
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(unused ->
                        NotificationApi.notifyRequestUpdate(rideId, "REJECTED", joinerUid)
                );
    }

    // ── Connections ───────────────────────────────────────────────────────────

    /**
     * Gets all active connections for the current user.
     */
    public Query getMyConnections(String uid) {
        return db.collection(CONNECTIONS_COLLECTION)
                .whereArrayContains("participants", uid)
                .whereEqualTo("isActive", true)
                .orderBy("connectedAt", Query.Direction.DESCENDING);
    }
}
