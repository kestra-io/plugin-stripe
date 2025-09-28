package io.kestra.plugin.stripe.balance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.model.Balance;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @PluginProperty(dynamic = true)
    private Property<String> apiKey;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Render API key
        String key = runContext.render(apiKey).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));
        Stripe.apiKey = key;

        Balance balance = Balance.retrieve();

        // Parse raw JSON into Map
        String rawJson = balance.getLastResponse().body();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> rawData = mapper.readValue(rawJson, new TypeReference<>() {});

        // Convert available & pending to list of maps (cast amount to Object)
        List<Map<String, Object>> available = balance.getAvailable().stream()
            .map(money -> Map.<String, Object>of(
                "currency", money.getCurrency(),
                "amount", money.getAmount()
            ))
            .collect(Collectors.toList());

        List<Map<String, Object>> pending = balance.getPending().stream()
            .map(money -> Map.<String, Object>of(
                "currency", money.getCurrency(),
                "amount", money.getAmount()
            ))
            .collect(Collectors.toList());

        return Output.builder()
            .available(available)
            .pending(pending)
            .raw(rawData)
            .build();
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "List of available balances in different currencies.")
        private List<Map<String, Object>> available;

        @Schema(title = "List of pending balances in different currencies.")
        private List<Map<String, Object>> pending;

        @Schema(title = "The raw JSON response from Stripe as a Map.")
        private Map<String, Object> raw;
    }
}