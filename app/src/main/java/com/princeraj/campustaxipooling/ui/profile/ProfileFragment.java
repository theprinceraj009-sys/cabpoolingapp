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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.EditProfileActivity;
import com.princeraj.campustaxipooling.LoginActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.repository.RideRepository;
import com.princeraj.campustaxipooling.repository.UserRepository;

/**
 * Full profile screen. Fetches user data from Firestore and shows:
 *  - Name, email, department, roll number, campus
 *  - Stats: rides posted, rides joined (connection count), subscription tier
 *  - Edit Profile and Logout actions
 */
public class ProfileFragment extends Fragment {

    private TextView avatarInitial, profileName, profileEmail;
    private TextView ridesPostedCount, ridesJoinedCount, tierBadge, tierLabel;
    private TextView rollNumberText, departmentText;
    private MaterialButton editProfileBtn, logoutBtn;

    private final UserRepository userRepo = UserRepository.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        ridesPostedCount = view.findViewById(R.id.ridesPostedCount);
        ridesJoinedCount = view.findViewById(R.id.ridesJoinedCount);
        tierBadge = view.findViewById(R.id.tierBadge);
        tierLabel = view.findViewById(R.id.tierLabel);
        rollNumberText = view.findViewById(R.id.rollNumberText);
        departmentText = view.findViewById(R.id.departmentText);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);

        editProfileBtn.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

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

        userRepo.getUserProfile(fbUser.getUid())
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;
                    bindUserData(doc, fbUser.getEmail());
                });

        loadStats(fbUser.getUid());
    }

    private void bindUserData(DocumentSnapshot doc, String email) {
        String name = doc.getString("name");
        String roll = doc.getString("rollNumber");
        String dept = doc.getString("department");
        String tier = doc.getString("subscriptionTier");
        if (name == null) name = "Campus Member";
        if (roll == null) roll = "N/A";
        if (dept == null) dept = "N/A";
        if (tier == null) tier = "FREE";

        profileName.setText(name);
        profileEmail.setText(email != null ? email : "");
        avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        rollNumberText.setText(roll);
        departmentText.setText(dept);

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
        db.collection("rides")
                .whereEqualTo("postedByUid", uid)
                .whereEqualTo("isDeleted", false)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    ridesPostedCount.setText(String.valueOf(snap.size()));
                });

        // Count rides joined (active connections)
        db.collection("connections")
                .whereArrayContains("participants", uid)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    ridesJoinedCount.setText(String.valueOf(snap.size()));
                });
    }
}
