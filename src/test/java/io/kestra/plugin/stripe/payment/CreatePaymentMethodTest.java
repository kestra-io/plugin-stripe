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
class CreatePaymentMethodTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void createCardPaymentMethod() throws Exception {
        Assumptions.assumeTrue(!canNotBeEnabled(), "Stripe API key is required");

        CreatePaymentMethod task = CreatePaymentMethod.builder()
            .apiKey(getApiKey())
            .type("card")
            .card(Map.of(
                "number", "4242424242424242",
                "exp_month", 12,
                "exp_year", 2026,
                "cvc", "123"
            ))
            .build();

        var runContext = runContextFactory.of(Map.of());
        CreatePaymentMethod.Output output = task.run(runContext);

        assertNotNull(output.getId());
        assertEquals("card", output.getType());
        assertNotNull(output.getRaw());
    }
}
