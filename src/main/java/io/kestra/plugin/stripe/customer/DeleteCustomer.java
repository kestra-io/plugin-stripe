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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete or deactivate a customer in Stripe.",
    description = "This task deletes (soft-deletes) a customer in Stripe using the Stripe Java SDK."
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
                    type: io.kestra.plugin.stripe.customer.DeleteCustomer
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    customerId: "cus_123456789"
                """
        )
    }
)
public class DeleteCustomer extends AbstractStripe implements RunnableTask<DeleteCustomer.Output> {

    @Schema(title = "The ID of the customer to delete.", required = true)
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
            customer = customer.delete(); // Deletes (soft-delete) the customer
        } catch (StripeException e) {
            throw new RuntimeException("Failed to delete Stripe customer: " + e.getMessage(), e);
        }

        return Output.builder()
            .customerId(customer.getId())
            .deleted(customer.getDeleted() != null && customer.getDeleted())
            .customerData(customer.toMap())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the deleted customer.")
        private final String customerId;

        @Schema(title = "Whether the customer has been deleted (true if soft-deleted).")
        private final Boolean deleted;

        @Schema(title = "The full customer object as a map.")
        @PluginProperty(additionalProperties = java.util.Map.class)
        private final java.util.Map<String, Object> customerData;
    }
}
