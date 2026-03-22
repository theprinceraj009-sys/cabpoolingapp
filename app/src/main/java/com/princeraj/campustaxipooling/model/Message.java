package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents a single chat message within a connection.
 * Stored in Firestore: connections/{connectionId}/messages/{messageId}
 */
public class Message {

    @DocumentId
    private String messageId;
    private String senderUid;
    private String text;
    private boolean isFlagged;
    private String flagReason;
    private boolean isBlocked;
    private Timestamp sentAt;

    public Message() {}

    public Message(String senderUid, String text) {
        this.senderUid = senderUid;
        this.text = text;
        this.isFlagged = false;
        this.isBlocked = false;
    }

    public String getMessageId() { return messageId; }
    public String getSenderUid() { return senderUid; }
    public String getText() { return text; }
    public boolean isFlagged() { return isFlagged; }
    public String getFlagReason() { return flagReason; }
    public boolean isBlocked() { return isBlocked; }
    public Timestamp getSentAt() { return sentAt; }

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }
    public void setText(String text) { this.text = text; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }
}
