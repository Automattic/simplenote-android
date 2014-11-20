# Simplenote for Android

Handcrafted by the Automattic Mobile Team

## Requirements

Target SDK: KitKat (android-20)
Min SDK: Ice Cream Sandwich (android-15)

## How to Configure

1) Clone repo

```bash
git clone https://github.com/Simperium/simplenote-android.git
cd simplenote-android
```

2) Import into Android Studio using the "Gradle" build option. You may need to create a `local.properties` file with the absolute path to the Android SDK:

Sample `local.properties`
```
sdk.dir=/Applications/Android Studio.app/sdk
```

3) Simperium Config

Add your simperium appid and key to Simplenote/gradle.properties

```
simperiumAppId=SIMPERIUM_APP_ID
simperiumAppKey=SIMPERIUM_KEY
hockeyAppId=HOCKEY_APP_ID
```

4) (Optional) Add the analytics.xml file to support Google Analytics

5) Install debug build with Android Studio or `./gradlew installDebug`

## Unit Tests

1) Clone simperium-android into a separate folder.

2) Go back to the Simplenote folder: 

```bash
cd path/to/simplenote-android
```

3) Add the Simperium folder:

```bash
ln -s /path/to/simperium-android/Simperium
```

4) Modify `Simperium/build.gradle` locally to default to the debug/test build:

```bash
defaultPublishConfig "supportDebug"
```

(Note this will change in the future)

5) Run the test task via gradle:

```bash
./gradlew ./gradlew :Simplenote:connectedCheck
```

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.
