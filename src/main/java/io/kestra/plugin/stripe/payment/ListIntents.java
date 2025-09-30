package io.kestra.plugin.stripe.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.serializers.JacksonMapper;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List recent Payment Intents",
    description = "Retrieve a list of recent payment intents from Stripe with optional filters."
)
@Plugin(
    examples = {
        @Example(
            title = "List the 5 most recent Payment Intents for a customer",
            full = true,
            code = """
                id: list_payment_intents
                namespace: company.team

                tasks:
                  - id: list_payment_intents
                    type: io.kestra.plugin.stripe.payment.ListIntents
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    limit: 5
                    customer: cus_123
                """
        )
    }
)
public class ListIntents extends AbstractStripe implements RunnableTask<ListIntents.Output> {

    @Schema(title = "Maximum number of PaymentIntents to retrieve")
    private Property<Long> limit;

    @Schema(title = "Optional Customer ID to filter PaymentIntents")
    private Property<String> customer;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Build parameters
        PaymentIntentListParams.Builder paramsBuilder = PaymentIntentListParams.builder();

        Long resolvedLimit = runContext.render(this.limit).as(Long.class).orElse(null);
        if (resolvedLimit != null) {
            paramsBuilder.setLimit(resolvedLimit);
        }

        String resolvedCustomer = runContext.render(this.customer).as(String.class).orElse(null);
        if (resolvedCustomer != null && !resolvedCustomer.isEmpty()) {
            paramsBuilder.setCustomer(resolvedCustomer);
        }

        // Use the client from AbstractStripe
        List<PaymentIntent> paymentIntents = client(runContext).paymentIntents()
            .list(paramsBuilder.build())
            .getData();

        // Convert each PaymentIntent to a Map
        List<Map<String, Object>> results = new ArrayList<>();
        for (PaymentIntent pi : paymentIntents) {
            Map<String, Object> piMap = JacksonMapper.ofJson().readValue(
                pi.toJson(),
                new TypeReference<Map<String, Object>>() {}
            );
            results.add(piMap);
        }

        return Output.builder()
            .count(results.size())
            .paymentIntents(results)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Number of payment intents returned")
        private final int count;

        @Schema(title = "List of payment intents as raw JSON maps")
        private final List<Map<String, Object>> paymentIntents;
    }
}