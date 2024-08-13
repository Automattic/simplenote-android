package org.wordpress.passcodelock;

import android.app.Application;

public class AppLockManager {
    private static AppLockManager instance;
    private AbstractAppLock currentAppLocker;

    public static AppLockManager getInstance() {
        if (instance == null) {
            instance = new AppLockManager();
        }
        return instance;
    }

    public void enableDefaultAppLockIfAvailable(Application currentApp) {
        if (!DefaultAppLock.isSupportedApi()) return;

        if (currentAppLocker != null) {
            if (currentAppLocker instanceof DefaultAppLock) {
                // A previous default applocker is already in place
                // No need to re-enable it
                return;
            }
            // A previous NON-default applockr is in place. Disable it.
            currentAppLocker.disable();
        }

        currentAppLocker = new DefaultAppLock(currentApp);
        currentAppLocker.enable();
    }

    public boolean isDefaultLock() {
        return getAppLock() != null && getAppLock() instanceof DefaultAppLock;
    }
    
    /**
     * @return true when an App lock is available. It could be either a the Default App lock on
     * Android-v14 or higher, or a non default App lock
     */
    public boolean isAppLockFeatureEnabled() {
        return getAppLock() != null && (!isDefaultLock() || DefaultAppLock.isSupportedApi());
    }
    
    public void setCurrentAppLock(AbstractAppLock newAppLocker) {
    	if( currentAppLocker != null ) {
    		currentAppLocker.disable(); //disable the old applocker if available
    	}
        currentAppLocker = newAppLocker;
    }
    
    public AbstractAppLock getAppLock() {
        return currentAppLocker;
    }
    
    public void setExtendedTimeout(){
        if (getAppLock() == null) return;
        getAppLock().setOneTimeTimeout(AbstractAppLock.EXTENDED_TIMEOUT_S);
    }
}
