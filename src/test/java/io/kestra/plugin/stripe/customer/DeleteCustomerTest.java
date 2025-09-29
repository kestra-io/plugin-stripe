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
class DeleteCustomerTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testDeleteCustomer() throws Exception {
        RunContext runContext = runContextFactory.of();

        // First, create a temporary customer to delete
        CreateCustomer createTask = CreateCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp Delete User"))
            .email(Property.ofValue("tempdelete@example.com"))
            .build();

        CreateCustomer.Output created = createTask.run(runContext);
        String customerId = created.getCustomerId();
        assertThat(customerId, is(notNullValue()));

        // Now delete the customer
        DeleteCustomer deleteTask = DeleteCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue(customerId))
            .build();

        DeleteCustomer.Output output = deleteTask.run(runContext);

        // Assertions
        assertThat(output.getCustomerId(), is(customerId));
        assertThat(output.getDeleted(), is(true));
        assertThat(output.getCustomerData(), is(notNullValue()));
        assertThat(output.getCustomerData().get("id"), is(customerId));
    }

    @Test
    void testDeleteCustomerInvalidId() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Attempt to delete a non-existent customer
        DeleteCustomer deleteTask = DeleteCustomer.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .customerId(Property.ofValue("cus_invalid123"))
            .build();

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            deleteTask.run(runContext);
        });

        assertThat(exception.getMessage(), containsString("Failed to delete Stripe customer"));
    }
}
