package com.princeraj.campustaxipooling.repository;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.AuthResult;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.util.SafeResult;

/**
 * Repository interface for all authentication and user profile operations.
 */
public interface IUserRepository {

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Returns the current authenticated user's UID, or null if not logged in.
     */
    String getCurrentUserUid();

    /**
     * Returns true if user is logged in AND email is verified.
     */
    boolean isUserLoggedInAndVerified();

    /**
     * Validates that the given email ends with the campus domain.
     */
    boolean isValidCampusEmail(String email);

    /**
     * Registers a new user with Firebase Auth and creates Firestore profile.
     */
    LiveData<SafeResult<com.google.firebase.auth.AuthResult>> registerUser(String email, String password,
                                                   String name, String rollNumber, String department, String campusId);

    /**
     * Signs in an existing user.
     */
    LiveData<SafeResult<AuthResult>> loginUser(String email, String password);

    /**
     * Sends a password reset email.
     */
    LiveData<SafeResult<Void>> sendPasswordReset(String email);

    /**
     * Signs out the current user.
     */
    void logout();

    // ── User Profile ──────────────────────────────────────────────────────────

    /**
     * Fetches the Firestore user document for the given UID.
     */
    LiveData<SafeResult<User>> getUserProfile(String uid);

    /**
     * Updates the FCM device token for this user.
     */
    LiveData<SafeResult<Void>> updateFcmToken(String uid, String token);

    /**
     * Checks if the user is banned.
     */
    LiveData<SafeResult<Boolean>> isUserBanned(String uid);

    /**
     * Updates user profile information.
     */
    LiveData<SafeResult<Void>> updateUserProfile(String uid, java.util.Map<String, Object> updates);

    /**
     * Returns the FirebaseUser instance, if any.
     */
    com.google.firebase.auth.FirebaseUser getCurrentFirebaseUser();

    /**
     * Uploads user profile photo to Firebase Storage.
     * Returns the download URL.
     */
    LiveData<SafeResult<String>> uploadProfilePicture(String uid, byte[] imageData);
}

