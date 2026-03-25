package com.princeraj.campustaxipooling.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.princeraj.campustaxipooling.EditProfileActivity;
import com.princeraj.campustaxipooling.LoginActivity;
import com.princeraj.campustaxipooling.ui.settings.SettingsActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.User;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Full profile screen. Fetches user data from Firestore and shows:
 *  - Name, email, department, roll number, campus
 *  - Stats: rides posted, rides joined (connection count), subscription tier
 *  - Edit Profile and Logout actions
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private TextView avatarInitial, profileName, profileEmail;
    private TextView ridesPostedCount, ridesJoinedCount, tierBadge, tierLabel;
    private TextView rollNumberText, departmentText;
    private MaterialButton editProfileBtn, settingsBtn, logoutBtn;
    private com.google.android.material.imageview.ShapeableImageView profileImageView;

    @Inject
    com.princeraj.campustaxipooling.repository.IUserRepository userRepo;

    @Inject
    com.princeraj.campustaxipooling.repository.IRideRepository rideRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatarInitial = view.findViewById(R.id.avatarInitial);
        profileImageView = view.findViewById(R.id.profileImageView);
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        ridesPostedCount = view.findViewById(R.id.ridesPostedCount);
        ridesJoinedCount = view.findViewById(R.id.ridesJoinedCount);
        tierBadge = view.findViewById(R.id.tierBadge);
        tierLabel = view.findViewById(R.id.tierLabel);
        rollNumberText = view.findViewById(R.id.rollNumberText);
        departmentText = view.findViewById(R.id.departmentText);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        settingsBtn = view.findViewById(R.id.settingsBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);

        editProfileBtn.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        settingsBtn.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        logoutBtn.setOnClickListener(v -> {
            userRepo.logout();
            requireActivity().startActivity(
                    new Intent(requireContext(), LoginActivity.class));
            requireActivity().finishAffinity();
        });

        loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile when returning from EditProfileActivity
        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser fbUser = userRepo.getCurrentFirebaseUser();
        if (fbUser == null) return;

        userRepo.getUserProfile(fbUser.getUid()).observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess() && result.getData() != null) {
                bindUserData(result.getData());
            }
        });

        loadStats(fbUser.getUid());
    }

    private void bindUserData(User user) {
        String name = user.getName();
        String roll = user.getRollNumber();
        String email = user.getEmail();
        String dept = user.getDepartment();
        String tier = user.getSubscriptionTier();
        String avatarUrl = user.getProfilePhotoUrl();
        
        if ((roll == null || roll.trim().isEmpty()) && email != null) {
            int atIndex = email.indexOf("@");
            if (atIndex > 0) {
                roll = email.substring(0, atIndex);
            }
        }
        
        if (name == null || name.isEmpty()) name = "Campus Member";
        if (roll == null || roll.trim().isEmpty()) roll = "N/A";
        if (dept == null) dept = "N/A";
        if (tier == null) tier = "FREE";

        profileName.setText(name);
        profileEmail.setText(email != null ? email : "");
        avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        rollNumberText.setText(roll);
        departmentText.setText(dept);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            avatarInitial.setText(""); // clear text
            com.bumptech.glide.Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .into(profileImageView);
        } else {
            profileImageView.setImageDrawable(null);
            avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        // Tier display
        if ("PREMIUM".equals(tier)) {
            tierBadge.setText("💎");
            tierLabel.setText("PREMIUM");
        } else {
            tierBadge.setText("⭐");
            tierLabel.setText("FREE");
        }
    }

    private void loadStats(String uid) {
        // Count rides posted
        rideRepo.getMyPostedRides(uid).observe(getViewLifecycleOwner(), result -> {
           if (result.getData() != null) {
               ridesPostedCount.setText(String.valueOf(result.getData().size()));
           }
        });

        // Count rides joined (active connections)
        rideRepo.getMyConnections(uid).observe(getViewLifecycleOwner(), result -> {
            if (result.getData() != null) {
                int activeCount = 0;
                for (com.princeraj.campustaxipooling.model.Connection conn : result.getData()) {
                    if (conn.isActive()) activeCount++;
                }
                ridesJoinedCount.setText(String.valueOf(activeCount));
            }
        });
    }
}
