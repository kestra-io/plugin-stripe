package io.kestra.plugin.stripe.payment;

import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.runners.RunnerUtils;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreatePaymentIntentTest extends AbstractStripeTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private RunnerUtils runnerUtils;

    @Test
    void createPaymentIntent() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        RunContext runContext = runContextFactory.of();

        CreatePaymentIntent task = CreatePaymentIntent.builder()
            .apiKey(getApiKey())
            .amount(1000L) // $10.00 if currency is USD
            .currency("usd")
            .build();

        CreatePaymentIntent.Output output = task.run(runContext);

        assertNotNull(output);
        assertNotNull(output.getPaymentIntentId(), "PaymentIntent ID should not be null");
        assertEquals("requires_payment_method", output.getStatus(), "Newly created intent should require payment method");
        assertNotNull(output.getClientSecret(), "Client secret should not be null");

        // raw response sanity check
        assertTrue(output.getRawResponse().containsKey("id"));
        assertTrue(output.getRawResponse().containsKey("status"));

        System.out.println("Created PaymentIntent: " + output.getPaymentIntentId());
    }
}
