package com.princeraj.campustaxipooling.repository;

import androidx.lifecycle.LiveData;

import com.princeraj.campustaxipooling.model.Message;
import com.princeraj.campustaxipooling.util.SafeResult;

import java.util.List;

/**
 * Repository interface for all chat message operations.
 */
public interface IChatRepository {

    /**
     * Fetches all messages for a connection (real-time updates).
     */
    LiveData<SafeResult<List<Message>>> getMessages(String connectionId);

    /**
     * Validates message content against moderation rules.
     * Returns a ModerationResult with details.
     */
    ModerationResult moderateMessage(String text);

    /**
     * Sends a message to Firestore after passing moderation.
     * Automatically notifies the recipient via push notification.
     */
    LiveData<SafeResult<Void>> sendMessage(String connectionId, Message message, String senderName);

    /**
     * Sends a flagged message (stored for admin review).
     */
    LiveData<SafeResult<Void>> sendFlaggedMessage(String connectionId,
                                                   String senderUid,
                                                   String text,
                                                   String flagReason);

    /**
     * Moderation result inner class.
     */
    class ModerationResult {
        public final boolean isClean;
        public final String flagReason;
        public final String userMessage;

        public ModerationResult(boolean isClean, String flagReason, String userMessage) {
            this.isClean = isClean;
            this.flagReason = flagReason;
            this.userMessage = userMessage;
        }
    }
}

