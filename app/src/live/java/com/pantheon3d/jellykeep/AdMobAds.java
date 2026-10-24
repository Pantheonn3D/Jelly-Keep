package com.pantheon3d.jellykeep;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AdMob rewarded + interstitial ads. Consent first (Google UMP: shows the GDPR/US-states form only where required),
 * then the SDK starts and keeps one rewarded and one interstitial ad loaded, retrying with backoff on no-fill.
 */
final class AdMobAds implements Ads {
    private final Activity act;
    private final Host host;
    private final ConsentInformation consent;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean started = new AtomicBoolean();
    private RewardedAd rewarded;
    private InterstitialAd inter;
    private boolean loadingR, loadingI;
    private int retries;

    AdMobAds(Activity act, Host host) {
        this.act = act;
        this.host = host;
        consent = UserMessagingPlatform.getConsentInformation(act);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(act, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(act, formError -> start()),
                requestError -> start());
        if (consent.canRequestAds()) start();
    }

    private void start() {
        if (!consent.canRequestAds() || !started.compareAndSet(false, true)) return;
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T).build());
        new Thread(() -> MobileAds.initialize(act, status -> main.post(() -> { load(); loadInterstitial(); }))).start();
    }

    @Override public int rewardedState() {
        if (rewarded != null) return READY;
        return loadingR || !started.get() ? LOADING : NONE;
    }

    @Override public void load() {
        if (!started.get() || rewarded != null || loadingR) return;
        loadingR = true;
        RewardedAd.load(act, act.getString(R.string.admob_rewarded), new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) { rewarded = ad; loadingR = false; retries = 0; }
            @Override public void onAdFailedToLoad(LoadAdError error) { loadingR = false; retryLater(); }
        });
    }

    private void retryLater() {
        long delay = Math.min(64_000L, 2000L << Math.min(retries++, 5));
        main.postDelayed(this::load, delay);
    }

    @Override public void showRewarded(String placement, Callback cb) {
        RewardedAd ad = rewarded;
        if (ad == null) { cb.done(false); load(); return; }
        rewarded = null;
        final boolean[] settled = {false};
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                if (!settled[0]) { settled[0] = true; cb.done(false); }
                load();
            }

            @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                if (!settled[0]) { settled[0] = true; cb.done(false); }
                load();
            }
        });
        ad.show(act, reward -> main.post(() -> {
            if (!settled[0]) { settled[0] = true; cb.done(true); }
        }));
    }

    private void loadInterstitial() {
        if (!started.get() || inter != null || loadingI) return;
        loadingI = true;
        InterstitialAd.load(act, act.getString(R.string.admob_interstitial), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(InterstitialAd ad) { inter = ad; loadingI = false; }
            @Override public void onAdFailedToLoad(LoadAdError error) { loadingI = false; }
        });
    }

    @Override public boolean interstitialReady() { return inter != null; }

    @Override public void showInterstitial(Runnable after) {
        InterstitialAd ad = inter;
        if (ad == null) { after.run(); loadInterstitial(); return; }
        inter = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { after.run(); loadInterstitial(); }

            @Override public void onAdFailedToShowFullScreenContent(AdError error) { after.run(); loadInterstitial(); }
        });
        ad.show(act);
    }

    @Override public boolean privacyOptionsRequired() {
        return consent.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    @Override public void showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(act, formError -> {
            if (formError != null) host.adMessage("Couldn't open privacy choices right now");
        });
    }
}
