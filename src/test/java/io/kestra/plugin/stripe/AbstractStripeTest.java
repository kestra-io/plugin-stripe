package io.kestra.plugin.stripe;

import com.google.common.base.Strings;
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
