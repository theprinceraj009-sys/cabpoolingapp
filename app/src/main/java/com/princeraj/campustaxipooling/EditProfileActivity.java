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

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import androidx.activity.result.ActivityResultLauncher;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.bumptech.glide.Glide;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.imageview.ShapeableImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows the user to edit their name, department, and optional phone number.
 * Writes directly to the Firestore users/{uid} document.
 * Phone number is stored but only surfaced after a connection is created.
 */
public class EditProfileActivity extends BaseActivity {

    private TextInputLayout nameLayout, departmentLayout, phoneLayout, rollNumberLayout;
    private TextInputEditText nameEt, departmentEt, phoneEt, rollNumberEt;
    private MaterialSwitch switchPrivacyPhone;
    private ShapeableImageView profileImageView;
    private MaterialButton saveBtn;

    private Uri selectedImageUri = null;
    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    selectedImageUri = result.getUriContent();
                    if (selectedImageUri != null) {
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
            });

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
        switchPrivacyPhone = findViewById(R.id.switchPrivacyPhone);
        profileImageView = findViewById(R.id.profileImageView);
        saveBtn = findViewById(R.id.saveBtn);

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());
        
        findViewById(R.id.avatarContainer).setOnClickListener(v -> startCrop());

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
                        String avatarUrl = doc.getString("profileImageUrl");
                        Boolean phonePrivacy = doc.getBoolean("isPhoneVisibleToMatches");

                        if (name != null) nameEt.setText(name);
                        if (dept != null) departmentEt.setText(dept);
                        if (phone != null) phoneEt.setText(phone);
                        
                        // Handle Roll Number: Firestore value > Email Substring
                        if (roll != null && !roll.isEmpty()) {
                            rollNumberEt.setText(roll);
                        } else if (user.getEmail() != null) {
                            String emailStr = user.getEmail();
                            int atIndex = emailStr.indexOf("@");
                            if (atIndex > 0) rollNumberEt.setText(emailStr.substring(0, atIndex));
                        }

                        if (phonePrivacy != null) {
                            switchPrivacyPhone.setChecked(phonePrivacy);
                        } else {
                            switchPrivacyPhone.setChecked(true); // default
                        }

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).into(profileImageView);
                        }
                    } else if (user.getEmail() != null) {
                        // Brand new profile - fallback to email for Roll Number
                        String emailStr = user.getEmail();
                        int atIndex = emailStr.indexOf("@");
                        if (atIndex > 0) rollNumberEt.setText(emailStr.substring(0, atIndex));
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
        updates.put("isPhoneVisibleToMatches", switchPrivacyPhone.isChecked());
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        if (selectedImageUri != null) {
            byte[] compressedBytes = compressImageForFreeTier(selectedImageUri);
            if (compressedBytes != null) {
                userRepo.uploadProfilePicture(user.getUid(), compressedBytes)
                    .addOnSuccessListener(v -> completeProfileUpdate(updates, user.getUid()))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Snackbar.make(saveBtn, "Image upload failed", Snackbar.LENGTH_LONG).show();
                    });
                return; // Exit here, completeProfileUpdate runs on success
            }
        }
        
        completeProfileUpdate(updates, user.getUid());
    }

    private void completeProfileUpdate(Map<String, Object> updates, String uid) {
        db.collection("users").document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Snackbar.make(saveBtn, "Profile updated!", Snackbar.LENGTH_SHORT).show();
                    saveBtn.postDelayed(this::finish, 1000);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(saveBtn, "Failed to update: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void startCrop() {
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeGallery = true;
        options.imageSourceIncludeCamera = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.fixAspectRatio = true;
        options.cropShape = CropImageView.CropShape.OVAL;
        cropImage.launch(new CropImageContractOptions(null, options));
    }

    private byte[] compressImageForFreeTier(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) return null;
            
            // Very aggressive WebP compression for Spark Plan limits
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 60, baos); 
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setLoading(boolean loading) {
        saveBtn.setEnabled(!loading);
        saveBtn.setText(loading ? "Saving…" : "Save Changes");
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
