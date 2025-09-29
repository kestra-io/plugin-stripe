package io.kestra.plugin.stripe.payment;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@KestraTest
class ListPaymentMethodsTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void listPaymentMethods() throws Exception {
        Assumptions.assumeTrue(!canNotBeEnabled(), "Stripe API key is required");

        // You need a test customer with attached payment methods in Stripe dashboard
        ListPaymentMethods task = ListPaymentMethods.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue("cus_test_id"))
            .type("card")
            .build();

        var runContext = runContextFactory.of(Map.of());
        ListPaymentMethods.Output output = task.run(runContext);

        assertEquals("cus_test_id", output.getCustomerId());
        assertNotNull(output.getPaymentMethodIds());
        assertFalse(output.getPaymentMethodIds().isEmpty());
        assertNotNull(output.getRaw());
    }
}
