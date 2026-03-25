package com.princeraj.campustaxipooling.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.princeraj.campustaxipooling.model.Message;
import com.princeraj.campustaxipooling.util.NotificationApi;
import com.princeraj.campustaxipooling.util.SafeResult;

import dagger.hilt.android.scopes.ActivityScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Refactored ChatRepository with SafeResult wrapper.
 * Includes client-side moderation before any Firestore write.
 */
public class ChatRepositoryImpl implements IChatRepository {

    private static final String TAG = "ChatRepository";
    private static final String CONNECTIONS_COLLECTION = "connections";
    private static final String MESSAGES_SUB = "messages";

    // Client-side moderation patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b[6-9]\\d{9}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final String[] FLAGGED_KEYWORDS = {
            "call me", "my number", "whatsapp", "telegram",
            "ping me", "contact me", "give me your number", "dm me",
            "watsapp", "wa me", "instagram", "insta id", "stupid", "idiot",
            "scam", "fraud", "password", "otp", "pin", "payment code"
    };

    private final FirebaseFirestore db;
    private final com.princeraj.campustaxipooling.db.CampusTaxiDatabase database;
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();

    @Inject
    public ChatRepositoryImpl(FirebaseFirestore db, com.princeraj.campustaxipooling.db.CampusTaxiDatabase database) {
        this.db = db;
        this.database = database;
    }

    @Override
    public LiveData<SafeResult<List<Message>>> getMessages(String connectionId) {
        MutableLiveData<SafeResult<List<Message>>> liveData = new MutableLiveData<>(SafeResult.loading());

        ListenerRegistration listener = db.collection(CONNECTIONS_COLLECTION)
                .document(connectionId)
                .collection(MESSAGES_SUB)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Chat fetch failed, falling back to local cache", error);
                        // ── Phase 3: Offline Fallback ──
                        new Thread(() -> {
                            List<com.princeraj.campustaxipooling.db.entity.MessageEntity> entities = 
                                    database.messageDao().getMessagesForConnection(connectionId);
                            List<Message> messages = mapMessageEntitiesToModels(entities);
                            liveData.postValue(SafeResult.errorWithCache(error, messages, "Offline. Showing cached messages."));
                        }).start();
                        return;
                    }

                    if (snapshot != null) {
                        List<Message> messages = snapshot.toObjects(Message.class);
                        liveData.setValue(SafeResult.success(messages));

                        // Update cache
                        new Thread(() -> {
                            for (Message msg : messages) {
                                database.messageDao().insertMessage(mapMessageToEntity(connectionId, msg, true));
                            }
                        }).start();
                    }
                });

        activeListeners.add(listener);
        return liveData;
    }

    @Override
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

    @Override
    public LiveData<SafeResult<Void>> sendMessage(String connectionId, Message message, String senderName) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        // Run moderation first
        ModerationResult modResult = moderateMessage(message.getText());

        if (!modResult.isClean) {
            Log.w(TAG, "Message flagged by moderation: " + modResult.flagReason);
            // Send flagged message and show user feedback
            sendFlaggedMessage(connectionId, message.getSenderUid(), message.getText(), modResult.flagReason)
                    .observeForever(result -> {
                        if (result.isSuccess()) {
                            liveData.setValue(SafeResult.success(null));
                        } else {
                            liveData.setValue(result);
                        }
                    });
            return liveData;
        }

        // ── Phase 3: Offline-First Send ──
        final String messageId = db.collection(CONNECTIONS_COLLECTION).document().getId();
        message.setSentAt(Timestamp.now());
        
        com.princeraj.campustaxipooling.db.entity.MessageEntity entity = mapMessageToEntity(connectionId, message, false);
        entity.setMessageId(messageId);

        new Thread(() -> {
            database.messageDao().insertMessage(entity);
            
            // Initiate Firestore upload
            db.collection(CONNECTIONS_COLLECTION)
                    .document(connectionId)
                    .collection(MESSAGES_SUB)
                    .document(messageId)
                    .set(message)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Message synced to Firestore");
                        entity.setSyncedAt(System.currentTimeMillis());
                        new Thread(() -> database.messageDao().updateMessage(entity)).start();
                        liveData.postValue(SafeResult.success(null));
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Message failed to sync (will retry background sync)", e);
                        liveData.postValue(SafeResult.success(null));
                    });
        }).start();

        return liveData;
    }

    private com.princeraj.campustaxipooling.db.entity.MessageEntity mapMessageToEntity(String connectionId, Message msg, boolean synced) {
        com.princeraj.campustaxipooling.db.entity.MessageEntity entity = new com.princeraj.campustaxipooling.db.entity.MessageEntity();
        entity.setConnectionId(connectionId);
        entity.setSenderUid(msg.getSenderUid());
        entity.setText(msg.getText());
        entity.setFlagged(msg.isFlagged());
        entity.setFlagReason(msg.getFlagReason());
        entity.setBlocked(msg.isBlocked());
        if (msg.getSentAt() != null) {
            entity.setSentAt(msg.getSentAt().toDate().getTime());
        } else {
            entity.setSentAt(System.currentTimeMillis());
        }
        entity.setSyncedAt(synced ? System.currentTimeMillis() : null);
        return entity;
    }

    private List<Message> mapMessageEntitiesToModels(List<com.princeraj.campustaxipooling.db.entity.MessageEntity> entities) {
        List<Message> messages = new ArrayList<>();
        for (com.princeraj.campustaxipooling.db.entity.MessageEntity entity : entities) {
            Message msg = new Message();
            msg.setSenderUid(entity.getSenderUid());
            msg.setText(entity.getText());
            msg.setFlagged(entity.isFlagged());
            msg.setFlagReason(entity.getFlagReason());
            msg.setBlocked(entity.isBlocked());
            msg.setSentAt(new Timestamp(new java.util.Date(entity.getSentAt())));
            messages.add(msg);
        }
        return messages;
    }

    @Override
    public LiveData<SafeResult<Void>> sendFlaggedMessage(String connectionId,
                                                          String senderUid,
                                                          String text,
                                                          String flagReason) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> data = new HashMap<>();
        data.put("senderUid", senderUid);
        data.put("text", text);
        data.put("isFlagged", true);
        data.put("flagReason", flagReason);
        data.put("isBlocked", true);
        data.put("sentAt", Timestamp.now());

        db.collection(CONNECTIONS_COLLECTION)
                .document(connectionId)
                .collection(MESSAGES_SUB)
                .add(data)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Flagged message stored for review");
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to store flagged message", e);
                    liveData.setValue(SafeResult.error(e, "Failed to send message."));
                });

        return liveData;
    }

    /**
     * Cleanup: remove all active listeners when repository is destroyed.
     */
    public void cleanup() {
        for (ListenerRegistration listener : activeListeners) {
            listener.remove();
        }
        activeListeners.clear();
    }
}

