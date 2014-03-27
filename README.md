# Simplenote for Android

Handcrafted by the Automattic Mobile Team

## Requirements

Target SDK: KitKat (android-19)
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

Run the gradle `connectedInstrumentTest` task:

```bash
./gradlew connectedAndroidTest
```
