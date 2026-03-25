package com.princeraj.campustaxipooling;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseUser;
import com.princeraj.campustaxipooling.repository.UserRepository;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Entry point. Checks auth state and routes accordingly.
 * Delay: 1.5 seconds for branding visibility.
 */
@AndroidEntryPoint
public class SplashActivity extends BaseActivity {

    private final UserRepository userRepo = UserRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::navigate, 1500);
    }

    private void navigate() {
        FirebaseUser user = userRepo.getCurrentFirebaseUser();

        if (user == null) {
            // Not logged in → go to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!user.isEmailVerified()) {
            // Logged in but email not verified → go to Login with verify message
            userRepo.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("show_verify_message", true);
            startActivity(intent);
            finish();
            return;
        }

        // Logged in + verified → get profile to check ban flag and admin status
        userRepo.getUserProfile(user.getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean isBanned = doc.getBoolean("isBanned");
                        if (Boolean.TRUE.equals(isBanned)) {
                            userRepo.logout();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.putExtra("show_ban_message", true);
                            startActivity(intent);
                        } else {
                            // Route admins to dashboard, regular users to home
                            Boolean isAdmin = doc.getBoolean("isAdmin");
                            if (Boolean.TRUE.equals(isAdmin) || "ADMIN".equals(doc.getString("role"))) {
                                startActivity(new Intent(this, com.princeraj.campustaxipooling.admin.AdminDashboardActivity.class));
                            } else {
                                startActivity(new Intent(this, HomeActivity.class));
                            }
                        }
                    } else {
                        // Profile missing, default to Home
                        startActivity(new Intent(this, HomeActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Network error — be lenient, allow entry
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
    }
}