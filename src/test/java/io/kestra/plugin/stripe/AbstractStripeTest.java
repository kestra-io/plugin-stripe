package io.kestra.plugin.stripe;

import com.google.common.base.Strings;
import io.kestra.core.junit.annotations.KestraTest;

@KestraTest
public abstract class AbstractStripeTest {
    private static final String STRIPE_API_KEY = "";

    /**
     * Returns true if the test cannot run because required environment variables are missing.
     */
    protected static boolean canNotBeEnabled() {
        return Strings.isNullOrEmpty(getApiKey());
    }

    /**
     * Returns the Stripe API key to use in tests.
     */
    protected static String getApiKey() {
        return STRIPE_API_KEY;
    }
}
