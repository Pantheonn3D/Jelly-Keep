# Art and audio requests (Jelly Keep 2.0)

Every file here is **optional**. The game already ships with a coded stand-in for each one. Drop a file into `app/src/main/assets/` under its exact name and the game uses it on the next build, with no code changes.

Order of value: Batch 1 (icons) and Sound batch A make the biggest difference.

## How to deliver

- **Images:** JPEG or PNG on the green screen, at least 1024 px. Claude keys them with `tools/key_green_adaptive.py`, using `cut(path, maxSide, out.webp)` with the max side listed for each item. Then Claude checks each one on the indigo background.
- **Sounds:** ElevenLabs zips with 3–4 takes per sound, named after the bank. `tools/process_sfx.py` trims, normalizes and fades them. They become `assets/sfx/<name>_<n>.ogg`, and the game picks a random take each time, never the same one twice in a row.
- **Pressed and "off" states are drawn in code.** Image models can't draw pressed buttons, and settings toggles get a slash in code. So every icon below is needed in its normal "on" look only.

---

## Image batch 1: icons (one message, 15 items)

Attach `assets/popper.webp` and `assets/ui_ic_strike.webp` as the style references, then paste:

```
Match the attached reference style exactly: hand-painted 2D mobile game art, soft painterly cel shading, thick slightly uneven dark brown outline, glossy highlights, warm top-left light. One subject per image, centered, square, solid flat pure green #00FF00 background, no shadow, no ground, no text. Friendly things use ONLY amber/orange/wood/gold/cream. Enemies use ONLY indigo/violet (deep navy to violet to lavender) with glowing pale lavender eyes, cute-spooky. Characters in 3/4 front view seen slightly from above.

These are UI icons: one bold object, seen straight from the front, filling about 80% of the square, chunky shapes that still read at 48 pixels, no thin details, nothing transparent or see-through.

1. An amber gemstone: a chunky cut jewel in warm amber and gold, a bright white glint on the top-left facet.
2. A drop of royal jelly: a glossy golden-honey droplet with a tiny gold crown sitting on its tip.
3. A rolled cream parchment scroll tied with an orange ribbon and a deep-orange wax seal.
4. A small cream-and-amber shopping bag with rope handles and a gold coin emblem on the front.
5. A golden crown inside one curved swirl arrow that loops around it, like a "start again" symbol.
6. A settings cog wheel in polished brass with a round wooden centre.
7. A lively flame, deep orange outside and yellow-cream at the core.
8. A round glossy bubble painted as a solid pale cream-amber ball with big white highlights (not transparent).
9. A bold checkmark in gold.
10. Two joined music notes in gold.
11. A rounded cream phone shape in a wooden frame, with two curved vibration lines on each side.
12. A brass hand bell with a wooden handle.
13. A leather coin pouch tied with a gold cord, three gold coins peeking out of the top.
14. A small wooden picture frame tilted sideways, with short motion lines on both sides.
15. A wrapped present: cream box, amber ribbon and a big bow.
```

| # | File | Used for | Cut max side |
|---|---|---|---|
| 1 | `ui_ic_gem.webp` | gems everywhere (HUD, Vault, Shop, rewards) | 200 |
| 2 | `ui_ic_jelly.webp` | Royal Jelly (Rebirth) | 200 |
| 3 | `ui_ic_quests.webp` | Quests nav button | 200 |
| 4 | `ui_ic_shop.webp` | Shop nav button | 200 |
| 5 | `ui_ic_rebirth.webp` | Rebirth nav button | 200 |
| 6 | `ui_ic_gear.webp` | settings button | 200 |
| 7 | `ui_ic_flame.webp` | wave streak flame | 200 |
| 8 | `ui_ic_bubble.webp` | Bubble Trap spell | 200 |
| 9 | `ui_ic_check.webp` | finished quests, owned items | 160 |
| 10 | `ui_ic_music.webp` | Settings: music | 160 |
| 11 | `ui_ic_haptics.webp` | Settings: vibration | 160 |
| 12 | `ui_ic_bell.webp` | Settings: reminders | 160 |
| 13 | `ui_ic_pouch.webp` | lucky coin pouch drop, Vault "Lucky Drops" | 200 |
| 14 | `ui_ic_shake.webp` | Settings: screen shake | 160 |
| 15 | `ui_ic_gift.webp` | daily gift | 200 |

## Image batch 2: courier and logo (one message, 2 items)

Attach `assets/popper.webp`, plus `assets/t_banker.webp` (the same piggy has to appear in item 1), then paste the same rules block followed by:

```
1. The attached Piggy Banker character riding in a little wicker basket under a small hot-air balloon with amber-and-cream stripes, a gift sack tied to the side of the basket, the piggy waving happily. The whole balloon fits in the frame. Not square: taller than wide.
2. EXCEPTION to "no text": a game logo with the words JELLY KEEP in two lines (JELLY above KEEP), chunky glossy amber-gold jelly letters with a thick dark brown outline and soft highlights, a tiny jelly crown on the J. It will sit over a dark indigo night sky, so it must read on dark. Wide, centered.
```

| # | File | Used for | Cut max side |
|---|---|---|---|
| 1 | `courier.webp` | the Royal Courier balloon and its gift panel | 420 |
| 2 | `logo.webp` | title screen (the coded JELLY/KEEP letters are the stand-in) | 1000 |

Check the logo for spelling before keying. Image models sometimes garble letters.

---

## Sounds (ElevenLabs Sound Effects)

Start every prompt with this shared line, then add the item's own text:

