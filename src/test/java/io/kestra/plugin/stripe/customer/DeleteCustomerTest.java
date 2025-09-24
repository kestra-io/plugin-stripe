package io.kestra.plugin.stripe.customer;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.MockRunContext;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteCustomerTest extends AbstractStripeTest {

    @Test
    void testDeleteCustomer() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // First, create a temporary customer to delete
        CreateCustomer createTask = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp Delete User"))
            .email(Property.ofValue("tempdelete@example.com"))
            .build();

        CreateCustomer.Output created = createTask.run(new MockRunContext());
        String customerId = created.getCustomerId();
        assertNotNull(customerId);

        // Now delete the customer
        DeleteCustomer deleteTask = DeleteCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .build();

        DeleteCustomer.Output output = deleteTask.run(new MockRunContext());

        // Assertions
        assertEquals(customerId, output.getCustomerId());
        assertTrue(output.getDeleted(), "Customer should be marked as deleted");
        assertNotNull(output.getCustomerData());
        assertEquals(customerId, output.getCustomerData().get("id"));
    }

    @Test
    void testDeleteCustomerInvalidId() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // Attempt to delete a non-existent customer
        DeleteCustomer deleteTask = DeleteCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue("cus_invalid123"))
            .build();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            deleteTask.run(new MockRunContext());
        });

        assertTrue(exception.getMessage().contains("Failed to delete Stripe customer"));
    }
}
