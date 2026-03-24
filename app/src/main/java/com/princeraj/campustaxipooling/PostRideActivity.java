package com.princeraj.campustaxipooling;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.repository.RideRepository;
import com.princeraj.campustaxipooling.repository.UserRepository;

import java.util.Calendar;

/**
 * Screen for posting a new ride.
 * Validates all fields before writing to Firestore via RideRepository.
 */
public class PostRideActivity extends BaseActivity {

    private TextInputLayout sourceLayout, destinationLayout;
    private TextInputLayout dateLayout, timeLayout, fareLayout, seatsLayout;
    private TextInputEditText sourceEt, destinationEt, routeDescEt;
    private TextInputEditText dateEt, timeEt, fareEt, seatsEt, prefsEt;
    private MaterialButton postRideBtn;

    private final RideRepository rideRepo = RideRepository.getInstance();
    private final UserRepository userRepo = UserRepository.getInstance();

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
        backBtn.setOnClickListener(v -> finish());

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
        // Date picker
        dateEt.setOnClickListener(v -> {
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

        // Time picker
        timeEt.setOnClickListener(v -> {
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
        // Clear all errors
        sourceLayout.setError(null);
        destinationLayout.setError(null);
        dateLayout.setError(null);
        timeLayout.setError(null);
        fareLayout.setError(null);
        seatsLayout.setError(null);

        String source = getText(sourceEt);
        String destination = getText(destinationEt);
        String routeDesc = getText(routeDescEt);
        String fareStr = getText(fareEt);
        String seatsStr = getText(seatsEt);
        String prefs = getText(prefsEt);

        boolean hasError = false;

        if (TextUtils.isEmpty(source)) {
            sourceLayout.setError("Source location is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(destination)) {
            destinationLayout.setError("Destination is required");
            hasError = true;
        }
        if (!dateSet) {
            dateLayout.setError("Select departure date");
            hasError = true;
        }
        if (!timeSet) {
            timeLayout.setError("Select departure time");
            hasError = true;
        }
        if (TextUtils.isEmpty(fareStr)) {
            fareLayout.setError("Enter total fare");
            hasError = true;
        }
        if (TextUtils.isEmpty(seatsStr)) {
            seatsLayout.setError("Enter available seats");
            hasError = true;
        }

        if (hasError) return;

        // Ensure journey is in the future (at least 30 minutes ahead)
        long journeyMillis = selectedDateTime.getTimeInMillis();
        if (journeyMillis < System.currentTimeMillis() + (30 * 60 * 1000)) {
            dateLayout.setError("Departure must be at least 30 minutes from now");
            return;
        }

        long fare = Long.parseLong(fareStr);
        int seats = Integer.parseInt(seatsStr);

        if (seats < 1 || seats > 8) {
            seatsLayout.setError("Seats must be between 1 and 8");
            return;
        }

        FirebaseUser fbUser = userRepo.getCurrentFirebaseUser();
        if (fbUser == null) {
            Snackbar.make(postRideBtn, "Session expired. Please log in again.",
                    Snackbar.LENGTH_SHORT).show();
            finish();
            return;
        }

        setLoading(true);

        // Fetch current user name from Firestore, then post ride
        userRepo.getUserProfile(fbUser.getUid())
                .addOnSuccessListener(doc -> {
                    String posterName = doc.getString("name");
                    if (posterName == null) posterName = fbUser.getEmail();

                    Ride ride = new Ride(
                            fbUser.getUid(),
                            posterName,
                            "CU_CHANDIGARH",
                            source,
                            destination,
                            new Timestamp(selectedDateTime.getTime()),
                            fare,
                            seats
                    );
                    ride.setRouteDescription(routeDesc);
                    ride.setPreferences(prefs);

                    rideRepo.postRide(ride)
                            .addOnSuccessListener(docRef -> {
                                setLoading(false);
                                Snackbar.make(postRideBtn,
                                        "Ride posted successfully! 🚕",
                                        Snackbar.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Snackbar.make(postRideBtn,
                                        "Failed to post ride: " + e.getMessage(),
                                        Snackbar.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(postRideBtn,
                            "Could not fetch your profile. Try again.",
                            Snackbar.LENGTH_SHORT).show();
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
