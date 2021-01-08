package com.automattic.simplenote;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import org.wordpress.passcodelock.DefaultAppLock;
import org.wordpress.passcodelock.PasscodeUnlockActivity;

/**
 * The DefaultAppLock class opens the activity using the application context, and using the flag
 * FLAG_ACTIVITY_NEW_TASK, and as we don't have any custom affinities in the app, this means that
 * when the app is already open, the system will bring the existing task to foreground, and add the
 * PasscodeUnlockActivity to it.
 * And the fact that the widget configuration activities are part of the launcher's stack, this causes an issue,
 * as when we quit the PasscodeUnlockActivity, we will go to the previous screen in the application's stack, instead
 * of the widget configuration activity.
 * <p>
 * This custom AppLock class fixes this, by launching the PasscodeUnlockActivity in the same stack when the resumed
 * activity is the widget configuration one.
 * <p>
 * This logic was added here as the passcode library is in maintenance mode, and we don't want to break something
 * by introducing some new changes to it.
 */
public class SimplenoteAppLock extends DefaultAppLock {
    private boolean isAlreadyLocked = false;

    public SimplenoteAppLock(Application app) {
        super(app);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        super.onActivityDestroyed(activity);
        if (activity instanceof NoteWidgetLightConfigureActivity || activity instanceof NoteWidgetDarkConfigureActivity) {
            isAlreadyLocked = false;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if ((activity instanceof NoteWidgetDarkConfigureActivity
            || activity instanceof NoteWidgetLightConfigureActivity)
            && isPasswordLocked()
            && !isAlreadyLocked) {
            Intent i = new Intent(activity.getApplicationContext(), PasscodeUnlockActivity.class);
            activity.startActivity(i);
            isAlreadyLocked = true;
        } else {
            super.onActivityResumed(activity);
        }
    }
}
