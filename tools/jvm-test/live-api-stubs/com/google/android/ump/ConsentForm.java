package com.google.android.ump;
import android.app.Activity;
public interface ConsentForm {
    void show(Activity activity, OnConsentFormDismissedListener listener);
    interface OnConsentFormDismissedListener { void onConsentFormDismissed(FormError formError); }
}
