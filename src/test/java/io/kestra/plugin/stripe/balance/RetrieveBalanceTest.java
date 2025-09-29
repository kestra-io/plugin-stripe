package io.kestra.plugin.stripe.balance;

import com.stripe.Stripe;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveBalanceTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Assumptions.assumeTrue(!canNotBeEnabled(), "Stripe API key is required");

        RetrieveBalance task = RetrieveBalance.builder()
            .apiKey(Property.of(Stripe.apiKey))
            .build();

        var runContext = runContextFactory.of();
        RetrieveBalance.Output output = task.run(runContext);

        assertNotNull(output.getAvailable());
        assertNotNull(output.getPending());
        assertNotNull(output.getRaw());
    }
}
