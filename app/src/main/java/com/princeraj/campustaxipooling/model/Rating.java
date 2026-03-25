package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents a rating given by a user to another user for a specific ride.
 */
public class Rating {
    
    @DocumentId
    private String ratingId;
    private String rideId;
    private String fromUid; // The user who is giving the rating
    private String toUid;   // The user who is being rated
    private float score;     // 1.0 to 5.0
    private String comment;
    private Timestamp createdAt;

    public Rating() {}

    public Rating(String rideId, String fromUid, String toUid, float score, String comment) {
        this.rideId = rideId;
        this.fromUid = fromUid;
        this.toUid = toUid;
        this.score = score;
        this.comment = comment;
        this.createdAt = Timestamp.now();
    }

    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getToUid() { return toUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
