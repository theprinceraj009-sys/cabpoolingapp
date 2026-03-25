package com.princeraj.campustaxipooling.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.princeraj.campustaxipooling.db.entity.UserEntity;

/**
 * Data Access Object for User profile caching.
 * Provides database queries for offline-first user operations.
 *
 * Used in Phase 3: Offline Resilience
 */
@Dao
public interface UserDao {

    /**
     * Inserts a user into the local cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    /**
     * Updates an existing user.
     */
    @Update
    void updateUser(UserEntity user);

    /**
     * Deletes a user.
     */
    @Delete
    void deleteUser(UserEntity user);

    // ── Query Methods ─────────────────────────────────────────────────────────

    /**
     * Gets a user by UID.
     * Returns null if not found.
     */
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    UserEntity getUserByUid(String uid);

    /**
     * Gets a user by email.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    /**
     * Gets all users for a campus.
     * Used for searching matches when offline.
     */
    @Query("SELECT * FROM users WHERE campus_id = :campusId")
    java.util.List<UserEntity> getUsersForCampus(String campusId);

    /**
     * Gets users that need to be synced to Firestore.
     */
    @Query(
            "SELECT * FROM users " +
            "WHERE synced_at IS NULL OR synced_at < updated_at"
    )
    java.util.List<UserEntity> getUnsyncedUsers();

    /**
     * Marks a user as synced.
     */
    @Query("UPDATE users SET synced_at = :currentTime WHERE uid = :uid")
    void markUserSynced(String uid, Long currentTime);

    /**
     * Clears all cached users.
     */
    @Query("DELETE FROM users")
    void clearAllUsers();
}

