package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents a seat join request under a ride.
 * Stored in Firestore: rides/{rideId}/seatRequests/{requestId}
 */
public class SeatRequest {

    @DocumentId
    private String requestId;
    private String requesterUid;
    private String requesterName;
    private String status;   // "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED"
    private String message;
    private Timestamp requestedAt;
    private Timestamp respondedAt;

    public SeatRequest() {}

    public SeatRequest(String requesterUid, String requesterName, String message) {
        this.requesterUid = requesterUid;
        this.requesterName = requesterName;
        this.message = message;
        this.status = "PENDING";
    }

    public String getRequestId() { return requestId; }
    public String getRequesterUid() { return requesterUid; }
    public String getRequesterName() { return requesterName; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Timestamp getRequestedAt() { return requestedAt; }
    public Timestamp getRespondedAt() { return respondedAt; }

    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setRequesterUid(String requesterUid) { this.requesterUid = requesterUid; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }
    public void setRespondedAt(Timestamp respondedAt) { this.respondedAt = respondedAt; }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isAccepted() { return "ACCEPTED".equals(status); }
}
