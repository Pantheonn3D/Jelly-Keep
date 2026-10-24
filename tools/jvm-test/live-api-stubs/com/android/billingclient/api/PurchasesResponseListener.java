package com.android.billingclient.api;
import java.util.List;
public interface PurchasesResponseListener { void onQueryPurchasesResponse(BillingResult result, List<Purchase> purchases); }
