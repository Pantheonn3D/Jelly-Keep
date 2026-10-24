package com.google.android.gms.ads.rewarded;
import android.app.Activity;
import android.content.Context;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
public abstract class RewardedAd {
    public static void load(Context context, String adUnitId, AdRequest adRequest, RewardedAdLoadCallback loadCallback) { }
    public abstract void setFullScreenContentCallback(FullScreenContentCallback callback);
    public abstract void show(Activity activity, OnUserEarnedRewardListener listener);
}
