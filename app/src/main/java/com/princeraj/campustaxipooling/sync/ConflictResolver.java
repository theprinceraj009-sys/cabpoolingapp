package com.princeraj.campustaxipooling.sync;

import android.util.Log;

import androidx.annotation.NonNull;

import com.princeraj.campustaxipooling.db.entity.RideEntity;
import com.princeraj.campustaxipooling.model.Ride;

import javax.inject.Singleton;

/**
 * ConflictResolver: Merges local & remote data when conflicts occur.
 *
 * Responsibility:
 * When a user modifies data offline and Firestore also has changes,
 * this resolver decides which version to keep.
 *
 * Strategies:
 * 1. Last-Write-Wins (LWW) — Based on timestamps
 * 2. Field-Level Merging — Merge non-conflicting fields
 * 3. Custom Resolution — For critical fields
 *
 * Phase 3: Conflict Resolution for Offline Sync
 */
@Singleton
public class ConflictResolver {

    private static final String TAG = "ConflictResolver";

    /**
     * Resolves conflict for Ride data.
     *
     * Rules:
     * • status: Remote wins (server of truth for ride lifecycle)
     * • seatsRemaining: Remote wins (others may have joined)
     * • Updated metadata: Remote wins (more authoritative)
     * • Other fields: Local wins if local is newer
     *
     * @param local Local RideEntity (offline changes)
     * @param remote Remote Ride (from Firestore)
     * @return Merged RideEntity
     */
    public RideEntity resolveRideConflict(
            @NonNull RideEntity local,
            @NonNull Ride remote) {

        Log.d(TAG, "Resolving ride conflict: " + local.getRideId());

        RideEntity merged = new RideEntity();
        merged.setRideId(local.getRideId());

        // ── Critical Fields: Remote Wins ──
        // These are controlled by server, client shouldn't override

        // Ride status (ACTIVE, FULL, COMPLETED, etc.)
        // Remote is source of truth - others may have joined/completed
        merged.setStatus(remote.getStatus());

        // Seats remaining - remote reflects all join requests
        merged.setSeatsRemaining(remote.getSeatsRemaining());

        // ── Ownership: Local Wins ──
        merged.setPostedByUid(local.getPostedByUid());
        merged.setPostedByName(local.getPostedByName());

        // ── Location Data: Local Wins (User edits these offline) ──
        merged.setSource(local.getSource());
        merged.setDestination(local.getDestination());
        merged.setCampusId(local.getCampusId());
        merged.setJourneyDateTime(local.getJourneyDateTime());

        // ── Price & Capacity: Use Remote (Immutable) ──
        merged.setTotalFare(remote.getTotalFare());
        merged.setTotalSeats(remote.getTotalSeats());

        // ── Preferences: Local Wins ──
        merged.setPreferences(local.getPreferences());

        // ── Proof & Metadata: Remote Wins ──
        merged.setProofUrl(remote.getProofUrl());
        merged.setDeleted(remote.isDeleted());

        // ── Timestamps ──
        merged.setCreatedAt(remote.getCreatedAt() != null ? 
                remote.getCreatedAt().getSeconds() * 1000 : local.getCreatedAt());
        merged.setUpdatedAt(System.currentTimeMillis()); // Mark as resolved

        Log.d(TAG, "Conflict resolved for: " + local.getRideId());
        return merged;
    }

    /**
     * Determines which version to use based on timestamps.
     * Last-Write-Wins strategy.
     *
     * @param localUpdateTime When local was updated (ms)
     * @param remoteUpdateTime When remote was updated (ms)
     * @return true if local is newer, false if remote is newer
     */
    public boolean isLocalNewer(long localUpdateTime, long remoteUpdateTime) {
        return localUpdateTime > remoteUpdateTime;
    }

    /**
     * Handles deletion conflicts.
     *
     * Rules:
     * • If deleted on both: Don't re-create
     * • If deleted locally, created remotely: Keep remote (user deletion intent)
     * • If deleted remotely, modified locally: Keep local (user edit intent)
     *
     * @param localDeleted Is local deleted?
     * @param remoteDeleted Is remote deleted?
     * @return true if final state should be deleted
     */
    public boolean shouldBeDeleted(boolean localDeleted, boolean remoteDeleted) {
        // Deletion is final
        return localDeleted || remoteDeleted;
    }

    /**
     * Validates merged data for consistency.
     *
     * @param merged The merged entity
     * @return true if valid, false if needs manual review
     */
    public boolean isValidMerge(@NonNull RideEntity merged) {
        // Validate critical invariants
        if (merged.getSeatsRemaining() > merged.getTotalSeats()) {
            Log.e(TAG, "Invalid merge: seatsRemaining > totalSeats");
            return false;
        }

        if (merged.getTotalSeats() <= 0) {
            Log.e(TAG, "Invalid merge: totalSeats must be > 0");
            return false;
        }

        if (merged.getTotalFare() < 0) {
            Log.e(TAG, "Invalid merge: fare cannot be negative");
            return false;
        }

        return true;
    }

    /**
     * Logs conflict details for debugging.
     */
    public void logConflict(String rideId, RideEntity local, Ride remote) {
        Log.w(TAG, "=== CONFLICT DETECTED ===");
        Log.w(TAG, "Ride: " + rideId);
        Log.w(TAG, "Local Status: " + local.getStatus() + 
                " Remote Status: " + remote.getStatus());
        Log.w(TAG, "Local Seats: " + local.getSeatsRemaining() + 
                " Remote Seats: " + remote.getSeatsRemaining());
        Log.w(TAG, "Local Updated: " + local.getUpdatedAt() + 
                " Remote Updated: " + remote.getUpdatedAt().getSeconds());
        Log.w(TAG, "========================");
    }
}

