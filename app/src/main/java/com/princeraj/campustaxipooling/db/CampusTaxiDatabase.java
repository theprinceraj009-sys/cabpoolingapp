package com.princeraj.campustaxipooling.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.princeraj.campustaxipooling.db.dao.MessageDao;
import com.princeraj.campustaxipooling.db.dao.RideDao;
import com.princeraj.campustaxipooling.db.dao.UserDao;
import com.princeraj.campustaxipooling.db.entity.MessageEntity;
import com.princeraj.campustaxipooling.db.entity.RideEntity;
import com.princeraj.campustaxipooling.db.entity.UserEntity;

/**
 * Room database for Campus Taxi Pooling application.
 * Handles offline caching of Firestore data.
 *
 * Phase 3: Offline Resilience
 * - Caches rides, messages, users locally
 * - Enables offline-first reads
 * - Queues offline writes for later sync
 *
 * Architecture:
 * Firestore (online) ←→ Room (local cache) → UI (always responsive)
 *
 * Thread Safety:
 * - All database operations must be on non-UI thread
 * - Use LiveData or Flow for reactive updates
 * - Hilt provides singleton instance
 */
@Database(
        entities = {RideEntity.class, MessageEntity.class, UserEntity.class},
        version = 2,
        exportSchema = false  // Set to true in production for migration support
)
public abstract class CampusTaxiDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "campus_taxi_pooling.db";

    // ── Abstract DAOs ─────────────────────────────────────────────────────────

    public abstract RideDao rideDao();
    public abstract MessageDao messageDao();
    public abstract UserDao userDao();

    // ── Singleton Instance ────────────────────────────────────────────────────

    private static volatile CampusTaxiDatabase instance;

    /**
     * Gets the singleton database instance.
     * Thread-safe lazy initialization.
     */
    public static CampusTaxiDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (CampusTaxiDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            CampusTaxiDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()  // Wipe old schema on upgrade (dev only)
                    .build();
                }
            }
        }
        return instance;
    }

    /**
     * Closes the database connection.
     * Should be called when app is shutting down.
     */
    public void close() {
        if (isOpen()) {
            super.close();
        }
    }
}

