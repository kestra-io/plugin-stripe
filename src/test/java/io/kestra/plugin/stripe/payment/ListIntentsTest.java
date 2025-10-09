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
class ListIntentsTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void listPaymentIntents() throws Exception {
        // Wrap all Property fields
        ListIntents task = ListIntents.builder()
            .apiKey(Property.ofValue(getApiKey()))   // ✅ wrap in Property
            .limit(Property.ofValue(3L))             // ✅ wrap in Property
            .build();

        RunContext runContext = runContextFactory.of(Map.of());
        ListIntents.Output output = task.run(runContext);

        assertNotNull(output.getPaymentIntents());
        assertTrue(output.getPaymentIntents().size() <= 3);

        System.out.println("Listed PaymentIntents count: " + output.getPaymentIntents().size());
    }
}
