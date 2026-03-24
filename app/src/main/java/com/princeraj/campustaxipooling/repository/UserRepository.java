package com.princeraj.campustaxipooling.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.princeraj.campustaxipooling.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for all authentication and user profile operations.
 * Activities and Fragments call ONLY this repository — never FirebaseAuth directly.
 */
public class UserRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String ALLOWED_EMAIL_DOMAIN = "@cuchd.in";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    // Singleton
    private static UserRepository instance;

    private UserRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Returns the current authenticated user, or null if not logged in.
     */
    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    /**
     * Returns true if user is logged in AND email is verified.
     */
    public boolean isUserLoggedInAndVerified() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    /**
     * Validates that the given email ends with the campus domain.
     */
    public boolean isValidCampusEmail(String email) {
        return email != null && email.toLowerCase().endsWith(ALLOWED_EMAIL_DOMAIN);
    }

    /**
     * Registers a new user with Firebase Auth and creates Firestore profile.
     */
    public Task<AuthResult> registerUser(String email, String password,
                                         String name, String rollNumber, String department) {
        return auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser fbUser = authResult.getUser();
                    if (fbUser != null) {
                        // Send email verification
                        fbUser.sendEmailVerification();

                        // Create Firestore user document
                        User user = new User(
                                fbUser.getUid(), name, email,
                                rollNumber, department, "STUDENT", "CU_CHANDIGARH"
                        );
                        db.collection(USERS_COLLECTION)
                                .document(fbUser.getUid())
                                .set(user);
                    }
                });
    }

    /**
     * Signs in an existing user.
     */
    public Task<AuthResult> loginUser(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    /**
     * Sends a password reset email.
     */
    public Task<Void> sendPasswordReset(String email) {
        return auth.sendPasswordResetEmail(email);
    }

    /**
     * Signs out the current user.
     */
    public void logout() {
        auth.signOut();
    }

    // ── Firestore Profile ─────────────────────────────────────────────────────

    /**
     * Fetches the Firestore user document for the given UID.
     * Returns a Task that resolves to a DocumentSnapshot — map to User.class.
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getUserProfile(String uid) {
        return db.collection(USERS_COLLECTION).document(uid).get();
    }

    /**
     * Updates the FCM device token for this user.
     * Called on every app launch to ensure push notifications work.
     */
    public void updateFcmToken(String uid, String token) {
        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken", token);
        db.collection(USERS_COLLECTION).document(uid).update(update);
    }

    /**
     * Checks if the user is banned. Returns Task<Boolean>.
     */
    public Task<Boolean> isUserBanned(String uid) {
        return db.collection(USERS_COLLECTION).document(uid).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().exists()) {
                        Boolean banned = task.getResult().getBoolean("isBanned");
                        return Boolean.TRUE.equals(banned);
                    }
                    return false;
                });
    }

    // ── Storage Operations ──────────────────────────────────────────────────

    /**
     * Uploads heavily compressed WebP image bytes to Firebase Storage to respect free-tier limits.
     * Maps the resulting public URL back to the user's Firestore profile.
     */
    public Task<Void> uploadProfilePicture(String uid, byte[] compressedImageBytes) {
        StorageReference profileRef = storage.getReference().child("profile_images/" + uid + ".webp");
        
        return profileRef.putBytes(compressedImageBytes)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return profileRef.getDownloadUrl();
                })
                .continueWithTask(uriTask -> {
                    if (!uriTask.isSuccessful() && uriTask.getException() != null) {
                        throw uriTask.getException();
                    }
                    String downloadUrl = uriTask.getResult().toString();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("profileImageUrl", downloadUrl);
                    return db.collection(USERS_COLLECTION).document(uid).update(updates);
                });
    }
}
