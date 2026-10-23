# Jelly Keep 2.0 — design spec

- **Date:** 2026-10-23
- **Status:** written by Claude from Jens's brief. Jens asked Claude to fill in the gaps, so work goes ahead on this spec; redirect at any time.
- **Baseline:** v1.6 (commit `aa374a0`, versionCode 8).

## 1. Intent

What Jens asked for:
- Treat v1.6 as a sparse baseline in which everything is suspect: audit it, fix it and expand it.
- Make the game genuinely fun and playable.
- Ads and monetization should each **do something different**. Watching ads should also **earn permanent upgrades**, like the gem upgrades in *Eatventure*.
- Fix the cold start. Players are currently dropped straight into a battle with no front end.
- Borrow proven retention tactics from *Block Blast* and *Candy Crush*.
- Research the target audience and build for it.
- Fill in whatever is missing.

Success means:
1. A new player understands the game within the first minute and wins wave 1 without reading anything.
2. Every session has a small goal it can finish, plus a reason to come back: a timer, a streak, a quest or a gift.
3. Every rewarded ad is opt-in, states its reward, and also moves a permanent meter.
4. Each IAP has one clear, distinct benefit.
5. The bot balance run shows steady progress with walls you can beat, not 24 defeats an hour.
6. Every new screen is verified headlessly in screenshots, using the existing art kit, with coded art only where art is still missing.

## 2. Audience

