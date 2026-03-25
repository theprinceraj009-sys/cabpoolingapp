package com.princeraj.campustaxipooling.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.princeraj.campustaxipooling.AccountSettingsActivity;
import com.princeraj.campustaxipooling.BaseActivity;
import com.princeraj.campustaxipooling.HomeActivity;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.util.LocaleHelper;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends BaseActivity {

    private ImageView backBtn;
    private MaterialButton btnEnglish, btnHindi;
    private MaterialSwitch switchDarkMode, switchPushNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        backBtn = findViewById(R.id.backBtn);
        btnEnglish = findViewById(R.id.btnEnglish);
        btnHindi = findViewById(R.id.btnHindi);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchPushNotifications = findViewById(R.id.switchPushNotifications);

        // Load existing prefs
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("is_dark_mode", false);
        boolean isPushEnabled = prefs.getBoolean("push_notifications", true);
        
        switchDarkMode.setChecked(isDarkMode);
        switchPushNotifications.setChecked(isPushEnabled);

        backBtn.setOnClickListener(v -> finish());

        btnEnglish.setOnClickListener(v -> setLanguage("en"));
        btnHindi.setOnClickListener(v -> setLanguage("hi"));

        MaterialButton btnGoToAccountSettings = findViewById(R.id.btnGoToAccountSettings);
        if (btnGoToAccountSettings != null) {
            btnGoToAccountSettings.setOnClickListener(v -> 
                startActivity(new Intent(this, com.princeraj.campustaxipooling.AccountSettingsActivity.class)));
        }

        switchDarkMode.setOnCheckedChangeListener((btnView, isChecked) -> {
            prefs.edit().putBoolean("is_dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked 
                ? AppCompatDelegate.MODE_NIGHT_YES 
                : AppCompatDelegate.MODE_NIGHT_NO);
        });

        switchPushNotifications.setOnCheckedChangeListener((btnView, isChecked) -> {
            prefs.edit().putBoolean("push_notifications", isChecked).apply();
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_rides");
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_rides");
            }
        });
    }

    private void setLanguage(String langCode) {
        LocaleHelper.setLocale(this, langCode);

        // Show a quick success message then restart the app to apply changes
        Snackbar.make(btnEnglish, "Language updated. Restarting...", Snackbar.LENGTH_SHORT).show();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, 800);
    }
}
