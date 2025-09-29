package io.kestra.plugin.stripe.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import io.kestra.plugin.stripe.AbstractStripe;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Retrieve a customer from Stripe by ID.",
    description = "This task fetches a Stripe customer object by its ID using the Stripe Java SDK."
)
@Plugin(
    examples = {
        @Example(
            title = "Get a customer by ID",
            full = true,
            code = """
                id: get_customer
                namespace: company.team

                tasks:
                  - id: get_customer
                    type: io.kestra.plugin.stripe.customer.GetCustomer
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    customerId: "cus_123456789"
                """
        )
    }
)
public class GetCustomer extends AbstractStripe implements RunnableTask<GetCustomer.Output> {

    @Schema(title = "The ID of the customer to retrieve.", required = true)
    @NotNull
    private Property<String> customerId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve customer ID
        String renderedCustomerId = runContext.render(this.customerId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("customerId is required"));

        Customer customer;
        try {
            customer = client(runContext).customers().retrieve(renderedCustomerId);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve Stripe customer: " + e.getMessage(), e);
        }

        // Convert Stripe customer JSON to Map<String,Object>
        String json = customer.getLastResponse().body();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> customerData = mapper.readValue(json, new TypeReference<>() {});

        return Output.builder()
            .customerId(customer.getId())
            .customerData(customerData)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the retrieved customer.")
        private final String customerId;

        @Schema(title = "The full customer object as a map.")
        @PluginProperty
        private final Map<String, Object> customerData;
    }
}