package com.princeraj.campustaxipooling.ui.chat;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.princeraj.campustaxipooling.R;
import com.princeraj.campustaxipooling.model.Rating;
import com.princeraj.campustaxipooling.repository.IRideRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RatingDialogFragment extends DialogFragment {

    private String rideId;
    private String fromUid;
    private String toUid;
    private String toName;

    @Inject
    IRideRepository rideRepo;

    public static RatingDialogFragment newInstance(String rideId, String fromUid, String toUid, String toName) {
        RatingDialogFragment fragment = new RatingDialogFragment();
        Bundle args = new Bundle();
        args.putString("rideId", rideId);
        args.putString("fromUid", fromUid);
        args.putString("toUid", toUid);
        args.putString("toName", toName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rideId = getArguments().getString("rideId");
            fromUid = getArguments().getString("fromUid");
            toUid = getArguments().getString("toUid");
            toName = getArguments().getString("toName");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rating, null);

        TextView titleTv = view.findViewById(R.id.ratingTitle);
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText commentEt = view.findViewById(R.id.commentEt);
        MaterialButton submitBtn = view.findViewById(R.id.submitRatingBtn);
        MaterialButton cancelBtn = view.findViewById(R.id.cancelBtn);

        titleTv.setText(getString(R.string.rate_partner_title) + ": " + toName);

        submitBtn.setOnClickListener(v -> {
            float score = ratingBar.getRating();
            if (score == 0) {
                Toast.makeText(getContext(), getString(R.string.rating_error), Toast.LENGTH_SHORT).show();
                return;
            }

            String comment = commentEt.getText().toString().trim();
            Rating rating = new Rating(rideId, fromUid, toUid, score, comment);

            submitBtn.setEnabled(false);
            submitBtn.setText(getString(R.string.status_active).equals("ACTIVE") ? "Submitting..." : "भेजा जा रहा है...");

            rideRepo.submitRating(rating).observe(this, result -> {
                if (result.isSuccess()) {
                    Toast.makeText(getContext(), getString(R.string.rating_success), Toast.LENGTH_SHORT).show();
                    dismiss();
                } else if (result.isError()) {
                    submitBtn.setEnabled(true);
                    submitBtn.setText(getString(R.string.btn_submit_rating));
                    Toast.makeText(getContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        cancelBtn.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }
}
