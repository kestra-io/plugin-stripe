package io.kestra.plugin.stripe.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.serializers.JacksonMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.StripeCollection;
import com.stripe.param.CustomerListParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import io.kestra.plugin.stripe.AbstractStripe;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List Stripe customers",
    description = "Lists customers with optional email filter and limit. Uses the provided secret key; outputs raw customer maps for further filtering."
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
                    type: io.kestra.plugin.stripe.customer.List
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    limit: 10
                """
        )
    }
)
public class List extends AbstractStripe implements RunnableTask<List.Output> {

    @Schema(title = "Maximum customers to return", description = "Defaults to 10; Stripe caps apply")
    @Min(1)
    @Builder.Default
    private Property<Integer> limit = Property.ofValue(10);

    @Schema(title = "Email filter", description = "If set, returns customers whose email matches the value")
    private Property<String> email;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Integer rLimit = runContext.render(this.limit).as(Integer.class).orElse(10);
        String rEmail = runContext.render(this.email).as(String.class).orElse(null);

        CustomerListParams.Builder paramsBuilder = CustomerListParams.builder()
            .setLimit(Long.valueOf(rLimit));

        if (rEmail != null && !rEmail.isEmpty()) {
            paramsBuilder.setEmail(rEmail);
        }

        StripeCollection<Customer> customers;
        try {
            // Use the client from AbstractStripe (instead of setting apiKey manually)
            customers = client(runContext).customers().list(paramsBuilder.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to list Stripe customers: " + e.getMessage(), e);
        }

        java.util.List<Map<String, Object>> customerList = customers.getData().stream()
            .map(customer -> {
                try {
                    String json = customer.getLastResponse().body();
                    return JacksonMapper.ofJson().readValue(json, new TypeReference<Map<String, Object>>() {});
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to parse customer JSON: " + ex.getMessage(), ex);
                }
            })
            .toList();

        return Output.builder()
            .customers(customerList)
            .totalCount(customerList.size())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Customer objects", description = "Each entry is the raw Stripe customer converted to a map")
        private final java.util.List<Map<String, Object>> customers;

        @Schema(title = "Returned count")
        private final Integer totalCount;
    }
}
