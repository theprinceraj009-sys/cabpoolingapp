package com.princeraj.campustaxipooling;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.princeraj.campustaxipooling.model.Report;
import com.princeraj.campustaxipooling.repository.ReportRepository;

/**
 * Report submission screen.
 * Accepts: targetUid (user report) or targetRideId (ride report) via Intent extras.
 * User picks a category chip, optionally describes the issue, and submits.
 */
public class ReportActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_UID   = "targetUid";
    public static final String EXTRA_TARGET_RIDE  = "targetRideId";
    public static final String EXTRA_TARGET_TYPE  = "targetType";

    private ChipGroup categoryChipGroup;
    private TextInputEditText descriptionEt;
    private MaterialButton submitReportBtn;

    private final ReportRepository reportRepo = ReportRepository.getInstance();

    private String targetUid;
    private String targetRideId;
    private String targetType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        targetUid   = getIntent().getStringExtra(EXTRA_TARGET_UID);
        targetRideId = getIntent().getStringExtra(EXTRA_TARGET_RIDE);
        targetType   = getIntent().getStringExtra(EXTRA_TARGET_TYPE);
        if (targetType == null) targetType = "RIDE";

        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        descriptionEt = findViewById(R.id.descriptionEt);
        submitReportBtn = findViewById(R.id.submitReportBtn);

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        submitReportBtn.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        // Validate category selection
        int selectedId = categoryChipGroup.getCheckedChipId();
        if (selectedId == -1) {
            Snackbar.make(submitReportBtn,
                    "Please select a category.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Chip selectedChip = findViewById(selectedId);
        String category = mapChipToCategory(selectedChip.getId());
        String description = descriptionEt.getText() != null
                ? descriptionEt.getText().toString().trim() : "";

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        setLoading(true);

        Report report = new Report(
                user.getUid(),
                targetUid,
                targetRideId,
                targetType,
                category,
                description
        );

        reportRepo.submitReport(report)
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Snackbar.make(submitReportBtn,
                            "Report submitted. We'll review it within 24 hours.",
                            Snackbar.LENGTH_LONG).show();
                    submitReportBtn.postDelayed(this::finish, 1500);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Snackbar.make(submitReportBtn,
                            "Failed to submit report. Please try again.",
                            Snackbar.LENGTH_SHORT).show();
                });
    }

    private String mapChipToCategory(int chipId) {
        if (chipId == R.id.chipHarassment)   return "HARASSMENT";
        if (chipId == R.id.chipFakeListing)  return "FAKE_LISTING";
        if (chipId == R.id.chipScam)         return "SCAM";
        if (chipId == R.id.chipInappropriate) return "INAPPROPRIATE";
        return "OTHER";
    }

    private void setLoading(boolean loading) {
        submitReportBtn.setEnabled(!loading);
        submitReportBtn.setText(loading ? "Submitting…" : "Submit Report");
    }
}
