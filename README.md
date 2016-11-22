# Simplenote for Android

A Simplenote client for Android. Learn more about Simplenote at [Simplenote.com](https://simplenote.com).

## Development Requirements (Не надо)
* A Simperium account. [Sign up here](https://simperium.com/signup/)
* A Simperium Application ID and key. [Create a new app here](https://simperium.com/app/new/)

## How to Configure

Скачать Android Studio и пакет Android SDK 23 версии (Android 6.0) через SDK Manager

1) Clone repo (Можно прям через Андроид Студио)

```bash
git clone https://github.com/Asmadek/simplenote-android.git
cd simplenote-android
```

2) Import into Android Studio using the "Gradle" build option. You may need to create a `local.properties` file with the absolute path to the Android SDK:

Sample `local.properties`
```
sdk.dir=/Applications/Android Studio.app/sdk
```

3) Simperium Config

Add your simperium appid and key to Simplenote/gradle.properties, and an empty googleAnalyticsId:

```
simperiumAppId=yield-cannons-00f
simperiumAppKey=f4d9e4bab5fd40909481d91ae1f0d932
googleAnalyticsId=
```

4) Install debug build with Android Studio or `./gradlew installDebug` (НЕ НАДО)

_Note: Simplenote API features such as sharing and publishing will not work with development builds._

5) Через AVD manager создать виртуальный девайс, например Nexus 5 и для него скачать android 6.0 

6) Запускаем девайс

7) Сбоку будет меню gradle, и там в Симплноут выбираем install -> installDebug 

8) Если нет списка, то нажимаем плюсик и там выбираем build.gradle из папки проекта (не Simplenote)

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.
