# Simplenote for Android
[![Version](https://img.shields.io/badge/version-2.2-blue.svg)](https://github.com/Automattic/simplenote-android/releases/tag/2.2) [![CircleCI](https://img.shields.io/circleci/build/gh/Automattic/simplenote-android.svg?label=circleci)](https://circleci.com/gh/Automattic/simplenote-android) [![Travis CI](https://img.shields.io/travis/Automattic/simplenote-android/develop.svg?label=travisci)](https://travis-ci.org/Automattic/simplenote-android)

Simplenote for Android. Learn more at [Simplenote.com](https://simplenote.com).

## How to Configure

* Clone repository.
```shell
git clone https://github.com/Automattic/simplenote-android.git
cd simplenote-android
```

* Import into Android Studio using the Gradle build option. You may need to create a `local.properties` file with the absolute path to the Android SDK. Sample `local.properties`:
```
sdk.dir=/Applications/Android Studio.app/sdk
```

* Install debug build with Android Studio or command line with:
```shell
./gradlew installDebug
```

* Create a new account in order to use a development build. Signing in with an existing Simplenote account won't work. Use the account for **testing purposes only** as all note data will be periodically cleared out on the server.

_Note: Simplenote API features such as sharing and publishing will not work with development builds._

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.
