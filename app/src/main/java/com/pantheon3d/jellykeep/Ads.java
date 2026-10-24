package com.pantheon3d.jellykeep;

/**
 * Seam for advertising. Every ad in the game goes through here, so the live AdMob build (src/live) and the
 * default simulated build share all game code. Rewarded ads are always opt-in and name their reward first.
 */
interface Ads {
    // rewarded placements: each one does something different (see the design spec, section 6.6)
    String BOOST = "boost", REVIVE = "revive", OFFLINE = "offline", CHEST = "chest", COURIER = "courier",
            GEMS = "gems", SPELL = "spell", DAILY = "daily";

    int READY = 0, LOADING = 1, NONE = 2;      // NONE: no fill or offline; a tap retries

    /** Where an ad implementation can show things inside the game (the simulated ad, messages). */
    interface Host {
        void simulateAd(boolean rewarded, Runnable done);
        void adMessage(String msg);
    }

    interface Callback { void done(boolean earned); }

    int rewardedState();

    /** Requests a rewarded ad if none is loaded or loading. */
    void load();

    /** Shows a rewarded ad; cb.done(true) only when the reward was earned. */
    void showRewarded(String placement, Callback cb);

    boolean interstitialReady();

    void showInterstitial(Runnable after);

    /** GDPR-style privacy choices must be reachable from settings when the consent SDK says so. */
    boolean privacyOptionsRequired();

    void showPrivacyOptions();

    /**
     * Default build: a short, clearly labelled stand-in ad drawn by the game, so the whole flow (loading, watching,
     * reward, Patron star) can be played and tested without an ad SDK, and nobody mistakes it for real ads.
     */
    final class Sim implements Ads {
        static final long LOAD_MS = 1200;
        private final Host host;
        private long readyAt;
        boolean noFill;                         // tests and QA: pretend the network has no ad

        Sim(Host host) { this.host = host; }

        @Override public int rewardedState() {
            if (noFill) return NONE;
            return android.os.SystemClock.uptimeMillis() < readyAt ? LOADING : READY;
        }

        @Override public void load() { }

        @Override public void showRewarded(String placement, Callback cb) {
            if (rewardedState() != READY) { cb.done(false); return; }
            host.simulateAd(true, () -> { readyAt = android.os.SystemClock.uptimeMillis() + LOAD_MS; cb.done(true); });
        }

        @Override public boolean interstitialReady() { return !noFill; }

        @Override public void showInterstitial(Runnable after) { host.simulateAd(false, after); }

        @Override public boolean privacyOptionsRequired() { return false; }

        @Override public void showPrivacyOptions() { }
    }
}
