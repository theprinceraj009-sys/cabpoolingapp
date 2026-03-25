package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents a posted ride.
 * Stored in Firestore: rides/{rideId}
 */
public class Ride {

    @DocumentId
    private String rideId;
    private String postedByUid;
    private String postedByName;
    private String campusId;
    private String source;
    private String destination;
    private String routeDescription;
    private Timestamp journeyDateTime;
    private long totalFare;
    private int totalSeats;
    private int seatsRemaining;
    private String preferences;
    private String proofUrl;
    private String status;     // "ACTIVE" | "FULL" | "STARTED" | "COMPLETED" | "CANCELLED"
    @PropertyName("isDeleted")
    private boolean isDeleted;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Required empty constructor for Firestore deserialization
    public Ride() {}

    public Ride(String postedByUid, String postedByName, String campusId,
                String source, String destination, Timestamp journeyDateTime,
                long totalFare, int totalSeats) {
        this.postedByUid = postedByUid;
        this.postedByName = postedByName;
        this.campusId = campusId;
        this.source = source;
        this.destination = destination;
        this.journeyDateTime = journeyDateTime;
        this.totalFare = totalFare;
        this.totalSeats = totalSeats;
        this.seatsRemaining = totalSeats;
        this.status = "ACTIVE";
        this.isDeleted = false;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getRideId() { return rideId; }
    public String getPostedByUid() { return postedByUid; }
    public String getPostedByName() { return postedByName; }
    public String getCampusId() { return campusId; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public String getRouteDescription() { return routeDescription; }
    public Timestamp getJourneyDateTime() { return journeyDateTime; }
    public long getTotalFare() { return totalFare; }
    public int getTotalSeats() { return totalSeats; }
    public int getSeatsRemaining() { return seatsRemaining; }
    public String getPreferences() { return preferences; }
    public String getProofUrl() { return proofUrl; }
    public String getStatus() { return status; }
    @PropertyName("isDeleted")
    public boolean isDeleted() { return isDeleted; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setRideId(String rideId) { this.rideId = rideId; }
    public void setPostedByUid(String postedByUid) { this.postedByUid = postedByUid; }
    public void setPostedByName(String postedByName) { this.postedByName = postedByName; }
    public void setCampusId(String campusId) { this.campusId = campusId; }
    public void setSource(String source) { this.source = source; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setRouteDescription(String routeDescription) { this.routeDescription = routeDescription; }
    public void setJourneyDateTime(Timestamp journeyDateTime) { this.journeyDateTime = journeyDateTime; }
    public void setTotalFare(long totalFare) { this.totalFare = totalFare; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public void setSeatsRemaining(int seatsRemaining) { this.seatsRemaining = seatsRemaining; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
    public void setStatus(String status) { this.status = status; }
    @PropertyName("isDeleted")
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean hasSeatsAvailable() {
        return seatsRemaining > 0 && isActive();
    }

    public boolean isActive() {
        boolean timeValid = journeyDateTime == null || 
                   journeyDateTime.toDate().after(new java.util.Date());
        return "ACTIVE".equals(status) && !isDeleted && timeValid;
    }

    public float getPerPersonFare() {
        if (totalSeats <= 0) return 0f;
        return (float) totalFare / totalSeats;
    }
}
