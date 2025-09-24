package io.kestra.plugin.stripe.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
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
    title = "Create a new customer in Stripe.",
    description = "This task creates a customer in Stripe with optional name, email, and metadata."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a customer with name, email, and metadata",
            full = true,
            code = """
                id: create_customer
                namespace: company.team

                tasks:
                  - id: create_customer
                    type: io.kestra.plugin.stripe.customer.CreateCustomer
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    name: "John Doe"
                    email: "john@example.com"
                    metadata:
                      plan: "pro"
                      signup_source: "landing_page"
                """
        )
    }
)
public class CreateCustomer extends AbstractStripe implements RunnableTask<CreateCustomer.Output> {

    @Schema(title = "The customer's name.")
    private Property<String> name;

    @Schema(title = "The customer's email address.")
    private Property<String> email;

    @Schema(title = "Key-value pairs for storing additional information.")
    private Property<Map<String, Object>> metadata;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve API key and initialize SDK client
        com.stripe.Stripe.apiKey = runContext.render(this.apiKey).asString().orElseThrow(
            () -> new IllegalArgumentException("Stripe API key is required")
        );

        // Resolve parameters
        String renderedName = runContext.render(this.name).asString().orElse(null);
        String renderedEmail = runContext.render(this.email).asString().orElse(null);
        Map<String, Object> renderedMetadata = runContext.render(this.metadata).asMap(String.class, Object.class).orElse(new HashMap<>());

        // Build customer create params
        CustomerCreateParams.Builder builder = CustomerCreateParams.builder();
        if (renderedName != null) builder.setName(renderedName);
        if (renderedEmail != null) builder.setEmail(renderedEmail);
        if (!renderedMetadata.isEmpty()) builder.putAllMetadata(renderedMetadata);

        CustomerCreateParams params = builder.build();

        // Create customer using Stripe SDK
        Customer customer;
        try {
            customer = Customer.create(params);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe customer: " + e.getMessage(), e);
        }

        // Return output
        return Output.builder()
            .customerId(customer.getId())
            .customerData(customer.toMap())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the created customer.")
        private final String customerId;

        @Schema(title = "The full customer object as a map.")
        @PluginProperty(additionalProperties = Map.class)
        private final Map<String, Object> customerData;
    }
}
