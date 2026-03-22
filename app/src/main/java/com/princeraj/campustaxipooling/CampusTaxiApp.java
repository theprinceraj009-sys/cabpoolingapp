package com.princeraj.campustaxipooling;

import android.app.Application;
import com.princeraj.campustaxipooling.notifications.NotificationHelper;

/**
 * Application class — runs before any Activity.
 * Creates all FCM notification channels on first launch.
 */
public class CampusTaxiApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Register notification channels (no-op on Android < 8)
        NotificationHelper.createChannels(this);
    }
}
