package io.kestra.plugin.stripe.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.serializers.JacksonMapper;
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
    title = "Fetch Stripe customer by ID",
    description = "Retrieves a customer object by ID using the provided secret key and returns the raw Stripe payload."
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
                    type: io.kestra.plugin.stripe.customer.Get
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    customerId: "cus_123456789"
                """
        )
    }
)
public class Get extends AbstractStripe implements RunnableTask<Get.Output> {

    @Schema(title = "Customer ID to retrieve", required = true)
    @NotNull
    private Property<String> customerId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve customer ID
        String rCustomerId = runContext.render(this.customerId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("customerId is required"));

        Customer customer;
        try {
            customer = client(runContext).customers().retrieve(rCustomerId);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve Stripe customer: " + e.getMessage(), e);
        }

        // Convert Stripe customer JSON to Map<String,Object>
        String json = customer.getLastResponse().body();
        Map<String, Object> customerData = JacksonMapper.ofJson()
            .readValue(json, new TypeReference<>() {});

        return Output.builder()
            .customerId(customer.getId())
            .customerData(customerData)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Retrieved customer ID")
        private final String customerId;

        @Schema(title = "Raw customer payload", description = "Stripe customer object converted to a map")
        private final Map<String, Object> customerData;
    }
}
