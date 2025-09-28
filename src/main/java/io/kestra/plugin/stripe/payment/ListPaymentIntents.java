package io.kestra.plugin.stripe.payment;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Plugin(
    examples = {
        @Example(
            title = "List recent Payment Intents",
            code = {
                "apiKey: sk_test_***",
                "limit: 5"
            }
        )
    }
)
@Getter
public class ListPaymentIntents implements RunnableTask<ListPaymentIntents.Output> {

    @PluginProperty(dynamic = true)
    private String apiKey;

    @PluginProperty(dynamic = true)
    private Long limit;

    @PluginProperty(dynamic = true)
    private String customer;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Render dynamic variables
        String key = runContext.render(this.apiKey);
        Stripe.apiKey = key;

        // Build parameters
        PaymentIntentListParams.Builder paramsBuilder = PaymentIntentListParams.builder();
        if (limit != null) {
            paramsBuilder.setLimit(limit);
        }
        if (customer != null) {
            paramsBuilder.setCustomer(runContext.render(customer));
        }

        // Fetch PaymentIntents
        List<PaymentIntent> paymentIntents = PaymentIntent.list(paramsBuilder.build()).getData();

        // Return output using Lombok builder
        return Output.builder()
            .paymentIntents(paymentIntents)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        private final List<PaymentIntent> paymentIntents;
    }
}
