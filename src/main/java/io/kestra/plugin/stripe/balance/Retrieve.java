package io.kestra.plugin.stripe.balance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Balance;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

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
                    type: io.kestra.plugin.stripe.balance.Retrieve
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                """
        )
    }
)
public class Retrieve extends AbstractStripe implements RunnableTask<Retrieve.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        Balance balance = client(runContext).balance().retrieve();

        // Parse raw JSON into Map
        String rawJson = balance.getLastResponse().body();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> rawData = mapper.readValue(rawJson, new TypeReference<>() {});

        List<Map<String, Object>> available = balance.getAvailable().stream()
            .map(money -> Map.<String, Object>of(
                "currency", money.getCurrency(),
                "amount", money.getAmount()
            ))
            .toList();

        List<Map<String, Object>> pending = balance.getPending().stream()
            .map(money -> Map.<String, Object>of(
                "currency", money.getCurrency(),
                "amount", money.getAmount()
            ))
            .toList();

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