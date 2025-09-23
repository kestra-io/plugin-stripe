package io.kestra.plugin.stripe.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.param.CustomerListParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import io.kestra.plugin.stripe.AbstractStripe;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List Stripe customers with optional filters and pagination.",
    description = "This task lists Stripe customers using the Stripe Java SDK. Supports optional limit and filters."
)
@Plugin(
    examples = {
        @Example(
            title = "List the first 10 customers",
            full = true,
            code = """
                id: list_customers
                namespace: company.team

                tasks:
                  - id: list_customers
                    type: io.kestra.plugin.stripe.customer.ListCustomers
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    limit: 10
                """
        )
    }
)
public class ListCustomers extends AbstractStripe implements RunnableTask<ListCustomers.Output> {

    @Schema(title = "Maximum number of customers to return", description = "Defaults to 10")
    @Min(1)
    @Builder.Default
    private Property<Integer> limit = Property.ofValue(10);

    @Schema(title = "Optional filter for email")
    private Property<String> email;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Initialize Stripe SDK
        com.stripe.Stripe.apiKey = runContext.render(this.apiKey)
            .asString()
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));

        Integer renderedLimit = runContext.render(this.limit).as(Integer.class).orElse(10);
        String renderedEmail = runContext.render(this.email).asString().orElse(null);

        CustomerListParams.Builder paramsBuilder = CustomerListParams.builder()
            .setLimit(Long.valueOf(renderedLimit));

        if (renderedEmail != null && !renderedEmail.isEmpty()) {
            paramsBuilder.setEmail(renderedEmail);
        }

        CustomerCollection customers;
        try {
            customers = Customer.list(paramsBuilder.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to list Stripe customers: " + e.getMessage(), e);
        }

        List<Map<String, Object>> customerList = customers.getData().stream()
            .map(Customer::toMap)
            .collect(Collectors.toList());

        return Output.builder()
            .customers(customerList)
            .totalCount(customerList.size())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "List of customer objects")
        @PluginProperty(additionalProperties = Map.class)
        private final List<Map<String, Object>> customers;

        @Schema(title = "Number of customers returned")
        private final Integer totalCount;
    }
}
