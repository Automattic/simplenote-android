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

After you import, you may see this error in Android Studio when you build:
```
Gradle: 
FAILURE: Could not determine which tasks to execute.

* What went wrong:
Task 'assemble' not found in root project 'MyProject'.

* Try:
Run gradle tasks to get a list of available tasks.
```

If you do, you can read this web page for more information: [I'm an inline-style link](https://www.google.com)

One way to deal with this error is to remove the following files and then re-import the project:
* delete the `.idea` directory
* remove all the `*.iml` files.

You may also have to remove the following tag from the file `simplenote-android.iml`:

 ```
  <component name="FacetManager">
     ...remove this element and everything inside such as <facet> elements...
  </component>
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
