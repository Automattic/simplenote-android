package com.automattic.simplenote.utils;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

import com.automattic.simplenote.R;

public class SimplenoteProgressDialogFragment extends DialogFragment {
    public static final String TAG = com.simperium.android.ProgressDialogFragment.class.getSimpleName();

    private static final String KEY_MESSAGE = "KEY_MESSAGE";

    public static SimplenoteProgressDialogFragment newInstance(String message) {
        SimplenoteProgressDialogFragment fragment = new SimplenoteProgressDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_MESSAGE, message);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(requireContext(), R.style.Dialog);

        // Use custom view for progress bar dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View progressBar = inflater.inflate(R.layout.progressbar_dialog, null);
        TextView messageView = progressBar.findViewById(R.id.message);
        messageView.setText(getArguments() != null ? getArguments().getString(KEY_MESSAGE) : "");

        return new AlertDialog.Builder(context).setView(progressBar).create();
    }
}
