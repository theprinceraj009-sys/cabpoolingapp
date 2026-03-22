package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.princeraj.campustaxipooling.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows the user to edit their name, department, and optional phone number.
 * Writes directly to the Firestore users/{uid} document.
 * Phone number is stored but only surfaced after a connection is created.
 */
public class EditProfileActivity extends AppCompatActivity {

    private TextInputLayout nameLayout, departmentLayout, phoneLayout, rollNumberLayout;
    private TextInputEditText nameEt, departmentEt, phoneEt, rollNumberEt;
    private MaterialButton saveBtn;

    private final UserRepository userRepo = UserRepository.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        nameLayout = findViewById(R.id.nameLayout);
        departmentLayout = findViewById(R.id.departmentLayout);
        phoneLayout = findViewById(R.id.phoneLayout);
        rollNumberLayout = findViewById(R.id.rollNumberLayout);
        
        nameEt = findViewById(R.id.nameEt);
        departmentEt = findViewById(R.id.departmentEt);
        phoneEt = findViewById(R.id.phoneEt);
        rollNumberEt = findViewById(R.id.rollNumberEt);
        saveBtn = findViewById(R.id.saveBtn);

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        prefillCurrentData();

        saveBtn.setOnClickListener(v -> saveChanges());
    }

    private void prefillCurrentData() {
        FirebaseUser user = userRepo.getCurrentFirebaseUser();
        if (user == null) return;

        userRepo.getUserProfile(user.getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String dept = doc.getString("department");
                        String phone = doc.getString("phoneNumber");
                        String roll = doc.getString("rollNumber");

                        if (name != null) nameEt.setText(name);
                        if (dept != null) departmentEt.setText(dept);
                        if (phone != null) phoneEt.setText(phone);
                        
                        if (roll != null) {
                            rollNumberEt.setText(roll);
                        } else if (user.getEmail() != null) {
                            String emailStr = user.getEmail();
                            int atIndex = emailStr.indexOf("@");
                            if (atIndex > 0) {
                                rollNumberEt.setText(emailStr.substring(0, atIndex));
                            }
                        }
                    } else if (user.getEmail() != null) {
                        String emailStr = user.getEmail();
                        int atIndex = emailStr.indexOf("@");
                        if (atIndex > 0) {
                            rollNumberEt.setText(emailStr.substring(0, atIndex));
                        }
                    }
                });
    }

    private void saveChanges() {
        nameLayout.setError(null);
        departmentLayout.setError(null);

        String name = getText(nameEt);
        String dept = getText(departmentEt);
        String phone = getText(phoneEt);
        String roll = getText(rollNumberEt);

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Name cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(dept)) {
            departmentLayout.setError("Department cannot be empty");
            return;
        }
        if (!phone.isEmpty() && !phone.matches("^[6-9]\\d{9}$")) {
            phoneLayout.setError("Enter a valid 10-digit Indian mobile number");
            return;
        }

        FirebaseUser user = userRepo.getCurrentFirebaseUser();
        if (user == null) { finish(); return; }

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("department", dept);
        if (!roll.isEmpty()) updates.put("rollNumber", roll);
        updates.put("role", "STUDENT"); // Default tag
        updates.put("email", user.getEmail());
        if (!phone.isEmpty()) updates.put("phoneNumber", phone);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Snackbar.make(saveBtn, "Profile updated!", Snackbar.LENGTH_SHORT).show();
                    // Delay finish to let snackbar show
                    saveBtn.postDelayed(this::finish, 1000);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(saveBtn,
                            "Failed to update: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        saveBtn.setEnabled(!loading);
        saveBtn.setText(loading ? "Saving…" : "Save Changes");
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
