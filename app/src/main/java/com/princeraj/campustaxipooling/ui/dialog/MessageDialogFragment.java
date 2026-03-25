package com.princeraj.campustaxipooling.ui.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Standard dialog for simple success/error messages.
 */
public class MessageDialogFragment extends BaseDialogFragment {
    
    private String title;
    private String message;
    private Runnable onDismiss;

    public static MessageDialogFragment newInstance(String title, String message, Runnable onDismiss) {
        MessageDialogFragment fragment = new MessageDialogFragment();
        fragment.title = title;
        fragment.message = message;
        fragment.onDismiss = onDismiss;
        return fragment;
    }

    @Override
    protected void onBuildDialog(MaterialAlertDialogBuilder builder) {
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                   if (onDismiss != null) onDismiss.run();
               });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (onDismiss != null) onDismiss.run();
    }
}
