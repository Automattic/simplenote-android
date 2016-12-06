# Simplenote for Android

A Simplenote client for Android. Learn more about Simplenote at [Simplenote.com](https://simplenote.com).

## Development Requirements
* A Simperium account. [Sign up here](https://simperium.com/signup/)
* A Simperium Application ID and key. [Create a new app here](https://simperium.com/app/new/)

## How to Configure

1. Clone repo
```bash
git clone https://github.com/Automattic/simplenote-android.git
cd simplenote-android
```

2. Import into Android Studio using the "Gradle" build option. You may need to create a `local.properties` file with the absolute path to the Android SDK:
Sample `local.properties`
```
sdk.dir=/Applications/Android Studio.app/sdk
```

3. Configure Simperium, copy the `Simplenote/gradle.properties-example` file to `Simplenote/gradle.properties` and edit your simperium appid and key (You can keep the `googleAnalyticsId` field empty). Your `Simplenote/gradle.properties` file should looks like:
```
simperiumAppId=SIMPERIUM_APP_ID
simperiumAppKey=SIMPERIUM_KEY
googleAnalyticsId=
```

4. Install debug build with Android Studio or:
```
./gradlew installDebug
```

_Note: Simplenote API features such as sharing and publishing will not work with development builds._

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.
