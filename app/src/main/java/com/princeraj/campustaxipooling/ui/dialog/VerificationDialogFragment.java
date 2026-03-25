package com.princeraj.campustaxipooling.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.princeraj.campustaxipooling.R;

/**
 * A lifecycle-aware dialog that prevents WindowLeaked errors.
 * Replaces direct AlertDialog calls in PostRideActivity.
 */
public class VerificationDialogFragment extends DialogFragment {

    public interface VerificationListener {
        void onGoToSettings();
        void onCancel();
    }

    private VerificationListener listener;

    public void setListener(VerificationListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.verification_required_title)
                .setMessage(R.string.verification_required_msg)
                .setPositiveButton(R.string.btn_go_to_settings, (dialog, which) -> {
                    if (listener != null) listener.onGoToSettings();
                })
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> {
                    if (listener != null) listener.onCancel();
                });

        return builder.create();
    }
}
