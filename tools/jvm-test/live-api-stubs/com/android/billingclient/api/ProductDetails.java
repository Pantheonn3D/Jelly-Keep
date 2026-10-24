package com.android.billingclient.api;
import java.util.List;
public final class ProductDetails {
    public String getProductId() { return ""; }
    public String getProductType() { return ""; }
    public OneTimePurchaseOfferDetails getOneTimePurchaseOfferDetails() { return null; }
    public List<OneTimePurchaseOfferDetails> getOneTimePurchaseOfferDetailsList() { return null; }
    public static final class OneTimePurchaseOfferDetails {
        public String getFormattedPrice() { return ""; }
        public long getPriceAmountMicros() { return 0; }
        public String getPriceCurrencyCode() { return ""; }
    }
}
