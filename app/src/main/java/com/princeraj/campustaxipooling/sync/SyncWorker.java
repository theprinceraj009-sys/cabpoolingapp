package com.princeraj.campustaxipooling.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.princeraj.campustaxipooling.CampusTaxiApp;

import javax.inject.Inject;

/**
 * Background worker that performs periodic sync.
 * This is a FREE alternative to real-time FCM updates.
 * Even if Google Play Services is missing, this will run in the background
 * to ensure data remains up-to-date.
 */
public class SyncWorker extends Worker {

    @Inject
    SyncManager syncManager;

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting periodic sync task...");
        
        // Access SyncManager via Application manually since WorkManager 
        // doesn't support Hilt injection in standard configurations
        // without extra boilerplate.
        Context context = getApplicationContext();
        if (context instanceof CampusTaxiApp) {
            SyncManager manager = ((CampusTaxiApp) context).getSyncManager();
            if (manager != null) {
                try {
                    // Perform sync and wait for it to finish (blocking call in worker thread)
                    Tasks.await(manager.syncAll());
                    Log.d(TAG, "Sync complete!");
                    return Result.success();
                } catch (Exception e) {
                    Log.e(TAG, "Sync failed in worker", e);
                    return Result.retry();
                }
            }
        }

        return Result.failure();
    }
}
