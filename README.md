# Simplenote for Android

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
./gradlew Simplenote:installDebug
```

* Create a new account in order to use a development build. Logging in with an existing Simplenote account won't work. Use the account for **testing purposes only** as all note data will be periodically cleared out on the server.

_Note: Simplenote API features such as sharing and publishing will not work with development builds._

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.

## Tests

To run the test suite, execute the following `gradle` command:

```bash
./gradlew testRelease
```

## Setup Credentials

Simplenote is powered by the [Simperium Sync'ing protocol](https://www.simperium.com). We distribute **testing credentials** that help us authenticate your application, and verify that the API calls being made are valid. Once the Simperium account is created, you can register an app and access the APP ID and necessary API keys.

**⚠️ Please note → We're not accepting any new Simperium accounts at this time.**

After you've created your own Simperium application, you can edit the fields `simperiumAppId` and `simperiumAppKey` in the file `Simplenote/gradle.properties`. 

This will allow you to compile and run the app on a device or a simulator. Please note that this will only work the Simperium account credentials, no other Simplenote account will work.

_Note: Simplenote API features such as sharing and publishing will not work with development builds._

## Contributing

Read our [Contributing Guide](CONTRIBUTING.md) to learn about reporting issues, contributing code, and more ways to contribute.