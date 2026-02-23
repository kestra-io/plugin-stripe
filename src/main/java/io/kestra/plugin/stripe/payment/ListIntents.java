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
    title = "List Stripe PaymentIntents",
    description = "Lists PaymentIntents with optional limit and customer filter using the secret key context. Returns raw intent payloads for further processing."
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

    @Schema(title = "Maximum PaymentIntents", description = "Optional limit; Stripe defaults apply when unset")
    private Property<Long> limit;

    @Schema(title = "Customer filter", description = "Optional customer ID to filter results")
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
        @Schema(title = "Returned count")
        private final int count;

        @Schema(title = "PaymentIntents payloads", description = "Each entry is a PaymentIntent converted to a map")
        private final List<Map<String, Object>> paymentIntents;
    }
}
