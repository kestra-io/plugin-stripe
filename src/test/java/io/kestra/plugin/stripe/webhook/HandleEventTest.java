package io.kestra.plugin.stripe.webhook;

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
class HandleEventTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void handleWebhookEvent() throws Exception {
        Assumptions.assumeTrue(!canNotBeEnabled(), "Stripe API key is required");

        // Example payload and signature (replace with real test webhook payload & signature from Stripe)
        String testPayload = "{\"id\": \"evt_test\", \"object\": \"event\", \"type\": \"payment_intent.succeeded\", \"data\": {\"object\": {\"id\": \"pi_test\"}}}";
        String testSignature = "t=123456,v1=test_signature";
        String testSecret = "whsec_test_secret";

        HandleEvent task = HandleEvent.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .payload(Property.ofValue(testPayload))
            .signatureHeader(Property.ofValue(testSignature))
            .endpointSecret(Property.ofValue(testSecret))
            .build();

        var runContext = runContextFactory.of(Map.of());

        // Since signature is fake, we expect RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> task.run(runContext));
        assertTrue(exception.getMessage().contains("Invalid Stripe webhook signature"));
    }
}
