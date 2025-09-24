package io.kestra.plugin.stripe.payment;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.tasks.PluginTask;
import io.kestra.core.tasks.runners.RunContext;

import java.util.List;
import java.util.Map;

@Plugin(
    description = "Retrieve a list of PaymentIntents with optional filtering."
)
@Example(
    title = "List recent Payment Intents",
    code = {
        "apiKey: sk_test_***",
        "limit: 5"
    }
)
public class ListPaymentIntents extends PluginTask<ListPaymentIntents.Output> {

    @PluginProperty(dynamic = true)
    private String apiKey;

    @PluginProperty(dynamic = true)
    private Long limit;

    @PluginProperty(dynamic = true)
    private String customer;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String key = runContext.render(this.apiKey);

        Stripe.apiKey = key;

        PaymentIntentListParams.Builder paramsBuilder = PaymentIntentListParams.builder();

        if (limit != null) {
            paramsBuilder.setLimit(limit);
        }

        if (customer != null) {
            paramsBuilder.setCustomer(runContext.render(customer));
        }

        List<PaymentIntent> paymentIntents = PaymentIntent.list(paramsBuilder.build()).getData();

        return Output.builder()
            .paymentIntents(paymentIntents)
            .build();
    }

    @lombok.Value
    @lombok.Builder
    public static class Output implements io.kestra.core.models.tasks.Output {
        List<PaymentIntent> paymentIntents;
    }
}
