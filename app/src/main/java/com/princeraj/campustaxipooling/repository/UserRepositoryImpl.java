package com.princeraj.campustaxipooling.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.util.SafeResult;

import dagger.hilt.android.scopes.ActivityScoped;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Refactored UserRepository with SafeResult wrapper.
 * Handles all authentication and user profile operations.
 */
public class UserRepositoryImpl implements IUserRepository {

    private static final String TAG = "UserRepository";
    private static final String USERS_COLLECTION = "users";
    private static final String ALLOWED_EMAIL_DOMAIN = "@cuchd.in";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    @Inject
    public UserRepositoryImpl(FirebaseAuth auth, FirebaseFirestore db, FirebaseStorage storage) {
        this.auth = auth;
        this.db = db;
        this.storage = storage;
    }

    // ── Authentication ────────────────────────────────────────────────────────

    @Override
    public String getCurrentUserUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @Override
    public boolean isUserLoggedInAndVerified() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    @Override
    public boolean isValidCampusEmail(String email) {
        return email != null && email.toLowerCase().endsWith(ALLOWED_EMAIL_DOMAIN);
    }

    @Override
    public LiveData<SafeResult<AuthResult>> registerUser(String email, String password,
                                                          String name, String rollNumber, String department, String campusId) {
        MutableLiveData<SafeResult<AuthResult>> liveData = new MutableLiveData<>(SafeResult.loading());

        // Validate input
        if (!isValidCampusEmail(email)) {
            liveData.setValue(SafeResult.error(
                    "INVALID_EMAIL",
                    null,
                    "Please use your campus email (@cuchd.in)",
                    false
            ));
            return liveData;
        }

        if (password == null || password.length() < 6) {
            liveData.setValue(SafeResult.error(
                    "WEAK_PASSWORD",
                    null,
                    "Password must be at least 6 characters",
                    false
            ));
            return liveData;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser fbUser = authResult.getUser();
                    if (fbUser != null) {
                        // Send email verification
                        fbUser.sendEmailVerification();

                        // Create Firestore user document
                        User user = new User(fbUser.getUid(), name, email, rollNumber, department, "STUDENT", campusId);
                        user.setCreatedAt(Timestamp.now());

                        db.collection(USERS_COLLECTION)
                                .document(fbUser.getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User profile created: " + fbUser.getUid());
                                    liveData.setValue(SafeResult.success(authResult));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to create user profile", e);
                                    liveData.setValue(SafeResult.error(e, "Failed to create user profile."));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Registration failed", e);
                    String userMsg = "Registration failed. ";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("INVALID_EMAIL")) {
                            userMsg += "Invalid email format.";
                        } else if (e.getMessage().contains("EMAIL_EXISTS")) {
                            userMsg += "This email is already registered.";
                        } else if (e.getMessage().contains("WEAK_PASSWORD")) {
                            userMsg += "Password is too weak.";
                        } else {
                            userMsg += "Please try again.";
                        }
                    }
                    liveData.setValue(SafeResult.error(e, userMsg));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<AuthResult>> loginUser(String email, String password) {
        MutableLiveData<SafeResult<AuthResult>> liveData = new MutableLiveData<>(SafeResult.loading());

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null && !user.isEmailVerified()) {
                        Log.w(TAG, "User logged in but email not verified");
                        liveData.setValue(SafeResult.error(
                                "EMAIL_NOT_VERIFIED",
                                authResult,
                                "Please verify your email before logging in.",
                                false
                        ));
                    } else {
                        Log.d(TAG, "User logged in successfully");
                        liveData.setValue(SafeResult.success(authResult));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed", e);
                    String userMsg = "Login failed. ";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("INVALID_LOGIN_CREDENTIALS")) {
                            userMsg += "Invalid email or password.";
                        } else if (e.getMessage().contains("USER_DISABLED")) {
                            userMsg += "This account has been disabled.";
                        } else {
                            userMsg += "Please try again.";
                        }
                    }
                    liveData.setValue(SafeResult.error(e, userMsg));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> sendPasswordReset(String email) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Password reset email sent");
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send password reset email", e);
                    liveData.setValue(SafeResult.error(e, "Failed to send reset email. Please try again."));
                });

        return liveData;
    }

    @Override
    public void logout() {
        auth.signOut();
        Log.d(TAG, "User logged out");
    }

    // ── User Profile ──────────────────────────────────────────────────────────

    // Phase 5 Performance: Memory cache for user profiles to avoid redundant reads
    private final Map<String, User> userCache = new HashMap<>();

    @Override
    public LiveData<SafeResult<User>> getUserProfile(String uid) {
        MutableLiveData<SafeResult<User>> liveData = new MutableLiveData<>(SafeResult.loading());

        // Check cache first
        if (userCache.containsKey(uid)) {
            liveData.setValue(SafeResult.success(userCache.get(uid)));
            // Continue fetching fresh data from server in background if needed (stale-while-revalidate)
        }

        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        if (user != null) {
                            userCache.put(uid, user);
                        }
                        liveData.setValue(SafeResult.success(user));
                    } else {
                        liveData.setValue(SafeResult.error(
                                "NOT_FOUND",
                                null,
                                "User profile not found",
                                false
                        ));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile", e);
                    // If we have cached data, don't show error to user
                    if (!userCache.containsKey(uid)) {
                        liveData.setValue(SafeResult.error(e, "Failed to load user profile."));
                    }
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> updateFcmToken(String uid, String token) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken", token);
        update.put("updatedAt", Timestamp.now());

        db.collection(USERS_COLLECTION)
                .document(uid)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token updated");
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update FCM token", e);
                    // Non-critical operation, don't show error to user
                    liveData.setValue(SafeResult.success(null));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Boolean>> isUserBanned(String uid) {
        MutableLiveData<SafeResult<Boolean>> liveData = new MutableLiveData<>(SafeResult.loading());

        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Boolean isBanned = snapshot.getBoolean("isBanned");
                        liveData.setValue(SafeResult.success(isBanned != null && isBanned));
                    } else {
                        liveData.setValue(SafeResult.success(false));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking ban status", e);
                    // Fail safely: assume not banned
                    liveData.setValue(SafeResult.success(false));
                });

        return liveData;
    }

    @Override
    public LiveData<SafeResult<Void>> updateUserProfile(String uid, java.util.Map<String, Object> updates) {
        MutableLiveData<SafeResult<Void>> liveData = new MutableLiveData<>(SafeResult.loading());

        updates.put("updatedAt", Timestamp.now());

        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated");
                    liveData.setValue(SafeResult.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user profile", e);
                    liveData.setValue(SafeResult.error(e, "Failed to update profile."));
                });

        return liveData;
    }

    @Override
    public com.google.firebase.auth.FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    @Override
    public LiveData<SafeResult<String>> uploadProfilePicture(String uid, byte[] imageData) {
        MutableLiveData<SafeResult<String>> liveData = new MutableLiveData<>(SafeResult.loading());

        com.google.firebase.storage.StorageReference ref = storage.getReference()
                .child("profiles")
                .child(uid + ".webp");

        ref.putBytes(imageData)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    db.collection(USERS_COLLECTION).document(uid)
                            .update("profilePhotoUrl", url)
                            .addOnSuccessListener(v -> liveData.setValue(SafeResult.success(url)))
                            .addOnFailureListener(e -> liveData.setValue(SafeResult.error(e, "Failed to save photo URL")));
                })
                .addOnFailureListener(e -> liveData.setValue(SafeResult.error(e, "Upload failed")));

        return liveData;
    }
}

