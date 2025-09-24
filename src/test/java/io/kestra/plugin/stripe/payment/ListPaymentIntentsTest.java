package io.kestra.plugin.stripe.payment;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.tasks.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@KestraTest
class ListPaymentIntentsTest extends AbstractStripePaymentTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void listPaymentIntents() throws Exception {
        Assumptions.assumeTrue(!canNotBeEnabled(), "Stripe API key is required");

        ListPaymentIntents task = ListPaymentIntents.builder()
            .apiKey(getApiKey())
            .limit(3L)
            .build();

        var runContext = runContextFactory.of(Map.of());
        ListPaymentIntents.Output output = task.run(runContext);

        assertNotNull(output.getPaymentIntents());
        assertTrue(output.getPaymentIntents().size() <= 3);
    }
}
