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
class CreatePaymentMethodTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void createCardPaymentMethod() throws Exception {
        RunContext runContext = runContextFactory.of();

        CreatePaymentMethod task = CreatePaymentMethod.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .paymentMethodType(Property.ofValue("card"))
            .cardNumber(Property.ofValue("4242424242424242"))
            .expMonth(Property.ofValue(12L))
            .expYear(Property.ofValue(2026L))
            .cvc(Property.ofValue("123"))
            .build();

        CreatePaymentMethod.Output output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getPaymentMethodId(), is(notNullValue()));
        assertThat(output.getType(), is("card"));
        assertThat(output.getRawResponse(), is(notNullValue()));
        assertThat(output.getRawResponse(), hasKey("id"));
        assertThat(output.getRawResponse(), hasKey("type"));
        assertThat(output.getRawResponse(), hasKey("card"));

        System.out.println("Created PaymentMethod: " + output.getPaymentMethodId());
    }
}
