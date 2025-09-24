package io.kestra.plugin.stripe.balance;

import com.stripe.Stripe;
import com.stripe.model.Balance;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Retrieve your Stripe account balance",
    description = "This task retrieves the current balance of your Stripe account."
)
@Plugin(
    examples = {
        @Example(
            title = "Get current balance",
            full = true,
            code = """
                id: get_balance
                namespace: company.team

                tasks:
                  - id: get_balance
                    type: io.kestra.plugin.stripe.balance.RetrieveBalance
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                """
        )
    }
)
public class RetrieveBalance extends AbstractStripe implements RunnableTask<RetrieveBalance.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        String apiKey = renderApiKey(runContext);

        // Set API key
        Stripe.apiKey = apiKey;

        Balance balance = Balance.retrieve();

        return Output.builder()
            .available(balance.getAvailable())
            .pending(balance.getPending())
            .raw(balance.toJson())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "List of available balances in different currencies.")
        private final java.util.List<com.stripe.model.Balance.Money> available;

        @Schema(title = "List of pending balances in different currencies.")
        private final java.util.List<com.stripe.model.Balance.Money> pending;

        @Schema(title = "The raw JSON response from Stripe.")
        private final String raw;
    }
}
