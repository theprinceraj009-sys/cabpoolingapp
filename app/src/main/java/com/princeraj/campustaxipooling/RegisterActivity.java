package com.princeraj.campustaxipooling;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import com.princeraj.campustaxipooling.util.AppConfig;

/**
 * Registration screen. Collects name, roll number, department, campus email, and password.
 * Enforces campus email domain before calling Firebase Auth.
 */
@AndroidEntryPoint
public class RegisterActivity extends BaseActivity {

    private TextInputLayout nameLayout, rollLayout, departmentLayout, emailLayout, passwordLayout;
    private TextInputEditText nameEt, rollEt, departmentEt, emailEt, passwordEt;
    private MaterialButton registerBtn;
    private TextView loginTxt;

    @Inject
    com.princeraj.campustaxipooling.repository.IUserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bindViews();
        setupClickListeners();
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.nameLayout);
        rollLayout = findViewById(R.id.rollLayout);
        departmentLayout = findViewById(R.id.departmentLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);

        nameEt = findViewById(R.id.nameEt);
        rollEt = findViewById(R.id.rollEt);
        departmentEt = findViewById(R.id.departmentEt);
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);

        registerBtn = findViewById(R.id.registerBtn);
        loginTxt = findViewById(R.id.loginTxt);
    }

    private void setupClickListeners() {
        registerBtn.setOnClickListener(v -> attemptRegistration());
        loginTxt.setOnClickListener(v -> finish());
    }

    private void attemptRegistration() {
        // Clear all errors
        nameLayout.setError(null);
        rollLayout.setError(null);
        departmentLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String name = getTextFrom(nameEt);
        String roll = getTextFrom(rollEt);
        String dept = getTextFrom(departmentEt);
        String email = getTextFrom(emailEt);
        String password = getTextFrom(passwordEt);

        // Validate all fields
        boolean hasError = false;
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Full name is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(roll)) {
            rollLayout.setError("Roll / Employee number is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(dept)) {
            departmentLayout.setError("Department is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            hasError = true;
        } else if (!userRepo.isValidCampusEmail(email)) {
            emailLayout.setError(getString(R.string.email_domain_error));
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            hasError = true;
        } else if (password.length() < 8) {
            passwordLayout.setError(getString(R.string.password_length_error));
            hasError = true;
        }

        if (hasError) return;

        setLoading(true);

        // Use AppConfig for dynamic campusId
        userRepo.registerUser(email, password, name, roll, dept, AppConfig.getCampusId()).observe(this, result -> {
            if (result.isLoading()) return;

            setLoading(false);
            if (result.isSuccess()) {
                Snackbar.make(registerBtn,
                        "Account created! Please verify your email before logging in.",
                        Snackbar.LENGTH_LONG).show();
                // Sign out immediately — force email verification first
                userRepo.logout();
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                }, 2000);
            } else {
                String msg = result.getMessage();
                if (msg != null && msg.contains("email address is already in use")) {
                    emailLayout.setError("This email is already registered. Try logging in.");
                } else {
                    Snackbar.make(registerBtn,
                            "Registration failed: " + msg,
                            Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        registerBtn.setEnabled(!loading);
        registerBtn.setText(loading ? "Creating account…" : getString(R.string.btn_register));
    }

    private String getTextFrom(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
