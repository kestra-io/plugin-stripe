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
class CreateIntentTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void createPaymentIntent() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Build the task using Property.ofValue for all fields
        CreateIntent task = CreateIntent.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .amount(Property.ofValue(1000L)) // $10.00 if currency is USD
            .currency(Property.ofValue("usd"))
            .build();

        CreateIntent.Output output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getPaymentIntentId(), is(notNullValue()));
        assertThat(output.getStatus(), is("requires_payment_method"));
        assertThat(output.getClientSecret(), is(notNullValue()));

        // raw response sanity check
        assertThat(output.getRawResponse(), hasKey("id"));
        assertThat(output.getRawResponse(), hasKey("status"));

        System.out.println("Created PaymentIntent: " + output.getPaymentIntentId());
    }
}
