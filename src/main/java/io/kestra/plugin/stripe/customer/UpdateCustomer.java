package io.kestra.plugin.stripe.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerUpdateParams;
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

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update an existing customer in Stripe.",
    description = "This task modifies a customer in Stripe with optional fields like name, email, and metadata."
)
@Plugin(
    examples = {
        @Example(
            title = "Update a customer's name and metadata",
            full = true,
            code = """
                id: update_customer
                namespace: company.team

                tasks:
                  - id: update_customer
                    type: io.kestra.plugin.stripe.customer.UpdateCustomer
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    customerId: "cus_123456789"
                    name: "John Updated"
                    metadata:
                      plan: "enterprise"
                      updated_by: "admin"
                """
        )
    }
)
public class UpdateCustomer extends AbstractStripe implements RunnableTask<UpdateCustomer.Output> {

    @Schema(title = "The ID of the customer to update.", required = true)
    @NotNull
    private Property<String> customerId;

    @Schema(title = "The customer's name.")
    private Property<String> name;

    @Schema(title = "The customer's email address.")
    private Property<String> email;

    @Schema(title = "Key-value pairs for storing additional information.")
    private Property<Map<String, Object>> metadata;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Initialize Stripe SDK
        com.stripe.Stripe.apiKey = runContext.render(this.apiKey)
            .asString()
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));

        String renderedCustomerId = runContext.render(this.customerId)
            .asString()
            .orElseThrow(() -> new IllegalArgumentException("customerId is required"));

        String renderedName = runContext.render(this.name).asString().orElse(null);
        String renderedEmail = runContext.render(this.email).asString().orElse(null);
        Map<String, Object> renderedMetadata = runContext.render(this.metadata).asMap(String.class, Object.class).orElse(new HashMap<>());

        // Build update params
        CustomerUpdateParams.Builder builder = CustomerUpdateParams.builder();
        if (renderedName != null) builder.setName(renderedName);
        if (renderedEmail != null) builder.setEmail(renderedEmail);
        if (!renderedMetadata.isEmpty()) builder.putAllMetadata(renderedMetadata);

        CustomerUpdateParams params = builder.build();

        // Update customer using Stripe SDK
        Customer customer;
        try {
            customer = Customer.retrieve(renderedCustomerId);
            customer = customer.update(params);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to update Stripe customer: " + e.getMessage(), e);
        }

        return Output.builder()
            .customerId(customer.getId())
            .customerData(customer.toMap())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the updated customer.")
        private final String customerId;

        @Schema(title = "The full customer object as a map.")
        @PluginProperty(additionalProperties = Map.class)
        private final Map<String, Object> customerData;
    }
}
