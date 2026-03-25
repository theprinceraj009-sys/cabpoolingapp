package com.princeraj.campustaxipooling.util;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Lightweight fire-and-forget HTTP client for the free-hosted
 * Express notification API (deployed on Render.com or similar).
 *
 * ──────────────────────────────────────────────────────
 * HOW TO UPDATE THE SERVER URL:
 * After you deploy the Express server (functions/index.js) to Render.com,
 * paste your public URL below (e.g. https://campus-taxi-api.onrender.com).
 * ──────────────────────────────────────────────────────
 */
public class NotificationApi {

    private static final String TAG = "NotificationApi";

    // ← Replace with your actual Render URL once deployed
    private static final String BASE_URL = "https://campus-taxi-api.onrender.com";

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Notifies ride poster that a new join request was submitted.
     *
     * @param rideId        ID of the ride
     * @param requesterName Name of the person who requested
     * @param posterUid     Firebase UID of the ride poster (who receives the push)
     */
    public static void notifyJoinRequest(String rideId, String requesterName, String posterUid) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("rideId", rideId);
                body.put("requesterName", requesterName);
                body.put("posterUid", posterUid);
                body.put("status", "PENDING");
                post("/notifyJoinRequest", body);
            } catch (Exception e) {
                Log.e(TAG, "notifyJoinRequest failed", e);
            }
        });
    }

    /**
     * Notifies the joiner when the poster accepts or rejects their request.
     *
     * @param rideId    ID of the ride
     * @param status    "ACCEPTED" or "REJECTED"
     * @param joinerUid Firebase UID of the joiner (who receives the push)
     */
    public static void notifyRequestUpdate(String rideId, String status, String joinerUid) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("rideId", rideId);
                body.put("status", status);
                body.put("joinerUid", joinerUid);
                post("/notifyRequestUpdate", body);
            } catch (Exception e) {
                Log.e(TAG, "notifyRequestUpdate failed", e);
            }
        });
    }

    /**
     * Notifies the other participant when a chat message is sent.
     *
     * @param connectionId Chat connection ID
     * @param senderUid    Firebase UID of the sender
     * @param senderName   Display name of the sender
     * @param text         Message text preview
     * @param isBlocked    Whether the message was flagged/blocked
     */
    public static void notifyChatMessage(String connectionId, String senderUid,
            String senderName, String text, boolean isBlocked) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("connectionId", connectionId);
                body.put("senderUid", senderUid);
                body.put("senderName", senderName);
                body.put("text", text);
                body.put("isBlocked", isBlocked);
                post("/notifyChatMessage", body);
            } catch (Exception e) {
                Log.e(TAG, "notifyChatMessage failed", e);
            }
        });
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static void post(String path, JSONObject body) {
        // Security Audit: Unified API secret for PaaS verification
        String apiSecret = "CAMPUS_TAXI_ADMIN_NOTIFICATION_KEY_2026";
        
        RequestBody reqBody = RequestBody.create(body.toString(), JSON_TYPE);
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .header("x-api-key", apiSecret)
                .post(reqBody)
                .build();
        try {
            client.newCall(request).execute().close();
        } catch (IOException e) {
            Log.w(TAG, "POST " + path + " failed (non-critical): " + e.getMessage());
        }
    }
}
