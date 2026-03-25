package com.princeraj.campustaxipooling.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for caching Ride documents locally.
 * Used in Phase 3: Offline Resilience
 *
 * Firestore → Room (when online)
 * Room → UI (when offline)
 */
@Entity(
        tableName = "rides",
        indices = {
                @Index("campus_id"),
                @Index("posted_by_uid"),
                @Index("status"),
                @Index(value = {"campus_id", "status"})  // Composite index for feed query
        }
)
public class RideEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ride_id")
    private String rideId;

    @ColumnInfo(name = "posted_by_uid")
    private String postedByUid;

    @ColumnInfo(name = "posted_by_name")
    private String postedByName;

    @ColumnInfo(name = "campus_id")
    private String campusId;

    private String source;
    private String destination;
    private String routeDescription;

    @ColumnInfo(name = "journey_date_time")
    private Long journeyDateTime;  // Timestamp in milliseconds

    @ColumnInfo(name = "total_fare")
    private long totalFare;

    @ColumnInfo(name = "total_seats")
    private int totalSeats;

    @ColumnInfo(name = "seats_remaining")
    private int seatsRemaining;

    private String preferences;

    @ColumnInfo(name = "proof_url")
    private String proofUrl;

    private String status;  // ACTIVE, FULL, STARTED, COMPLETED, CANCELLED

    @ColumnInfo(name = "is_deleted")
    private boolean isDeleted;

    @ColumnInfo(name = "created_at")
    private Long createdAt;  // Timestamp in milliseconds

    @ColumnInfo(name = "updated_at")
    private Long updatedAt;

    @ColumnInfo(name = "synced_at")
    private Long syncedAt;  // When this was last synced from Firestore

    // ── Constructors ──────────────────────────────────────────────────────────

    public RideEntity() {}

    @androidx.room.Ignore
    public RideEntity(String rideId, String postedByUid, String postedByName, String campusId,
                      String source, String destination, Long journeyDateTime,
                      long totalFare, int totalSeats, String status) {
        this.rideId = rideId;
        this.postedByUid = postedByUid;
        this.postedByName = postedByName;
        this.campusId = campusId;
        this.source = source;
        this.destination = destination;
        this.journeyDateTime = journeyDateTime;
        this.totalFare = totalFare;
        this.totalSeats = totalSeats;
        this.seatsRemaining = totalSeats;
        this.status = status;
        this.isDeleted = false;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getPostedByUid() { return postedByUid; }
    public void setPostedByUid(String postedByUid) { this.postedByUid = postedByUid; }

    public String getPostedByName() { return postedByName; }
    public void setPostedByName(String postedByName) { this.postedByName = postedByName; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getRouteDescription() { return routeDescription; }
    public void setRouteDescription(String routeDescription) { this.routeDescription = routeDescription; }

    public Long getJourneyDateTime() { return journeyDateTime; }
    public void setJourneyDateTime(Long journeyDateTime) { this.journeyDateTime = journeyDateTime; }

    public long getTotalFare() { return totalFare; }
    public void setTotalFare(long totalFare) { this.totalFare = totalFare; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getSeatsRemaining() { return seatsRemaining; }
    public void setSeatsRemaining(int seatsRemaining) { this.seatsRemaining = seatsRemaining; }

    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }

    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public Long getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Long syncedAt) { this.syncedAt = syncedAt; }
}

