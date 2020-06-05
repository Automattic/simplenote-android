package com.automattic.simplenote;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.automattic.simplenote.utils.DisplayUtils;

public class ShortcutDialogFragment extends AppCompatDialogFragment {
    public final static String DIALOG_TAG = "shortcut_tag";
    public final static String DIALOG_VISIBLE = "shortcut_visible";

    private boolean mIsPreview;

    private ShortcutDialogFragment(boolean isPreview) {
        mIsPreview = isPreview;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getContext() != null && getActivity() != null) {
            View view = View.inflate(requireContext(), getLayout(), null);
            return new AlertDialog.Builder(new ContextThemeWrapper(requireContext(), R.style.Dialog))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        } else {
            return super.onCreateDialog(savedInstanceState);
        }
    }

    private @LayoutRes int getLayout() {
        return DisplayUtils.isLargeScreenLandscape(requireContext()) ? R.layout.dialog_shortcuts_all :
                !(getActivity() instanceof NoteEditorActivity) ? R.layout.dialog_shortcuts_list :
                        mIsPreview ? R.layout.dialog_shortcuts_editor_preview :
                                R.layout.dialog_shortcuts_editor_edit;
    }

    public static void showShortcuts(@NonNull FragmentActivity activity, boolean isPreview) {
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);

        if (fragment != null) {
            transaction.remove(fragment);
        }

        ShortcutDialogFragment dialog = new ShortcutDialogFragment(isPreview);
        dialog.show(transaction, DIALOG_TAG);
    }
}
