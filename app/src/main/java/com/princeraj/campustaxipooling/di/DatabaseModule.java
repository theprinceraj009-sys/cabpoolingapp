package com.princeraj.campustaxipooling.di;

import android.content.Context;

import com.princeraj.campustaxipooling.db.CampusTaxiDatabase;
import com.princeraj.campustaxipooling.sync.ConflictResolver;
import com.princeraj.campustaxipooling.sync.SyncManager;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/**
 * Database dependency injection module.
 * Provides Room database and sync managers.
 *
 * Phase 3: Offline Persistence & Synchronization
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    /**
     * Provides singleton CampusTaxiDatabase instance.
     * Room database for offline caching.
     */
    @Provides
    @Singleton
    public static CampusTaxiDatabase provideDatabase(
            @ApplicationContext Context context) {
        return CampusTaxiDatabase.getInstance(context);
    }

    /**
     * Provides singleton ConflictResolver.
     * Resolves conflicts when syncing offline data.
     */
    @Provides
    @Singleton
    public static ConflictResolver provideConflictResolver() {
        return new ConflictResolver();
    }

    /**
     * Provides singleton SyncManager.
     * Manages offline write queuing and synchronization.
     */
    @Provides
    @Singleton
    public static SyncManager provideSyncManager(
            @ApplicationContext Context context,
            com.google.firebase.firestore.FirebaseFirestore db,
            CampusTaxiDatabase database,
            ConflictResolver conflictResolver,
            com.princeraj.campustaxipooling.util.AppExecutors executors) {
        return new SyncManager(context, db, database, conflictResolver, executors);
    }
}
