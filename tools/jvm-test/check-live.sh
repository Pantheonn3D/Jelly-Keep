#!/usr/bin/env bash
# Compiles the live build's sources (app/src/main + app/src/live) against hand-written stand-ins for the
# AdMob / UMP / Play Billing / in-app review APIs (live-api-stubs/, written from Google's documented signatures).
# For sandboxes where Google Maven is blocked: it catches typos, missing imports and wrong listener shapes, but it is
# not the real SDK. Android Studio's `-Plive=true` build is the real check. Needs run.sh to have run once (R.java + jar).
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
root="$(cd "$here/../.." && pwd)"
jar="$here/build/robo-deps/android-all-15-robolectric-13954326.jar"
gen="$here/build/gen"
[ -f "$jar" ] && [ -d "$gen" ] || { echo "run tools/jvm-test/run.sh once first"; exit 1; }
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
find "$here/live-api-stubs" "$root/app/src/main/java" "$root/app/src/live/java" "$gen" -name '*.java' > "$out/sources"
javac -nowarn -source 17 -target 17 -cp "$jar" -d "$out" @"$out/sources" 2>&1 | grep -v '^warning: \[options\]' || true
[ -f "$out/com/pantheon3d/jellykeep/PlayStore.class" ] && [ -f "$out/com/pantheon3d/jellykeep/AdMobAds.class" ] \
    && echo "live sources compile against the API stand-ins" || { echo "live compile FAILED"; exit 1; }
