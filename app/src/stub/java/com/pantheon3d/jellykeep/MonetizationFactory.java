package com.pantheon3d.jellykeep;

import android.app.Activity;

/**
 * Default build: simulated ads and test purchases, no SDKs. The live build (gradle -Plive=true) swaps in
 * src/live/java/.../MonetizationFactory.java, which returns AdMob and Play Billing implementations.
 */
final class MonetizationFactory {
    static final boolean LIVE = false;

    private MonetizationFactory() { }

    static Ads ads(Activity activity, Ads.Host host) { return new Ads.Sim(host); }

    static Store store(Activity activity, Store.Host host) { return new Store.Stub(host); }

    /** Play in-app review; live build only. */
    static void askForReview(Activity activity) { }
}
