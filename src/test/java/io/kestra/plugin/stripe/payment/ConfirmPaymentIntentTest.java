package io.kestra.plugin.stripe.payment;

import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@EnabledIf("!io.kestra.plugin.stripe.AbstractStripeTest.canNotBeEnabled()")
class ConfirmPaymentIntentTest extends AbstractStripeTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        // ⚠️ Requires a valid PaymentIntent ID (replace with one from test mode)
        ConfirmPaymentIntent task = ConfirmPaymentIntent.builder()
            .apiKey(getApiKey())
            .paymentIntentId("pi_test_123") // Replace with a real test PaymentIntent ID
            .build();

        ConfirmPaymentIntent.Output output = task.run(runContext);

        assertThat(output.getPaymentIntentId(), notNullValue());
        assertThat(output.getStatus(), not(isEmptyOrNullString()));
        assertThat(output.getRaw(), containsString("payment_intent"));
    }
}
