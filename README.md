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

* Set up credentials before your first build — see [Setup Credentials](#setup-credentials) below. A fresh clone builds against the placeholder Simperium app in `Simplenote/gradle.properties-example`, and logging in with a real Simplenote account against it fails with "Invalid username or password" even though the same credentials work on the web.

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

Simplenote is powered by the [Simperium Sync'ing protocol](https://www.simperium.com). The app authenticates against a specific Simperium application, identified by the `simperiumAppId` and `simperiumAppKey` fields in `Simplenote/gradle.properties`.

That file is gitignored. If it does not exist, the `copyGradlePropertiesIfMissing` task in `Simplenote/build.gradle` creates it automatically from `Simplenote/gradle.properties-example`, which contains a **placeholder Simperium app**. A build using those placeholder values cannot authenticate any real Simplenote account — login fails with "Invalid username or password" even for credentials that work at [app.simplenote.com](https://app.simplenote.com). If login is failing unexpectedly, check `BuildConfig.SIMPERIUM_APP_ID` before investigating anything else.

### Automattic contributors

Install the real credentials with:

```shell
bundle install
bundle exec fastlane run configure_apply
```

This requires access to the internal secrets store. It overwrites `Simplenote/gradle.properties` with the production values, and also populates `wpcomClientId`, which the "Log in with WordPress.com" button needs.

**Run this before your first build.** Once a build has created the placeholder `Simplenote/gradle.properties`, `configure_apply` detects a pre-existing file that differs from the encrypted one and stops to ask whether you want to see a diff — which fails outright in a non-interactive shell such as CI or an agent session. If you have already built, delete the placeholder first:

```shell
rm Simplenote/gradle.properties
bundle exec fastlane run configure_apply
```

Deleting it is safe as long as it is still identical to `Simplenote/gradle.properties-example`; verify with `diff` if unsure.

### External contributors

Registering your own Simperium application would let you build and run the app, but **Simperium is not accepting new accounts at this time**, so this path is currently closed. Contributions that do not require signing in are still very welcome — see the [Contributing Guide](CONTRIBUTING.md).

Note that the in-app signup screen does not create an account in your own Simperium application. It posts to the production endpoint at `app.simplenote.com/account/request-signup` and sends a confirmation email, so the resulting account lives in production Simperium and will not work with a development build.

_Note: Simplenote API features such as sharing and publishing will not work with development builds._

## Contributing

Read our [Contributing Guide](CONTRIBUTING.md) to learn about reporting issues, contributing code, and more ways to contribute.