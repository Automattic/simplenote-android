package com.automattic.simplenote.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.automattic.simplenote.R;

public class BrowserUtils {
    public static boolean isBrowserInstalled(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.simperium_url)));
        return (intent.resolveActivity(context.getPackageManager()) != null);
    }

    public static boolean copyToClipboard(Context base, String url) {
        Context context = new ContextThemeWrapper(base, base.getTheme());

        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(context.getString(R.string.app_name), url);

            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return  false;
        }
    }

    public static void launchBrowserOrShowError(@NonNull Context context, String url) {
        if (BrowserUtils.isBrowserInstalled(context)) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } else {
            BrowserUtils.showDialogErrorBrowser(context, url);
        }
    }

    public static void showDialogErrorBrowser(Context base, final String url) {
        final Context context = new ContextThemeWrapper(base, base.getTheme());
        new AlertDialog.Builder(context)
            .setTitle(R.string.simperium_dialog_title_error_browser)
            .setMessage(R.string.simperium_error_browser)
            .setNeutralButton(R.string.simperium_dialog_button_copy_url,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        copyToClipboard(context, url);
                    }
                }
            )
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }
}
