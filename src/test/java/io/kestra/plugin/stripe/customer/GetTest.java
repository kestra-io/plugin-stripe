package io.kestra.plugin.stripe.customer;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stripe.AbstractStripeTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
@DisabledIf(
    value = "canNotBeEnabled",
    disabledReason = "Needs Stripe API key to work"
)
class GetTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testGetCustomer() throws Exception {
        RunContext runContext = runContextFactory.of();

        // First, create a temporary customer to retrieve
        Create createTask = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp Get User"))
            .email(Property.ofValue("tempget@example.com"))
            .build();

        Create.Output created = createTask.run(runContext);
        String customerId = created.getCustomerId();
        assertThat(customerId, is(notNullValue()));

        // Retrieve the customer
        Get getTask = Get.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .build();

        Get.Output output = getTask.run(runContext);

        // Assertions
        assertThat(output.getCustomerId(), is(customerId));
        assertThat(output.getCustomerData(), is(notNullValue()));
        assertThat(output.getCustomerData().get("name"), is("Temp Get User"));
        assertThat(output.getCustomerData().get("email"), is("tempget@example.com"));
    }

    @Test
    void testGetCustomerInvalidId() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Attempt to retrieve a non-existent customer
        Get getTask = Get.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue("cus_invalid123"))
            .build();

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            getTask.run(runContext);
        });

        assertThat(exception.getMessage(), containsString("Failed to retrieve Stripe customer"));
    }
}
