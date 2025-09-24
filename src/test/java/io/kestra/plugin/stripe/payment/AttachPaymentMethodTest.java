package io.kestra.plugin.stripe.payment;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.tasks.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@KestraTest
class AttachPaymentMethodTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void attachPaymentMethod() throws Exception {
        Assumptions.assumeTrue(!canNotBeEnabled(), "Stripe API key is required");

        // You need to create a test customer and payment method in Stripe dashboard before running this
        AttachPaymentMethod task = AttachPaymentMethod.builder()
            .apiKey(getApiKey())
            .paymentMethodId("pm_test_id")
            .customerId("cus_test_id")
            .build();

        var runContext = runContextFactory.of(Map.of());
        AttachPaymentMethod.Output output = task.run(runContext);

        assertNotNull(output.getId());
        assertEquals("cus_test_id", output.getCustomer());
        assertNotNull(output.getType());
        assertNotNull(output.getRaw());
    }
}
