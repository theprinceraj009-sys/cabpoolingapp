package com.princeraj.campustaxipooling.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.princeraj.campustaxipooling.db.entity.MessageEntity;

import java.util.List;

/**
 * Data Access Object for Message caching.
 * Provides database queries for offline-first chat operations.
 *
 * Used in Phase 3: Offline Resilience
 */
@Dao
public interface MessageDao {

    /**
     * Inserts a message into the local cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    /**
     * Inserts multiple messages.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> messages);

    /**
     * Updates an existing message.
     */
    @Update
    void updateMessage(MessageEntity message);

    /**
     * Deletes a message.
     */
    @Delete
    void deleteMessage(MessageEntity message);

    // ── Query Methods ─────────────────────────────────────────────────────────

    /**
     * Gets all messages for a connection, ordered by time.
     * Used to display chat history when offline.
     */
    @Query(
            "SELECT * FROM messages " +
            "WHERE connection_id = :connectionId " +
            "ORDER BY sent_at ASC"
    )
    List<MessageEntity> getMessagesForConnection(String connectionId);

    /**
     * Gets unsynced messages (not yet sent to Firestore).
     */
    @Query(
            "SELECT * FROM messages " +
            "WHERE synced_at IS NULL OR synced_at < sent_at"
    )
    List<MessageEntity> getUnsyncedMessages();

    /**
     * Marks a message as synced.
     */
    @Query("UPDATE messages SET synced_at = :currentTime WHERE message_id = :messageId")
    void markMessageSynced(String messageId, Long currentTime);

    /**
     * Gets count of messages for a connection.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE connection_id = :connectionId")
    int getMessageCountForConnection(String connectionId);

    /**
     * Clears all messages for a connection.
     */
    @Query("DELETE FROM messages WHERE connection_id = :connectionId")
    void clearMessagesForConnection(String connectionId);

    /**
     * Clears all messages.
     */
    @Query("DELETE FROM messages")
    void clearAllMessages();
}

