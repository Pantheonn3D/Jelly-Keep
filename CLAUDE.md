# Jelly Keep: handoff for the next Claude

You are continuing work on **Jelly Keep**, an Android idle/clicker tower defense game for Google Play, owned by Jens (pantheon3d.contact@gmail.com). Read this file fully before you change anything. The design rationale for 2.0 (audience research, what was borrowed from Eatventure, Block Blast and Candy Crush, and why) is in `docs/superpowers/specs/2026-10-23-jelly-keep-2-design.md`.

## How Jens works with you
- **Jens makes the art and sound.** He uses image models (ChatGPT, Gemini, "GPT image") and ElevenLabs. You write the prompts, then process and integrate what he sends back. Don't draw characters or UI in code (it looks "vibe coded"). Coded art is only a fallback.
- **Batch prompts.** He can batch-generate images: give him one message with a shared rules block plus numbered items. The open batches are in `docs/art-and-audio-requests.md`.
- **Green screen.** Images come back as JPEGs on a green screen. Key them with `tools/key_green_adaptive.py` (see the pipeline below).
- **Pressed buttons.** Image models can't draw pressed button states. Do pressed states in code (the face sinks onto its lip and darkens).
- **Game feel.** He wants the "12 principles of game feel" used deliberately: juice, input responsiveness, camera shake, squash & stretch, particles, hit stop, weight/physics, anticipation, screen flash, trails, audio feedback, appeal/character.
- **UX laws section.** His standing preference: after every design deliverable, add a short "UX laws" section. List laws/patterns applied, one line each (law → where/how), then those skipped (law → why it doesn't fit). He also wants in-context onboarding (no upfront tutorials), skeleton screens instead of spinners, and clarity over delight.
- **Visible verification.** Verify visually. Render screenshots headlessly (see Tests) and look at them before claiming something works.

## Build environment
- **Build stack:** pure Java, a single `View` with Canvas rendering, and no engine. AGP 8.13.2, Gradle wrapper 8.14.3, compileSdk/targetSdk 36 (Play requires 35+), minSdk 26, Java 17.
- **Android SDK:** needs platform 36, build-tools 36.0.0 and platform-tools. Put the path in `local.properties` (`sdk.dir=...`). This file is not in the repo.
- **Maven Central rate limit:** if Maven Central returns 429, `settings.gradle` already lists the Google mirror `https://maven-central.storage-download.googleapis.com/maven2/` first.
- **Test build (default):** `./gradlew assembleRelease bundleRelease`
  - No ad or billing SDK at all.
  - It uses a clearly labelled simulated ad and an in-game "Test purchase" confirm.
  - Fine for your phone and the closed test. **Never use it for production**, because every reward is free.
- **Live build:** `./gradlew -Plive=true bundleRelease`
  - Compiles `app/src/live/java` instead of `app/src/stub/java`: AdMob 25.5.0 + UMP 4.0.0 consent, Play Billing 9.1.0, Play in-app review 2.0.2.
  - Verified on 2026-10-24: it compiles, shrinks with R8, passes lint and signs against the real SDKs. It has **not** run on a device yet (the cloud container has no KVM for an emulator).
  - Debugging on a phone: `adb logcat -s JellyKeep` shows why consent, an ad or the store failed.
  - Ad IDs come from `-PadmobAppId`, `-PadmobRewarded` and `-PadmobInterstitial` (keep them in `~/.gradle/gradle.properties`). Without them it uses Google's test IDs.
  - All steps are in `PUBLISHING.md`.
- **Outputs:**
  - `app/build/outputs/apk/release/app-release.apk` (to sideload)
  - `app/build/outputs/bundle/release/app-release.aab` (to upload to Play)
- **Signing:** `jellykeep-upload.jks` + `keystore.properties` at the repo root (the password is inside). This is the upload key: never regenerate or lose it, and never commit it publicly. `.gitignore` blocks `*.jks` and `keystore.properties`.
- **Version:** current is versionName 2.0 / versionCode 9 (Play has seen 8). Bump both for every Play upload.
- **Package:** `com.pantheon3d.jellykeep`. App name "Jelly Keep".

### Claude's cloud sandbox
Jens allowed all network domains for this cloud environment on 2026-10-24, so real AGP builds work there. A fresh container needs the SDK first:
```
mkdir -p /opt/android-sdk/cmdline-tools && cd /tmp
curl -sSO https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip    # newest in repository2-3.xml
unzip -q commandlinetools-linux-*_latest.zip && mv cmdline-tools /opt/android-sdk/cmdline-tools/latest
yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/opt/android-sdk --licenses
/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/opt/android-sdk "platforms;android-36" "build-tools;36.0.0" "platform-tools"
echo "sdk.dir=/opt/android-sdk" > local.properties
```
- Release builds also need `jellykeep-upload.jks` + `keystore.properties` at the repo root. Both are gitignored and must never be committed. They come from Jens's source zip; ask him if they're missing.
- There is no KVM, so no emulator. Device testing is Jens's.

If a sandbox blocks `dl.google.com` again, two tools stand in:
- **`tools/jvm-test/run.sh`** runs the real `app/src/test` suite on a plain JVM.
  - It uses Robolectric 4.17 with native graphics, and gets aapt2 and framework-res from Ubuntu packages and the framework jar from Maven Central.
  - Screenshots are real Skia renders. See `tools/jvm-test/README.md`.
- **`tools/jvm-test/check-live.sh`** compiles `src/main` + `src/live` against `tools/jvm-test/live-api-stubs/`.
  - Those are hand-written stand-ins for the AdMob, UMP, Billing and Review APIs the live code calls, copied from Google's documented signatures.
  - It catches typos, missing imports and wrong listener shapes. It does not catch real API drift: Android Studio's `-Plive=true` build is the real check.

## Code map (`app/src/main/java/com/pantheon3d/jellykeep/`)

| File | What it holds |
|---|---|
| `MainActivity` | Fullscreen, cutouts, back handling (OnBackInvokedCallback on 33+), lifecycle → `Game.resume()/pause()`. |
| `Game` (~2750 lines) | The battle (simulation, input, HUD, rendering) plus the shared ad and purchase flows. Implements `Ads.Host` and `Store.Host`. |
| `Meta` | Persistent profile and rules: gems, Royal Vault, Royal Patron, Royal Jelly, daily gift, quests, entitlements, settings, interstitial gating, v1.6 migration. Prefs keys start with `m.`; `m.ver=2`. No drawing. |
| `Ui` | Immediate-mode hit registry (what's drawn this frame registers its rect + id), press state, cached kit bitmaps, word wrap. |
| `Screen` / `Screens` | The modal stack: Title, Story, Reward, Defeat, Courier, Vault, Quests, Daily, Shop, Rebirth, Settings, Confirm, AdSim, Offer. Most of them pause the battle. |
| `Art` | Sprites with coded fallbacks, 9-slice UI kit, Lilita One text, optional painted icons (`ui_ic_*.webp`, `courier.webp`, `logo.webp`). |
| `Sfx` | Synth fallbacks + recorded banks `assets/sfx/<name>_<n>.ogg`. `Game.cue(bank, synth, vol, rate)` plays the bank if it exists, else the synth. |
| `Music` | Loops the first `assets/music/*.ogg`, if any. |
| `Notify` | Opt-in local reminders (inexact AlarmManager + receiver, 09:00–21:00): treasury full, or daily gift ready. |
| `Ads`, `Store` | Monetization seams. The default build gets `Ads.Sim` and `Store.Stub` from `src/stub/.../MonetizationFactory`; `src/live` has `AdMobAds`, `PlayStore` and its own `MonetizationFactory`. |

Input rules (they fix real bugs, so keep them):
- Battle buttons fire on touch-down; modal buttons fire on touch-up, with drag-scrolling.
- The gesture that opens a modal can't also tap inside it (`gestureOnModal`).
- An enemy under the finger beats the stump behind it.
- `Game.inAd` marks a pause caused by our own full-screen ad, so coming back from an ad doesn't count as "time away".

## Rules and numbers (2.0)

### Battle
- **Waves:**
  - 4, 6 and 8 enemies for waves 1–3, then min(6 + 1.3w, 42).
  - A **boss** comes every 10th wave. It summons 2 gloom every 5 s and enrages below 30% HP. First kill of each boss wave: 25 gems; repeat kills: 3.
  - **Elite** waves come at w % 10 == 5, from wave 15: Swarm, Stampede, Air Raid, Iron Wall, Giants.
  - **Sawtooth:** the two waves after each boss get HP ×0.85 and ×0.92.
- **HP:** `hpScale = 8·1.17^(w−1)·saw(w)·HP_RAMP^((w−40)²)` for w > 40.
  - The ramp is what keeps walls in the late game; see Balance.
- **Rewards:** `rewardScale = 2·1.12^(w−1)`.
- **Towers:**
  - Damage: `T_DMG·1.45^lv·2^(lv/10)`. A **star** every 10 levels doubles damage.
  - Upgrade cost: `T_COST·1.7^(lv+1)`.
  - Selling refunds 50% of everything spent.
  - Banker pays `rewardScale·1.2·BANK_GROW^lv` per payout.
  - Drummer buffs its two ring neighbours by `1.15 + 0.35·(1 − 0.88^lv)`, capped at +50%.
- **Tap (STRIKE):**
  - Damage: `3·1.5^lv × Vault × combo`, with cost `15·1.8^lv`.
  - Combo: ×(1 + 0.0125·min(combo, 40)), so at most ×1.5. Callouts come at combo milestones.
  - The king also bonks adjacent enemies for tap damage once a second, so pure idle play still progresses.
- **FORTIFY:** king HP `100·1.3^lv`, costing `25·1.7^lv`. It heals on purchase.
- **Streak:** +2% coins per consecutive cleared wave, capped at +30%. A defeat ends it; a revive keeps it.
- **Spells:**

  | Spell | Unlocks at | Effect | Cooldown | Ad option |
  |---|---|---|---|---|
  | Jelly Slam | wave 6 | 12× tap damage within 45u of the king | 40 s | one ad recharge per spell per wave |
  | Bubble Trap | wave 12 | everything frozen for 4 s and takes +25% damage | 60 s | same |
  | Gold Rush | wave 18 | coins ×2 for 15 s | 90 s | same |

- **Defeat:**
  - The screen shows near-miss copy and REVIVE (ad), which gives full HP, a 3 s shield and knockback, once per wave, then a 240 s cooldown.
  - Otherwise, Retreat goes back 3 waves, with a 10 s auto-retreat for idle players.
- **Chest:** every 5 cleared waves; worth 0.6 min of income, ×2 with an ad.
- **Courier:** from wave 5, every 6–9 min, a balloon crosses the field for 18 s.
  - With an ad: (4 + 0.5·Vault lv) minutes of income, or 20 gems (Gem Finder chance).
  - Free: 0.5 min of income.
- **Income/min** (what lump rewards are sized by): max(smoothed live rate, rewardScale·40·coinMult). Boosts don't feed the smoothed rate.

### Meta (survives Rebirth)
- **Royal Vault** (gems, Eatventure-style): 12 upgrades in 3 groups of 4 (Battle, Income, Ads & bonuses). Costs are `⌈base·growth^lv⌉`. The tables are at the top of `Meta`.
- **Royal Patron:** every rewarded ad, or one skipped with the Royal Pass, gives +1 star. Perks come at 1/3/5/8/12/16/20/25/30/40/50 stars, then +50 gems every 10.
- **Royal Jelly:**
  - Rebirth unlocks at run best 30.
  - Gain: `⌊5·1.08^(best−30)⌋`. Each Royal Jelly gives +5% coins and +5% damage (additive).
  - The panel spells out what resets and what stays.
- **Daily gift:**
  - A 7-day streak: 20 gems, then 15 min of coins, 30 gems, 30 min of coins, 50 gems, 60 min of coins + 10 min boost, 100 gems.
  - A missed day resets it to day 1. An ad doubles the claim.
- **Quests:** 3 per day plus a 25-gem bonus. They are seeded by the date and gated by unlocks.
- **Free gems:** 15 gems per ad, 5 times a day.
- **Gem Jar:** +2 gems per cleared wave, up to 600. It can be bought once it holds 150.
- **v1.6 players** get a one-off 50-gem thank-you, and the basics hints are already marked as seen.

### Ads and purchases
Every rewarded placement does something different: BOOST, REVIVE, OFFLINE ×2, CHEST ×2, COURIER, GEMS, SPELL recharge, DAILY ×2.
- They are opt-in, and the reward is printed on the button.
- A loading ad shows a shimmer skeleton; with no fill, the button says so and a tap retries.
- The single interstitial plays only after a Retreat, at least 5 min after any ad, never in the first 15 min of lifetime play, and never for Royal Pass owners.
- The IAPs and their effects are in `PUBLISHING.md`.
  - Royal Pass skips ads but still pays rewards and Patron stars.
  - Starter Pack is offered once, at session start, from wave 8.

## Balance (seeded bot sims in `PlayTest`, 60 simulated minutes each)
Three bots (`PlayTest.bot`) play the real game loop:
- They tap the nearest enemy, cast spells on crowds and bosses, spend coins greedily, and open chests and couriers.
- They retreat on defeat, and rebirth after stalling for 4 minutes past wave 30.
- The RNG is seeded, so a change shows up as a different curve rather than noise.

Run best wave over time (identical in AGP and in `tools/jvm-test`, run after run):
- **Tapper** (3 taps/s, no ads, no gems):
  - Waves 15 / 24 / 30 at 5 / 10 / 15 min, then a rebirth at 18.8 min (+5 RJ).
  - The second run is back at 30 by 35 min and reaches 41 at 45 min and 48 at 55 min, then rebirths at 56.9 min (+19 RJ).
  - 13 defeats.
- **Ads** (2 taps/s, boost always on, every ad, gems into the Vault):
  - Waves 17 / 28 / 39 / 45 / 51 / 57 at 5–30 min, then a rebirth at 35.1 min at wave 59 (+46 RJ).
  - The second run passes 59 at about 55 min and ends at 73.
  - 5 defeats + 4 revives.
- **Idle** (no taps): waves 6 / 9 / 10 / 10 / 14 / 16 at 5–30 min, best 20. It lives on the push/retreat loop, with 30 defeats.
- **Reproducibility:**
  - `bot()` seeds `Game.rnd`. Anything gated by the wall clock (sound rate limits, `meta.now()`) must never draw from `rnd`. Sound pitch uses `sndRnd` for that reason.
  - Before that fix, two runs of the same code gave different curves.
- **Why the HP ramp exists:**
  - Banker pay grows with both the wave and its own level. So income per wave grows about as fast as the cost of the upgrades needed to beat a flat 17%-per-wave HP curve.
  - Without the ramp, the ads bot's run after a rebirth climbed 7–12 waves every 5 minutes with no walls. Its next rebirth would have paid ~19× the first.
- **How the ramp was tuned:**
  - A 3-seed sweep tried `HP_RAMP` at 1.0006, 1.0010 and 1.0015, and `BANK_GROW` at 1.25, 1.22 and 1.20.
  - At 1.0015, the ads bot stalls at 63–74 regardless of when it rebirths.
  - Banker growth barely moved the curves, so it stays at 1.25.
  - The tapper and idle curves don't change, because the ramp only starts at wave 40.
- **Long-term shape:** past ~wave 80 each wave adds 30–40% HP, so each rebirth buys a few waves. A second prestige layer is the natural next step for players who get there.
- **Guards:** `balanceWithAds` asserts best ≤ 85, so a future buff can't quietly bring the runaway back. `balance` asserts 25 ≤ best ≤ 80.
- **Re-run after every economy change:** `./gradlew testReleaseUnitTest --tests '*PlayTest.balance*'`. The curves are in the test-results XML (`app/build/test-results/testReleaseUnitTest/`). Without the SDK, use `tools/jvm-test/run.sh --tests '*PlayTest.balance*'`; its curves are in `tools/jvm-test/build/test-results/test/`.

## Tests
- Run `./gradlew testReleaseUnitTest` wherever the SDK is installed, including the cloud sandbox now.
  - Robolectric downloads its Android runtime jar from Maven Central on the first run. A one-off "Failed to fetch maven artifact" is a network blip, so run it again.
  - `tools/jvm-test/run.sh` is the fallback when Google's hosts are blocked; it gives the same results.
- **`MetaTest`:** vault costs and effects, patron ladder, gift streak with a fake clock, quests, rebirth math, save/load, v1.6 migration.
- **`PlayTest`:**
  - `firstSession`, `menusAndScreens`, `battleSystems`, `auditFixes` and `adsAndPurchases` drive real touches and write every screen to `app/build/shots/`. Look at them.
  - `balance`, `balanceIdle` and `balanceWithAds` are the bots.
- **`AspectTest`:** 5 screen sizes, including a landscape tablet (the letterboxed portrait stage).
- **`BanksTest`:** every sound bank loads.

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
  - Run `tools/process_sfx.py`: it decodes to 44.1 kHz mono, trims lead silence, slices multi-hit takes, splices where needed, normalizes loudness and fades tails. Its per-sound rules were written for the first batch, so extend them for new names.
  - Encode to OGG Vorbis q4 as `assets/sfx/<name>_<n>.ogg`. `noCompress 'ogg'` is set.
  - Check that sounds start on the first frame, have no clicks or clipping, and that tails reach silence.
  - Jens is picky about artifacts.
- **Sound banks the code already calls** (each falls back to synth or silence when its bank is missing) are listed with prompts in `docs/art-and-audio-requests.md`.
- **Bubble SFX library:** Jens also has a "Bubble SFX" library (a skill in some sessions). Only its `02_Clean_Originals` are safe to ship. It currently supplies: strike, strike_hit, click, panel_open/close, deny and the 8 coin pings.

## Image prompt rules block (prepend to batches; attach the Pebble Popper image `assets/popper.webp` as the style reference)
```
Match the attached reference style exactly: hand-painted 2D mobile game art, soft painterly cel shading, thick slightly uneven dark brown outline, glossy highlights, warm top-left light. One subject per image, centered, square, solid flat pure green #00FF00 background, no shadow, no ground, no text. Friendly things use ONLY amber/orange/wood/gold/cream. Enemies use ONLY indigo/violet (deep navy to violet to lavender) with glowing pale lavender eyes, cute-spooky. Characters in 3/4 front view seen slightly from above.
```
- **Palette rule:** two hue families only. Amber = friend and UI; indigo = night and foe.
- **UI pieces** must be flat and front-on, with equal border thickness and a plain centre so they can be 9-sliced.

## TODO (most important first)
1. **Real phone, test build.** Check:
   - performance (Canvas, about 500 particles max)
   - touch feel, sound, haptics
   - the Android 13+ notification ask after the first welcome-back reward
   - predictive back
   - a tablet or foldable, for the letterboxed stage
2. **Go live:** follow `PUBLISHING.md`.
   - AdMob units and privacy messages, Play products, privacy policy URL (`privacy_url` in `res/values/res.xml`), Data safety, the `-Plive=true` build tested on a phone, then the closed test.
   - The live code compiles against the real SDKs but has never run on a device. First test: Google test ads load, a rewarded ad pays, the consent form shows in the EU, and a purchase sheet opens from an internal-testing install.
3. **Art and audio:** the two image batches and the sound batches in `docs/art-and-audio-requests.md`.
4. **Later (out of scope so far):** cloud save (Play Games), leaderboards, localization, events, analytics, new towers and enemies (they need art), a second prestige layer for players past ~wave 100.
5. **Dependencies:** the live build uses the newest SDKs as of 2026-10-24: GMA 25.5.0, UMP 4.0.0, Billing 9.1.0 and Review 2.0.2. Billing 9 already meets Play's current "Billing 8+" rule and the likely next one. Check Google Maven for newer versions before each release.
6. **Known limits:**
   - No server-side purchase verification.
   - Refunds don't revoke kept-forever items.
   - Reminders don't survive a reboot until the game is opened again.
   - Meta's clock is the wall clock, bounded by caps.

## Older sibling project
"Squish" (`com.pantheon3d.squish`) was the first prototype, a one-tap blob clicker, and is superseded by Jelly Keep. It has its own key (`squish-upload.jks`), kept separately. Don't mix the two keys up.
