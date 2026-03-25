package com.princeraj.campustaxipooling.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.princeraj.campustaxipooling.LoginActivity;
import com.princeraj.campustaxipooling.BaseActivity;
import com.princeraj.campustaxipooling.R;

import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Admin Dashboard entry point.
 *
 * SECURITY: This Activity enforces a server-side isAdmin check on load.
 * Even if client-side navigation bypasses the check, every Firestore admin
 * operation is protected by Security Rules (isAdmin == true required).
 *
 * Sub-screens loaded as Fragments into adminFragmentContainer.
 */
@AndroidEntryPoint
public class AdminDashboardActivity extends BaseActivity {

    private TextView adminEmailText;
    private TextView totalUsersCount, activeRidesCount, pendingReportsCount;

    @Inject
    com.princeraj.campustaxipooling.repository.IAdminRepository adminRepo;
    
    @Inject
    com.princeraj.campustaxipooling.repository.IUserRepository userRepo;

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
        String currentUid = userRepo.getCurrentUserUid();
        if (currentUid == null) {
            ejectToLogin();
            return;
        }

        // Use UserRepository for simple check
        userRepo.getUserProfile(currentUid).observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                adminEmailText.setText(result.getData().getEmail());
                if (!result.getData().isAdmin()) {
                    ejectToLogin();
                } else {
                    // Admin confirmed — load default sub-screen and stats
                    loadFragment(new AdminReportsFragment());
                    loadStats();
                }
            } else if (result.isError()) {
                ejectToLogin();
            }
        });
    }

    private void loadStats() {
        adminRepo.getSystemStats().observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                Map<String, Long> stats = result.getData();
                
                Long totalUsers = stats.get("totalUsers");
                totalUsersCount.setText(String.valueOf(totalUsers != null ? totalUsers : 0L));
                
                Long activeRides = stats.get("activeRides");
                activeRidesCount.setText(String.valueOf(activeRides != null ? activeRides : 0L));
                
                Long pendingReports = stats.get("pendingReports");
                pendingReportsCount.setText(String.valueOf(pendingReports != null ? pendingReports : 0L));
            }
        });
    }

    void loadFragment(androidx.fragment.app.Fragment fragment) {
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
