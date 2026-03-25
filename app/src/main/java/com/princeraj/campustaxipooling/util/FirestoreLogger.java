package com.princeraj.campustaxipooling.util;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * ERP-level logging system for audit trails.
 */
public class FirestoreLogger {

    private static final String LOGS_COLLECTION = "system_logs";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static FirestoreLogger instance;

    public static synchronized FirestoreLogger getInstance() {
        if (instance == null) instance = new FirestoreLogger();
        return instance;
    }

    public void logAction(String uid, String actionType, String description) {
        Map<String, Object> log = new HashMap<>();
        log.put("uid", uid);
        log.put("action", actionType);
        log.put("description", description);
        log.put("timestamp", Timestamp.now());

        db.collection(LOGS_COLLECTION).add(log);
    }

    public void logError(String tag, String message) {
        Map<String, Object> log = new HashMap<>();
        log.put("tag", tag);
        log.put("error", message);
        log.put("timestamp", Timestamp.now());
        log.put("severity", "ERROR");

        db.collection(LOGS_COLLECTION).add(log);
    }
}
