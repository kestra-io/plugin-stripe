package io.kestra.plugin.stripe.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

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
                    type: io.kestra.plugin.stripe.customer.Update
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
public class Update extends AbstractStripe implements RunnableTask<Update.Output> {

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
        // Resolve customer ID
        String rCustomerId = runContext.render(this.customerId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("customerId is required"));

        // Resolve optional fields
        String rName = this.name != null ? runContext.render(this.name).as(String.class).orElse(null) : null;
        String rEmail = this.email != null ? runContext.render(this.email).as(String.class).orElse(null) : null;
        Map<String, Object> rMetadata = this.metadata != null
            ? runContext.render(this.metadata).asMap(String.class, Object.class)
            : new HashMap<>();

        // Convert metadata to Map<String,String> for Stripe
        Map<String, String> metadataForStripe = rMetadata.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() == null ? "" : e.getValue().toString()
            ));

        // Build update params
        CustomerUpdateParams.Builder builder = CustomerUpdateParams.builder();
        if (rName != null) builder.setName(rName);
        if (rEmail != null) builder.setEmail(rEmail);
        if (!metadataForStripe.isEmpty()) builder.putAllMetadata(metadataForStripe);

        CustomerUpdateParams params = builder.build();

        // Update customer using AbstractStripe client
        Customer customer;
        try {
            customer = client(runContext).customers().update(rCustomerId, params);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to update Stripe customer: " + e.getMessage(), e);
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
        @Schema(title = "The ID of the updated customer.")
        private final String customerId;

        @Schema(title = "The full customer object as a map.")
        @PluginProperty
        private final Map<String, Object> customerData;
    }
}
