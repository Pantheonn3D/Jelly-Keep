package com.google.android.gms.ads;
public class RequestConfiguration {
    public static final String MAX_AD_CONTENT_RATING_G = "G", MAX_AD_CONTENT_RATING_PG = "PG",
            MAX_AD_CONTENT_RATING_T = "T", MAX_AD_CONTENT_RATING_MA = "MA";
    public static class Builder {
        public Builder() { }
        public Builder setMaxAdContentRating(String rating) { return this; }
        public RequestConfiguration build() { return new RequestConfiguration(); }
    }
}
