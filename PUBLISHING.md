# Jelly Keep: notes

## Files
- `JellyKeep-1.0.aab`: upload to Google Play.
- `JellyKeep-1.0.apk`: install on your phone to test.
- `jellykeep-upload.jks` + `keystore.properties` (in the source zip): the upload key for THIS app. Back it up.

## Build facts
- Package `com.pantheon3d.jellykeep`, targetSdk 36, minSdk 26 (Android 8+).
- No internet permission, no ads SDK yet, no analytics. Only permission: VIBRATE.
- Font: Lilita One (SIL Open Font License, see FONT-LICENSE-OFL.txt). Free for commercial use.
- All character, UI and effect art: your generated images, keyed and packed as assets/*.webp. Tower sounds: your ElevenLabs batch (assets/sfx); the rest are synthesized or from your Bubble SFX library (clean originals only).

## Monetization seam
All rewarded placements go through `Ads.java` (×2 coins, revive, offline ×2, chest ×3).
Right now `Ads.Stub` grants the reward with no ad. Replace it with AdMob before
release, or the buttons simply give free rewards.

## Play Console
- Category: Game → Strategy (or Casual)
- Data safety: no data collected (this changes once AdMob is added: then declare Advertising ID + diagnostics)
- Ads: "No" for now; "Yes" after AdMob
- Privacy policy text (host it anywhere):
  > Jelly Keep does not collect or share personal data. Progress is saved only on your device. Contact: pantheon3d.contact@gmail.com
- New personal developer accounts need a closed test (12 testers, 14 days) before production.

## Tests
`./gradlew testReleaseUnitTest` runs a headless playthrough (screens to app/build/shots) and a
45-minute bot balance run that prints the wave curve.

## Content (v1.6)
- 10 towers, unlocked by best wave: Popper/Wall/Mortar (1), Sparkler (3), Mage (4), Banker (5), Sniper (6), Drummer (8), Bomb (10), Lantern (13).
- 8 enemies: Gloom, Skitter, Brute, Boss (every 10th wave), Flapper (7+, flies), Shieldling (9+, blocks shots), Splitter (11+), Wisp Healer (13+).
- Towers can be sold for 50% of everything spent on them.
- Bot balance run (60 min, 3 taps/s): wave ~38, bosses act as walls, retreat/push loop throughout.
