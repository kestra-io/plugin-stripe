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
class RefundPaymentTest extends AbstractStripeTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        // ⚠️ Replace with a real test Charge ID or PaymentIntent ID
        RefundPayment task = RefundPayment.builder()
            .apiKey(getApiKey())
            .chargeId("ch_test_123") // Replace with a real test charge ID
            .amount(500L) // partial refund of $5.00 (if charge was more)
            .build();

        RefundPayment.Output output = task.run(runContext);

        assertThat(output.getRefundId(), notNullValue());
        assertThat(output.getStatus(), anyOf(equalTo("succeeded"), equalTo("pending")));
        assertThat(output.getRaw(), containsString("refund"));
    }
}