Research summary (sources in §12):
- Idle/incremental players skew **core, not casual**. In Quantic Foundry's sample, about 70% called themselves core gamers. They are driven most by **Completion** and **Power** (numbers going up, collections, milestones) and least by Excitement or Fantasy.
- About half of adult mobile gamers are 35+ and about half are women (US, 2026). The cute painted style and low-stress play also draw this casual puzzle crowd (Candy Crush's core is women 35+).

**Primary audience:** adults 25–44 who play on the go, in short bursts of 2–10 minutes several times a day: commute, breaks, before bed. They enjoy watching a keep get stronger, and like a light layer of strategy they can push on when they want to.

**Secondary audience:** casual puzzle players 35+ who come for the cute art and stay for daily habits and gentle goals.

What this means for the design:
- **Short sessions.** Every system pays off within 5 minutes: chests every 5 waves, a courier every 4–7 minutes, quests that finish in a session.
- **Progress while away.** Offline earnings, a meaningful return reward, and reminders the player can switch off.
- **Visible, permanent power.** Gems feed the Vault, ads feed the Patron ladder, and Rebirth earns Royal Jelly.
- **Low punishment.** Defeat costs a few waves and never coins. You can always revive or retreat, and play never blocks on a modal.
- **One thumb, portrait.** Primary actions sit in the bottom third. Text is short, with no reading walls.
- **Not for children.** Set Play target audience to 16+ or 18+. That keeps it outside the Families policy, so standard AdMob applies. The cute art is not a "designed for kids" signal on its own, but the store listing must not market to children.

## 3. What we borrow, and why

| Source | Mechanic | Jelly Keep version |
|---|---|---|
| Eatventure | **Vault:** gems buy permanent upgrades that survive every new city. These include ad boost multiplier (Remote, up to ×6), ad boost duration (TV, up to ×8), offline cap and value, tip chance and value, and investor offers. | **Royal Vault**: 12 gem-bought upgrades in 3 groups (§6.4) that survive Rebirth. |
| Eatventure | **Investor:** walks in and offers a lump of cash or sometimes gems for an ad. | **Royal Courier**: a balloon that drifts across every 4–7 min (§6.8). |
| Eatventure | "2× profit forever" purchase | **Golden Spoon** IAP |
| The Tower | Ad gems every ~10 min. Ad coin bonus banks up to 2 h. 5 daily missions feed a weekly reward. A paid "no ads but keep bonuses" option. | Courier gems, a banked boost (bank holds 6× the duration of one ad), daily quests, and a **Royal Pass** that skips ads but keeps every reward. |
| Block Blast | Zero-friction start, juicy **combos** with escalating callouts, a high-score chase, and a rewarded revive at the natural failure point. Interstitials only at breaks. | One-tap title screen, **tap combos** with callouts and rising pitch, a **NEW BEST!** moment, revive on defeat, and one interstitial only after a retreat. |
| Candy Crush, Royal Match | **Win streaks** you lose on failure (Royal Match's streak drove +38% revenue MoM). Near-miss framing. Sawtooth difficulty: hard levels, then breathers. Daily booster gift. Boosters. | Wave **streak** coin bonus (a revive keeps it), a **"So close!"** defeat screen, boss and elite spikes followed by eased waves, a **7-day gift** streak, and three **King's spells**. |
| Idle staples | Milestone multipliers (×2 at milestone levels), prestige | **Tower stars** (×2 every 10 levels) and **Rebirth** for **Royal Jelly** |

What we deliberately skip:
- **Energy or lives.** They are hostile to idle play.
- **Paid random boxes.** They need odds disclosure and draw review risk.
- **Review gating.** Play policy forbids it. We use the official in-app review API, once, at a peak moment.

## 4. Audit of v1.6

### 4.1 Bugs, all verified

1. **Drummer buffs nothing.** Its range is 32u, but the nearest neighbouring stumps are 35–39u away on every tested screen. Measured `tBuff = 1.00` on 1080×2340, 1080×1920, 720×1600 and 1440×3200. → Make it buff the two ring-adjacent stumps, and draw link lines to them.
2. **Battle runs behind reward overlays.** Only `updateWave` checks `overlay`, so enemies keep biting during a chest or welcome-back reward. A defeat then replaces the overlay and **loses the reward**. → Pause the simulation under every modal.
3. **Taps on enemies near a stump open the stump panel**, because stumps win the hit test. → An enemy under the finger wins.
4. **Defeat blocks forever.** Idle and away-from-keyboard play stops at the first loss. → Auto-retreat after a 10 s countdown.
5. **Wave and new-enemy banners cover the king and the fight** for 2+ s mid-wave. → Move banners into the top band and make them compact.
6. **Offline earnings are lost.** If the app is backgrounded on the defeat screen, `resume()` skips the reward and overwrites `lastSaveMs`.
7. **An unopened chest is not saved** and vanishes if the process dies.
8. **Ad boost inflates the offline rate.** Boosted kills feed `coinRate`, the smoothed income rate that offline earnings use.
9. **Per-frame allocations cause jank.**
   - A new `RadialGradient` is built every frame for the warm light and lantern glow.
   - `saveLayerAlpha` runs per floating text, up to 36 offscreen layers per frame.
   - `new Path()` runs per frame in fallback enemy drawing.
10. **`ground.webp` is decoded even when `ground_stumps.webp` exists**, wasting about 8 MB.
11. **`rewardedReady()` is never checked.** With a real SDK, a no-fill ad leaves buttons that do nothing. → Show ad state: ready, loading as a skeleton shimmer, or unavailable with retry.
12. **Back exits the app even with a panel open.**
13. **Large screens and landscape are unplayable.** Android 16 ignores the portrait lock on screens 600dp and wider at targetSdk 36. The cover-scaled background then crops the stumps off-screen. → Letterbox a portrait stage.
14. **`Ads.Stub` grants rewards silently.** A release built without real ads quietly gives free rewards, and testers can't see the ad flow. → A visible simulated ad with a 2 s countdown.
15. **Fortify fades out.** Cost grows ×1.85 per level against HP ×1.3, and HP refills every wave. → Re-tune it and let it heal on purchase.

### 4.2 Design problems

- **No front end.** Players land mid-battle.
- **The only goal is the next wave.** Nothing carries over between failures except coins.
- **The bot run is dominated by pushing and retreating.** It loses 24 times in 60 minutes, and the wave 30 boss walls it for 13 minutes.
- **Waves have no identity.** Each one is a random mix, and bosses are just big HP.
- **Tapping has no skill expression and no escalation.** Upgrades are linear numbers with no milestones.
- **The bottom panel spends about 20% of the screen on a static tip line and two buttons.**
- **Indigo enemies on indigo ground read poorly** at the screen edges.
- **Only one setting.** No music, haptics or motion toggles, no privacy entry and no notifications.

## 5. Player journey

### 5.1 First session, minutes 0–10

0. **Title:** Jelly King idling, logo, a big PLAY. On first launch only, PLAY shows one story card: "Night falls. The Gloom wants the Jelly King's crown!" A tap moves on.
1. **Wave 1:** 4 slow gloom. A pulsing ring and "Tap to squish!" sit on the first one. The wave is won by tapping alone.
2. **After wave 1:** coins ≥ 30. A pointing hand bobs over the bottom stump with "Build a tower!".
   - The countdown **waits** until the first tower is built.
   - The Popper card glows in the build menu.
3. **Waves 2–4:**
   - The STRIKE upgrade hint appears once it's affordable.
   - The ×2 COINS boost button appears with a one-line hint.
   - Sparkler (wave 3) and Mage (wave 4) unlock with a NEW TOWER ribbon.
   - Quests unlock at wave 4.
4. **Wave 5:** the first chest appears, along with the first courier.
5. **Wave 6:** the first spell, Jelly Slam, unlocks with a hint on the button.
6. **Wave 10:** BOSS. A name and HP bar sit at the top, with the hint "Use Jelly Slam!".
   - Defeating the boss gives a big celebration and **+25 gems**.
   - The gems introduce the Vault: its button lights up with a NEW badge and a hint.

### 5.2 First week

- **Every day:**
  - A 7-day gift, where the streak resets if a day is missed.
  - 3 daily quests, with a bonus for finishing all 3.
  - The free-gems ad (5 per day).
  - Courier visits.
  - A notification when offline storage is full or the gift is ready (opt-in).
- **Medium-term goals:**
  - Vault levels.
  - The next Patron perk.
  - The next tower star.
  - The next boss.
  - Rebirth, which unlocks at best wave 30.

## 6. Systems

### 6.1 Front end, flow and settings

- **Screens:**
  - **Title** shows the logo, PLAY and settings.
  - **Battle** is the main screen.
  - **Modal panels** slide up and **pause the battle**: Vault, Quests (with the Daily gift), Shop, Rebirth, Settings, Courier, Reward, Defeat, Confirm and Ad.
- **Back:** closes the top modal; if no modal is open, it goes from Battle to Title, and from Title it exits.
- **HUD:**
  - Top left: coins pill, with the gems pill under it.
  - Top centre: WAVE plank, progress bar or boss bar, and the streak flame.
  - Top right: settings, with the ×2 COINS boost under it.
- **Bottom panel:**
  - Row 1: STRIKE and FORTIFY.
  - Row 2: nav buttons for Quests, Vault, Shop and Rebirth.
  - Each nav button has a badge and a lock state. Each appears when unlocked (progressive disclosure).
  - Spells sit as round buttons on the panel's top edge, on the left.
- **Settings:** sound, music, haptics, screen shake, notifications, privacy choices (only when consent requires it), restore purchases, privacy policy, credits/version.
- **Large screens:** the game renders into a centred portrait stage (aspect at most 0.62, width over height). The side bars show a dimmed, blurred copy of the field.

### 6.2 Battle

- **Tap combos:**
  - A hit within 1.0 s of the last hit adds 1 to the combo; a miss or a timeout resets it.
  - Tap damage is multiplied by 1 + 0.025 × min(combo, 40), up to ×2.
  - Callouts appear at 10 NICE!, 20 GREAT!, 30 AMAZING!, 40 JELLY-TASTIC! and 60+ UNSTOPPABLE!.
  - The combo counter pops with a scale bump, and coin sounds rise in pitch.
- **Multi-kills:** 2 or more kills from one tap or blast show DOUBLE! / TRIPLE! / MULTI ×N!.
- **King's spells:** tap a button to cast; each has a cooldown with a radial sweep.

  | Spell | Unlocks at | Effect | Cooldown |
  |---|---|---|---|
  | Jelly Slam | wave 6 | 25× tap damage to everything within 45u of the king, with knockback, a shockwave and shake | 40 s |
  | Bubble Trap | wave 12 | Every enemy frozen for 4 s and takes +25% damage | 60 s |
  | Gold Rush | wave 18 | Coins ×3 for 15 s, with a coin rain | 90 s |

  - When a spell is on cooldown, tapping it offers an instant recharge for an ad, once per spell per wave.
- **Wave identity:**
  - Waves 1–3 have 4, 6 and 8 enemies.
  - **Boss** every 10th wave:
    - A named boss with an HP bar at the top.
    - It summons 2 gloom every 5 s.
    - It enrages below 30% HP: +40% speed.
    - First kill of each boss wave: +25 gems; later kills: +3.
  - **Elite** waves (wave mod 10 = 5, from wave 15) rotate through five modifiers:

    | Modifier | Effect |
    |---|---|
    | Swarm | ×2 count, ×0.5 HP |
    | Stampede | skitters, +30% speed |
    | Air Raid | flappers |
    | Iron Wall | shieldlings |
    | Giants | brutes, ×0.6 count |

  - **Sawtooth:** waves ending in 1 or 2 get HP ×0.85 and ×0.92, as breathers.
- **Wave streak:**
  - +3% coins per consecutive cleared wave, capped at +45%.
  - The flame number by the wave plank flares on each clear.
  - A defeat ends it; a revive keeps it.
- **Tower stars:**
  - Every 10 levels, damage ×2 at that level and a star on the badge.
  - The panel shows "★ in 3 lv → ×2 damage".
  - Base growth is ×1.47 per level. Before: ×1.55 per level with no milestones.
- **Drummer:** buffs its 2 ring neighbours, with link lines drawn.
- **Hold to upgrade:** holding UPGRADE, STRIKE or FORTIFY repeats purchases, accelerating.
- **Defeat:**
  - Near-miss copy: "So close! Boss at 12%" or "3 gloom left".
  - Two buttons: [▶ REVIVE · keep 🔥N], once per wave, and [Retreat to wave W-3], with a 10 s auto-retreat ring.
- **Readability:** a soft lavender rim glow under enemies, and banners kept out of the centre.

### 6.3 Currencies

| Currency | Earned from | Spent on | Survives Rebirth? |
|---|---|---|---|
| **Coins** | kills, bankers, waves, chests, courier | towers, Strike, Fortify | no |
| **Gems** (amber gems) | boss first kills, daily gift, quests, courier (Gem Finder), free-gems ad, Patron tiers, IAP | Vault, gem crates (gems → coins), Gem Jar | yes |
| **Royal Jelly** | Rebirth | passive +5% coins & damage each | yes |

**Income/min** (used for sizing rewards) = max(smoothed live rate, rewardScale(wave) × 40).

### 6.4 Royal Vault (gem upgrades, permanent)

Cost = ⌈base × growth^level⌉.

| Group | Item | Per level | Max | Base, growth |
|---|---|---|---|---|
| Battle | Sharp Taps | tap damage +15% | 30 | 15, 1.22 |
| Battle | Swift Towers | tower attack speed +3% | 25 | 20, 1.25 |
| Battle | Thick Jelly | king HP +10% | 30 | 15, 1.20 |
| Battle | Spell Mastery | spell cooldown −3% | 20 | 25, 1.25 |
| Income | Lucky Drops | 2% chance per kill of a coin pouch | 15 (30%) | 15, 1.25 |
| Income | Fat Pouches | pouch ×5 +1 | 20 (×25) | 20, 1.20 |
| Income | Long Nap | offline cap 4 h +1 h | 20 (24 h) | 20, 1.22 |
| Income | Night Shift | offline value 50% +10% | 15 (200%) | 20, 1.25 |
| Ads & bonuses | Boost Power | ad boost ×2 +0.1 | 20 (×4) | 25, 1.25 |
| Ads & bonuses | Boost Time | per ad 5 min +1 min | 15 (20 min) | 20, 1.25 |
| Ads & bonuses | Courier's Purse | courier gift 10 min of income +2 min | 20 | 15, 1.22 |
| Ads & bonuses | Gem Finder | courier brings gems 10% +3% | 14 (52%) | 30, 1.30 |

The 3 groups of 4 keep each chunk small (Miller's law).

### 6.5 Royal Patron: ads → permanent perks

Every rewarded ad watched, or skipped with the Royal Pass, adds **+1 Patron star**. Milestones unlock permanent perks:

| Stars | Perk |
|---|---|
| 1 | +10% coins |
| 3 | +30 gems |
| 5 | +10% tower damage |
| 8 | boost bank +30 min |
| 12 | +60 gems |
| 16 | +15% offline earnings |
| 20 | +15% coins |
| 25 | +100 gems |
| 30 | +15% tower damage |
| 40 | +150 gems |
| 50 | Golden Crown: +25% coins & damage |
| every 10 after | +50 gems |

The ladder is always visible in the Vault, e.g. "7 ★ → next at 8". The first rungs come fast to create goal-gradient pull.

### 6.6 Rewarded ads: every placement does something different

| Placement | Where | Reward | Cap |
|---|---|---|---|
| BOOST | HUD ×2 button | coins ×(2 + 0.1·Power) for (5 + Time) min, banking up to 6× | bank cap |
| REVIVE | defeat | full HP, 3 s shield, streak kept | 1 per wave |
| OFFLINE | welcome back | ×2 the offline earnings | per return |
| CHEST | treasure chest | ×3 the chest | per chest |
| COURIER | balloon | 10+ min of income, or gems (Gem Finder) | per courier |
| GEMS | Shop / Vault | +15 gems | 5 per day |
| SPELL | spell on cooldown | instant recharge | 1 per spell per wave |
| DAILY | daily gift | ×2 | 1 per day |

**Rules for every placement:**
- Opt-in only, with the exact reward printed on the button.
- +1 Patron star each.
- The button shows skeleton shimmer while loading, and "Ad unavailable · retry" on no fill.

**One interstitial:**
- Shown only after choosing Retreat.
- At least 5 min apart, and only after 15 min of lifetime play.
- Never for Royal Pass owners.
- Its gap is a single constant, so it can be tuned or switched off.

### 6.7 In-app purchases

| SKU | Name | Type | Price | Effect |
|---|---|---|---|---|
| royal_pass | Royal Pass | permanent | $4.99 | no interstitials; rewarded ads skipped but rewarded (+1 ★ each); +100 gems |
| starter_pack | Starter Pack | permanent, offered from wave 8 | $1.99 | 300 gems + permanent +50% coins |
| golden_spoon | Golden Spoon | permanent | $3.99 | permanent ×2 coins |
| gems_s / gems_m / gems_l | Gem packs | consumable | $0.99 / $4.99 / $9.99 | 80 / 500 / 1200 gems |
| gem_jar | Gem Jar | consumable | $2.99 | fills 2 gems per cleared wave up to 600; break it to collect (buyable from 150) |

Gem crates (gems → coins): 40 gems → 15 min of income, 120 gems → 60 min.

### 6.8 Retention systems

- **Daily gift (7-day streak):**

  | Day | Reward |
  |---|---|
  | 1 | 20 gems |
  | 2 | 15 min of coins |
  | 3 | 30 gems |
  | 4 | 30 min of coins |
  | 5 | 50 gems |
  | 6 | 60 min of coins + 10 min boost |
  | 7 | 100 gems |

  - It cycles after day 7.
  - A missed day resets the streak to day 1.
  - An ad doubles the claim.
  - It shows automatically once per day from the second session on, and is unlocked from wave 5 in the first session.
- **Daily quests:**
  - 3 per day, seeded by the local date and reset at local midnight.
  - The pool is gated by what the player has unlocked:
    - squish N by tap
    - defeat N enemies
    - clear N waves
    - defeat a boss
    - reach an ×N combo
    - cast N spells
    - upgrade N times
    - open N chests
    - collect a courier gift
    - reach an N streak
  - Each quest pays 10–15 gems, and finishing all three pays a 25-gem bonus.
- **Royal Courier:**
  - From wave 5, every 4–7 min, a balloon with the Piggy Banker drifts over the field for about 18 s.
  - Tapping it offers ▶ a big gift, or a free small gift (1 min of income).
- **Milestones:**
  - First clear of each 10th wave gives a big fanfare and gems.
  - Beating your best wave shows a **NEW BEST!** ribbon (peak).
  - Rebirth itself is a memorable end-of-run moment (peak-end rule).
- **Notifications** (opt-in, local, at most one per day):
  - "Your treasury is full" when the offline cap is reached.
  - "Your daily gift is ready" the next day, between 09:00 and 21:00.
  - Asked in context on Android 13+ after the first welcome-back reward.
- **In-app review:** the official Play API, once, after the first boss kill, live builds only.

### 6.9 Rebirth

- **Unlocks** at best wave 30. The button shows a locked teaser before that.
- **Royal Jelly gained** = ⌊5 × 1.12^(run best − 30)⌋. That gives:

  | Run best | Royal Jelly |
  |---|---|
  | 30 | 5 |
  | 40 | 15 |
  | 50 | 48 |
  | 60 | 149 |

- **Bonus:** each Royal Jelly adds +5% coins and damage.
- **Resets:** wave, coins, towers, Strike and Fortify, streak.
- **Keeps:** gems, Vault, Patron, Royal Jelly, best wave (so tower and spell unlocks stay), quests, gift streak, purchases.
- **The panel spells out exactly** what resets and what stays, and confirms first.

### 6.10 In-context onboarding

Each hint shows once and is stored in a bitmask:
1. tap to squish
2. build a tower (the countdown waits)
3. upgrade STRIKE
4. ×2 boost
5. chest
6. tower upgrade
7. spell
8. boss incoming
9. gems → Vault
10. quests
11. courier
12. Rebirth

Hints are short, point at their target (a bobbing hand and a ring), and never block play except hint 2.

## 7. Architecture

The code stays pure Java with a single Canvas view and no engine.

New systems live in focused classes so `Game.java` stays the battle:

| Unit | Responsibility |
|---|---|
| `Game` (View) | loop, battle sim and render, HUD, input routing, screen/modal stack |
| `Meta` | persistent profile and rules: gems, vault, patron, royal jelly, daily gift, quests, entitlements, settings, save/load, v1.6 migration. No drawing. |
| `Ui` | immediate-mode hit registry (rects → action ids), drag scrolling, shared widgets (buttons, pills, bars, wrapped text) on top of `Art` |
| `Screens` | title and modal panels: draw + tap handling, built on `Ui` |
| `Ads`, `Store` | monetization seams. `Ads.Sim` shows a visible fake ad with a countdown; `Store.Stub` confirms in-game. |
| `Monetization` | picks the implementations. It loads `live.LiveMonetization` by reflection when built with `-Plive=true`, otherwise the stubs. |
| `Notify` | local reminders: AlarmManager + receiver, framework APIs only |
| `Music` | loops `assets/music/*.ogg` when present, silent otherwise |

`src/live/java` (opt-in) holds AdMob rewarded and interstitial ads with UMP consent, Play Billing, and Play in-app review. The default build keeps **zero third-party runtime dependencies**, as it has today.

**Time:**
- `Meta.now()` = wall clock + a test offset. Daily logic uses the local date.
- Clock tampering is bounded: offline earnings are capped, and the gift needs a strictly newer date.

**Save format:**
- SharedPreferences `"jellykeep"` keeps the v1.6 keys.
- New keys are added, with `ver=2`.
- v1.6 players get a 50-gem "thanks for testing" gift.

## 8. Monetization integration

**Default build: `./gradlew assembleRelease`**
- Uses the stubs.
- The simulated ad is clearly labelled.
- The store says "Test purchase".
- Safe for closed testing.

**Live build: `./gradlew -Plive=true bundleRelease`**
1. Adds `play-services-ads`, `user-messaging-platform`, `billing` and `review`.
2. Puts the AdMob App ID in the manifest through a placeholder.
3. Reads ad unit IDs from `gradle.properties`. They default to Google's **test** IDs.

Jens must:
1. Create the AdMob app and its units.
2. Create the Play products using the SKUs in §6.7.
3. Update the Data safety form (Advertising ID, purchase history, app interactions), the content rating, "Contains ads" and the privacy policy.

That code can't be compiled in the cloud sandbox: `dl.google.com` is blocked there. It is written against the documented APIs and is isolated, so a mistake there never breaks the default build.

## 9. Art and audio asks (for Jens)

Prompts are in `docs/art-and-audio-requests.md`, written as one batch with the shared rules block. Until the assets arrive, the game uses coded fallbacks drawn from the kit's shapes.
- **Icons:** amber gem, Royal Jelly drop, vault, quests scroll, shop bag, rebirth, gear, music, haptics, bell, flame, the three spell icons, check, hand pointer.
- **Title logo.**
- **Courier balloon.**
- **Coin pouch.**
- **Sounds:** combo ticks, the three spells, courier, gift, NEW BEST, a rebirth whoosh, a music loop and night ambience.

## 10. Testing

`tools/jvm-test/run.sh` runs the same `app/src/test` suite headlessly (Robolectric, native graphics) without the Android SDK. AGP's `testReleaseUnitTest` keeps working too.

| Test | Covers |
|---|---|
| `MetaTest` | vault costs and effects; patron ladder; daily streak with a fake clock (claim, double claim, miss a day, cycle); quest seeding, progress and reset; rebirth math; save/load round-trip; v1.6 migration |
| `MonetizationTest` | simulated ad lifecycle (loading → showing → reward → +1 ★), no-fill path, Royal Pass skip, stub purchases granting entitlements, consumables |
| `PlayTest` | title → play → hints; combos; spells; drummer buff; the overlay pause fix; the defeat auto-retreat; and screenshots of every screen and modal |
| `PlayTest.balance` | a bot that taps, buys, casts spells and rebirths at the right time. It prints the curve. Targets:<br>• wave 10 at about 5 min<br>• wave 20 at about 15 min<br>• rebirth unlock at about 35–45 min<br>• no more than 10 defeats per hour outside boss walls |
| `AspectTest` | adds a landscape tablet size to check the letterbox |

## 11. Out of scope for this pass

- Cloud save (Play Games).
- Leaderboards.
- Localization.
- Live-ops events.
- Season pass.
- Analytics: there is a seam, but no SDK.
- New tower or enemy types beyond wave modifiers and boss behaviour (they need art).

## 12. Sources

- **Eatventure vault:**
  - [TalkAndroid vault guide](https://www.talkandroid.com/23261-guide-to-eatventure-vault-items-2023/)
  - [Fandom vault](https://eatventure.fandom.com/wiki/Vault)
  - [SimpleGameGuide](https://simplegameguide.com/eatventure-vault-items/)
  - [Dexerto](https://www.dexerto.com/gaming/eatventure-codes-free-cash-gems-2616635/)
- **The Tower:**
  - [Fandom currency](https://the-tower-idle-tower-defense.fandom.com/wiki/Currency)
  - [Tower Hub gems](https://www.tower-hub.com/wiki/currency/currency-gems)
- **Block Blast:**
  - [Udonis stats](https://www.blog.udonis.co/statistics/block-blast)
  - [Balancy deconstruction](https://balancy.co/blog/2025/03/26/how-could-block-blast-by-hungry-studio-earn-more-monetization-and-gameplay-deconstruction/)
  - [Applixir playbook](https://www.applixir.com/blog/the-block-blast-playbook-what-127m-in-ad-revenue-actually-bought/)
- **Win streaks and near-misses:**
  - [Liquid & Grit](https://www.liquidandgrit.com/march-2024-casual-report/)
  - [This Week in LiveOps](https://thisweekinliveops.substack.com/p/how-royal-match-uses-almost-wins)
  - [Waterloo near-miss thesis](https://uwspace.uwaterloo.ca/handle/10012/12522)
  - [PocketGamer.biz on boosters](https://www.pocketgamer.biz/ux-in-candy-crush-and-the-deceptively-complex-world-of-boosters/)
- **Audience:**
  - [Quantic Foundry idle clickers](https://quanticfoundry.com/2016/07/06/idle-clickers/)
  - [Prado 2026 mobile audience](https://prado.co/post/what-the-mobile-gaming-audience-actually-looks-like-in-2026)
- **Ads policy:**
  - [AdMob Families](https://support.google.com/admob/answer/6223431)
  - [Play Families ads SDK policy](https://support.google.com/googleplay/android-developer/answer/12918983)
  - [Applixir idle placements](https://www.applixir.com/blog/the-ultimate-guide-to-game-monetization-models-and-metrics/)
