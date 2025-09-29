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
class CreateTest extends AbstractStripeTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testCreateCustomerFull() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Test User"))
            .email(Property.ofValue("testuser@example.com"))
            .metadata(Property.ofValue(Map.of(
                "plan", "pro",
                "signup_source", "landing_page"
            )))
            .build();

        Create.Output output = task.run(runContext);

        assertThat(output.getCustomerId(), is(notNullValue()));
        assertThat(output.getCustomerData().get("name"), is("Test User"));
        assertThat(output.getCustomerData().get("email"), is("testuser@example.com"));

        Map<String, Object> metadata = (Map<String, Object>) output.getCustomerData().get("metadata");
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.get("plan"), is("pro"));
        assertThat(metadata.get("signup_source"), is("landing_page"));
    }

    @Test
    void testCreateCustomerMinimal() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .email(Property.ofValue("minimal@example.com"))
            .build();

        Create.Output output = task.run(runContext);

        assertThat(output.getCustomerId(), is(notNullValue()));
        assertThat(output.getCustomerData().get("email"), is("minimal@example.com"));
    }
}
