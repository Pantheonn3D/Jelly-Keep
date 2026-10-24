package com.android.billingclient.api;
import java.util.List;
public interface PurchasesUpdatedListener { void onPurchasesUpdated(BillingResult result, List<Purchase> purchases); }
