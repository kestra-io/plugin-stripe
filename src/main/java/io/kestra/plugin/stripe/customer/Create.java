package io.kestra.plugin.stripe.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.serializers.JacksonMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
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
import java.util.stream.Collectors;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a new customer in Stripe.",
    description = "This task creates a customer in Stripe with optional name, email, and metadata. " +
        "⚠️ By default only the customerId is returned in the output. " +
        "If includeFullCustomerData is set to true, the full customer object will be included, " +
        "which may contain personal data (PII)."
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
                    type: io.kestra.plugin.stripe.customer.Create
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
public class Create extends AbstractStripe implements RunnableTask<Create.Output> {

    @Schema(title = "The customer's name.")
    @NotNull
    private Property<String> name;

    @Schema(title = "The customer's email address.")
    @NotNull
    private Property<String> email;

    @Schema(title = "Key-value pairs for storing additional information.")
    private Property<Map<String, Object>> metadata;

    @Schema(
        title = "Whether to include full customer object in output.",
        description = "⚠️ May contain personal data (PII). Use only if necessary."
    )
    @Builder.Default
    private Property<Boolean> includeFullCustomerData = Property.of(false);

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve parameters
        String rName = runContext.render(this.name)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Property 'name' is required and cannot be null"));

        String rEmail = runContext.render(this.email).as(String.class).orElse(null);

        Map<String, Object> rMetadata = runContext.render(this.metadata).asMap(String.class, Object.class);

        // Convert metadata values to String for Stripe
        Map<String, String> metadataForStripe = rMetadata.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() == null ? "" : e.getValue().toString()
            ));

        // Build customer create params
        CustomerCreateParams.Builder builder = CustomerCreateParams.builder();
        if (rName != null) builder.setName(rName);
        if (rEmail != null) builder.setEmail(rEmail);
        if (!metadataForStripe.isEmpty()) builder.putAllMetadata(metadataForStripe);

        Customer customer;
        try {
            customer = client(runContext).customers().create(builder.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe customer: " + e.getMessage(), e);
        }

        // Always return customerId
        Output.OutputBuilder output = Output.builder()
            .customerId(customer.getId());

        // Only include full customer data if explicitly requested
        boolean includeFull = runContext.render(this.includeFullCustomerData).as(Boolean.class).orElse(false);
        if (includeFull) {
            String json = customer.getLastResponse().body();
            Map<String, Object> customerData = JacksonMapper.ofJson().readValue(json, new TypeReference<>() {});
            output.customerData(customerData);
        }

        return output.build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the created customer.")
        private final String customerId;

        @Schema(title = "The full customer object as a map (only if includeFullCustomerData = true).")
        private final Map<String, Object> customerData;
    }
}
