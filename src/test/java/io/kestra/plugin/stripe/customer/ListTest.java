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
class ListTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testListCustomers() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Create a temporary customer to ensure at least one exists
        Create createTask = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Temp List User"))
            .email(Property.ofValue("templist@example.com"))
            .build();

        Create.Output created = createTask.run(runContext);
        assertThat(created.getCustomerId(), is(notNullValue()));

        // List customers
        List listTask = List.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .limit(Property.ofValue(5))
            .build();

        List.Output output = listTask.run(runContext);

        assertThat(output.getCustomers(), is(notNullValue()));
        assertThat(output.getCustomers().isEmpty(), is(false));
        assertThat(output.getTotalCount(), greaterThan(0));

        // Optionally, check that the recently created customer is in the list
        boolean found = output.getCustomers().stream()
            .anyMatch(c -> c.get("id").equals(created.getCustomerId()));
        assertThat(found, is(true));
    }

    @Test
    void testListCustomersWithEmailFilter() throws Exception {
        RunContext runContext = runContextFactory.of();

        // Create a customer with a unique email
        String uniqueEmail = "filtertest+" + System.currentTimeMillis() + "@example.com";
        Create createTask = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Filter Test User"))
            .email(Property.ofValue(uniqueEmail))
            .build();

        Create.Output created = createTask.run(runContext);
        assertThat(created.getCustomerId(), is(notNullValue()));

        // List customers filtered by email
        List listTask = List.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .email(Property.ofValue(uniqueEmail))
            .build();

        List.Output output = listTask.run(runContext);

        assertThat(output.getCustomers(), is(notNullValue()));
        assertThat(output.getCustomers().size(), is(1));
        Map<String, Object> customer = output.getCustomers().get(0);
        assertThat(customer.get("email"), is(uniqueEmail));
    }
}
