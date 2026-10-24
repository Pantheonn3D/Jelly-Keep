package com.pantheon3d.jellykeep;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Google Play Billing (library 8). All products are one-time "in-app products": Royal Pass, Starter Pack and Golden
 * Spoon are acknowledged and kept; gem packs and the Gem Jar are consumed. Owned items are re-applied at every start,
 * so a reinstall restores them; consumables are granted only once Play confirms the consume.
 */
final class PlayStore implements Store, PurchasesUpdatedListener {
    private final Activity act;
    private final Host host;
    private final BillingClient client;
    private final HashMap<String, ProductDetails> details = new HashMap<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HashSet<String> consumed = new HashSet<>(), pendingTold = new HashSet<>();   // purchase tokens
    private boolean ready;

    PlayStore(Activity act, Host host) {
        this.act = act;
        this.host = host;
        client = BillingClient.newBuilder(act)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        connect();
    }

    private void connect() {
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
                ready = true;
                queryProducts();
                queryOwned(false);
            }

            // with auto reconnection the client reconnects on the next call by itself
            @Override public void onBillingServiceDisconnected() { }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        for (String sku : ALL)
            list.add(QueryProductDetailsParams.Product.newBuilder().setProductId(sku).setProductType(BillingClient.ProductType.INAPP).build());
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(list).build(), (result, products) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
            List<ProductDetails> found = products.getProductDetailsList();
            main.post(() -> { for (ProductDetails d : found) details.put(d.getProductId(), d); });
        });
    }

    @Override public String price(String sku) {
        ProductDetails d = details.get(sku);
        if (d == null || d.getOneTimePurchaseOfferDetails() == null) return null;
        return d.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    @Override public void buy(String sku) {
        ProductDetails d = details.get(sku);
        if (!ready || d == null) {
            host.storeMessage("The store isn't ready yet, try again in a moment");
            if (!ready) connect(); else queryProducts();
            return;
        }
        BillingFlowParams.ProductDetailsParams p = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build();
        int code = client.launchBillingFlow(act, BillingFlowParams.newBuilder().setProductDetailsParamsList(Collections.singletonList(p)).build())
                .getResponseCode();
        if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) queryOwned(true);
        else if (code != BillingClient.BillingResponseCode.OK && code != BillingClient.BillingResponseCode.USER_CANCELED)
            host.storeMessage("The store couldn't open, try again in a moment");
    }

    @Override public void restore() {
        if (!ready) { connect(); host.storeMessage("Connecting to Google Play..."); return; }
        queryOwned(true);
    }

    @Override public void refresh() { if (ready) queryOwned(false); }

    private void queryOwned(boolean announce) {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (result, purchases) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
            main.post(() -> {
                for (Purchase p : purchases) handle(p);
                if (announce) host.storeMessage("Purchases restored");
            });
        });
    }

    @Override public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        int code = result.getResponseCode();
        if (code == BillingClient.BillingResponseCode.OK && purchases != null) main.post(() -> { for (Purchase p : purchases) handle(p); });
        else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) queryOwned(false);
        else if (code != BillingClient.BillingResponseCode.USER_CANCELED) main.post(() -> host.storeMessage("The purchase didn't go through"));
    }

    private void handle(Purchase p) {
        String token = p.getPurchaseToken();
        if (p.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            if (pendingTold.add(token)) host.storeMessage("Purchase pending: it unlocks once payment clears");
            return;
        }
        if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        for (String sku : p.getProducts()) {
            if (Store.consumable(sku)) {
                if (!consumed.add(token)) continue;             // already being consumed (an update and a refresh crossed)
                client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(token).build(), (result, t) -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) main.post(() -> host.granted(sku));
                    else main.post(() -> consumed.remove(token));          // try again on the next refresh
                });
            } else {
                host.granted(sku);                         // idempotent for things already owned
                if (!p.isAcknowledged())
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(), r -> { });
            }
        }
    }
}
