package com.pantheon3d.jellykeep;

import android.app.Activity;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/** Live build (gradle -Plive=true): AdMob ads behind UMP consent, Google Play Billing, Play in-app review. */
final class MonetizationFactory {
    static final boolean LIVE = true;

    private MonetizationFactory() { }

    static Ads ads(Activity activity, Ads.Host host) { return activity == null ? new Ads.Sim(host) : new AdMobAds(activity, host); }

    static Store store(Activity activity, Store.Host host) { return activity == null ? new Store.Stub(host) : new PlayStore(activity, host); }

    /** Google decides whether the card actually shows; we only ask once, after the first boss. */
    static void askForReview(Activity activity) {
        if (activity == null) return;
        ReviewManager m = ReviewManagerFactory.create(activity);
        m.requestReviewFlow().addOnCompleteListener(task -> {
            if (task.isSuccessful()) m.launchReviewFlow(activity, task.getResult());
        });
    }
}
