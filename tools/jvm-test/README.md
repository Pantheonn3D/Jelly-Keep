# JVM test harness

This runs the app's own Robolectric tests (`app/src/test`) on a plain JVM. It needs no Android SDK and no Google Maven access. That suits cloud sandboxes that block `dl.google.com`. On a normal machine, keep using the regular command:

```
./gradlew testReleaseUnitTest
```

## Run

```
sudo apt install aapt android-framework-res     # Ubuntu/Debian: aapt2 + framework-res.apk
tools/jvm-test/run.sh                           # whole suite
tools/jvm-test/run.sh --tests '*PlayTest.screens'
```

Screenshots land in `app/build/shots/`, the same place as with the Android Gradle plugin (AGP). Test stdout, such as the balance curve, is in `tools/jvm-test/build/test-results/test/*.xml`.

## What it does

AGP normally does four things for a unit-test run. `run.sh` does them by hand:

1. It downloads Robolectric's `android-all-instrumented` jar (SDK 35) from the Maven Central mirror.
2. It writes the merged manifest. That means adding the package, version and `uses-sdk` from `app/build.gradle`.
3. It links the resource APK with `aapt2`. That APK holds the assets, with `.ogg` files stored uncompressed. The `resValue` strings from `app/build.gradle` (the default AdMob test unit ids) are generated into it too, as AGP would.
4. It writes `com/android/tools/test_config.properties` so that Robolectric finds those files.

Robolectric depends on `androidx.test:monitor`, which is published only on Google Maven. `androidx-stubs/` holds the dozen tiny classes Robolectric 4.17 actually calls. They are test-only and never reach the app.

## The live build's sources

```
tools/jvm-test/check-live.sh        # after run.sh has run once
```

This script compiles `app/src/main` with `app/src/live` (AdMob, UMP, Play Billing 8, in-app review) against `live-api-stubs/`. Those are compile-only stand-ins for exactly the API surface the live code uses, written from Google's documented signatures.

It catches typos, missing imports and wrong listener shapes. It does not catch drift in the real SDKs. The real check is Android Studio's `./gradlew -Plive=true bundleRelease`.

## Probes and seeded balance runs

`-PextraTests=<dir>` adds a directory of throwaway test classes, such as a parameter sweep over the bot in `PlayTest`. `-PshowOutput` streams their stdout.

The balance bots seed `Game.rnd`, so two runs of the same code print the same curve.
