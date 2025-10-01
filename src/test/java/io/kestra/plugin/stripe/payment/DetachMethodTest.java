package io.kestra.plugin.stripe.payment;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
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
class DetachMethodTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void detachPaymentMethod() throws Exception {
        // Wrap all Property fields
        DetachMethod task = DetachMethod.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .paymentMethodId(Property.ofValue("pm_test_id"))
            .build();

        RunContext runContext = runContextFactory.of(Map.of());
        DetachMethod.Output output = task.run(runContext);

        assertNotNull(output.getId());
        // After detachment, customer should be null
        assertNull(output.getCustomer());
        assertNotNull(output.getType());
        assertNotNull(output.getRawResponse());  // ✅ use getRawResponse()

        System.out.println("Detached PaymentMethod ID: " + output.getId());
    }
}
