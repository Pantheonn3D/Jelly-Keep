package com.google.android.gms.ads.interstitial;
import android.app.Activity;
import android.content.Context;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
public abstract class InterstitialAd {
    public static void load(Context context, String adUnitId, AdRequest adRequest, InterstitialAdLoadCallback loadCallback) { }
    public abstract void setFullScreenContentCallback(FullScreenContentCallback callback);
    public abstract void show(Activity activity);
}
