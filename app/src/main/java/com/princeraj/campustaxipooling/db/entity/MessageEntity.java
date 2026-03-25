package com.princeraj.campustaxipooling.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for caching chat messages locally.
 * Used in Phase 3: Offline Resilience
 *
 * Firestore connections/{id}/messages → Room messages table
 */
@Entity(
        tableName = "messages",
        indices = {
                @Index("connection_id"),
                @Index(value = {"connection_id", "sent_at"})  // For ordered queries
        }
)
public class MessageEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "message_id")
    private String messageId;

    @ColumnInfo(name = "connection_id")
    private String connectionId;

    @ColumnInfo(name = "sender_uid")
    private String senderUid;

    private String text;

    @ColumnInfo(name = "is_flagged")
    private boolean isFlagged;

    @ColumnInfo(name = "flag_reason")
    private String flagReason;

    @ColumnInfo(name = "is_blocked")
    private boolean isBlocked;

    @ColumnInfo(name = "sent_at")
    private Long sentAt;  // Timestamp in milliseconds

    @ColumnInfo(name = "synced_at")
    private Long syncedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public MessageEntity() {}

    @androidx.room.Ignore
    public MessageEntity(String messageId, String connectionId, String senderUid, String text) {
        this.messageId = messageId;
        this.connectionId = connectionId;
        this.senderUid = senderUid;
        this.text = text;
        this.isFlagged = false;
        this.isBlocked = false;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConnectionId() { return connectionId; }
    public void setConnectionId(String connectionId) { this.connectionId = connectionId; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public Long getSentAt() { return sentAt; }
    public void setSentAt(Long sentAt) { this.sentAt = sentAt; }

    public Long getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Long syncedAt) { this.syncedAt = syncedAt; }
}

