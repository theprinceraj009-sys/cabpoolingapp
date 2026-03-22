package com.princeraj.campustaxipooling.notifications;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.princeraj.campustaxipooling.repository.UserRepository;

import java.util.Map;

/**
 * FCM (Firebase Cloud Messaging) service.
 *
 * Handles two events:
 *  1. onNewToken()       — Refresh the FCM device token in Firestore.
 *  2. onMessageReceived()— Parse the payload and dispatch the correct local notification.
 *
 * FCM Payload structure (sent from Cloud Functions):
 * {
 *   "data": {
 *     "type": "JOIN_REQUEST" | "REQUEST_ACCEPTED" | "REQUEST_REJECTED" | "CHAT_MESSAGE",
 *     "rideId": "...",
 *     "connectionId": "...",
 *     "senderName": "...",
 *     "rideRoute": "...",
 *     "messagePreview": "..."
 *   }
 * }
 *
 * NOTE: We use data-only messages (not notification messages) so we control
 * all notification display logic at the app level.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);

        // Update token in Firestore so Cloud Functions can reach this device
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserRepository.getInstance().updateFcmToken(user.getUid(), token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty()) return;

        String type         = data.get("type");
        String rideId       = data.get("rideId");
        String connectionId = data.get("connectionId");
        String senderName   = orEmpty(data.get("senderName"));
        String rideRoute    = orEmpty(data.get("rideRoute"));
        String msgPreview   = orEmpty(data.get("messagePreview"));

        if (type == null) return;

        Log.d(TAG, "FCM received: type=" + type);

        switch (type) {
            case "JOIN_REQUEST":
                // Notify the ride POSTER
                NotificationHelper.notifyNewJoinRequest(
                        this, senderName, rideRoute, rideId);
                break;

            case "REQUEST_ACCEPTED":
                // Notify the JOINER
                NotificationHelper.notifyRequestAccepted(
                        this, senderName, rideRoute, connectionId);
                break;

            case "REQUEST_REJECTED":
                // Notify the JOINER
                NotificationHelper.notifyRequestRejected(this, rideRoute);
                break;

            case "CHAT_MESSAGE":
                // Notify when app is in background
                NotificationHelper.notifyNewChatMessage(
                        this, senderName, msgPreview, connectionId);
                break;

            default:
                Log.w(TAG, "Unknown FCM message type: " + type);
        }
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }
}
