package io.kestra.plugin.stripe.customer;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
@DisabledIf(
    value = "canNotBeEnabled",
    disabledReason = "Needs Stripe API key to work"
)
class UpdateTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testUpdateCustomerFull() throws Exception {
        RunContext runContext = runContextFactory.of();

        // First, create a temporary customer to update
        Create createTask = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp User"))
            .email(Property.ofValue("tempuser@example.com"))
            .metadata(Property.ofValue(Map.of("plan", "trial")))
            .build();

        Create.Output created = createTask.run(runContext);
        String customerId = created.getCustomerId();
        assertThat(customerId, is(notNullValue()));

        // Now update the customer
        Update updateTask = Update.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .name(Property.ofValue("Updated User"))
            .metadata(Property.ofValue(Map.of(
                "plan", "pro",
                "updated_by", "test"
            )))
            .build();

        Update.Output updated = updateTask.run(runContext);

        // Assertions
        assertThat(updated.getCustomerId(), is(customerId));
        assertThat(updated.getCustomerData(), is(notNullValue()));
        assertThat(updated.getCustomerData().get("name"), is("Updated User"));

        Map<String, Object> metadata = (Map<String, Object>) updated.getCustomerData().get("metadata");
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.get("plan"), is("pro"));
        assertThat(metadata.get("updated_by"), is("test"));
    }

    @Test
    void testUpdateCustomerMinimal() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Create a temporary customer
        Create createTask = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .email(Property.ofValue("minimalupdate@example.com"))
            .build();

        Create.Output created = createTask.run(runContext);
        String customerId = created.getCustomerId();
        assertThat(customerId, is(notNullValue()));

        // Update only the email to same value (minimal update)
        Update updateTask = Update.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .build();

        Update.Output updated = updateTask.run(runContext);

        // Assertions
        assertThat(updated.getCustomerId(), is(customerId));
        assertThat(updated.getCustomerData(), is(notNullValue()));
        assertThat(updated.getCustomerData().get("email"), is("minimalupdate@example.com"));
    }
}
