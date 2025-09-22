package io.kestra.plugin.stripe.customer;

import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripeTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

class CreateCustomerTest extends AbstractStripeTest {

    @Test
    void run() throws Exception {
        // Skip test if API key is not set
        assumeTrue(!canNotBeEnabled(), "Stripe API key is not set, skipping test");

        // Mock RunContext for dynamic variables
        RunContext runContext = RunContextMocks.simple();

        // Build the task
        CreateCustomer task = CreateCustomer.builder()
            .apiKey(getApiKey())
            .email("john@example.com")
            .name("John Doe")
            .build();

        // Execute task
        Object result = task.run(runContext);

        assertNotNull(result, "Result should not be null");

        if (result instanceof Map<?, ?> mapResult) {
            assertEquals("john@example.com", mapResult.get("email"));
            assertEquals("John Doe", mapResult.get("name"));
            assertNotNull(mapResult.get("id"), "Customer ID should be returned");
        } else {
            fail("Expected result to be a Map but got: " + result.getClass());
        }
    }
}
