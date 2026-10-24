#!/usr/bin/env bash
# Runs app/src/test with Robolectric on a plain JVM (no Android SDK, no Google Maven).
# Needs: JDK 17+, aapt2 + framework-res.apk (Debian/Ubuntu: apt install aapt android-framework-res).
# Usage: tools/jvm-test/run.sh [gradle test args, e.g. --tests '*PlayTest.screens' -PshowOutput]
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd); root=$(cd "$here/../.." && pwd); out="$here/build"
mirror=${MAVEN_MIRROR:-https://maven-central.storage-download.googleapis.com/maven2}
fw=${FRAMEWORK_RES:-/usr/share/android-framework-res/framework-res.apk}
ver=15-robolectric-13954326
aa=android-all-instrumented-$ver-i7.jar       # what Robolectric runs (SDK 35)
plain=android-all-$ver.jar                    # the same framework, uninstrumented: what the app compiles against
mkdir -p "$out/robo-deps" "$out/res" "$out/generated-test-config/com/android/tools"

[ -s "$out/robo-deps/$aa" ] || curl -fsS --retry 4 --retry-delay 2 -o "$out/robo-deps/$aa" \
    "$mirror/org/robolectric/android-all-instrumented/$ver-i7/$aa"
[ -s "$out/robo-deps/$plain" ] || curl -fsS --retry 4 --retry-delay 2 -o "$out/robo-deps/$plain" \
    "$mirror/org/robolectric/android-all/$ver/$plain"

# Copy for the plain JVM classpath (JUnit reflects on test classes there): without android.os.Build*, so
# Conscrypt's "am I on Android?" probe gets ClassNotFound instead of running framework code outside the sandbox.
[ -s "$out/robo-deps/classpath-android.jar" ] || python3 -I - "$out/robo-deps/$plain" "$out/robo-deps/classpath-android.jar" <<'PY'
import sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as zi, zipfile.ZipFile(dst + '.tmp', 'w', zipfile.ZIP_DEFLATED) as zo:
    for e in zi.infolist():
        if not e.filename.startswith('android/os/Build'): zo.writestr(e, zi.read(e.filename))
import os; os.replace(dst + '.tmp', dst)
PY

# what AGP's manifest merger would add: package, version, uses-sdk
g="$root/app/build.gradle"
pkg=$(sed -n "s/.*applicationId '\([^']*\)'.*/\1/p" "$g"); vc=$(sed -n 's/.*versionCode \([0-9]*\).*/\1/p' "$g")
vn=$(sed -n "s/.*versionName '\([^']*\)'.*/\1/p" "$g"); min=$(sed -n 's/.*minSdk \([0-9]*\).*/\1/p' "$g"); tgt=$(sed -n 's/.*targetSdk \([0-9]*\).*/\1/p' "$g")
sed -e "s|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"|& package=\"$pkg\" android:versionCode=\"$vc\" android:versionName=\"$vn\"|" \
    -e "s|^\(\s*\)<application|\1<uses-sdk android:minSdkVersion=\"$min\" android:targetSdkVersion=\"$tgt\" />\n\1<application|" \
    -e 's|\${admobAppId}|ca-app-pub-3940256099942544~3347511713|' \
    "$root/app/src/main/AndroidManifest.xml" > "$out/AndroidManifest.xml"

rm -rf "$out/res/compiled.zip" "$out/gen"; mkdir -p "$out/gen"
aapt2 compile --dir "$root/app/src/main/res" -o "$out/res/compiled.zip"
aapt2 link -o "$out/resources.ap_" -I "$fw" --manifest "$out/AndroidManifest.xml" --java "$out/gen" \
    -A "$root/app/src/main/assets" -0 ogg --auto-add-overlay "$out/res/compiled.zip"

cat > "$out/generated-test-config/com/android/tools/test_config.properties" <<CFG
android_merged_manifest=$out/AndroidManifest.xml
android_merged_assets=$root/app/src/main/assets
android_resource_apk=$out/resources.ap_
android_custom_package=$pkg
CFG

cd "$here" && exec "$root/gradlew" --no-daemon -q -p "$here" test "$@"
