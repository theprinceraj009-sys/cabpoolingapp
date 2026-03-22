package com.princeraj.campustaxipooling.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.princeraj.campustaxipooling.HomeActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.SeatRequestsActivity;

/**
 * Central helper for building and displaying all local push notifications.
 *
 * Notification Channels (Android 8+):
 *  • RIDE_REQUESTS  — new join requests to the poster
 *  • RIDE_UPDATES   — request accepted/rejected to the joiner
 *  • CHAT_MESSAGES  — new chat messages
 */
public class NotificationHelper {

    // Channel IDs
    public static final String CHANNEL_RIDE_REQUESTS = "ride_requests";
    public static final String CHANNEL_RIDE_UPDATES  = "ride_updates";
    public static final String CHANNEL_CHAT          = "chat_messages";

    // Notification IDs
    private static final int NOTIF_ID_REQUEST  = 1001;
    private static final int NOTIF_ID_UPDATE   = 1002;
    private static final int NOTIF_ID_CHAT     = 1003;

    /**
     * Must be called once at app startup (in Application class or SplashActivity).
     * Creates all required notification channels.
     */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel rideReqChannel = new NotificationChannel(
                CHANNEL_RIDE_REQUESTS,
                "Join Requests",
                NotificationManager.IMPORTANCE_HIGH);
        rideReqChannel.setDescription("Someone wants to join your ride");
        manager.createNotificationChannel(rideReqChannel);

        NotificationChannel rideUpdChannel = new NotificationChannel(
                CHANNEL_RIDE_UPDATES,
                "Ride Updates",
                NotificationManager.IMPORTANCE_DEFAULT);
        rideUpdChannel.setDescription("Status updates for rides you've requested to join");
        manager.createNotificationChannel(rideUpdChannel);

        NotificationChannel chatChannel = new NotificationChannel(
                CHANNEL_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH);
        chatChannel.setDescription("New messages from your ride partner");
        manager.createNotificationChannel(chatChannel);
    }

    // ── Specific notification builders ──────────────────────────────────────

    /**
     * Notify the ride POSTER that a new join request arrived.
     *
     * @param rideId  Used to open SeatRequestsActivity directly.
     */
    public static void notifyNewJoinRequest(Context context,
                                             String requesterName,
                                             String rideRoute,
                                             String rideId) {
        Intent intent = new Intent(context, SeatRequestsActivity.class)
                .putExtra("rideId", rideId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_RIDE_REQUESTS)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle("New join request! 🚕")
                .setContentText(requesterName + " wants to join your ride: " + rideRoute)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(requesterName + " wants to join your ride: " + rideRoute
                                + "\n\nTap to view and respond."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        showNotification(context, NOTIF_ID_REQUEST, builder);
    }

    /**
     * Notify the JOINER that their request was accepted.
     *
     * @param connectionId  Used to open the chat screen.
     */
    public static void notifyRequestAccepted(Context context,
                                              String posterName,
                                              String rideRoute,
                                              String connectionId) {
        Intent intent = new Intent(context, com.princeraj.campustaxipooling.ChatActivity.class)
                .putExtra("connectionId", connectionId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_RIDE_UPDATES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Request accepted! ✅")
                .setContentText(posterName + " accepted you for: " + rideRoute)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Great news! " + posterName
                                + " accepted your request for: " + rideRoute
                                + "\n\nYou can now chat with them!"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi);

        showNotification(context, NOTIF_ID_UPDATE, builder);
    }

    /**
     * Notify the JOINER that their request was rejected.
     */
    public static void notifyRequestRejected(Context context,
                                              String rideRoute) {
        Intent intent = new Intent(context, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(context, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_RIDE_UPDATES)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Request not accepted")
                .setContentText("Your request for \"" + rideRoute + "\" was declined.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pi);

        showNotification(context, NOTIF_ID_UPDATE + 1, builder);
    }

    /**
     * Notify about a new chat message when the app is in background.
     */
    public static void notifyNewChatMessage(Context context,
                                             String senderName,
                                             String messagePreview,
                                             String connectionId) {
        Intent intent = new Intent(context, com.princeraj.campustaxipooling.ChatActivity.class)
                .putExtra("connectionId", connectionId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(context, 3, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_CHAT)
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentTitle(senderName)
                .setContentText(messagePreview)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        showNotification(context, NOTIF_ID_CHAT, builder);
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private static void showNotification(Context context, int id,
                                          NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted — silently skip
        }
    }
}
