# Simplenote for Android

A Simplenote client for Android. Learn more about Simplenote at [Simplenote.com](https://simplenote.com).

## How to Configure

* Clone repo
```shell
git clone https://github.com/Automattic/simplenote-android.git
cd simplenote-android
```

* Import into Android Studio using the "Gradle" build option. You may need to create a `local.properties` file with the absolute path to the Android SDK:
Sample `local.properties`
```
sdk.dir=/Applications/Android Studio.app/sdk
```

* Install debug build with Android Studio or:
```shell
./gradlew installDebug
```

* Create a new account in order to use a dev build. Signing in with an existing Simplenote account won't work. Use the account for **testing purposes only** as all note data will be periodically cleared out on the server.

_Note: Simplenote API features such as sharing and publishing will not work with development builds._

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.
