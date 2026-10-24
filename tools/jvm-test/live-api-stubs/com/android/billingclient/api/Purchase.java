package com.android.billingclient.api;
import java.util.List;
public class Purchase {
    public @interface PurchaseState { int UNSPECIFIED_STATE = 0, PURCHASED = 1, PENDING = 2; }
    public int getPurchaseState() { return 0; }
    public List<String> getProducts() { return null; }
    public String getPurchaseToken() { return ""; }
    public boolean isAcknowledged() { return false; }
    public int getQuantity() { return 1; }
}
