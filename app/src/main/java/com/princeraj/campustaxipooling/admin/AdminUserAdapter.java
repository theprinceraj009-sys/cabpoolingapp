package com.princeraj.campustaxipooling.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.repository.ReportRepository;

import java.util.List;

/**
 * Adapter for the admin user management list.
 * Shows ban toggle that updates Firestore in-place.
 * Flagged users (reportCount >= 3) are shown with a warning indicator.
 */
public class AdminUserAdapter extends
        RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private final List<User> users;
    private final ReportRepository reportRepo;

    public AdminUserAdapter(List<User> users, ReportRepository reportRepo) {
        this.users = users;
        this.reportRepo = reportRepo;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(users.get(position), reportRepo);
    }

    @Override
    public int getItemCount() { return users.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class UserViewHolder extends RecyclerView.ViewHolder {

        private final TextView userInitial, userName, userEmail;
        private final TextView reportCountText, userDeptText;
        private final TextView bannedBadge;
        private final MaterialButton banToggleBtn;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userInitial = itemView.findViewById(R.id.userInitial);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            reportCountText = itemView.findViewById(R.id.reportCountText);
            userDeptText = itemView.findViewById(R.id.userDeptText);
            bannedBadge = itemView.findViewById(R.id.bannedBadge);
            banToggleBtn = itemView.findViewById(R.id.banToggleBtn);
        }

        void bind(User user, ReportRepository repo) {
            // Avatar initial
            String name = user.getName() != null ? user.getName() : "?";
            userInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            userName.setText(name);
            userEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            userDeptText.setText(user.getDepartment() != null ? user.getDepartment() : "");

            // Report count with warning for flagged users
            int count = user.getReportCount();
            String countLabel = count + " report" + (count != 1 ? "s" : "");
            if (count >= 3) countLabel = "⚠️ " + countLabel;
            reportCountText.setText(countLabel);

            // Banned state
            boolean banned = user.isBanned();
            bannedBadge.setVisibility(banned ? View.VISIBLE : View.GONE);

            // Background tint for flagged-but-not-banned
            if (count >= 3 && !banned) {
                itemView.setBackgroundColor(0x22FFB300); // Amber warning tint
            } else {
                itemView.setBackgroundColor(0x00000000);
            }

            // Ban / Unban button
            if (banned) {
                banToggleBtn.setText("Unban");
                banToggleBtn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF00D4AA));
                banToggleBtn.setOnClickListener(v -> {
                    banToggleBtn.setEnabled(false);
                    repo.unbanUser(user.getUserId())
                            .addOnSuccessListener(a -> banToggleBtn.setEnabled(true))
                            .addOnFailureListener(e -> banToggleBtn.setEnabled(true));
                });
            } else {
                banToggleBtn.setText("Ban");
                banToggleBtn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFFF5370));
                banToggleBtn.setOnClickListener(v -> {
                    banToggleBtn.setEnabled(false);
                    repo.banUser(user.getUserId(), "Banned by admin")
                            .addOnSuccessListener(a -> banToggleBtn.setEnabled(true))
                            .addOnFailureListener(e -> banToggleBtn.setEnabled(true));
                });
            }
        }
    }
}
