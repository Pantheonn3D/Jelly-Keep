package com.pantheon3d.jellykeep;

/**
 * Seam for in-app purchases. SKUs here must match the products created in Play Console (all "in-app products";
 * the first three are bought once, the rest are consumed). The live build talks to Play Billing (src/live).
 */
interface Store {
    String ROYAL_PASS = "royal_pass", STARTER = "starter_pack", SPOON = "golden_spoon",
            GEMS_S = "gems_s", GEMS_M = "gems_m", GEMS_L = "gems_l", JAR = "gem_jar";
    String[] ALL = {ROYAL_PASS, STARTER, SPOON, GEMS_S, GEMS_M, GEMS_L, JAR};

    static boolean consumable(String sku) {
        return sku.equals(GEMS_S) || sku.equals(GEMS_M) || sku.equals(GEMS_L) || sku.equals(JAR);
    }

    interface Host {
        /** A purchase completed (or a bought item was restored): apply it. */
        void granted(String sku);
        /** Default build only: ask in-game before a pretend purchase. */
        void confirmTestPurchase(String sku, String price, Runnable buy);
        void storeMessage(String msg);
    }

    /** Localized price, or null while it is still loading (draw a skeleton). */
    String price(String sku);

    void buy(String sku);

    /** Re-delivers bought non-consumables (new phone, reinstall). */
    void restore();

    /** Default build: test prices, purchases confirmed in-game and granted on the spot. */
    final class Stub implements Store {
        private static final String[] PRICES = {"$4.99", "$1.99", "$3.99", "$0.99", "$4.99", "$9.99", "$2.99"};
        private final Host host;

        Stub(Host host) { this.host = host; }

        @Override public String price(String sku) {
            for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(sku)) return PRICES[i];
            return null;
        }

        @Override public void buy(String sku) { host.confirmTestPurchase(sku, price(sku), () -> host.granted(sku)); }

        @Override public void restore() { host.storeMessage("Test build: nothing to restore"); }
    }
}
