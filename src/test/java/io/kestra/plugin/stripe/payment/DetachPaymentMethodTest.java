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
class DetachPaymentMethodTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void detachPaymentMethod() throws Exception {
        Assumptions.assumeTrue(!canNotBeEnabled(), "Stripe API key is required");

        // You need to create a test customer and attach a payment method first
        DetachPaymentMethod task = DetachPaymentMethod.builder()
            .apiKey(getApiKey())
            .paymentMethodId("pm_test_id")
            .build();

        var runContext = runContextFactory.of(Map.of());
        DetachPaymentMethod.Output output = task.run(runContext);

        assertNotNull(output.getId());
        // After detachment, customer should be null
        assertNull(output.getCustomer());
        assertNotNull(output.getType());
        assertNotNull(output.getRaw());
    }
}
