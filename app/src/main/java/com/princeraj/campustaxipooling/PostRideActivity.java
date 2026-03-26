package com.princeraj.campustaxipooling;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.ui.dialog.MessageDialogFragment;

import java.util.Calendar;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Screen for posting a new ride.
 * Validates all fields before writing to local DB and Firestore.
 */
@AndroidEntryPoint
public class PostRideActivity extends BaseActivity {

    private TextInputLayout sourceLayout, destinationLayout;
    private TextInputLayout dateLayout, timeLayout, fareLayout, seatsLayout;
    private TextInputEditText sourceEt, destinationEt, routeDescEt;
    private TextInputEditText dateEt, timeEt, fareEt, seatsEt, prefsEt;
    private MaterialButton postRideBtn;

    @Inject
    com.princeraj.campustaxipooling.repository.IRideRepository rideRepo;
    
    @Inject
    com.princeraj.campustaxipooling.repository.IUserRepository userRepo;

    private final Calendar selectedDateTime = Calendar.getInstance();
    private boolean dateSet = false;
    private boolean timeSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_ride);

        bindViews();
        setupDateTimePickers();

        ImageView backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        postRideBtn.setOnClickListener(v -> attemptPostRide());
    }

    private void bindViews() {
        sourceLayout = findViewById(R.id.sourceLayout);
        destinationLayout = findViewById(R.id.destinationLayout);
        dateLayout = findViewById(R.id.dateLayout);
        timeLayout = findViewById(R.id.timeLayout);
        fareLayout = findViewById(R.id.fareLayout);
        seatsLayout = findViewById(R.id.seatsLayout);

        sourceEt = findViewById(R.id.sourceEt);
        destinationEt = findViewById(R.id.destinationEt);
        routeDescEt = findViewById(R.id.routeDescEt);
        dateEt = findViewById(R.id.dateEt);
        timeEt = findViewById(R.id.timeEt);
        fareEt = findViewById(R.id.fareEt);
        seatsEt = findViewById(R.id.seatsEt);
        prefsEt = findViewById(R.id.prefsEt);
        postRideBtn = findViewById(R.id.postRideBtn);
    }

    private void setupDateTimePickers() {
        dateEt.setOnClickListener(v -> {
            if (isFinishing() || isDestroyed()) return;
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        selectedDateTime.set(Calendar.YEAR, year);
                        selectedDateTime.set(Calendar.MONTH, month);
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                        dateEt.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
                        dateSet = true;
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        timeEt.setOnClickListener(v -> {
            if (isFinishing() || isDestroyed()) return;
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this,
                    (view, hour, minute) -> {
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                        selectedDateTime.set(Calendar.MINUTE, minute);
                        String amPm = hour < 12 ? "AM" : "PM";
                        int displayHour = hour % 12 == 0 ? 12 : hour % 12;
                        timeEt.setText(String.format("%02d:%02d %s", displayHour, minute, amPm));
                        timeSet = true;
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    false)
                    .show();
        });
    }

    private void attemptPostRide() {
        String source = getText(sourceEt);
        String destination = getText(destinationEt);
        String fareStr = getText(fareEt);
        String seatsStr = getText(seatsEt);

        if (TextUtils.isEmpty(source) || TextUtils.isEmpty(destination) || !dateSet || !timeSet ||
                TextUtils.isEmpty(fareStr) || TextUtils.isEmpty(seatsStr)) {
            Snackbar.make(postRideBtn, R.string.fill_all_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }

        long journeyMillis = selectedDateTime.getTimeInMillis();
        if (journeyMillis < System.currentTimeMillis()) {
            Snackbar.make(postRideBtn, "Departure time must be in the future", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String currentUid = userRepo.getCurrentUserUid();
        if (currentUid == null) {
            Snackbar.make(postRideBtn, "Session expired. Please log in again.", Snackbar.LENGTH_SHORT).show();
            finish();
            return;
        }

        setLoading(true);

        // State guard to prevent duplicate posting if LiveData emits multiple times (e.g. Cache + Network)
        final boolean[] postStarted = {false};

        userRepo.getUserProfile(currentUid)
                .observe(this, result -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (result.isLoading()) return;
                    if (postStarted[0]) return; // Already triggered the post

                    if (result.isError()) {
                        setLoading(false);
                        Snackbar.make(postRideBtn, "Error fetching profile: " + result.getMessage(), Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    User user = result.getData();
                    postStarted[0] = true; // Mark as started to ignore subsequent emissions
                    
                    String name = user != null ? user.getName() : "Anonymous";
                    Ride ride = new Ride(
                            currentUid,
                            name,
                            com.princeraj.campustaxipooling.util.AppConfig.getCampusId(),
                            source,
                            destination,
                            new Timestamp(selectedDateTime.getTime()),
                            Long.parseLong(fareStr),
                            Integer.parseInt(seatsStr)
                    );
                    ride.setRouteDescription(getText(routeDescEt));
                    ride.setPreferences(getText(prefsEt));

                    rideRepo.postRide(ride).observe(this, rideResult -> {
                        if (rideResult.isLoading()) return;
                        
                        if (rideResult.isSuccess()) {
                            MessageDialogFragment.newInstance("Success", "Ride posted successfully! 🚕", this::finish)
                                    .show(getSupportFragmentManager(), "post_success");
                        } else {
                            setLoading(false);
                            postStarted[0] = false; // Allow retry on error
                            MessageDialogFragment.newInstance("Error", "Failed to post: " + rideResult.getMessage(), null)
                                    .show(getSupportFragmentManager(), "post_error");
                        }
                    });
                });
    }

    private void setLoading(boolean loading) {
        postRideBtn.setEnabled(!loading);
        postRideBtn.setText(loading ? "Posting…" : "Post Ride 🚕");
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
