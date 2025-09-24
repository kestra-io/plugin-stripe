package io.kestra.plugin.stripe.customer;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.MockRunContext;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CreateCustomerTest extends AbstractStripeTest {

    @Test
    void testCreateCustomerFull() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // Build the task
        CreateCustomer task = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Test User"))
            .email(Property.ofValue("testuser@example.com"))
            .metadata(Property.ofValue(Map.of("plan", "pro", "signup_source", "landing_page")))
            .build();

        // Run the task
        CreateCustomer.Output output = task.run(new MockRunContext());

        // Assertions
        assertNotNull(output.getCustomerId(), "Customer ID should not be null");
        assertNotNull(output.getCustomerData(), "Customer data should not be null");

        assertEquals("Test User", output.getCustomerData().get("name"));
        assertEquals("testuser@example.com", output.getCustomerData().get("email"));

        Map<String, Object> metadata = (Map<String, Object>) output.getCustomerData().get("metadata");
        assertNotNull(metadata);
        assertEquals("pro", metadata.get("plan"));
        assertEquals("landing_page", metadata.get("signup_source"));
    }

    @Test
    void testCreateCustomerMinimal() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // Build the task with only email
        CreateCustomer task = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .email(Property.ofValue("minimal@example.com"))
            .build();

        // Run the task
        CreateCustomer.Output output = task.run(new MockRunContext());

        // Assertions
        assertNotNull(output.getCustomerId(), "Customer ID should not be null");
        assertNotNull(output.getCustomerData(), "Customer data should not be null");

        assertEquals("minimal@example.com", output.getCustomerData().get("email"));
    }
}
