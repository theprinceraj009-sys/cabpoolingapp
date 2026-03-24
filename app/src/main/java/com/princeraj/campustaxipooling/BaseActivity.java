package com.princeraj.campustaxipooling;

import android.content.Context;
import android.os.Bundle;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;

import com.princeraj.campustaxipooling.util.LocaleHelper;

/**
 * All Application activities must extend BaseActivity.
 * This ensures that the custom Locale (Language) selected by the user
 * is injected into the context before the UI renders.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Intercept context attachment to apply chosen language dynamically
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        // Apply Dark Mode from Settings
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("is_dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isDarkMode 
            ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
    }
}
