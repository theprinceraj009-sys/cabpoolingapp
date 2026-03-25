package com.princeraj.campustaxipooling;

import android.app.Application;
import com.princeraj.campustaxipooling.notifications.NotificationHelper;
import dagger.hilt.android.HiltAndroidApp;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Application class — runs before any Activity.
 * Creates all FCM notification channels on first launch.
 * Enables Hilt dependency injection across the entire application.
 */
@HiltAndroidApp
public class CampusTaxiApp extends Application {

    @Inject
    com.princeraj.campustaxipooling.util.ConnectivityObserver connectivityObserver;

    @Inject
    com.princeraj.campustaxipooling.sync.SyncManager syncManager;

    public com.princeraj.campustaxipooling.sync.SyncManager getSyncManager() {
        return syncManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Register notification channels (no-op on Android < 8)
        // OSMDroid configuration (Free Maps Alternative)
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());
        
        NotificationHelper.createChannels(this);

        // Start network monitoring for automatic background synchronization
        connectivityObserver.startObserving();

        // ── Schedule Periodic Sync (Free replacement for FCM heartbeats) ────────
        scheduleSyncWorker();
    }

    private void scheduleSyncWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                com.princeraj.campustaxipooling.sync.SyncWorker.class,
                15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "app_sync_task",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest);
    }
}
