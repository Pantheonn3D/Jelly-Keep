package com.google.android.ump;
import android.app.Activity;
public interface ConsentInformation {
    void requestConsentInfoUpdate(Activity activity, ConsentRequestParameters params,
            OnConsentInfoUpdateSuccessListener onSuccess, OnConsentInfoUpdateFailureListener onFailure);
    boolean canRequestAds();
    PrivacyOptionsRequirementStatus getPrivacyOptionsRequirementStatus();
    enum PrivacyOptionsRequirementStatus { UNKNOWN, NOT_REQUIRED, REQUIRED }
    interface OnConsentInfoUpdateSuccessListener { void onConsentInfoUpdateSuccess(); }
    interface OnConsentInfoUpdateFailureListener { void onConsentInfoUpdateFailure(FormError error); }
}
