package com.princeraj.campustaxipooling.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.princeraj.campustaxipooling.sync.SyncManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import com.princeraj.campustaxipooling.sync.SyncWorker;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Proactively observes network connectivity changes.
 * When network returns, triggers SyncManager to push offline changes.
 *
 * Phase 3: Automated synchronization.
 */
@Singleton
public class ConnectivityObserver {

    private static final String TAG = "ConnectivityObserver";
    private final ConnectivityManager connectivityManager;
    private final SyncManager syncManager;
    private final Context context;

    @Inject
    public ConnectivityObserver(
            @ApplicationContext Context context,
            SyncManager syncManager) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.syncManager = syncManager;
    }

    /**
     * Start observing network changes.
     */
    public void startObserving() {
        Log.d(TAG, "Starting connectivity monitoring...");

        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null, cannot observe network.");
            return;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.i(TAG, "Network restored. Triggering background sync.");

                // ── Use WorkManager for 100% thread safety (NEVER on main thread) ──
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints)
                        .setInitialDelay(2, java.util.concurrent.TimeUnit.SECONDS) // Stabilization delay
                        .build();

                WorkManager.getInstance(context).enqueue(syncRequest);
                Log.i(TAG, "SyncWorker enqueued successfully.");
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.w(TAG, "Network lost. App entering offline mode.");
            }
        });
    }

    /**
     * Helper to check current network status.
     */
    public boolean isConnected() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            @SuppressWarnings("deprecation")
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
}
