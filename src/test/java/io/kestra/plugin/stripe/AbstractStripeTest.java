package io.kestra.plugin.stripe;

import io.kestra.core.junit.annotations.KestraTest;

@KestraTest
public abstract class AbstractStripeTest {
    private static final String STRIPE_API_KEY = "";

    protected static boolean canNotBeEnabled() {
        return STRIPE_API_KEY == null || STRIPE_API_KEY.isEmpty();
    }

    protected String getApiKey() {
        return STRIPE_API_KEY;
    }

}
