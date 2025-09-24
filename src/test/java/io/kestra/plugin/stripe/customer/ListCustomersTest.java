package io.kestra.plugin.stripe.customer;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.MockRunContext;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ListCustomersTest extends AbstractStripeTest {

    @Test
    void testListCustomers() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // Create a temporary customer to ensure at least one exists
        CreateCustomer createTask = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp List User"))
            .email(Property.ofValue("templist@example.com"))
            .build();

        CreateCustomer.Output created = createTask.run(new MockRunContext());
        assertNotNull(created.getCustomerId());

        // List customers
        ListCustomers listTask = ListCustomers.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .limit(Property.ofValue(5))
            .build();

        ListCustomers.Output output = listTask.run(new MockRunContext());

        assertNotNull(output.getCustomers());
        assertFalse(output.getCustomers().isEmpty(), "Customer list should not be empty");
        assertTrue(output.getTotalCount() > 0, "Total count should be greater than 0");

        // Optionally, check that the recently created customer is in the list
        boolean found = output.getCustomers().stream()
            .anyMatch(c -> c.get("id").equals(created.getCustomerId()));
        assertTrue(found, "Created customer should be in the list");
    }

    @Test
    void testListCustomersWithEmailFilter() throws Exception {
        if (canNotBeEnabled()) {
            System.out.println("Stripe API key not set. Skipping test.");
            return;
        }

        // Create a customer with a unique email
        String uniqueEmail = "filtertest+" + System.currentTimeMillis() + "@example.com";
        CreateCustomer createTask = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Filter Test User"))
            .email(Property.ofValue(uniqueEmail))
            .build();

        CreateCustomer.Output created = createTask.run(new MockRunContext());
        assertNotNull(created.getCustomerId());

        // List customers filtered by email
        ListCustomers listTask = ListCustomers.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .email(Property.ofValue(uniqueEmail))
            .build();

        ListCustomers.Output output = listTask.run(new MockRunContext());

        assertNotNull(output.getCustomers());
        assertEquals(1, output.getCustomers().size(), "Should return only one customer");
        Map<String, Object> customer = output.getCustomers().get(0);
        assertEquals(uniqueEmail, customer.get("email"));
    }
}
