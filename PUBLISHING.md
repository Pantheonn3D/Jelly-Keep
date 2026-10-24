# Jelly Keep: publishing notes (2.0)

## Two builds

### Ready-made builds
Signed builds made in the cloud session on 2026-10-24, all version 2.0 (code 9):

| File | What it is | Use it for |
|---|---|---|
| `JellyKeep-2.0-test.apk` | test build (simulated ads, test purchases) | your phone: play the whole game |
| `JellyKeep-2.0-test.aab` | the same, as a bundle | not needed for Play |
| `JellyKeep-2.0-live-testads.apk` | live build with Google's **test** ad IDs | your phone: real AdMob test ads and the consent flow. Purchases only work when installed from Play. |
| `JellyKeep-2.0-live-testads.aab` | the same, as a bundle | an internal or closed testing track (test ads, real billing for license testers) |

For **production**, rebuild the live variant with your real AdMob IDs and bump the version, because versionCode 9 is used up once uploaded.

### Building them yourself
| | Test build (default) | Live build |
|---|---|---|
| Command | `./gradlew assembleRelease bundleRelease` | `./gradlew -Plive=true bundleRelease` (plus the ad IDs below) |
| Ads | a clearly labelled **simulated** ad drawn by the game | AdMob rewarded + interstitial, consent form via Google UMP |
| Purchases | in-game "Test purchase" confirm, nothing charged | Google Play Billing 9 |
| Third-party SDKs | none | play-services-ads 25.5.0, user-messaging-platform 4.0.0, billing 9.1.0, review 2.0.2 |
| Use it for | your phone, quick checks | the closed test (Google's test ad IDs), then production (your real IDs) |

**Never ship the test build to production.** Its rewards and purchases are free.

Outputs:
- `app/build/outputs/apk/release/app-release.apk` (to sideload)
- `app/build/outputs/bundle/release/app-release.aab` (to upload to Play)

The current version is **2.0 / versionCode 9**. Bump both for every Play upload.

## Going live, in order

### 1. AdMob
1. Create an AdMob account and add the app (Android, "Jelly Keep").
2. Create two ad units:
   - **Rewarded**: used for every "▶" button
   - **Interstitial**: shown only after a Retreat, at most once per 5 minutes, never in the first 15 minutes of play, never for Royal Pass owners
3. Under **Privacy & messaging**:
   - Create and publish a **European regulations (GDPR)** message.
   - Create and publish a **US state regulations** message.

   The game shows them through UMP where required. Settings → "Privacy choices" appears only for players who need it.
4. Put the IDs in your own `~/.gradle/gradle.properties`, never in the repo:
   ```
   admobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
   admobRewarded=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
   admobInterstitial=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
   ```
   Without them, the live build uses Google's **test** ad IDs, which is right for trying it on your phone. Never tap your own real ads.
5. Once the app is published, set up **app-ads.txt** on the website listed in your Play store listing. AdMob shows the line to paste.

### 2. Play Console products
Create these as **one-time products**. Use the IDs exactly as written, because the code matches on them:

| Product ID | Name | Suggested price | Kind (the code handles this) |
|---|---|---|---|
| `royal_pass` | Royal Pass | $4.99 | kept forever: no interstitials, every ▶ reward without watching, +100 gems |
| `starter_pack` | Starter Pack | $1.99 | kept forever: 300 gems + permanent +50% coins (offered from wave 8) |
| `golden_spoon` | Golden Spoon | $3.99 | kept forever: ×2 coins |
| `gems_s` | Pouch of gems | $0.99 | consumable: 80 gems |
| `gems_m` | Chest of gems | $4.99 | consumable: 500 gems |
| `gems_l` | Vault of gems | $9.99 | consumable: 1200 gems |
| `gem_jar` | Gem Jar | $2.99 | consumable: collects 2 gems per cleared wave (max 600); you can buy it once it holds 150 |

Kept-forever items are acknowledged and come back automatically on reinstall. Settings → "Restore purchases" also re-applies them. Gem packs are consumed and granted once Play confirms.

Play's rule: from **Aug 31, 2026**, new apps and updates must use Billing Library 8 or later. The game uses 9.1.0, so it should also meet the next deadline.

### 3. Privacy policy
Host the text below at a public URL, for example on Google Sites or GitHub Pages. Then put the URL in `app/src/main/res/values/res.xml`:

```xml
<string name="privacy_url" translatable="false">https://your-url</string>
```

Settings shows a "Privacy policy" link once this string is set.

> **Jelly Keep privacy policy**
>
> Jelly Keep saves your progress only on your device. We do not run servers and do not collect your name, email or contacts.
>
> **Ads.** The game shows ads through Google AdMob. Google may collect your device's advertising ID, IP address (approximate location), app interactions and diagnostic information to show ads, measure them and prevent fraud. In regions where consent is required, you are asked first, and you can change your choice at any time in Settings → Privacy choices. Google's policy: https://policies.google.com/privacy
>
> **Purchases.** In-app purchases are processed by Google Play. We never see your payment details.
>
> **Notifications.** If you turn on reminders, the game schedules them on your device. Nothing is sent to us.
>
> **Children.** Jelly Keep is not directed at children under 13.
>
> **Contact:** pantheon3d.contact@gmail.com

### 4. App content (Play Console → Policy → App content)
- **Ads:** Yes, the app contains ads.
- **Target audience:** pick **18 and over**, or 16–17 and 18+. Do not include any age under 13. That keeps the game out of the Families program, so standard AdMob rules apply.
  - The art is cute, so if Play asks whether the store listing could appeal to children, answer honestly.
  - Keep the listing's wording and screenshots aimed at adults ("cozy idle defense"), not at kids.
- **Content rating (IARC questionnaire):**
  - It's a game with mild cartoon fantasy violence: you squish blob monsters, with no blood or gore.
  - It has in-app purchases of digital goods.
  - It has no gambling, no paid random boxes, and no user-generated content or chat.
- **Data safety** (from Google's AdMob guidance; recheck the [current page](https://developers.google.com/admob/android/privacy/play-data-disclosure) for the SDK version you build with):
  - Does the app collect or share data? **Yes** (because of the AdMob SDK).
  - **Location → Approximate location** (from IP address): collected and shared, for Advertising, Analytics and Fraud prevention.
  - **App activity → App interactions:** collected and shared, for Advertising and Analytics.
  - **App info and performance → Diagnostics:** collected and shared, for Analytics and Fraud prevention.
  - **Device or other IDs** (advertising ID): collected and shared, for Advertising, Analytics and Fraud prevention.
  - **Encrypted in transit:** Yes.
  - **Purchases:**
    - The game itself sends no purchase data anywhere; Google Play processes payments.
    - If the form's wording leaves you unsure, the cautious answer is to declare **Financial info → Purchase history**: collected, not shared, for App functionality.
  - Progress and settings stay on the device, so they are not "collected".
- **Permissions:**
  - **VIBRATE** (haptics, can be switched off)
  - **POST_NOTIFICATIONS** (opt-in reminders, asked in context)
  - Live build only, merged in by the SDKs:
    - **INTERNET**, **ACCESS_NETWORK_STATE**
    - **AD_ID** and **ACCESS_ADSERVICES_AD_ID / _ATTRIBUTION / _TOPICS** (Android's Privacy Sandbox APIs, used by AdMob)
    - **BILLING**
    - **WAKE_LOCK** and **FOREGROUND_SERVICE**, from WorkManager inside the AdMob SDK. The game never starts a foreground service, and no foreground-service type is declared.

### 5. Testing and release
1. Build the live variant and install it on your phone. With test IDs you'll see Google test ads.
   - **In the EU before your own AdMob app exists:** the consent step uses Google's sample app ID, which may have no consent message. The ▶ buttons may then say "No ad available". Once your own app ID and GDPR message are published, the consent form shows and ads load.
   - If an ad doesn't appear, `adb logcat -s JellyKeep` prints the consent, ad-load or store error.
   - Check that the consent form appears when you're in the EU, or use a VPN or AdMob's test-geography setting.
   - Check that a rewarded ad pays, a "Test purchase" no longer appears, and a real purchase flow opens.
2. Add yourself as a **license tester** in Play Console so purchases are free test purchases.
3. New personal developer accounts need a **closed test with at least 12 testers for 14 consecutive days** before production. Use the live build with test ads for that, or the real IDs and no tapping your own ads.
4. Production: same AAB, real IDs, bump the version.

## Category and listing
- **Category:** Game → Strategy, or Casual.
- **Short description idea:** "Defend the Jelly King: tap, build, upgrade, rebirth. A cozy idle tower defense."
- **Store assets:** the 512 px icon, the 1024×500 feature graphic and the screenshots come from the store assets zip of the first session. Retake the screenshots for 2.0: title, battle, Vault, Shop and Rebirth.

## Build facts
- Package `com.pantheon3d.jellykeep`, targetSdk 36, minSdk 26 (Android 8+).
- Font: Lilita One (SIL Open Font License, see FONT-LICENSE-OFL.txt), free for commercial use.
- Art and sound: your generated images and ElevenLabs sounds, plus clean originals from your Bubble SFX library. What's still missing is listed in `docs/art-and-audio-requests.md`.
- **Signing:** `jellykeep-upload.jks` + `keystore.properties` at the repo root. That's the upload key for this app: back it up, never commit it, and don't mix it up with Squish's key.
