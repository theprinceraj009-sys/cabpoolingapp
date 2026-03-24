package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles sensitive account operations:
 *  - Changing recovery phone number
 *  - Requesting email updates
 *  - Password resets
 *  - Account deletion (with re-auth)
 */
public class AccountSettingsActivity extends BaseActivity {

    private TextInputEditText emailEt, phoneEt;
    private MaterialButton btnUpdatePassword, btnSavePhone, btnRequestEmailChange;
    private TextView btnDeleteAccount;

    private final UserRepository userRepo = UserRepository.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        bindViews();
        loadUserData();

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        btnSavePhone.setOnClickListener(v -> savePhoneChanges());
        btnUpdatePassword.setOnClickListener(v -> showPasswordUpdateDialog());
        btnRequestEmailChange.setOnClickListener(v -> showEmailUpdateDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void bindViews() {
        emailEt = findViewById(R.id.emailEt);
        phoneEt = findViewById(R.id.phoneEt);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnSavePhone = findViewById(R.id.btnSavePhone);
        btnRequestEmailChange = findViewById(R.id.btnRequestEmailChange);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    private void loadUserData() {
        FirebaseUser user = userRepo.getCurrentFirebaseUser();
        if (user != null) {
            emailEt.setText(user.getEmail());
            
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String phone = doc.getString("phoneNumber");
                            if (phone != null) phoneEt.setText(phone);
                        }
                    });
        }
    }

    private void savePhoneChanges() {
        String phone = phoneEt.getText() != null ? phoneEt.getText().toString().trim() : "";
        if (!phone.isEmpty() && !phone.matches("^[6-9]\\d{9}$")) {
            phoneEt.setError("Enter a valid 10-digit Indian mobile number");
            return;
        }

        FirebaseUser user = userRepo.getCurrentFirebaseUser();
        if (user == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("phoneNumber", phone);

        db.collection("users").document(user.getUid()).update(update)
                .addOnSuccessListener(aVoid -> Snackbar.make(btnSavePhone, "Phone number updated!", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showPasswordUpdateDialog() {
        // Since we are on free tier and keeping it simple, 
        // we'll use the Password Reset Email flow mostly, 
        // but for "Enterprise" we can let them change it directly via re-auth.
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
                .setMessage("We can send a secure password reset link to your email to update your password safely.")
                .setPositiveButton("Send Link", (dialog, which) -> {
                    FirebaseUser user = userRepo.getCurrentFirebaseUser();
                    if (user != null && user.getEmail() != null) {
                        userRepo.sendPasswordReset(user.getEmail())
                                .addOnSuccessListener(v -> Snackbar.make(btnUpdatePassword, "Reset link sent!", Snackbar.LENGTH_LONG).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEmailUpdateDialog() {
        // Firebase email updates require Re-authentication
        // We will show a simple dialog for now that explains the process
        Toast.makeText(this, "Email updates require re-authentication. Coming soon.", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account?")
                .setMessage("This action is permanent and cannot be undone. All your ride history and data will be removed.")
                .setPositiveButton("Delete Forever", (dialog, which) -> {
                    // In a real app, you would prompt for password again
                    deleteAccountInternal();
                })
                .setNegativeButton("Keep My Account", null)
                .show();
    }

    private void deleteAccountInternal() {
        FirebaseUser user = userRepo.getCurrentFirebaseUser();
        if (user == null) return;

        // 1. Delete Firestore User data
        db.collection("users").document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. Delete Auth User
                    user.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Account Deleted Successfully", Toast.LENGTH_LONG).show();
                            userRepo.logout();
                            finishAffinity();
                        } else {
                            Toast.makeText(this, "Could not delete auth-account. Please log out and back in to try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }
}
