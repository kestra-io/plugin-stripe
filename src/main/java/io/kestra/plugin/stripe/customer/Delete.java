package io.kestra.plugin.stripe.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.serializers.JacksonMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import io.kestra.plugin.stripe.AbstractStripe;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a Stripe customer",
    description = "Soft-deletes a Stripe customer by ID. Stripe retains the record but marks it deleted; output includes the deleted flag and raw payload."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a customer by ID",
            full = true,
            code = """
                id: delete_customer
                namespace: company.team

                tasks:
                  - id: delete_customer
                    type: io.kestra.plugin.stripe.customer.Delete
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    customerId: "cus_123456789"
                """
        )
    }
)
public class Delete extends AbstractStripe implements RunnableTask<Delete.Output> {

    @Schema(title = "Customer ID to delete", required = true, description = "Target customer identifier; operation is a Stripe soft delete")
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
            customer = client(runContext).customers().delete(rCustomerId);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to delete Stripe customer: " + e.getMessage(), e);
        }

        // Convert Stripe customer JSON to Map<String,Object>
        String json = customer.getLastResponse().body();
        Map<String, Object> customerData = JacksonMapper.ofJson().readValue(json, new TypeReference<>() {});

        return Output.builder()
            .customerId(customer.getId())
            .deleted(customer.getDeleted() != null && customer.getDeleted())
            .customerData(customerData)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deleted customer ID")
        private final String customerId;

        @Schema(title = "Soft delete flag", description = "True when Stripe marks the customer as deleted")
        private final Boolean deleted;

        @Schema(title = "Raw customer payload", description = "Full customer object returned by Stripe after deletion")
        private final Map<String, Object> customerData;
    }
}