```
Cute, chunky, warm cartoon sound for a cozy mobile tower-defense game with jelly blob monsters. Clean and dry, no music, no voice, no reverb tail, starts instantly.
```

Generate 4 takes of each, at the length listed. In the processed takes, the sound starts on the first frame, has no clicks, and its tail fades fully to silence.

### Batch A: the big moments (18)

| Bank | Length | Prompt |
|---|---|---|
| `enemy_die` | 0.4 s | A small squishy jelly blob popping with a wet plop. (Plays constantly, so it must be short and never tiring.) |
| `king_hurt` | 0.5 s | A rubbery boing-squish, like a jelly king being bumped. |
| `wave_start` | 1.2 s | A short bright wooden horn toot announcing a new wave, cheerful and quick. |
| `wave_clear` | 1.5 s | A short triumphant marimba and bells flourish, rising, a happy victory sting. |
| `new_best` | 2 s | A bigger celebratory fanfare: brass toot, bells and a sparkle shimmer, a record-breaking moment. |
| `defeat` | 2 s | A soft sad descending cartoon trombone wah-wah, gentle, not harsh. |
| `revive` | 1.5 s | A magical rising shimmer whoosh ending on a warm chime, coming back to life. |
| `boss_appear` | 2 s | A deep ominous cartoon gong with a low rumbling growl, spooky but cute. |
| `boss_die` | 2 s | A big gooey explosion: one deep splat, then a cascade of small pops and sparkles. |
| `combo` | 0.6 s | A bright rising arpeggio pling, a combo counter going up. (The game raises its pitch at each combo milestone.) |
| `spell_slam` | 1.2 s | A heavy jelly body-slam: a deep wobbly thump and a shockwave whoomp. |
| `spell_freeze` | 1.5 s | Lots of bubbles blowing up and wrapping around, glassy shimmer, ending in a soft frozen tink. |
| `spell_rush` | 1.5 s | A shower of gold coins jingling with a magical sparkle swoosh. |
| `chest_open` | 1.2 s | A wooden treasure chest creaking open with a bright magical sparkle. |
| `coin_shower` | 1.2 s | A cascade of many small gold coins pouring and jingling, very satisfying. |
| `gem_shower` | 1.2 s | Crystal gems tinkling down, bright glassy chimes. |
| `rebirth` | 2.5 s | A huge magical whoosh that sucks inward, then bursts out into a shimmering pad and bells. |
| `courier` | 1.5 s | A little hot-air balloon puffing, a cute piggy snort and a jingle bell. |

### Batch B: tower voices (7)

| Bank | Length | Prompt |
|---|---|---|
| `sniper_shot` | 0.5 s | A sharp wooden crossbow twang with a quick whoosh. |
| `sparkler_fizz` | 0.4 s | A short crackling sparkler fizz-pop, bright. |
| `bomb_blast` | 1 s | A chunky round cartoon bomb explosion, punchy, not scary. |
| `bubble_pop` | 0.5 s | A soft bloop of a bubble wand puffing out a cloud of bubbles. |
| `drum_hit` | 0.3 s | One warm wooden hand-drum thump, round and soft. |
| `lantern_crackle` | 0.6 s | A short warm fire crackle from a lantern flame. |
| `banker_coin` | 0.4 s | One coin dropping into a piggy bank: a clink and a tiny rattle. |

### Batch C: small stingers (13)

| Bank | Length | Prompt |
|---|---|---|
| `unlock` | 1.2 s | A shiny "new item" reveal: a rising glissando and a sparkle. |
| `perk` | 1.5 s | A royal little trumpet flourish with a sparkle, earning a royal reward. |
| `tower_star` | 1 s | A power-up ding rising into a tiny star twinkle. |
| `boost_on` | 1 s | A magical power-up whoosh rising into a bright chime. |
| `spell_ready` | 0.5 s | A soft gentle chime, an ability is ready again. |
| `lucky_pouch` | 0.7 s | A small coin pouch bursting open with a jingle, a lucky surprise. |
| `multi_kill` | 0.6 s | A punchy cartoon pop with a bright sparkle. (The game raises its pitch for bigger multi-kills.) |
| `gem_land` | 0.2 s | One tiny crystal tink. |
| `king_bonk` | 0.3 s | A rubbery belly-bonk, a jelly king bumping an enemy away. |
| `enemy_reveal` | 1.2 s | A curious spooky little wooden-flute sting, a new monster appears. |
| `boss_summon` | 0.8 s | A low gloopy bubbling, a blob splitting off. |
| `boss_enrage` | 1 s | An angry rumbling jelly growl, rising, cartoonish. |
| `healer_heal` | 0.6 s | A soft eerie shimmer, a ghostly wisp healing its friends. |

### Music (one file)

| File | Length | Prompt (ElevenLabs Music, or any loop you own the rights to) |
|---|---|---|
| `assets/music/night_loop.ogg` | 60–90 s, seamless loop | Cozy, playful fantasy night music: pizzicato strings, soft marimba, a little woodwind melody, gentle crickets in the background, 90 bpm, light and calm, never intense, loops seamlessly. |

The game loops the first OGG it finds in `assets/music/`, sits it under the sound effects, and Settings → Music turns it off. There is no separate ambience player, so mix any crickets or night ambience into this one loop.

The old TODO list mapped onto these banks:
- `gloom_die` and `brute_die` → `enemy_die`
- `enemy_bite` → `king_hurt`
- `night_loop` → the music loop

`king_cheer`, `gloom_spawn` and `ui_wood_press` have no call site. Wave clears and button clicks already cover those moments.
