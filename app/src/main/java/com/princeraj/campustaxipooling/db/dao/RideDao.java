package com.princeraj.campustaxipooling.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.princeraj.campustaxipooling.db.entity.RideEntity;

import java.util.List;

/**
 * Data Access Object for Ride caching.
 * Provides database queries for offline-first ride operations.
 *
 * Used in Phase 3: Offline Resilience
 * - insertRide() — Cache rides from Firestore
 * - getRideFeed() — Get rides locally when offline
 * - getUserRides() — Get user's own rides
 * - updateRide() — Update ride status locally before sync
 */
@Dao
public interface RideDao {

    /**
     * Inserts a ride into the local cache.
     * If ride already exists, replaces it (REPLACE strategy).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRide(RideEntity ride);

    /**
     * Inserts multiple rides at once (batch operation).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRides(List<RideEntity> rides);

    /**
     * Updates an existing ride.
     */
    @Update
    void updateRide(RideEntity ride);

    /**
     * Deletes a single ride.
     */
    @Delete
    void deleteRide(RideEntity ride);

    /**
     * Soft-deletes all rides (marks isDeleted=true).
     * Called during sync cleanup.
     */
    @Query("UPDATE rides SET is_deleted = 1 WHERE ride_id = :rideId")
    void softDeleteRide(String rideId);

    // ── Query Methods ─────────────────────────────────────────────────────────

    /**
     * Gets a single ride by ID.
     * Returns null if not found.
     */
    @Query("SELECT * FROM rides WHERE ride_id = :rideId LIMIT 1")
    RideEntity getRideById(String rideId);

    /**
     * Gets the ride feed for a campus (active, not deleted).
     * Ordered by most recent first.
     * Used when offline to show cached rides.
     */
    @Query(
            "SELECT * FROM rides " +
            "WHERE campus_id = :campusId " +
            "AND status = 'ACTIVE' " +
            "AND is_deleted = 0 " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit"
    )
    List<RideEntity> getRideFeed(String campusId, int limit);

    /**
     * Gets all rides posted by a specific user.
     */
    @Query(
            "SELECT * FROM rides " +
            "WHERE posted_by_uid = :uid " +
            "AND is_deleted = 0 " +
            "ORDER BY created_at DESC"
    )
    List<RideEntity> getUserRides(String uid);

    /**
     * Gets rides that need to be synced to Firestore.
     * (synced_at is null or older than updated_at)
     */
    @Query(
            "SELECT * FROM rides " +
            "WHERE synced_at IS NULL OR synced_at < updated_at"
    )
    List<RideEntity> getUnsyncedRides();

    /**
     * Marks all rides as successfully synced.
     */
    @Query("UPDATE rides SET synced_at = :currentTime")
    void markAllRidesSynced(Long currentTime);

    /**
     * Deletes all cached rides (for cache invalidation).
     */
    @Query("DELETE FROM rides")
    void clearAllRides();

    /**
     * Gets count of rides for a campus (for pagination).
     */
    @Query("SELECT COUNT(*) FROM rides WHERE campus_id = :campusId AND is_deleted = 0")
    int getRideCountForCampus(String campusId);
}

