package com.princeraj.campustaxipooling.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.LoginActivity;
import com.princeraj.campustaxipooling.R;

/**
 * Admin Dashboard entry point.
 *
 * SECURITY: This Activity enforces a server-side isAdmin check on load.
 * Even if client-side navigation bypasses the check, every Firestore admin
 * operation is protected by Security Rules (isAdmin == true required).
 *
 * Sub-screens loaded as Fragments into adminFragmentContainer.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView adminEmailText;
    private TextView totalUsersCount, activeRidesCount, pendingReportsCount;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        bindViews();
        guardAndLoad();
    }

    private void bindViews() {
        adminEmailText = findViewById(R.id.adminEmailText);
        totalUsersCount = findViewById(R.id.totalUsersCount);
        activeRidesCount = findViewById(R.id.activeRidesCount);
        pendingReportsCount = findViewById(R.id.pendingReportsCount);

        // Quick-action cards
        findViewById(R.id.reportsCard).setOnClickListener(v ->
                loadFragment(new AdminReportsFragment()));
        findViewById(R.id.usersCard).setOnClickListener(v ->
                loadFragment(new AdminUsersFragment()));
        findViewById(R.id.ridesCard).setOnClickListener(v ->
                loadFragment(new AdminRidesFragment()));
    }

    /**
     * Double-checks admin flag server-side before showing any content.
     * If not admin: ejects back to LoginActivity.
     */
    private void guardAndLoad() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            ejectToLogin();
            return;
        }

        adminEmailText.setText(user.getEmail());

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    Boolean isAdmin = doc.getBoolean("isAdmin");
                    if (isAdmin == null || !isAdmin) {
                        ejectToLogin();
                        return;
                    }
                    // Admin confirmed — load default sub-screen and stats
                    loadFragment(new AdminReportsFragment());
                    loadStats();
                })
                .addOnFailureListener(e -> ejectToLogin());
    }

    private void loadStats() {
        // Total users
        db.collection("users").get()
                .addOnSuccessListener(snap ->
                        totalUsersCount.setText(String.valueOf(snap.size())));

        // Active rides
        db.collection("rides").whereEqualTo("status", "ACTIVE").get()
                .addOnSuccessListener(snap ->
                        activeRidesCount.setText(String.valueOf(snap.size())));

        // Pending reports
        db.collection("reports").whereEqualTo("status", "PENDING").get()
                .addOnSuccessListener(snap -> {
                    int count = snap.size();
                    pendingReportsCount.setText(String.valueOf(count));
                });
    }

    void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.adminFragmentContainer, fragment)
                .commit();
    }

    private void ejectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}
