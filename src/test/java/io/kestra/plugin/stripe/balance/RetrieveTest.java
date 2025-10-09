package io.kestra.plugin.stripe.balance;

import com.stripe.Stripe;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.junit.jupiter.api.Assertions.*;


@KestraTest
@DisabledIf(
    value = "canNotBeEnabled",
    disabledReason = "Needs Stripe API key to work"
)
class RetrieveTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        Retrieve task = Retrieve.builder()
            .apiKey(Property.of(Stripe.apiKey))
            .build();

        var runContext = runContextFactory.of();
        Retrieve.Output output = task.run(runContext);

        assertNotNull(output.getAvailable());
        assertNotNull(output.getPending());
        assertNotNull(output.getRaw());
    }
}
