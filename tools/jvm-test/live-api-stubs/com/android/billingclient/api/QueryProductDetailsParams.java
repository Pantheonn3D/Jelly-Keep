package com.android.billingclient.api;
import java.util.List;
public final class QueryProductDetailsParams {
    public static Builder newBuilder() { return new Builder(); }
    public static final class Builder {
        public Builder setProductList(List<Product> productList) { return this; }
        public QueryProductDetailsParams build() { return new QueryProductDetailsParams(); }
    }
    public static final class Product {
        public static Builder newBuilder() { return new Builder(); }
        public static final class Builder {
            public Builder setProductId(String id) { return this; }
            public Builder setProductType(String type) { return this; }
            public Product build() { return new Product(); }
        }
    }
}
