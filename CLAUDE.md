# Jelly Keep: handoff for the next Claude

You are continuing work on **Jelly Keep**, an Android idle/clicker tower defense game for Google Play, owned by Jens (pantheon3d.contact@gmail.com). Everything below was built in one earlier session. Read this file fully before you change anything.

## How Jens works with you
- **Jens makes the art and sound.** He uses image models (ChatGPT, Gemini, "GPT image") and ElevenLabs. You write the prompts, then process and integrate what he sends back. Don't draw characters or UI in code (it looks "vibe coded"). Coded art is only a fallback.
- **Batch prompts.** He can batch-generate images: give him one message with a shared rules block plus numbered items.
- **Green screen.** Images come back as JPEGs on a green screen. Key them with `tools/key_green_adaptive.py` (see the pipeline below).
- **Pressed buttons.** Image models can't draw pressed button states. Do pressed states in code (the face sinks onto its lip and darkens).
- **Game feel.** He wants the "12 principles of game feel" used deliberately: juice, input responsiveness, camera shake, squash & stretch, particles, hit stop, weight/physics, anticipation, screen flash, trails, audio feedback, appeal/character.
- **UX laws section.** His standing preference: after every design deliverable, add a short "UX laws" section. List laws/patterns applied, one line each (law → where/how), then those skipped (law → why it doesn't fit). He also wants in-context onboarding (no upfront tutorials), skeleton screens instead of spinners, and clarity over delight.
- **Visible verification.** Verify visually. Render screenshots headlessly (see Tests) and look at them before claiming something works.

## Build environment
- **Build stack:** pure Java, a single `View` with Canvas rendering, and no engine or third-party runtime libraries. AGP 8.13.2, Gradle wrapper 8.14.3, compileSdk/targetSdk 36 (Play requires 35+), minSdk 26, Java 17.
- **Android SDK:** needs platform 36, build-tools 36.0.0 and platform-tools. Put the path in `local.properties` (`sdk.dir=...`). This file is not in the zip.
- **Maven Central rate limit:** if Maven Central returns 429, `settings.gradle` already lists the Google mirror `https://maven-central.storage-download.googleapis.com/maven2/` first.
- **Build:** `./gradlew assembleRelease bundleRelease`. Outputs:
  - `app/build/outputs/apk/release/app-release.apk` (to sideload)
  - `app/build/outputs/bundle/release/app-release.aab` (to upload to Play)
- **Signing:** `jellykeep-upload.jks` + `keystore.properties` at the repo root (the password is inside). This is the upload key: never regenerate or lose it, and never commit it publicly.
- **Version:** current is versionName 1.6 / versionCode 8. Bump both for every Play upload.
- **Package:** `com.pantheon3d.jellykeep`. App name "Jelly Keep".

## Code map (`app/src/main/java/com/pantheon3d/jellykeep/`)
- `MainActivity.java`: fullscreen immersive, display-cutout handling, hosts `Game`.
- `Game.java` (~1900 lines): the whole game: simulation, input, rendering and UI.
  - **Tower tables:** `T_*` arrays (name, cost, damage, rate, range, wind-up, unlock wave, flip, sprite height). `DISPLAY` = build-menu order.
  - **Enemy tables:** `E_*` arrays (hp multiplier, speed, radius, damage, reward, sprite height, flip, intro name and hint).
  - **Economy:**
    - `hpScale(w)=8*1.165^(w-1)`, `rewardScale(w)=2*1.12^(w-1)`
    - `towerDmg = T_DMG*1.55^lv`, `upCost = T_COST*1.7^(lv+1)`
    - tap damage `3*1.5^tapLv`, base hp `100*1.3^fortLv`
    - Banker pays `rewardScale*1.2*1.25^lv` every 3 s. This was 1.5^lv, which made the economy run away; keep it ≤1.25.
  - **Flow:** waves run as a countdown, then fighting, then clear or defeat. Defeat offers Revive (rewarded ad) or Retreat (−3 waves). A boss comes every 10th wave. A chest appears every 5 cleared waves.
  - **Persistence:** SharedPreferences `"jellykeep"`. Offline earnings = 50% of the smoothed coin rate, capped at 4 h, collected via a ×2 rewarded-ad dialog.
  - **Layout:** everything scales by `u = min(W, H*0.5)/100`. Pads and the king are anchored to the painted stumps in `ground_stumps.webp` through `Art.STUMPS`/`KING_GX/GY` and `Art.cover()`, which keeps them aligned on every aspect ratio.
  - **Feel systems:** `trauma` (screen shake), `hitStop`, `flash`, springs (`tSq`/`baseSq`/`coinBump`), and particles (`SPARK, COIN, CONF, RING, DUST, WISP, GOO, POOF, BURSTFX, ARROW, TRACER, BUBBLE`).
- `Art.java`:
  - loads every sprite, each with a coded fallback if the file is missing
  - the painted UI kit drawn as 9-slices (`nine()`): panel, gold pill, plank, slot, round button
  - outlined text in the Lilita One font (OFL licence, `FONT-LICENSE-OFL.txt`)
- `Sfx.java`:
  - synthesized fallback sounds
  - recorded banks loaded from `assets/sfx/name_N.ogg`
  - `bank(name, vol, pitchVar, minGapMs[, basePitch])` picks a random take, never the same twice in a row, rate-limited
- `Ads.java`: the monetization seam. All rewarded placements (`DOUBLE_COINS`, `REVIVE`, `OFFLINE_X2`, `CHEST`) call `ads.showRewarded()`. **Right now it is a stub that grants the reward without showing an ad.**

## Content (v1.6)
- **Towers (best wave to unlock):**
  - Popper, Wall, Mortar (1)
  - Sparkler (3): rapid sparks
  - Mage (4): bubble slow aura
  - Banker (5): passive coins
  - Sniper (6): long range, targets the strongest
  - Drummer (8): +25% speed (+8%/level) to nearby towers
  - Bomb (10): big ground blast
  - Lantern (13): burn aura
  - Wall, Mortar and Bomb can't hit flyers.
- **Selling:** towers sell for 50% of total spend.
- **Enemies:**
  - Gloom, Skitter (wave 3+), Brute (wave 5+), Boss (every 10th wave)
  - Flapper (7+): flies
  - Shieldling (9+): takes 30% from direct shots
  - Splitter (11+): splits into 2 minis
  - Wisp Healer (13+): heals 8% of max hp nearby every 1.1 s
  - Each new type gets a first-sighting banner (saved in `seenMask`).
- **King (the base):** has three moods (neutral, happy, hurt) and pulses red below 30% hp.
- **Start:** new players start with 40 coins.
- **Balance (bot sim, 60 min, 3 taps/s):** reaches about wave 38 with a steady push/retreat rhythm. Bosses at 10/20/30 act as walls. Re-run the sim after every economy change.

## Tests (headless, Robolectric, under `app/src/test`)
- `./gradlew testReleaseUnitTest` runs:
  - `PlayTest.screens` / `newContent`: drive real touches, assert behaviour, and write PNG frames to `app/build/shots/`. Look at them.
  - `PlayTest.balance`: a 60-minute bot playthrough that prints the wave curve. Run with `-i` to see the output.
  - `AspectTest`: renders 16:9, 20:9 and 720p screens to check that stumps and towers line up.
  - `BanksTest`: every sound bank loads.
- Robolectric needs `android.useAndroidX=true` (already set) and `app/src/test/resources/robolectric.properties` (`sdk=35`, `graphicsMode=NATIVE`).

## Asset pipeline
- **Images:**
  - Jens sends JPEGs on green.
  - Run `tools/key_green_adaptive.py`: `cut(path, maxSide, out.webp)`. It samples each image's own backdrop, un-mixes the green from the edges, despills, crops and resizes.
  - Save to `app/src/main/assets/` as WebP.
  - Check the result on the indigo game background before using it.
- **Character sets:** if a set has one broken image, transplant the face. The happy/hurt king images are edits of one base, so they align pixel-perfectly. Cut sets with a shared crop box so the sprite doesn't jump when the face changes.
- **Baked stumps:**
  - The stumps are painted into the background. `tools/ground_with_stumps.png` + `layout.json` hold the composite that was sent to the image model, and the model kept the stumps in place.
  - If the background is regenerated, re-verify the stump positions with template matching.
- **Sounds:**
  - Jens sends ElevenLabs zips (MP3s, several takes each).
  - Run `tools/process_sfx.py`: it decodes to 44.1 kHz mono, trims lead silence, slices multi-hit takes, splices where needed, normalizes loudness and fades tails.
  - Encode to OGG Vorbis q4 as `assets/sfx/<name>_<n>.ogg`. `noCompress 'ogg'` is set.
  - Check that sounds start on the first frame, have no clicks or clipping, and that tails reach silence.
  - Jens is picky about artifacts.
- **Bubble SFX library:** Jens also has a "Bubble SFX" library (a skill in some sessions). Only its `02_Clean_Originals` are safe to ship. It currently supplies: strike, strike_hit, click, panel_open/close, deny and the 8 coin pings.

## Image prompt rules block (prepend to batches; attach the Pebble Popper image `assets/popper.webp` as the style reference)
```
Match the attached reference style exactly: hand-painted 2D mobile game art, soft painterly cel shading, thick slightly uneven dark brown outline, glossy highlights, warm top-left light. One subject per image, centered, square, solid flat pure green #00FF00 background, no shadow, no ground, no text. Friendly things use ONLY amber/orange/wood/gold/cream. Enemies use ONLY indigo/violet (deep navy to violet to lavender) with glowing pale lavender eyes, cute-spooky. Characters in 3/4 front view seen slightly from above.
```
- **Palette rule:** two hue families only. Amber = friend and UI; indigo = night and foe.
- **UI pieces** must be flat and front-on, with equal border thickness and a plain centre so they can be 9-sliced.

## TODO (most important first)
1. **AdMob.** Replace `Ads.Stub` with a real AdMob rewarded implementation (add the `play-services-ads` dependency, an App ID in the manifest, consent via UMP). Keep the stub for debug builds.
   - Then update the Play Data safety form (Advertising ID, diagnostics) and declare "Contains ads".
   - Possible IAPs: remove ads, starter pack.
2. **Test on a real phone.** Check performance (Canvas, about 500 particles max), sound and haptics. Then do a Play closed test: new personal accounts need 12 testers for 14 days.
3. **Remaining ElevenLabs sounds:** these still use synth fallbacks.
   - gloom_spawn, gloom_die, brute_die, enemy_bite, boss_appear, boss_die
   - king_hurt, king_cheer
   - wave_start, wave_clear, defeat, revive
   - chest_open, coin_shower, boost_on, ui_wood_press
   - night_loop (20 s ambience); optional music loop
   - new towers: sparkler fizz, sniper shot, bomb explosion, bubble pop, drum hit, lantern crackle, banker coin
   - Wire each through `sfx.bank()` at the existing call sites; each call site falls back to synth when a bank is missing.
4. **Prestige / rebirth** for late-game longevity: progress slows after about wave 40, which is a natural place for it.
5. **Polish:** the fixed portrait orientation is ignored on tablets (600dp+) on Android 16, so consider a tablet layout check.

## Publishing notes
See `PUBLISHING.md`. In short:
- **Category:** Game → Strategy/Casual.
- **Data safety:** currently "no data collected"; this changes with AdMob.
- **Privacy policy:** a URL is required. The text is in PUBLISHING.md.
- **Store assets:** the store assets zip from the session holds the 512 icon, the 1024×500 feature graphic and screenshots.

## Older sibling project
"Squish" (`com.pantheon3d.squish`) was the first prototype, a one-tap blob clicker, and is superseded by Jelly Keep. It has its own key (`squish-upload.jks`), kept separately. Don't mix the two keys up.
