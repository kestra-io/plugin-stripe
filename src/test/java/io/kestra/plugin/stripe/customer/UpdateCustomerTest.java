package io.kestra.plugin.stripe.customer;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.MockRunContext;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateCustomerTest extends AbstractStripeTest {

    @Test
    void testUpdateCustomerFull() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // First, create a temporary customer to update
        CreateCustomer createTask = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp User"))
            .email(Property.ofValue("tempuser@example.com"))
            .metadata(Property.ofValue(Map.of("plan", "trial")))
            .build();

        CreateCustomer.Output created = createTask.run(new MockRunContext());

        String customerId = created.getCustomerId();
        assertNotNull(customerId);

        // Now update the customer
        UpdateCustomer updateTask = UpdateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .name(Property.ofValue("Updated User"))
            .metadata(Property.ofValue(Map.of("plan", "pro", "updated_by", "test")))
            .build();

        UpdateCustomer.Output updated = updateTask.run(new MockRunContext());

        // Assertions
        assertEquals(customerId, updated.getCustomerId());
        assertNotNull(updated.getCustomerData());

        assertEquals("Updated User", updated.getCustomerData().get("name"));

        Map<String, Object> metadata = (Map<String, Object>) updated.getCustomerData().get("metadata");
        assertNotNull(metadata);
        assertEquals("pro", metadata.get("plan"));
        assertEquals("test", metadata.get("updated_by"));
    }

    @Test
    void testUpdateCustomerMinimal() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // Create a temporary customer
        CreateCustomer createTask = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .email(Property.ofValue("minimalupdate@example.com"))
            .build();

        CreateCustomer.Output created = createTask.run(new MockRunContext());

        String customerId = created.getCustomerId();
        assertNotNull(customerId);

        // Update only the email to same value (minimal update)
        UpdateCustomer updateTask = UpdateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .build();

        UpdateCustomer.Output updated = updateTask.run(new MockRunContext());

        // Assertions
        assertEquals(customerId, updated.getCustomerId());
        assertNotNull(updated.getCustomerData());
        assertEquals("minimalupdate@example.com", updated.getCustomerData().get("email"));
    }
}
