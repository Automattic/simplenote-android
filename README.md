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

2) Import into Android Studio using the "Gradle" build option.

3) You should be set.

### Simperium Config

Add your simperium appid and key to /res/raw/config.properties

```
simperium.appid=SIMPERIUM\_APP\_ID
simperium.key=SIMPERIUM\_KEY
```
