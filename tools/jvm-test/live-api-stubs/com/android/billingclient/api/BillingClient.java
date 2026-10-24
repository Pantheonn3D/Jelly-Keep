package com.android.billingclient.api;
import android.app.Activity;
import android.content.Context;
public abstract class BillingClient {
    public @interface BillingResponseCode {
        int SERVICE_TIMEOUT = -3, FEATURE_NOT_SUPPORTED = -2, SERVICE_DISCONNECTED = -1, OK = 0, USER_CANCELED = 1,
                SERVICE_UNAVAILABLE = 2, BILLING_UNAVAILABLE = 3, ITEM_UNAVAILABLE = 4, DEVELOPER_ERROR = 5, ERROR = 6,
                ITEM_ALREADY_OWNED = 7, ITEM_NOT_OWNED = 8, NETWORK_ERROR = 12;
    }
    public @interface ProductType { String INAPP = "inapp", SUBS = "subs"; }
    public static Builder newBuilder(Context context) { return new Builder(); }
    public static final class Builder {
        public Builder setListener(PurchasesUpdatedListener listener) { return this; }
        public Builder enablePendingPurchases(PendingPurchasesParams params) { return this; }
        public Builder enableAutoServiceReconnection() { return this; }
        public BillingClient build() { return null; }
    }
    public abstract void startConnection(BillingClientStateListener listener);
    public abstract void endConnection();
    public abstract void queryProductDetailsAsync(QueryProductDetailsParams params, ProductDetailsResponseListener listener);
    public abstract BillingResult launchBillingFlow(Activity activity, BillingFlowParams params);
    public abstract void queryPurchasesAsync(QueryPurchasesParams params, PurchasesResponseListener listener);
    public abstract void consumeAsync(ConsumeParams params, ConsumeResponseListener listener);
    public abstract void acknowledgePurchase(AcknowledgePurchaseParams params, AcknowledgePurchaseResponseListener listener);
}
