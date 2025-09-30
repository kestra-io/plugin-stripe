package io.kestra.plugin.stripe.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

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

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve parameters
        String rName = this.name != null
            ? runContext.render(this.name).as(String.class).orElse(null)
            : null;

        String rEmail = this.email != null
            ? runContext.render(this.email).as(String.class).orElse(null)
            : null;

        Map<String, Object> rMetadata = this.metadata != null
            ? runContext.render(this.metadata).asMap(String.class, Object.class)
            : new HashMap<>();

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
            // Use the client from AbstractStripe
            customer = client(runContext).customers().create(builder.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe customer: " + e.getMessage(), e);
        }

        // Convert Stripe customer JSON to Map<String,Object> for output
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
        @Schema(title = "The ID of the created customer.")
        private final String customerId;

        @Schema(title = "The full customer object as a map.")
        @PluginProperty
        private final Map<String, Object> customerData;
    }
}