package com.princeraj.campustaxipooling.repository;

import androidx.lifecycle.LiveData;

import com.google.android.gms.tasks.Task;
import com.princeraj.campustaxipooling.model.Connection;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.SeatRequest;
import com.princeraj.campustaxipooling.util.SafeResult;

import java.util.List;

/**
 * Repository interface for all Ride operations.
 * Abstraction that allows for:
 * - Easy mock implementation in unit tests
 * - Swapping Firebase implementation with another backend
 * - Separation of concerns (UI layer doesn't know about Firebase)
 */
public interface IRideRepository {

    // ── Ride CRUD ─────────────────────────────────────────────────────────────

    /**
     * Posts a new ride.
     * Returns LiveData<SafeResult<String>> with the rideId on success.
     */
    LiveData<SafeResult<String>> postRide(Ride ride);

    /**
     * Fetches the ride feed (paginated, active rides).
     * Returns LiveData that updates in real-time as new rides are added.
     */
    LiveData<SafeResult<List<Ride>>> getRideFeed(String campusId, String currentUserUid, int limit);

    /**
     * Fetches rides posted by the current user.
     */
    LiveData<SafeResult<List<Ride>>> getMyPostedRides(String uid);

    /**
     * Fetches rides where the current user is a passenger (accepted connection).
     */
    LiveData<SafeResult<List<Ride>>> getMyJoinedRides(String uid);

    /**
     * Fetches a single ride by ID.
     */
    LiveData<SafeResult<Ride>> getRideById(String rideId);

    /**
     * Soft-deletes and cancels a ride.
     */
    LiveData<SafeResult<Void>> cancelRide(String rideId);

    /**
     * Updates ride status (ACTIVE, FULL, STARTED, COMPLETED, CANCELLED).
     */
    LiveData<SafeResult<Void>> updateRideStatus(String rideId, String newStatus);

    // ── Seat Requests ─────────────────────────────────────────────────────────

    /**
     * Sends a join request for a ride.
     * Automatically notifies the poster.
     */
    LiveData<SafeResult<String>> sendJoinRequest(String rideId, SeatRequest request, String posterUid);

    /**
     * Fetches all pending seat requests for a ride (poster view).
     */
    LiveData<SafeResult<List<SeatRequest>>> getPendingRequestsForRide(String rideId);

    /**
     * Fetches a single seat request by ID.
     */
    LiveData<SafeResult<SeatRequest>> getSeatRequest(String rideId, String requestId);

    /**
     * Accepts a seat request using atomic transaction.
     * Prevents race conditions (multiple people joining last seat).
     */
    LiveData<SafeResult<Void>> acceptSeatRequest(
            String rideId,
            String requestId,
            String requesterUid,
            String requesterName,
            String posterUid
    );

    /**
     * Rejects a seat request.
     */
    LiveData<SafeResult<Void>> rejectSeatRequest(String rideId, String requestId, String rejectionReason);

    // ── Connections ───────────────────────────────────────────────────────────

    /**
     * Fetches active connections for the current user (for chat list).
     */
    LiveData<SafeResult<List<Connection>>> getMyConnections(String uid);

    /**
     * Marks a connection as completed.
     */
    LiveData<SafeResult<Void>> completeRide(String connectionId);

    /**
     * Fetches details of a specific connection.
     */
    LiveData<SafeResult<com.princeraj.campustaxipooling.model.Connection>> getConnection(String connectionId);

    /**
     * Submits a rating for a user.
     * Updates the user's running rating average.
     */
    LiveData<SafeResult<Void>> submitRating(com.princeraj.campustaxipooling.model.Rating rating);
}

