package com.automattic.simplenote.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import androidx.preference.PreferenceManager;

import com.automattic.simplenote.Simplenote;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.utils.AppLog.Type;

import org.wordpress.passcodelock.AppLockManager;

import java.nio.charset.StandardCharsets;

import static com.automattic.simplenote.Simplenote.SCROLL_POSITION_PREFERENCES;
import static com.automattic.simplenote.Simplenote.SYNC_TIME_PREFERENCES;

public class AuthUtils {
    public static void logOut(Simplenote application) {
        application.getSimperium().deauthorizeUser();

        application.getAccountBucket().reset();
        application.getNotesBucket().reset();
        application.getTagsBucket().reset();
        application.getPreferencesBucket().reset();

        application.getAccountBucket().stop();
        AppLog.add(Type.SYNC, "Stopped account bucket (AuthUtils)");
        application.getNotesBucket().stop();
        AppLog.add(Type.SYNC, "Stopped note bucket (AuthUtils)");
        application.getTagsBucket().stop();
        AppLog.add(Type.SYNC, "Stopped tag bucket (AuthUtils)");
        application.getPreferencesBucket().stop();
        AppLog.add(Type.SYNC, "Stopped preference bucket (AuthUtils)");

        // Resets analytics user back to 'anon' type
        AnalyticsTracker.refreshMetadata(null);

        // Remove wp.com token
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(application).edit();
        editor.remove(PrefUtils.PREF_WP_TOKEN);

        // Remove WordPress sites
        editor.remove(PrefUtils.PREF_WORDPRESS_SITES);
        editor.apply();

        // Remove note scroll positions
        application.getSharedPreferences(SCROLL_POSITION_PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply();

        // Remove note last sync times
        application.getSharedPreferences(SYNC_TIME_PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply();

        // Remove Passcode Lock password
        AppLockManager.getInstance().getAppLock().setPassword("");

        WidgetUtils.updateNoteWidgets(application);
    }

    public static void magicLinkLogin(Simplenote application, Uri uri) {
        String userEmail = extractEmailFromMagicLink(uri);
        String spToken = uri.getQueryParameter("token");

        application.loginWithToken(userEmail, spToken);
    }

    public static String extractEmailFromMagicLink(Uri uri) {
        String userEmailEncoded = uri.getQueryParameter("email");
        return new String(Base64.decode(userEmailEncoded, Base64.NO_WRAP), StandardCharsets.UTF_8);
    }
}
