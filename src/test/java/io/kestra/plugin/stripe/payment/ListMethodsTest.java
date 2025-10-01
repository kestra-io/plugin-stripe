package io.kestra.plugin.stripe.payment;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@KestraTest
@DisabledIf(
    value = "canNotBeEnabled",
    disabledReason = "Needs Stripe API key to work"
)
class ListMethodsTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void listPaymentMethods() throws Exception {
        // You need a test customer with attached payment methods in Stripe dashboard
        ListMethods task = ListMethods.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue("cus_test_id"))
            .type("card")
            .build();

        var runContext = runContextFactory.of(Map.of());
        ListMethods.Output output = task.run(runContext);

        assertEquals("cus_test_id", output.getCustomerId());
        assertNotNull(output.getPaymentMethodIds());
        assertFalse(output.getPaymentMethodIds().isEmpty());
        assertNotNull(output.getRaw());
    }
}
