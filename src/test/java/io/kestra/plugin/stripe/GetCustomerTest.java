package io.kestra.plugin.stripe.customer;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.MockRunContext;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GetCustomerTest extends AbstractStripeTest {

    @Test
    void testGetCustomer() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // First, create a temporary customer to retrieve
        CreateCustomer createTask = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp Get User"))
            .email(Property.ofValue("tempget@example.com"))
            .build();

        CreateCustomer.Output created = createTask.run(new MockRunContext());
        String customerId = created.getCustomerId();
        assertNotNull(customerId);

        // Retrieve the customer
        GetCustomer getTask = GetCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .build();

        GetCustomer.Output output = getTask.run(new MockRunContext());

        // Assertions
        assertEquals(customerId, output.getCustomerId());
        assertNotNull(output.getCustomerData());
        assertEquals("Temp Get User", output.getCustomerData().get("name"));
        assertEquals("tempget@example.com", output.getCustomerData().get("email"));
    }

    @Test
    void testGetCustomerInvalidId() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // Attempt to retrieve a non-existent customer
        GetCustomer getTask = GetCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue("cus_invalid123"))
            .build();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            getTask.run(new MockRunContext());
        });

        assertTrue(exception.getMessage().contains("Failed to retrieve Stripe customer"));
    }
}
