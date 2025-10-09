package io.kestra.plugin.stripe.payment;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
@DisabledIf(
    value = "canNotBeEnabled",
    disabledReason = "Needs Stripe API key to work"
)
class ConfirmIntentTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        // ⚠️ Requires a valid PaymentIntent ID from your Stripe test account
        ConfirmIntent task = ConfirmIntent.builder()
            .apiKey(Property.ofValue(getApiKey()))        // ✅ Wrap in Property.ofValue
            .paymentIntentId(Property.ofValue("pi_test_123")) // ✅ Also wrap
            .build();

        ConfirmIntent.Output output = task.run(runContext);

        assertThat(output.getPaymentIntentId(), notNullValue());
        assertThat(output.getStatus(), not(isEmptyOrNullString()));
        assertThat(output.getRaw(), containsString("payment_intent"));
    }
}
