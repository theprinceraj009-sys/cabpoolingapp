package com.princeraj.campustaxipooling.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.princeraj.campustaxipooling.model.Message;

import com.princeraj.campustaxipooling.util.NotificationApi;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Single source of truth for all chat message operations.
 * Includes client-side moderation before any Firestore write.
 */
public class ChatRepository {

    private static final String CONNECTIONS_COLLECTION = "connections";
    private static final String MESSAGES_SUB = "messages";

    // Client-side moderation patterns
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b[6-9]\\d{9}\\b");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final String[] FLAGGED_KEYWORDS = {
            "call me", "my number", "whatsapp", "telegram",
            "ping me", "contact me", "give me your number", "dm me",
            "watsapp", "wa me"
    };

    private final FirebaseFirestore db;
    private static ChatRepository instance;

    private ChatRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static ChatRepository getInstance() {
        if (instance == null) {
            instance = new ChatRepository();
        }
        return instance;
    }

    // ── Message Operations ────────────────────────────────────────────────────

    /**
     * Returns a real-time query for messages in a connection, ordered by time.
     * Attach a SnapshotListener to this in the Activity or Fragment.
     */
    public Query getMessagesQuery(String connectionId) {
        return db.collection(CONNECTIONS_COLLECTION)
                .document(connectionId)
                .collection(MESSAGES_SUB)
                .orderBy("sentAt", Query.Direction.ASCENDING);
    }

    /**
     * Sends a message. Runs client-side moderation first.
     *
     * @return ModerationResult describing whether the message was clean or flagged.
     */
    public ModerationResult moderateMessage(String text) {
        String lower = text.toLowerCase();

        // Check phone number
        if (PHONE_PATTERN.matcher(text).find()) {
            return new ModerationResult(false, "PHONE_DETECTED",
                    "Phone numbers cannot be shared in chat.");
        }

        // Check email
        if (EMAIL_PATTERN.matcher(text).find()) {
            return new ModerationResult(false, "EMAIL_DETECTED",
                    "Email addresses cannot be shared in chat.");
        }

        // Check keywords
        for (String keyword : FLAGGED_KEYWORDS) {
            if (lower.contains(keyword)) {
                return new ModerationResult(false, "KEYWORD_DETECTED",
                        "This message contains restricted content.");
            }
        }

        return new ModerationResult(true, null, null);
    }

    /**
     * Sends a message to Firestore after passing moderation,
     * then notifies the other participant via the free API.
     * Call moderateMessage() first; only call this if result.isClean == true.
     *
     * @param connectionId Chat connection ID
     * @param message      The Message object to store
     * @param senderName   Display name of the sender (for the notification preview)
     */
    public Task<DocumentReference> sendMessage(String connectionId, Message message, String senderName) {
        return db.collection(CONNECTIONS_COLLECTION)
                .document(connectionId)
                .collection(MESSAGES_SUB)
                .add(message)
                .addOnSuccessListener(ref ->
                        NotificationApi.notifyChatMessage(
                                connectionId,
                                message.getSenderUid(),
                                senderName,
                                message.getText(),
                                false
                        )
                );
    }

    /**
     * Sends a flagged message (still stored for admin review, but isBlocked=true).
     */
    public Task<DocumentReference> sendFlaggedMessage(String connectionId,
                                                       String senderUid,
                                                       String text,
                                                       String flagReason) {
        Message flagged = new Message(senderUid, text);
        Map<String, Object> data = new HashMap<>();
        data.put("senderUid", senderUid);
        data.put("text", text);
        data.put("isFlagged", true);
        data.put("flagReason", flagReason);
        data.put("isBlocked", true);
        data.put("sentAt", Timestamp.now());

        return db.collection(CONNECTIONS_COLLECTION)
                .document(connectionId)
                .collection(MESSAGES_SUB)
                .add(data);
    }

    // ── Inner Class: Moderation Result ────────────────────────────────────────

    public static class ModerationResult {
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
