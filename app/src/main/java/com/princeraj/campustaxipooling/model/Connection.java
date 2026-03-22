package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import java.util.List;

/**
 * Represents an active connection between two users after a ride request is accepted.
 * Stored in Firestore: connections/{connectionId}
 */
public class Connection {

    @DocumentId
    private String connectionId;
    private String rideId;
    private String posterUid;
    private String joinerUid;
    private List<String> participants;  // [posterUid, joinerUid] for array-contains queries
    private boolean chatEnabled;
    private boolean isActive;
    private Timestamp connectedAt;

    public Connection() {}

    public Connection(String rideId, String posterUid, String joinerUid, List<String> participants) {
        this.rideId = rideId;
        this.posterUid = posterUid;
        this.joinerUid = joinerUid;
        this.participants = participants;
        this.chatEnabled = true;
        this.isActive = true;
    }

    public String getConnectionId() { return connectionId; }
    public String getRideId() { return rideId; }
    public String getPosterUid() { return posterUid; }
    public String getJoinerUid() { return joinerUid; }
    public List<String> getParticipants() { return participants; }
    public boolean isChatEnabled() { return chatEnabled; }
    public boolean isActive() { return isActive; }
    public Timestamp getConnectedAt() { return connectedAt; }

    public void setConnectionId(String connectionId) { this.connectionId = connectionId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public void setPosterUid(String posterUid) { this.posterUid = posterUid; }
    public void setJoinerUid(String joinerUid) { this.joinerUid = joinerUid; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
    public void setActive(boolean active) { isActive = active; }
    public void setConnectedAt(Timestamp connectedAt) { this.connectedAt = connectedAt; }
}
