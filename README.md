# Simplenote for Android

Handcrafted by the Automattic Mobile Team

## Requirements

Target SDK: Jelly Bean (android-17)
Min SDK: Ice Cream Sandwich (android-15)

## How to Configure

1) Clone and init/update the submodules.

```bash
git clone https://github.com/Simperium/simplenote-android.git -b android-studio
cd simplenote-android
git submodule init
git submodule update
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
```
4) Install debug build with Android Studio or `./gradlew installDebug`

## Tests

Unit tests are located in `Simplenote/src/instrumentTest`. To run the tests from the command line:

```
./gradlew :Simplenote:connectedInstrumentTest # :Simplenote:cIT works as well
```
Only test failures will be reported in the terminal. The full results of the test are located in `Simplenote/build/instrumentTest-results`