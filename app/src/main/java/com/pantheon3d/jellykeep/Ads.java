package com.pantheon3d.jellykeep;

/**
 * Seam for monetization. Every reward placement in the game goes through here, so swapping in
 * AdMob (or another SDK) later touches only this file. The stub grants rewards immediately.
 */
interface Ads {
    String DOUBLE_COINS = "double_coins", REVIVE = "revive", OFFLINE_X2 = "offline_x2", CHEST = "chest";

    boolean rewardedReady();

    void showRewarded(String placement, Runnable onReward);

    /** Placeholder until a real SDK is wired in: no ad, reward granted. */
    final class Stub implements Ads {
        @Override public boolean rewardedReady() { return true; }
        @Override public void showRewarded(String placement, Runnable onReward) { onReward.run(); }
    }
}
