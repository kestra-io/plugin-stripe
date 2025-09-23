package io.kestra.plugin.stripe.customer;

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
        // Initialize Stripe SDK
        com.stripe.Stripe.apiKey = runContext.render(this.apiKey)
            .asString()
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));

        String renderedCustomerId = runContext.render(this.customerId)
            .asString()
            .orElseThrow(() -> new IllegalArgumentException("customerId is required"));

        Customer customer;
        try {
            customer = Customer.retrieve(renderedCustomerId);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve Stripe customer: " + e.getMessage(), e);
        }

        return Output.builder()
            .customerId(customer.getId())
            .customerData(customer.toMap())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the retrieved customer.")
        private final String customerId;

        @Schema(title = "The full customer object as a map.")
        @PluginProperty(additionalProperties = Map.class)
        private final Map<String, Object> customerData;
    }
}
