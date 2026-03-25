package com.princeraj.campustaxipooling.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Base class for all app dialogs to prevent WindowLeaked crashes.
 */
public abstract class BaseDialogFragment extends DialogFragment {
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        onBuildDialog(builder);
        return builder.create();
    }

    protected abstract void onBuildDialog(MaterialAlertDialogBuilder builder);
}
