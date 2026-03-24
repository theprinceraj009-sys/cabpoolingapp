package com.princeraj.campustaxipooling;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.princeraj.campustaxipooling.repository.UserRepository;

/**
 * Login screen. Validates campus email domain and ban state before allowing entry.
 */
public class LoginActivity extends BaseActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEt, passwordEt;
    private MaterialButton loginBtn;
    private TextView registerTxt, forgotPasswordTxt;

    private final UserRepository userRepo = UserRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bindViews();
        handleIntentExtras();
        setupClickListeners();
    }

    private void bindViews() {
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        loginBtn = findViewById(R.id.loginBtn);
        registerTxt = findViewById(R.id.registerTxt);
        forgotPasswordTxt = findViewById(R.id.forgotPasswordTxt);
    }

    private void handleIntentExtras() {
        if (getIntent().getBooleanExtra("show_verify_message", false)) {
            Snackbar.make(loginBtn, "Please verify your email before logging in.",
                    Snackbar.LENGTH_LONG).show();
        }
        if (getIntent().getBooleanExtra("show_ban_message", false)) {
            Snackbar.make(loginBtn, getString(R.string.account_banned),
                    Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    private void setupClickListeners() {
        loginBtn.setOnClickListener(v -> attemptLogin());

        registerTxt.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        forgotPasswordTxt.setOnClickListener(v -> handleForgotPassword());
    }

    private void attemptLogin() {
        // Clear previous errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = getTextFrom(emailEt);
        String password = getTextFrom(passwordEt);

        // Validate
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            return;
        }
        if (!userRepo.isValidCampusEmail(email)) {
            emailLayout.setError(getString(R.string.email_domain_error));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            return;
        }

        setLoading(true);

        userRepo.loginUser(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null && !authResult.getUser().isEmailVerified()) {
                        setLoading(false);
                        userRepo.logout();
                        Snackbar.make(loginBtn,
                                "Please verify your email first.",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String uid = authResult.getUser() != null ? authResult.getUser().getUid() : "";
                    userRepo.isUserBanned(uid)
                            .addOnSuccessListener(isBanned -> {
                                setLoading(false);
                                if (isBanned) {
                                    userRepo.logout();
                                    Snackbar.make(loginBtn,
                                            getString(R.string.account_banned),
                                            Snackbar.LENGTH_INDEFINITE).show();
                                } else {
                                    startActivity(new Intent(this, HomeActivity.class));
                                    finishAffinity();
                                }
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Snackbar.make(loginBtn,
                                        "Unable to verify account status. Please check your connection.",
                                        Snackbar.LENGTH_LONG).show();
                                userRepo.logout();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    emailLayout.setError("Invalid email or password");
                });
    }

    private void handleForgotPassword() {
        String email = getTextFrom(emailEt);
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Enter your email above first");
            return;
        }
        userRepo.sendPasswordReset(email)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setLoading(boolean loading) {
        loginBtn.setEnabled(!loading);
        loginBtn.setText(loading ? "Signing in…" : getString(R.string.btn_login));
    }

    private String getTextFrom(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
