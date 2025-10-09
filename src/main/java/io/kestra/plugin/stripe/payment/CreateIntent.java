package io.kestra.plugin.stripe.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.serializers.JacksonMapper;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a PaymentIntent in Stripe.",
    description = "This task creates a PaymentIntent with amount, currency, and optional customer."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a payment intent for $10 USD linked to a customer",
            full = true,
            code = """
                id: create_payment_intent
                namespace: company.team

                tasks:
                  - id: create_payment_intent
                    type: io.kestra.plugin.stripe.payment.CreateIntent
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    amount: 1000
                    currency: "usd"
                    customer: "{{ outputs.create_customer.customerId }}"
                """
        )
    }
)
public class CreateIntent extends AbstractStripe implements RunnableTask<CreateIntent.Output> {
    @NotNull
    @Schema(
        title = "Amount intended to be collected by this PaymentIntent (in the smallest currency unit)."
    )
    private Property<Long> amount;

    @NotNull
    @Schema(
        title = "Three-letter ISO currency code, in lowercase (e.g. `usd`, `inr`)."
    )
    private Property<String> currency;

    @NotNull
    @Schema(
        title = "ID of an existing customer to associate with this PaymentIntent."
    )
    private Property<String> customer;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve input fields
        Long rAmount = runContext.render(this.amount).as(Long.class)
            .orElseThrow(() -> new IllegalArgumentException("Amount is required"));
        String rCurrency = runContext.render(this.currency).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Currency is required"));
        String rCustomer = runContext.render(this.customer).as(String.class).orElse(null);

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
            .setAmount(rAmount)
            .setCurrency(rCurrency);

        if (rCustomer != null) {
            paramsBuilder.setCustomer(rCustomer);
        }

        // Use the client from AbstractStripe
        PaymentIntent intent = client(runContext).paymentIntents().create(paramsBuilder.build());

        // Convert Stripe object JSON into Map
        Map<String, Object> paymentIntentMap = JacksonMapper.ofJson().readValue(
            intent.toJson(),
            new TypeReference<>() {}
        );

        return Output.builder()
            .paymentIntentId(intent.getId())
            .status(intent.getStatus())
            .rawResponse(paymentIntentMap)
            .build();
    }


    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the created PaymentIntent.")
        private final String paymentIntentId;

        @Schema(title = "The status of the PaymentIntent.")
        private final String status;

        @Schema(title = "The raw PaymentIntent object.")
        private final Map<String, Object> rawResponse;
    }
}