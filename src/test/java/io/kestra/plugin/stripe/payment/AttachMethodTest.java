package io.kestra.plugin.stripe.payment;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
@DisabledIf(
    value = "canNotBeEnabled",
    disabledReason = "Needs Stripe API key to work"
)
class AttachMethodTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void attachPaymentMethod() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Use actual test IDs from your Stripe test account
        AttachMethod task = AttachMethod.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .paymentMethodId(Property.ofValue("pm_test_id"))
            .customerId(Property.ofValue("cus_test_id"))
            .build();

        AttachMethod.Output output = task.run(runContext);

        assertThat(output.getPaymentMethodId(), is(notNullValue()));
        assertThat(output.getCustomerId(), is("cus_test_id"));
        assertThat(output.getType(), is(notNullValue()));
        assertThat(output.getPaymentMethodData(), is(notNullValue()));
    }
}
